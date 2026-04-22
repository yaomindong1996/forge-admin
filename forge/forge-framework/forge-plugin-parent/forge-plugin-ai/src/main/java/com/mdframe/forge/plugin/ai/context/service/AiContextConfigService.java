package com.mdframe.forge.plugin.ai.context.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.context.domain.AiContextConfig;
import com.mdframe.forge.plugin.ai.context.mapper.AiContextConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AiContextConfigService extends ServiceImpl<AiContextConfigMapper, AiContextConfig> {

    public List<AiContextConfig> listByAgentCode(String agentCode) {
        return list(new LambdaQueryWrapper<AiContextConfig>()
                .eq(AiContextConfig::getAgentCode, agentCode)
                .eq(AiContextConfig::getStatus, "0")
                .orderByAsc(AiContextConfig::getSort));
    }
}
