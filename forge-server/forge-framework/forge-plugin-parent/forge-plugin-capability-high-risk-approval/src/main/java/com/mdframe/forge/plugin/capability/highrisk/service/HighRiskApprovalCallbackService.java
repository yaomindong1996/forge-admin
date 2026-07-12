package com.mdframe.forge.plugin.capability.highrisk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.flow.client.annotation.FlowBind;
import com.mdframe.forge.flow.client.annotation.FlowCallback;
import com.mdframe.forge.flow.client.annotation.FlowEventContext;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityClientMapper;
import com.mdframe.forge.plugin.capability.highrisk.domain.AiCapabilityApproval;
import com.mdframe.forge.plugin.capability.highrisk.domain.AiCapabilityPolicy;
import com.mdframe.forge.plugin.capability.highrisk.mapper.CapabilityApprovalMapper;
import com.mdframe.forge.plugin.capability.highrisk.support.HighRiskApprovalFlowDefinition;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionCatalogService;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionPublishedModelPolicy;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionStepValidator;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessActionExecuteDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessActionExecutionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectActionService;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@FlowBind(modelKey = "forge_capability_high_risk_approval", businessType = "capability-approval")
public class HighRiskApprovalCallbackService {
    private final CapabilityApprovalMapper approvalMapper;
    private final CapabilityPolicyService policyService;
    private final AiCapabilityClientMapper clientMapper;
    private final IUserLoadService userLoadService;
    private final SecureActionCatalogService catalogService;
    private final CapabilitySchemaValidator schemaValidator;
    private final BusinessObjectActionService actionService;
    private final SecureActionStepValidator stepValidator;
    private final SecureActionPublishedModelPolicy modelPolicy;
    private final BusinessActionExecutionService executionService;
    private final HighRiskApprovalSubmissionService submissionService;
    private final HighRiskBusinessStateService stateService;
    private final ObjectMapper objectMapper;

    @FlowCallback(on = {
            FlowCallback.ON_COMPLETED,
            FlowCallback.ON_REJECTED,
            FlowCallback.ON_CANCELED
    })
    @Transactional(rollbackFor = Exception.class)
    public void onFlowResult(FlowEventContext context) {
        Long approvalId = approvalId(context == null ? null : context.getBusinessKey());
        Long tenantId = context == null ? null : context.getTenantId();
        if (approvalId == null || tenantId == null) {
            return;
        }
        AiCapabilityApproval approval = approvalMapper.selectForUpdate(tenantId, approvalId);
        if (approval == null || terminal(approval.getExecuteStatus())) {
            return;
        }
        if (FlowCallback.ON_REJECTED.equals(context.getEvent())) {
            finish(approval, "REJECTED", "REJECTED", null);
            return;
        }
        if (FlowCallback.ON_CANCELED.equals(context.getEvent())) {
            finish(approval, "CANCELLED", "CANCELLED", null);
            return;
        }
        if (approval.getExpiresAt().isBefore(LocalDateTime.now())) {
            finish(approval, "EXPIRED", "EXPIRED", "APPROVAL_EXPIRED");
            return;
        }
        executeApproved(approval);
    }

    private void executeApproved(AiCapabilityApproval approval) {
        AiCapabilityPolicy policy;
        try {
            policy = policyService.requireActive(
                    approval.getTenantId(), approval.getCapabilityId(), approval.getCapabilityVersion());
        }
        catch (RuntimeException exception) {
            finish(approval, "FAILED", "FAILED", "AUTHORIZATION_REVOKED");
            return;
        }
        if (!HighRiskApprovalFlowDefinition.MODEL_KEY.equals(policy.getApprovalFlowModelKey())
                || !approval.getFlowModelKey().equals(policy.getApprovalFlowModelKey())) {
            finish(approval, "FAILED", "FAILED", "POLICY_MISMATCH");
            return;
        }
        AiCapabilityClient client = clientMapper.selectTenantById(
                approval.getTenantId(), approval.getClientId());
        if (client == null || !"ENABLED".equals(client.getStatus())
                || (client.getExpiresAt() != null && !client.getExpiresAt().isAfter(LocalDateTime.now()))
                || !approval.getCredentialVersion().equals(client.getCredentialVersion())
                || !approval.getServiceUserId().equals(client.getServiceUserId())
                || !approval.getActiveOrgId().equals(client.getActiveOrgId())) {
            finish(approval, "FAILED", "FAILED", "AUTHORIZATION_REVOKED");
            return;
        }
        LoginUser user = userLoadService.loadUserByUserId(
                approval.getActorUserId(), approval.getTenantId(), approval.getActiveOrgId());
        if (user == null || !Integer.valueOf(1).equals(user.getUserStatus())
                || !approval.getTenantId().equals(user.getTenantId())
                || !approval.getActiveOrgId().equals(user.getActiveOrgId())) {
            finish(approval, "FAILED", "FAILED", "AUTHORIZATION_REVOKED");
            return;
        }
        LoginUser serviceUser = userLoadService.loadUserByUserId(
                approval.getServiceUserId(), approval.getTenantId(), approval.getActiveOrgId());
        if (serviceUser == null || !Integer.valueOf(1).equals(serviceUser.getUserStatus())
                || !approval.getTenantId().equals(serviceUser.getTenantId())
                || !approval.getActiveOrgId().equals(serviceUser.getActiveOrgId())) {
            finish(approval, "FAILED", "FAILED", "AUTHORIZATION_REVOKED");
            return;
        }
        ExecutionIdentity identity = new ExecutionIdentity(user, "USER", approval.getActorUserId(),
                approval.getServiceUserId(), approval.getClientId(), client.getClientCode(),
                "approval:" + approval.getId(), Set.of("capability:invoke:" + approval.getCapabilityCode()));
        try (var ignored = ExecutionIdentityContextHolder.open(identity)) {
            SecureActionDescriptor descriptor = catalogService.requireAuthorized(approval.getCapabilityCode());
            if (!approval.getCapabilityId().equals(descriptor.capabilityId())
                    || !approval.getCapabilityVersion().equals(descriptor.version())
                    || !"HIGH".equals(descriptor.riskLevel())) {
                finish(approval, "FAILED", "FAILED", "AUTHORIZATION_REVOKED");
                return;
            }
            Map<String, Object> input = submissionService.decryptAndVerify(approval);
            schemaValidator.validateInstance(descriptor.inputSchema(), objectMapper.valueToTree(input));
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) input.get("arguments");
            if (arguments == null || !descriptor.allowedFields().containsAll(arguments.keySet())) {
                throw new BusinessException(409, "POLICY_MISMATCH");
            }
            var published = actionService.resolvePublishedAction(
                    descriptor.suiteCode(), descriptor.objectCode(), descriptor.actionCode(),
                    descriptor.publishedObjectVersion());
            stepValidator.validate(published.action());
            if (!modelPolicy.writableFields(published.version()).keySet()
                    .containsAll(descriptor.allowedFields())) {
                throw new BusinessException(409, "POLICY_MISMATCH");
            }
            if (!approval.getBusinessStateDigest().equals(stateService.snapshot(descriptor, input))) {
                finish(approval, "FAILED", "FAILED", "BUSINESS_STATE_CHANGED");
                return;
            }
            approval.setExecuteStatus("EXECUTING");
            approval.setResultCode("EXECUTING");
            approval.setErrorCode(null);
            if (approvalMapper.updateState(approval) != 1) {
                throw new BusinessException("APPROVAL_AUDIT_UNAVAILABLE");
            }
            BusinessActionExecuteDTO command = new BusinessActionExecuteDTO();
            command.setSuiteCode(descriptor.suiteCode());
            command.setObjectCode(descriptor.objectCode());
            command.setActionCode(descriptor.actionCode());
            command.setRecordId(input.get("recordId") == null ? null : String.valueOf(input.get("recordId")));
            command.setIdempotencyKey(approval.getIdempotencyKey());
            command.setFormData(new LinkedHashMap<>(arguments));
            var result = executionService.executePublished(
                    command, descriptor.publishedObjectVersion(), approval.getRequestId());
            Map<String, Object> safe = new LinkedHashMap<>();
            safe.put("executeStatus", result.getExecuteStatus());
            safe.put("message", result.getMessage());
            safe.put("correlationId", result.getCorrelationId());
            safe.put("idempotentHit", Boolean.TRUE.equals(result.getIdempotentHit()));
            approval.setResultSnapshot(writeJson(safe));
            if (!"SUCCESS".equals(result.getExecuteStatus())) {
                finish(approval, "FAILED", "FAILED", "BUSINESS_EXECUTION_NOT_SUCCESS");
                return;
            }
            finish(approval, "SUCCESS", "SUCCESS", null);
        }
        catch (RuntimeException exception) {
            finish(approval, "FAILED", "FAILED", stableError(exception));
        }
    }

    private void finish(AiCapabilityApproval approval, String status, String resultCode, String errorCode) {
        approval.setExecuteStatus(status);
        approval.setResultCode(resultCode);
        approval.setErrorCode(errorCode);
        approval.setCompletedAt(LocalDateTime.now());
        if (approvalMapper.updateState(approval) != 1) {
            throw new BusinessException("APPROVAL_AUDIT_UNAVAILABLE");
        }
    }

    private boolean terminal(String status) {
        return Set.of("SUCCESS", "FAILED", "REJECTED", "EXPIRED", "CANCELLED").contains(status);
    }

    private Long approvalId(String businessKey) {
        if (businessKey == null || !businessKey.startsWith("capability-approval:")) return null;
        try { return Long.valueOf(businessKey.substring("capability-approval:".length())); }
        catch (NumberFormatException exception) { return null; }
    }

    private String stableError(RuntimeException exception) {
        String message = exception.getMessage();
        return message != null && message.matches("^[A-Z][A-Z0-9_]{0,63}$")
                ? message : "APPROVAL_EXECUTION_FAILED";
    }

    private String writeJson(Map<String, Object> value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { throw new BusinessException("APPROVAL_AUDIT_UNAVAILABLE"); }
    }
}
