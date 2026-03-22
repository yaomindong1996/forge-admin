package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 流程任务 Mapper
 */
@Mapper
public interface FlowTaskMapper extends BaseMapper<FlowTask> {
    
    /**
     * 根据任务ID查询任务
     */
    @Select("SELECT * FROM sys_flow_task WHERE task_id = #{taskId}")
    FlowTask selectByTaskId(String taskId);
}