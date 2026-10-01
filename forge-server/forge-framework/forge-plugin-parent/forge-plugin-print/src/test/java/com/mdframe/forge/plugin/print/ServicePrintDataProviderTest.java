package com.mdframe.forge.plugin.print;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.print.entity.PrintBusinessSource;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.mapper.PrintBusinessSourceMapper;
import com.mdframe.forge.plugin.print.service.PrintParameterValidator;
import com.mdframe.forge.plugin.print.service.ServicePrintDataProvider;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.plugin.print.spi.PrintBusinessDataProvider;
import com.mdframe.forge.plugin.print.spi.PrintRecordRequest;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicePrintDataProviderTest {

    @Test
    void shouldDispatchOnlyThroughServerRegisteredProviderCode() {
        var sources = mock(PrintBusinessSourceMapper.class);
        var businessProvider = mock(PrintBusinessDataProvider.class);
        var actor = new PrintActor(1L, 9L, 2L);
        var source = new PrintSourceRequest(
                7L, "purchase_order", null, PrintSourceType.SERVICE,
                null, null, "purchase_order");
        var record = new PrintRecordRequest(
                source, "42", PrintScene.DETAIL, null, null, null,
                Map.of("locale", "zh-CN"));
        var row = businessSource();
        var catalog = new PrintFieldCatalogVO(List.of());
        when(sources.selectScoped(1L, 7L)).thenReturn(row);
        when(businessProvider.code()).thenReturn("purchase-service");
        when(businessProvider.catalog(actor, row)).thenReturn(catalog);
        var provider = new ServicePrintDataProvider(
                sources, List.of(businessProvider),
                new PrintParameterValidator(new ObjectMapper()));

        var context = provider.authorize(actor, record);

        assertThat(context.catalog()).isSameAs(catalog);
        verify(businessProvider).authorizeRecord(
                actor, row, record, Map.of("locale", "zh-CN"));
    }

    private PrintBusinessSource businessSource() {
        var row = new PrintBusinessSource();
        row.setId(7L);
        row.setTenantId(1L);
        row.setSourceCode("purchase_order");
        row.setSourceType(PrintSourceType.SERVICE.getCode());
        row.setProviderCode("purchase-service");
        row.setObjectCode("purchase_order");
        row.setParameterSchemaJson(
                "{\"locale\":{\"type\":\"string\",\"enum\":[\"zh-CN\",\"en-US\"]}}");
        row.setSourceRevision(2L);
        row.setStatus(1);
        row.setDelFlag(0L);
        return row;
    }
}
