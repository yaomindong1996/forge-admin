package com.mdframe.forge.plugin.ai.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.mapper.AiAgentMapper;
import com.mdframe.forge.plugin.ai.routing.constant.AiModelSelectionMode;
import com.mdframe.forge.plugin.ai.routing.domain.AiModelRoutePolicy;
import com.mdframe.forge.plugin.ai.routing.mapper.AiModelRoutePolicyMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiAgentService extends ServiceImpl<AiAgentMapper, AiAgent> {

    private final AiModelRoutePolicyMapper policyMapper;

    /**
     * 根据编码获取 Agent
     */
    public AiAgent getByCode(String agentCode) {
        return baseMapper.selectEnabledByCode(agentCode);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createAgent(AiAgent agent) { normalize(agent); if (!save(agent)) throw new BusinessException("智能体新增失败"); }

    @Transactional(rollbackFor = Exception.class)
    public void updateAgent(AiAgent agent) { if (agent == null || agent.getId() == null) throw new BusinessException("智能体ID不能为空"); normalize(agent); if (!updateById(agent)) throw new BusinessException("智能体更新失败"); }

    private void normalize(AiAgent agent) {
        AiModelSelectionMode mode = AiModelSelectionMode.fromNullable(agent.getModelSelectionMode());
        agent.setModelSelectionMode(mode.name());
        if (mode == AiModelSelectionMode.PINNED) {
            agent.setRoutePolicyId(null);
            if (agent.getProviderId() == null) throw new BusinessException("固定模型模式必须选择供应商");
        } else {
            if (agent.getRoutePolicyId() == null) throw new BusinessException("路由策略模式必须选择策略");
            AiModelRoutePolicy policy = policyMapper.selectById(agent.getRoutePolicyId());
            if (policy == null || !"0".equals(policy.getStatus())) throw new BusinessException("路由策略不存在或已停用");
        }
    }
}
