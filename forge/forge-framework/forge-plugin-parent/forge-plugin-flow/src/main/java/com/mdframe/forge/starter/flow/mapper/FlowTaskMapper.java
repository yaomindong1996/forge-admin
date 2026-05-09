package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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

    /**
     * 分页查询待办任务（带分类关联）
     */
    IPage<FlowTask> selectTodoTasks(Page<FlowTask> page, @Param("userId") String userId,
                                     @Param("title") String title, @Param("category") String category,
                                     @Param("status") Integer status);

    /**
     * 分页查询已办任务（带分类关联）
     */
    IPage<FlowTask> selectDoneTasks(Page<FlowTask> page, @Param("userId") String userId,
                                      @Param("title") String title, @Param("category") String category,
                                      @Param("status") Integer status);

    /**
     * 分页查询我发起的任务（带分类关联）
     */
    IPage<FlowTask> selectStartedTasks(Page<FlowTask> page, @Param("userId") String userId,
                                         @Param("title") String title, @Param("category") String category,
                                         @Param("status") Integer status);
}