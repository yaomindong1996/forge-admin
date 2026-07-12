package com.mdframe.forge.plugin.capability.highrisk.publish;

import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityPublishDTO;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
import com.mdframe.forge.plugin.capability.highrisk.config.HighRiskApprovalProperties;
import com.mdframe.forge.plugin.capability.highrisk.domain.AiCapabilityPolicy;
import com.mdframe.forge.plugin.capability.highrisk.service.CapabilityPolicyService;
import com.mdframe.forge.plugin.capability.highrisk.service.HighRiskApprovalFlowModelService;
import com.mdframe.forge.plugin.capability.highrisk.support.HighRiskApprovalFlowDefinition;
import com.mdframe.forge.plugin.capability.secureaction.publish.BusinessActionCapabilityPublisher;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@RequiredArgsConstructor
public class HighRiskActionPublisher {
    private static final Pattern GROUP = Pattern.compile("^[A-Za-z0-9_.:-]{1,64}$");
    private final BusinessActionCapabilityPublisher definitionFactory;
    private final CapabilityCatalogService catalogService;
    private final CapabilityPolicyService policyService;
    private final HighRiskApprovalProperties properties;
    private final HighRiskApprovalFlowModelService flowModelService;

    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long tenantId, HighRiskActionPublishDTO dto) {
        if (!HighRiskApprovalFlowDefinition.MODEL_KEY.equals(properties.getFlowModelKey())) {
            throw new BusinessException("HIGH_RISK_FLOW_MODEL_KEY_INVALID");
        }
        if (!GROUP.matcher(dto.getApprovalCandidateGroup()).matches()) {
            throw new BusinessException("审批候选组格式无效");
        }
        flowModelService.ensureModel(tenantId);
        CapabilityPublishDTO definition = definitionFactory.buildDefinition(dto.getAction(), "HIGH");
        Long capabilityId = catalogService.publishHighRiskBusinessAction(tenantId, definition);
        AiCapabilityPolicy policy = new AiCapabilityPolicy();
        policy.setTenantId(tenantId);
        policy.setCapabilityId(capabilityId);
        policy.setCapabilityVersion(definition.version());
        policy.setRiskLevel("HIGH");
        policy.setApprovalFlowModelKey(properties.getFlowModelKey());
        policy.setApprovalCandidateGroup(dto.getApprovalCandidateGroup());
        policy.setExpireSeconds(dto.getExpireSeconds());
        policy.setStatus("ENABLED");
        policy.setDelFlag(0);
        policyService.save(policy);
        return capabilityId;
    }
}
