package com.mdframe.forge.business.core.purchase.printing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.business.core.purchase.service.SamplePurchaseOrderService;
import com.mdframe.forge.business.core.purchase.support.SamplePurchaseOrderFlowDefinition;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationVersionMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationRuntimeService;
import com.mdframe.forge.plugin.generator.service.printing.FlowPrintAccessPolicy;
import com.mdframe.forge.plugin.generator.service.printing.FlowPrintContextResolver;
import com.mdframe.forge.plugin.generator.service.printing.FlowPrintHistoryAdapter;
import com.mdframe.forge.plugin.generator.service.printing.LowcodePrintResourceAccess;
import com.mdframe.forge.plugin.generator.service.printing.PrintApplicationAccessAdapter;
import com.mdframe.forge.plugin.generator.service.printing.PrintApplicationSnapshotCodec;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateVersionMapper;
import com.mdframe.forge.plugin.print.service.PrintDocumentAccess;
import com.mdframe.forge.plugin.print.service.PrintIdentity;
import com.mdframe.forge.plugin.print.spi.AuthorizedPrintSource;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SamplePurchaseOrderPrintDataProviderTest {

    private static final PrintActor ACTOR = new PrintActor(1L, 7L, 3L);
    private static final PrintSourceRequest SOURCE = new PrintSourceRequest(
            2L, PrintSourceType.CODE, null, SamplePurchaseOrderFlowDefinition.FORM_KEY,
            SamplePurchaseOrderFlowDefinition.BUSINESS_TYPE);

    private SamplePurchaseOrderPrintDataProvider provider;

    @BeforeEach
    void setup() {
        PrintIdentity identity = mock(PrintIdentity.class);
        FlowPrintHistoryAdapter history = mock(FlowPrintHistoryAdapter.class);
        when(identity.current()).thenReturn(ACTOR);
        when(history.catalog()).thenReturn(List.of(
                new PrintFieldCatalogVO.Field("flow.history", "审批记录", "COLLECTION")));
        provider = new SamplePurchaseOrderPrintDataProvider(
                identity,
                mock(PrintApplicationAccessAdapter.class),
                mock(BusinessApplicationRuntimeService.class),
                mock(BusinessApplicationVersionMapper.class),
                mock(PrintApplicationSnapshotCodec.class),
                mock(PrintTemplateVersionMapper.class),
                mock(SamplePurchaseOrderService.class),
                mock(FlowPrintContextResolver.class),
                mock(FlowPrintAccessPolicy.class),
                history,
                mock(LowcodePrintResourceAccess.class),
                new PrintDocumentAccess(new ObjectMapper()));
    }

    @Test
    void exposesBusinessFieldsAndFlowCatalogWithoutInternalOrFileIds() {
        PrintFieldCatalogVO catalog = provider.catalog(new AuthorizedPrintSource(ACTOR, SOURCE));

        assertThat(catalog.fields()).extracting(PrintFieldCatalogVO.Field::path)
                .contains("main.orderNo", "main.title", "main.amountCent", "flow.history")
                .doesNotContain("main.id", "main.processInstanceId", "main.arrivalListFileIds");
        assertThat(catalog.fields()).filteredOn(field -> "main.amountCent".equals(field.path()))
                .extracting(PrintFieldCatalogVO.Field::type).containsExactly("MONEY");
    }

    @Test
    void onlyClaimsItsExactCodeFormSource() {
        assertThat(provider.supports(SOURCE)).isTrue();
        assertThat(provider.supports(new PrintSourceRequest(2L, PrintSourceType.CODE, null,
                "other-form", SamplePurchaseOrderFlowDefinition.BUSINESS_TYPE))).isFalse();
        assertThat(provider.supports(new PrintSourceRequest(2L, PrintSourceType.LOWCODE, "page", null,
                SamplePurchaseOrderFlowDefinition.BUSINESS_TYPE))).isFalse();
    }
}
