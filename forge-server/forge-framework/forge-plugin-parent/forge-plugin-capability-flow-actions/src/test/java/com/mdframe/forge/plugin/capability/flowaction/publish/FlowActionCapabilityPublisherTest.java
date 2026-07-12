package com.mdframe.forge.plugin.capability.flowaction.publish;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityPublishDTO;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
import com.mdframe.forge.plugin.capability.flowaction.source.FlowActionSourceRow;
import com.mdframe.forge.plugin.capability.flowaction.source.FlowActionSourceService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlowActionCapabilityPublisherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FlowActionSourceService sourceService = mock(FlowActionSourceService.class);
    private final CapabilityCatalogService catalogService = mock(CapabilityCatalogService.class);
    private final FlowActionCapabilityPublisher publisher = new FlowActionCapabilityPublisher(
            sourceService, catalogService, objectMapper);

    @Test
    void shouldPublishStartFromPublishedBindingSnapshot() {
        when(sourceService.requirePublished(1L, "purchase", "order")).thenReturn(source("order_config"));
        when(catalogService.publishFlowAction(eq(1L), any())).thenReturn(9L);
        FlowActionCapabilityPublishDTO dto = dto("START");

        Long capabilityId = publisher.publish(1L, dto);

        ArgumentCaptor<CapabilityPublishDTO> captor = ArgumentCaptor.forClass(CapabilityPublishDTO.class);
        verify(catalogService).publishFlowAction(eq(1L), captor.capture());
        CapabilityPublishDTO command = captor.getValue();
        assertThat(capabilityId).isEqualTo(9L);
        assertThat(command.sourceType()).isEqualTo("FLOW_ACTION");
        assertThat(command.behavior()).isEqualTo("FLOW");
        assertThat(command.sourceKey()).isEqualTo("purchase/order/START");
        assertThat(command.policySnapshot().path("bindingId").asLong()).isEqualTo(71L);
        assertThat(command.policySnapshot().path("flowModelKey").asText()).isEqualTo("order_approval");
        assertThat(command.inputSchema().path("properties").path("arguments")
                .path("additionalProperties").asBoolean()).isFalse();
    }

    @Test
    void shouldRequireRejectCommentInPublishedSchema() {
        when(sourceService.requirePublished(1L, "purchase", "order")).thenReturn(source("order_config"));
        when(catalogService.publishFlowAction(eq(1L), any())).thenReturn(9L);

        publisher.publish(1L, dto("reject"));

        ArgumentCaptor<CapabilityPublishDTO> captor = ArgumentCaptor.forClass(CapabilityPublishDTO.class);
        verify(catalogService).publishFlowAction(eq(1L), captor.capture());
        assertThat(captor.getValue().inputSchema().path("properties").path("arguments")
                .path("required").toString()).contains("taskId", "comment");
    }

    @Test
    void shouldRejectGenericStartForCodeOnlyObject() {
        when(sourceService.requirePublished(1L, "purchase", "order")).thenReturn(source(null));

        assertThatThrownBy(() -> publisher.publish(1L, dto("START")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("平台托管");
    }

    private FlowActionCapabilityPublishDTO dto(String operation) {
        FlowActionCapabilityPublishDTO dto = new FlowActionCapabilityPublishDTO();
        dto.setCapabilityCode("purchase.order.flow." + operation.toLowerCase());
        dto.setVersion("1.0.0");
        dto.setSuiteCode("purchase");
        dto.setObjectCode("order");
        dto.setOperation(operation);
        return dto;
    }

    private FlowActionSourceService.ResolvedFlowActionSource source(String configKey) {
        FlowActionSourceRow row = new FlowActionSourceRow();
        row.setObjectId(11L);
        row.setSuiteCode("purchase");
        row.setObjectCode("order");
        row.setObjectName("采购单");
        row.setConfigKey(configKey);
        row.setPublishedObjectVersion(3);
        row.setBindingId(71L);
        row.setBindingKey("order_approval");
        return new FlowActionSourceService.ResolvedFlowActionSource(row, "order_approval");
    }
}
