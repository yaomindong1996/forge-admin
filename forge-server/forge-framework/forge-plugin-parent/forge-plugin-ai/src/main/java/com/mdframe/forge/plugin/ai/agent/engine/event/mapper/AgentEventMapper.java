package com.mdframe.forge.plugin.ai.agent.engine.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.ai.agent.engine.event.persistence.AgentEventEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Agent 事件 Mapper
 */
@Mapper
public interface AgentEventMapper extends BaseMapper<AgentEventEntity> {

    int insertBatch(@Param("events") List<AgentEventEntity> events);

    int deleteBefore(@Param("beforeTime") java.time.LocalDateTime beforeTime);
}
