package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 流程任务 Mapper
 */
@Mapper
public interface FlowTaskMapper extends BaseMapper<FlowTask> {
    
    /**
     * 根据任务ID查询任务
     */
    FlowTask selectByTaskId(String taskId);

    /**
     * 锁定任务记录，用于最终办理授权与幂等判定。
     */
    FlowTask selectByTaskIdForUpdate(@Param("taskId") String taskId);

    /**
     * 根据任务ID或流程任务记录ID查询任务
     */
    FlowTask selectByIdOrTaskId(@Param("taskId") String taskId);

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

    /**
     * 分页查询已逾期但仍未完成的任务。
     */
    IPage<FlowTask> selectOverduePendingTasks(Page<FlowTask> page, @Param("now") LocalDateTime now);

    /**
     * 统计工作台待办数。
     */
    Long countWorkspaceTodo(@Param("userId") String userId);

    /**
     * 统计指定时间后的已办数。
     */
    Long countWorkspaceDoneSince(@Param("userId") String userId, @Param("since") LocalDateTime since);

    /**
     * 统计我发起且仍在流转中的流程数。
     */
    Long countWorkspaceStartedRunning(@Param("userId") String userId);
}
