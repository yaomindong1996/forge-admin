package com.mdframe.forge.plugin.ai.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiAgentMapper extends BaseMapper<AiAgent> {
    AiAgent selectEnabledByCode(@Param("agentCode") String agentCode);
}
