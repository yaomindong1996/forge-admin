package com.mdframe.forge.plugin.capability.highrisk.service;

import com.mdframe.forge.plugin.capability.highrisk.domain.AiCapabilityPolicy;
import com.mdframe.forge.plugin.capability.highrisk.mapper.CapabilityPolicyMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CapabilityPolicyService {
    private final CapabilityPolicyMapper mapper;

    public AiCapabilityPolicy requireActive(Long tenantId, Long capabilityId, String version) {
        AiCapabilityPolicy policy = mapper.selectActive(tenantId, capabilityId, version);
        if (policy == null) {
            throw new BusinessException(409, "HIGH_RISK_POLICY_REQUIRED");
        }
        return policy;
    }

    public void save(AiCapabilityPolicy policy) {
        AiCapabilityPolicy existing = mapper.selectActive(
                policy.getTenantId(), policy.getCapabilityId(), policy.getCapabilityVersion());
        if (existing != null) {
            throw new BusinessException("高风险能力版本已存在启用审批策略");
        }
        mapper.insert(policy);
    }
}
