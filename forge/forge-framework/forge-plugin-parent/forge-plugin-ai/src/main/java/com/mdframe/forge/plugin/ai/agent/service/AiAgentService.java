package com.mdframe.forge.plugin.ai.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.mapper.AiAgentMapper;
import org.springframework.stereotype.Service;

@Service
public class AiAgentService extends ServiceImpl<AiAgentMapper, AiAgent> {

    /**
     * 根据编码获取 Agent
     */
    public AiAgent getByCode(String agentCode) {
        return getOne(new LambdaQueryWrapper<AiAgent>()
                .eq(AiAgent::getAgentCode, agentCode)
                .eq(AiAgent::getStatus, "0"));
    }
}
