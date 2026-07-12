package com.mdframe.forge.plugin.capability.highrisk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.flow.client.annotation.FlowCallback;
import com.mdframe.forge.flow.client.annotation.FlowEventContext;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityClientMapper;
import com.mdframe.forge.plugin.capability.highrisk.domain.AiCapabilityApproval;
import com.mdframe.forge.plugin.capability.highrisk.domain.AiCapabilityPolicy;
import com.mdframe.forge.plugin.capability.highrisk.mapper.CapabilityApprovalMapper;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionCatalogService;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionPublishedModelPolicy;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionStepValidator;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectDesignVersion;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessActionExecutionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectActionService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessActionExecuteResultVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectActionVO;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HighRiskApprovalCallbackServiceTest {

    private final CapabilityApprovalMapper approvalMapper = mock(CapabilityApprovalMapper.class);
    private final CapabilityPolicyService policyService = mock(CapabilityPolicyService.class);
    private final AiCapabilityClientMapper clientMapper = mock(AiCapabilityClientMapper.class);
    private final IUserLoadService userLoadService = mock(IUserLoadService.class);
    private final SecureActionCatalogService catalogService = mock(SecureActionCatalogService.class);
    private final CapabilitySchemaValidator schemaValidator = mock(CapabilitySchemaValidator.class);
    private final BusinessObjectActionService actionService = mock(BusinessObjectActionService.class);
    private final SecureActionStepValidator stepValidator = mock(SecureActionStepValidator.class);
    private final SecureActionPublishedModelPolicy modelPolicy = mock(SecureActionPublishedModelPolicy.class);
    private final BusinessActionExecutionService executionService = mock(BusinessActionExecutionService.class);
    private final HighRiskApprovalSubmissionService submissionService = mock(HighRiskApprovalSubmissionService.class);
    private final HighRiskBusinessStateService stateService = mock(HighRiskBusinessStateService.class);
    private final HighRiskApprovalCallbackService service = new HighRiskApprovalCallbackService(
            approvalMapper, policyService, clientMapper, userLoadService, catalogService, schemaValidator,
            actionService, stepValidator, modelPolicy, executionService, submissionService,
            stateService, new ObjectMapper());

    private AiCapabilityApproval approval;

    @BeforeEach
    void setUp() {
        approval = approval();
        when(approvalMapper.selectForUpdate(1L, 99L)).thenReturn(approval);
        when(approvalMapper.updateState(any())).thenReturn(1);
        AiCapabilityPolicy policy = new AiCapabilityPolicy();
        policy.setApprovalFlowModelKey("forge_capability_high_risk_approval");
        when(policyService.requireActive(1L, 100L, "1.0.0")).thenReturn(policy);
        when(clientMapper.selectTenantById(1L, 30L)).thenReturn(client());
        when(userLoadService.loadUserByUserId(10L, 1L, 40L)).thenReturn(user());
        when(userLoadService.loadUserByUserId(20L, 1L, 40L)).thenReturn(serviceUser());
        when(catalogService.requireAuthorized("business.order.confirm")).thenReturn(descriptor());
        when(submissionService.decryptAndVerify(approval)).thenReturn(Map.of(
                "recordId", "1001", "idempotencyKey", "order-confirm-1001",
                "arguments", Map.of("status", "CONFIRMED")));
        when(actionService.resolvePublishedAction("purchase", "order", "confirm", 3))
                .thenReturn(published());
        when(modelPolicy.writableFields(any())).thenReturn(Map.of(
                "status", new com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema()));
        when(stateService.snapshot(any(), any())).thenReturn("sha256:business");
        BusinessActionExecuteResultVO result = new BusinessActionExecuteResultVO();
        result.setExecuteStatus("SUCCESS");
        result.setMessage("完成");
        result.setCorrelationId("correlation-1");
        result.setIdempotentHit(false);
        when(executionService.executePublished(any(), eq(3), eq("request-1"))).thenReturn(result);
    }

    @Test
    void shouldExecuteApprovedRequestAtMostOnce() {
        service.onFlowResult(event(FlowCallback.ON_COMPLETED));
        service.onFlowResult(event(FlowCallback.ON_COMPLETED));

        assertThat(approval.getExecuteStatus()).isEqualTo("SUCCESS");
        verify(executionService, times(1)).executePublished(any(), eq(3), eq("request-1"));
        verify(approvalMapper, times(2)).updateState(approval);
    }

    @Test
    void shouldRejectWithoutDecryptingOrExecuting() {
        service.onFlowResult(event(FlowCallback.ON_REJECTED));

        assertThat(approval.getExecuteStatus()).isEqualTo("REJECTED");
        verifyNoInteractions(submissionService, executionService);
    }

    @Test
    void shouldFailClosedWhenClientCredentialVersionChanged() {
        AiCapabilityClient client = client();
        client.setCredentialVersion(3);
        when(clientMapper.selectTenantById(1L, 30L)).thenReturn(client);

        service.onFlowResult(event(FlowCallback.ON_COMPLETED));

        assertThat(approval.getExecuteStatus()).isEqualTo("FAILED");
        assertThat(approval.getErrorCode()).isEqualTo("AUTHORIZATION_REVOKED");
        verifyNoInteractions(executionService);
    }

    @Test
    void shouldFailClosedWhenServiceUserIsDisabled() {
        LoginUser serviceUser = serviceUser();
        serviceUser.setUserStatus(0);
        when(userLoadService.loadUserByUserId(20L, 1L, 40L)).thenReturn(serviceUser);

        service.onFlowResult(event(FlowCallback.ON_COMPLETED));

        assertThat(approval.getExecuteStatus()).isEqualTo("FAILED");
        assertThat(approval.getErrorCode()).isEqualTo("AUTHORIZATION_REVOKED");
        verifyNoInteractions(executionService);
    }

    private FlowEventContext event(String type) {
        return FlowEventContext.builder().event(type).tenantId(1L)
                .businessKey("capability-approval:99").build();
    }

    private AiCapabilityApproval approval() {
        AiCapabilityApproval value = new AiCapabilityApproval();
        value.setId(99L);
        value.setTenantId(1L);
        value.setClientId(30L);
        value.setCredentialVersion(2);
        value.setCapabilityId(100L);
        value.setCapabilityCode("business.order.confirm");
        value.setCapabilityVersion("1.0.0");
        value.setActorUserId(10L);
        value.setServiceUserId(20L);
        value.setActiveOrgId(40L);
        value.setIdempotencyKey("order-confirm-1001");
        value.setRequestId("request-1");
        value.setRequestDigest("sha256:request");
        value.setBusinessStateDigest("sha256:business");
        value.setFlowModelKey("forge_capability_high_risk_approval");
        value.setExecuteStatus("PENDING_APPROVAL");
        value.setExpiresAt(LocalDateTime.now().plusHours(1));
        return value;
    }

    private AiCapabilityClient client() {
        AiCapabilityClient value = new AiCapabilityClient();
        value.setId(30L);
        value.setClientCode("client-a");
        value.setStatus("ENABLED");
        value.setCredentialVersion(2);
        value.setServiceUserId(20L);
        value.setActiveOrgId(40L);
        return value;
    }

    private LoginUser user() {
        LoginUser user = new LoginUser();
        user.setUserId(10L);
        user.setTenantId(1L);
        user.setActiveOrgId(40L);
        user.setUserStatus(1);
        user.setPermissions(Set.of("ai:capability:business-action:invoke", "purchase:order:confirm"));
        return user;
    }

    private LoginUser serviceUser() {
        LoginUser user = new LoginUser();
        user.setUserId(20L);
        user.setTenantId(1L);
        user.setActiveOrgId(40L);
        user.setUserStatus(1);
        return user;
    }

    private SecureActionDescriptor descriptor() {
        ObjectMapper mapper = new ObjectMapper();
        return new SecureActionDescriptor(100L, "business.order.confirm", "确认订单",
                "确认订单", "1.0.0", "BUSINESS_ACTION", "purchase/order/confirm", "3",
                "ACTION", "HIGH", "purchase", "order", "confirm", 3,
                "purchase:order:confirm", Set.of("status"), Set.of("status"),
                mapper.createObjectNode(), mapper.createObjectNode(), mapper.createObjectNode());
    }

    private BusinessObjectActionService.ResolvedPublishedBusinessAction published() {
        AiBusinessObject object = new AiBusinessObject();
        BusinessObjectActionVO action = new BusinessObjectActionVO();
        AiBusinessObjectDesignVersion version = new AiBusinessObjectDesignVersion();
        version.setPublishVersion(3);
        return new BusinessObjectActionService.ResolvedPublishedBusinessAction(object, action, version);
    }
}
