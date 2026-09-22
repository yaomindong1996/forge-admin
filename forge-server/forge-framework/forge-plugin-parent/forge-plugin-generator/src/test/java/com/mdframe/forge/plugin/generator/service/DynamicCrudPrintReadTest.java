package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.plugin.generator.service.formula.VirtualFormulaRuntime;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.*;
import com.mdframe.forge.starter.crypto.desensitize.strategy.*;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynamicCrudPrintReadTest {
    final DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
    final DynamicDataScopeService scope = mock(DynamicDataScopeService.class);
    final LowcodeRuntimeDataSourceResolver resolver = mock(LowcodeRuntimeDataSourceResolver.class);
    final DesensitizeStrategyFactory masks = mock(DesensitizeStrategyFactory.class);
    final DictValueProvider dictionaries = mock(DictValueProvider.class);
    final PersistentCryptoService crypto = mock(PersistentCryptoService.class);
    final VirtualFormulaRuntime formulas = mock(VirtualFormulaRuntime.class);
    final AiCrudConfig config = new AiCrudConfig();
    final DynamicCrudRepository.SqlCondition condition = new DynamicCrudRepository.SqlCondition("create_by = :owner", Map.of("owner", 9L));
    DynamicCrudService service;
    @BeforeEach void setup() {
        var json = new ObjectMapper();
        service = new DynamicCrudService(repository, null, json, dictionaries, masks, crypto,
                new LowcodeEncryptConfigParser(json), scope, null, null, null, null, formulas, resolver, null);
        config.setConfigKey("synthetic"); config.setObjectCode("synthetic"); config.setTenantId(1L); config.setTableName("test_record");
        config.setModelSchema("""
            {"fields":[{"field":"amount","columnName":"amount","dataType":"bigint","businessFieldType":"MONEY"},
             {"field":"phone","columnName":"phone","dataType":"varchar"}]}
            """);
        config.setDesensitizeConfig("{\"phone\":{\"type\":\"PHONE\"}}");
        config.setEncryptConfig("{\"phone\":{\"algorithm\":\"SM4\"}}");
        config.setTransConfig("{\"status\":{\"dictType\":\"synthetic_status\"}}");
        when(resolver.resolve(config)).thenReturn(LowcodeRuntimeDataSourceContext.master("test_record"));
        when(scope.buildCondition(config, "test_record", null)).thenReturn(condition);
        when(crypto.decrypt("synthetic-cipher", "SM4")).thenReturn("synthetic-plain");
        when(masks.getStrategy(DesensitizeType.PHONE)).thenReturn(value -> "masked-value");
        when(dictionaries.getLabel("synthetic_status", "A")).thenReturn("已确认");
        doAnswer(call -> {
            List<Map<String, Object>> rows = call.getArgument(0);
            rows.get(0).put("computedValue", "calculated"); return null;
        }).when(formulas).calculateForPrint(anyList(), any(), any());
    }
    Map<String, Object> raw() {
        return new LinkedHashMap<>(Map.of("id", 7L, "amount", 1230L, "phone", "synthetic-cipher", "status", "A"));
    }
    @Test void mainAndChildrenShareRecordScopeAndStrictReadPipeline() {
        when(repository.selectById("test_record", "id", "7", condition)).thenReturn(raw());
        when(repository.selectTreeChildren("test_record", "parent_id", 7L, "id ASC", 501, condition)).thenReturn(List.of(raw()));
        var main = service.selectPrintById(config, "7");
        var children = service.selectPrintChildren(config, "parent_id", 7L);
        for (var row : List.of(main.values(), children.get(0))) {
            assertThat(row).containsEntry("phone", "masked-value").containsEntry("statusName", "已确认")
                    .containsEntry("computedValue", "calculated");
            assertThat(row.get("amount").toString()).isEqualTo("12.30");
        }
        assertThat(main.columns().get("phone")).isEqualTo("synthetic-cipher");
        verify(scope, times(2)).buildCondition(config, "test_record", null);
        verify(repository, never()).selectListByColumn(anyString(), anyString(), any());
        assertThat(LowcodeRuntimeDataSourceContextHolder.get()).isNull();
    }
    @Test void noRecordDoesNotLoadChildrenAndEmptyRelationNeverBecomesTreeRootQuery() {
        when(repository.selectById("test_record", "id", "denied", condition)).thenReturn(null);
        assertThat(service.selectPrintById(config, "denied")).isNull();
        assertThat(service.selectPrintChildren(config, "parent_id", null)).isEmpty();
        assertThat(service.selectPrintChildren(config, "parent_id", "")).isEmpty();
        verify(repository, never()).selectTreeChildren(anyString(), anyString(), any(), anyString(), anyInt(), any());
    }
    @Test void tooManyChildrenAreRejectedBeforeValueProcessing() {
        when(repository.selectTreeChildren("test_record", "parent_id", 7L, "id ASC", 501, condition))
                .thenReturn(Collections.nCopies(501, raw()));
        assertThatThrownBy(() -> service.selectPrintChildren(config, "parent_id", 7L)).hasMessageContaining("500");
        verifyNoInteractions(crypto);
    }
    @Test void maskFailureCannotReturnPlaintextOrSkipChildSecurity() {
        when(repository.selectTreeChildren("test_record", "parent_id", 7L, "id ASC", 501, condition)).thenReturn(List.of(raw()));
        when(masks.getStrategy(DesensitizeType.PHONE)).thenReturn(value -> { throw new IllegalStateException("synthetic"); });
        assertThatThrownBy(() -> service.selectPrintChildren(config, "parent_id", 7L)).hasMessageContaining("脱敏处理失败");
        when(masks.getStrategy(DesensitizeType.PHONE)).thenReturn(null);
        assertThatThrownBy(() -> service.selectPrintChildren(config, "parent_id", 7L)).hasMessageContaining("脱敏处理失败");
    }
    @Test void dictionaryFailureStopsPrintInsteadOfReturningPartialTranslation() {
        when(repository.selectById("test_record", "id", "7", condition)).thenReturn(raw());
        when(dictionaries.getLabel("synthetic_status", "A")).thenThrow(new IllegalStateException("synthetic"));
        assertThatThrownBy(() -> service.selectPrintById(config, "7")).hasMessageContaining("翻译失败");
    }
}
