package com.mdframe.forge.plugin.capability.highrisk.service;

import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;
import com.mdframe.forge.plugin.capability.secureaction.spi.GovernedCapabilityExecutionAdapter;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionPublishedModelPolicy;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionStepValidator;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectActionService;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class HighRiskApprovalExecutionAdapter implements GovernedCapabilityExecutionAdapter {
    private final CapabilityPolicyService policyService;
    private final HighRiskApprovalSubmissionService submissionService;
    private final BusinessObjectActionService actionService;
    private final SecureActionStepValidator stepValidator;
    private final SecureActionPublishedModelPolicy publishedModelPolicy;

    @Override
    public boolean supports(SecureActionDescriptor descriptor) {
        return descriptor != null && "BUSINESS_ACTION".equals(descriptor.sourceType())
                && "ACTION".equals(descriptor.behavior()) && "HIGH".equals(descriptor.riskLevel());
    }

    @Override
    public void validate(SecureActionDescriptor descriptor, Map<String, Object> input) {
        ExecutionIdentity identity = identity();
        policyService.requireActive(identity.loginUser().getTenantId(),
                descriptor.capabilityId(), descriptor.version());
        var published = actionService.resolvePublishedAction(
                descriptor.suiteCode(), descriptor.objectCode(), descriptor.actionCode(),
                descriptor.publishedObjectVersion());
        stepValidator.validate(published.action());
        if (!publishedModelPolicy.writableFields(published.version()).keySet()
                .containsAll(descriptor.allowedFields())) {
            throw new BusinessException(409, "POLICY_MISMATCH");
        }
        Object rawArguments = input.get("arguments");
        if (!(rawArguments instanceof Map<?, ?> arguments)
                || !descriptor.allowedFields().containsAll(
                arguments.keySet().stream().map(String::valueOf).toList())) {
            throw new BusinessException(409, "POLICY_MISMATCH");
        }
    }

    @Override
    public Map<String, Object> execute(SecureActionDescriptor descriptor,
                                       Map<String, Object> input,
                                       String requestId) {
        return submissionService.submit(descriptor, identity(), input, requestId);
    }

    private ExecutionIdentity identity() {
        ExecutionIdentity identity = ExecutionIdentityContextHolder.current()
                .orElseThrow(() -> new BusinessException(401, "缺少可信 MCP 执行身份"));
        if (!"USER".equals(identity.actorType())) {
            throw new BusinessException(403, "USER_DELEGATION_REQUIRED");
        }
        return identity;
    }
}
