package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodePolicyService;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DynamicDataScopeService")
class DynamicDataScopeServiceTest {

    private final IDataScopeService dataScopeService = mock(IDataScopeService.class);
    private final DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
    private final FlowRelatedRecordQuery relatedRecordQuery = mock(FlowRelatedRecordQuery.class);
    private final DynamicDataScopeService service = new DynamicDataScopeService(
            dataScopeService,
            repository,
            new ObjectMapper(),
            new LowcodePolicyService(),
            relatedRecordQuery);

    @Test
    @DisplayName("FOLLOW_SYSTEM resolves the role override with the business object module code")
    void resolvesObjectModuleOverride() {
        AiCrudConfig config = config("ORDER");
        when(dataScopeService.getCurrentUserDataScope("ai:business:ORDER")).thenReturn(allScope());

        assertNull(service.buildCondition(config, "biz_order", "o"));

        verify(dataScopeService).getCurrentUserDataScope("ai:business:ORDER");
        verify(dataScopeService, never()).getCurrentUserDataScope();
    }

    @Test
    @DisplayName("missing object code safely falls back to the role default data scope")
    void missingObjectCodeFallsBackToDefaultScope() {
        AiCrudConfig config = config(" ");
        when(dataScopeService.getCurrentUserDataScope()).thenReturn(allScope());

        assertNull(service.buildCondition(config, "biz_order", null));

        verify(dataScopeService).getCurrentUserDataScope();
        verify(dataScopeService, never()).getCurrentUserDataScope("ai:business:ORDER");
        verify(relatedRecordQuery, never()).listBusinessIds(any(), any(), anyString());
    }

    @Test
    @DisplayName("SELF read scope ORs flow related record ids")
    void selfReadIncludesRelatedRecords() {
        AiCrudConfig config = config("ORDER");
        when(dataScopeService.getCurrentUserDataScope("ai:business:ORDER")).thenReturn(selfScope());
        when(repository.getTableColumns("biz_order")).thenReturn(Set.of("id", "create_by", "create_dept"));
        when(relatedRecordQuery.listBusinessIds(1L, 9L, "ORDER")).thenReturn(List.of("88"));

        DynamicCrudRepository.SqlCondition condition = service.buildCondition(config, "biz_order", "o");

        assertTrue(condition.sql().contains("o.create_by = :__dsUserId"));
        assertTrue(condition.sql().contains("OR"));
        assertTrue(condition.sql().contains("o.id IN (:__dsRelatedIds)"));
        verify(relatedRecordQuery).listBusinessIds(1L, 9L, "ORDER");
    }

    @Test
    @DisplayName("SELF write scope does not OR flow related record ids")
    void selfWriteExcludesRelatedRecords() {
        AiCrudConfig config = config("ORDER");
        when(dataScopeService.getCurrentUserDataScope("ai:business:ORDER")).thenReturn(selfScope());
        when(repository.getTableColumns("biz_order")).thenReturn(Set.of("id", "create_by", "create_dept"));

        DynamicCrudRepository.SqlCondition condition = service.buildWriteCondition(config, "biz_order", "o");

        assertTrue(condition.sql().contains("o.create_by = :__dsUserId"));
        assertFalse(condition.sql().contains("OR"));
        verify(relatedRecordQuery, never()).listBusinessIds(any(), any(), anyString());
    }

    private AiCrudConfig config(String objectCode) {
        AiCrudConfig config = new AiCrudConfig();
        config.setConfigKey("order_list");
        config.setBuildMode("LOWCODE");
        config.setObjectCode(objectCode);
        config.setModelSchema("{\"policies\":{\"dataScope\":\"FOLLOW_SYSTEM\"}}");
        return config;
    }

    private DataScopeContext allScope() {
        return new DataScopeContext(9L, List.of(2L), 2L, List.of(7L), 1,
                Set.of(), 1L, null, null, null);
    }

    private DataScopeContext selfScope() {
        return new DataScopeContext(9L, List.of(2L), 2L, List.of(7L), 5,
                Set.of(), 1L, null, null, null);
    }
}
