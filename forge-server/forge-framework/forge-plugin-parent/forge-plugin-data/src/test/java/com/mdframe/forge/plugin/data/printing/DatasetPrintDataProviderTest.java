package com.mdframe.forge.plugin.data.printing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.data.dto.DataDatasetQueryDTO;
import com.mdframe.forge.plugin.data.service.DataDatasetRuntimeService;
import com.mdframe.forge.plugin.data.vo.DataDatasetFieldVO;
import com.mdframe.forge.plugin.data.vo.DataDatasetMetadataVO;
import com.mdframe.forge.plugin.data.vo.DataDatasetQueryResultVO;
import com.mdframe.forge.plugin.print.entity.PrintBusinessSource;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.mapper.PrintBusinessSourceMapper;
import com.mdframe.forge.plugin.print.service.PrintParameterValidator;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.plugin.print.spi.PrintRecordRequest;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetPrintDataProviderTest {

    private final PrintBusinessSourceMapper sources = mock(PrintBusinessSourceMapper.class);
    private final DataDatasetRuntimeService datasets = mock(DataDatasetRuntimeService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final DatasetPrintDataProvider provider = new DatasetPrintDataProvider(
            sources, datasets, new PrintParameterValidator(mapper), mapper);

    private final PrintActor actor = new PrintActor(1L, 9L, 2L);
    private final PrintSourceRequest source = new PrintSourceRequest(
            7L, "purchase_order", null, PrintSourceType.DATASET,
            null, null, "purchase_order");

    @BeforeEach
    void setUp() {
        when(sources.selectScoped(1L, 7L)).thenReturn(businessSource());
        when(datasets.metadata(21L)).thenReturn(metadata());
        when(datasets.query(any())).thenReturn(queryResult());
    }

    @Test
    void shouldDelegateAclAndRowScopeQueryWithControlledParameters() {
        var record = new PrintRecordRequest(
                source, "42", PrintScene.DETAIL, null, null, null,
                Map.of("status", "OPEN"));

        var authorized = provider.authorize(actor, record);
        var data = provider.load(authorized, null);

        var query = ArgumentCaptor.forClass(DataDatasetQueryDTO.class);
        verify(datasets, org.mockito.Mockito.atLeastOnce()).query(query.capture());
        assertThat(query.getValue().getParams())
                .containsEntry("id", new BigDecimal("42"))
                .containsEntry("status", "OPEN");
        assertThat(data.main()).containsEntry("orderNo", "PO-42");
        assertThat(data.children()).containsKey("items");
        assertThat(authorized.applicationVersionId()).isNull();
        assertThat(authorized.versions()).isEmpty();
    }

    @Test
    void shouldRejectUnknownParameterBeforeDatasetQuery() {
        var record = new PrintRecordRequest(
                source, "42", PrintScene.DETAIL, null, null, null,
                Map.of("tenantId", 2));

        assertThatThrownBy(() -> provider.authorize(actor, record))
                .isInstanceOf(BusinessException.class);
    }

    private PrintBusinessSource businessSource() {
        var row = new PrintBusinessSource();
        row.setId(7L);
        row.setTenantId(1L);
        row.setSourceCode("purchase_order");
        row.setSourceType(PrintSourceType.DATASET.getCode());
        row.setDatasetId(21L);
        row.setObjectCode("purchase_order");
        row.setParameterSchemaJson("{\"status\":{\"type\":\"string\"}}");
        row.setMappingJson("{\"recordIdParam\":\"id\",\"childrenKey\":\"items\",\"maxRows\":100}");
        row.setSourceRevision(3L);
        row.setStatus(1);
        row.setDelFlag(0L);
        return row;
    }

    private DataDatasetMetadataVO metadata() {
        var metadata = new DataDatasetMetadataVO();
        metadata.setDatasetId(21L);
        metadata.setParamSchemaJson("""
                [
                  {"paramName":"id","dataType":"NUMBER"},
                  {"paramName":"status","dataType":"STRING"}
                ]
                """);
        var field = new DataDatasetFieldVO();
        field.setFieldName("orderNo");
        field.setFieldLabel("单号");
        field.setDataType("STRING");
        field.setDisplayEnabled(1);
        field.setSensitiveLevel("PUBLIC");
        metadata.setFields(List.of(field));
        return metadata;
    }

    private DataDatasetQueryResultVO queryResult() {
        var result = new DataDatasetQueryResultVO();
        result.setSource(List.of(Map.of("orderNo", "PO-42")));
        result.setTotal(1L);
        return result;
    }
}
