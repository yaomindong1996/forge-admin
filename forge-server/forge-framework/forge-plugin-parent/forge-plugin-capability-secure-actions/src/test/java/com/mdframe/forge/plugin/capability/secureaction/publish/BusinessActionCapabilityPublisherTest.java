package com.mdframe.forge.plugin.capability.secureaction.publish;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityPublishDTO;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectDesignVersion;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectActionService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectActionVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessActionCapabilityPublisherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BusinessObjectActionService actionService = mock(BusinessObjectActionService.class);
    private final CapabilityCatalogService catalogService = mock(CapabilityCatalogService.class);
    private final BusinessActionCapabilityPublisher publisher = new BusinessActionCapabilityPublisher(
            actionService, catalogService, new SecureActionStepValidator(),
            new SecureActionPublishedModelPolicy(objectMapper), objectMapper);

    @Test
    void shouldPublishOnlyWhitelistedFieldsFromPublishedSnapshot() throws Exception {
        when(actionService.resolvePublishedAction("purchase", "order", "confirm", null))
                .thenReturn(resolved("UPDATE_FIELD"));
        when(catalogService.publishBusinessAction(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any())).thenReturn(99L);
        BusinessActionCapabilityPublishDTO dto = command();

        Long capabilityId = publisher.publish(1L, dto);

        assertThat(capabilityId).isEqualTo(99L);
        ArgumentCaptor<CapabilityPublishDTO> captor = ArgumentCaptor.forClass(CapabilityPublishDTO.class);
        verify(catalogService).publishBusinessAction(org.mockito.ArgumentMatchers.eq(1L), captor.capture());
        CapabilityPublishDTO published = captor.getValue();
        assertThat(published.sourceType()).isEqualTo("BUSINESS_ACTION");
        assertThat(published.sourceKey()).isEqualTo("purchase/order/confirm");
        assertThat(published.behavior()).isEqualTo("ACTION");
        assertThat(published.riskLevel()).isEqualTo("MEDIUM");
        assertThat(published.inputSchema().path("$schema").asText())
                .isEqualTo("https://json-schema.org/draft/2020-12/schema");
        assertThat(published.outputSchema().path("$schema").asText())
                .isEqualTo("https://json-schema.org/draft/2020-12/schema");
        assertThat(published.inputSchema().path("properties").path("arguments")
                .path("additionalProperties").asBoolean()).isFalse();
        assertThat(published.inputSchema().path("properties").path("arguments")
                .path("properties").has("status")).isTrue();
        assertThat(published.policySnapshot().path("confirmationMode").asText())
                .isEqualTo("MCP_ELICITATION");
    }

    @Test
    void shouldRejectFlowOrMessageSteps() throws Exception {
        when(actionService.resolvePublishedAction("purchase", "order", "confirm", null))
                .thenReturn(resolved("START_FLOW"));

        assertThatThrownBy(() -> publisher.publish(1L, command()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("禁止发布动作步骤");
    }

    @Test
    void shouldRejectUnsafeNestedSteps() throws Exception {
        var resolved = resolved("UPDATE_FIELD");
        resolved.action().setActionConfig(Map.of("steps", List.of(Map.of(
                "stepType", "UPDATE_FIELD",
                "stepConfig", Map.of("childSteps", List.of(Map.of("stepType", "START_FLOW")))))));
        when(actionService.resolvePublishedAction("purchase", "order", "confirm", null))
                .thenReturn(resolved);

        assertThatThrownBy(() -> publisher.publish(1L, command()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("禁止嵌套");
    }

    @Test
    void shouldRejectNestedStepsEvenWhenNestedTypeIsOtherwiseAllowed() throws Exception {
        var resolved = resolved("UPDATE_FIELD");
        resolved.action().setActionConfig(Map.of("steps", List.of(Map.of(
                "stepType", "UPDATE_FIELD",
                "stepConfig", Map.of("childSteps", List.of(Map.of("stepType", "CREATE_RECORD")))))));
        when(actionService.resolvePublishedAction("purchase", "order", "confirm", null))
                .thenReturn(resolved);

        assertThatThrownBy(() -> publisher.publish(1L, command()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("禁止嵌套");
    }

    @Test
    void shouldValidateLegacyTopLevelStepList() throws Exception {
        var resolved = resolved("UPDATE_FIELD");
        resolved.action().setActionConfig(Map.of("stepList", List.of(Map.of("stepType", "START_FLOW"))));
        when(actionService.resolvePublishedAction("purchase", "order", "confirm", null))
                .thenReturn(resolved);

        assertThatThrownBy(() -> publisher.publish(1L, command()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("禁止发布动作步骤");
    }

    @Test
    void shouldRejectReadonlyOrUnknownFields() throws Exception {
        when(actionService.resolvePublishedAction("purchase", "order", "confirm", null))
                .thenReturn(resolved("UPDATE_FIELD"));
        BusinessActionCapabilityPublishDTO dto = command();
        dto.setAllowedFields(Set.of("secret"));

        assertThatThrownBy(() -> publisher.publish(1L, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在或不可写");
    }

    private BusinessActionCapabilityPublishDTO command() {
        BusinessActionCapabilityPublishDTO dto = new BusinessActionCapabilityPublishDTO();
        dto.setSuiteCode("purchase");
        dto.setObjectCode("order");
        dto.setActionCode("confirm");
        dto.setCapabilityCode("business.order.confirm");
        dto.setAllowedFields(Set.of("status"));
        dto.setRequiredFields(Set.of("status"));
        return dto;
    }

    private BusinessObjectActionService.ResolvedPublishedBusinessAction resolved(String stepType) throws Exception {
        AiBusinessObject object = new AiBusinessObject();
        object.setId(10L);
        object.setTenantId(1L);
        object.setSuiteCode("purchase");
        object.setObjectCode("order");
        object.setObjectName("采购订单");
        object.setStatus(1);
        BusinessObjectActionVO action = new BusinessObjectActionVO();
        action.setActionCode("confirm");
        action.setActionName("确认采购订单");
        action.setPermission("purchase:order:confirm");
        action.setStatus(1);
        action.setActionConfig(Map.of("steps", List.of(Map.of(
                "stepCode", "update", "stepType", stepType))));
        LowcodeFieldSchema status = new LowcodeFieldSchema();
        status.setField("status");
        status.setLabel("状态");
        status.setDataType("string");
        LowcodeFieldSchema secret = new LowcodeFieldSchema();
        secret.setField("secret");
        secret.setReadonly(true);
        LowcodeModelSchema model = new LowcodeModelSchema();
        model.setFields(List.of(status, secret));
        AiBusinessObjectDesignVersion version = new AiBusinessObjectDesignVersion();
        version.setPublishVersion(3);
        version.setModelSnapshot(objectMapper.writeValueAsString(model));
        return new BusinessObjectActionService.ResolvedPublishedBusinessAction(object, action, version);
    }
}
