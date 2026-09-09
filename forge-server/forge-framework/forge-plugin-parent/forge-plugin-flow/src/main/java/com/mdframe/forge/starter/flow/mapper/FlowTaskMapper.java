package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 流程任务 Mapper
 */
@Mapper
public interface FlowTaskMapper extends BaseMapper<FlowTask> {
    
    /**
     * 根据任务ID查询任务
     */
    FlowTask selectByTaskId(String taskId);

    FlowTask selectByTaskIdAndTenant(@Param("taskId") String taskId,
                                     @Param("tenantId") Long tenantId);

    /**
     * 锁定任务记录，用于最终办理授权与幂等判定。
     *
     * @deprecated 写操作必须使用 {@link #selectByTaskIdForUpdateAndTenant(String, Long)}，
     *             保留该方法仅兼容历史内部调用。
     */
    @Deprecated
    FlowTask selectByTaskIdForUpdate(@Param("taskId") String taskId);

    /**
     * 按任务 ID 和可信租户锁定任务记录。
     *
     * <p>Flow 服务的部分入口使用 {@code @IgnoreTenant}，不能依赖租户拦截器
     * 自动补充条件。租户必须进入同一条 SELECT ... FOR UPDATE，避免先锁定跨租户
     * 行再在 Java 层拒绝，造成越权探测和并发窗口。</p>
     */
    FlowTask selectByTaskIdForUpdateAndTenant(@Param("taskId") String taskId,
                                               @Param("tenantId") Long tenantId);

    /**
     * 根据任务ID或流程任务记录ID查询任务
     */
    FlowTask selectByIdOrTaskId(@Param("taskId") String taskId);

    FlowTask selectByIdOrTaskIdAndTenant(@Param("taskId") String taskId,
                                         @Param("tenantId") Long tenantId);

    /**
     * 按任务 ID 和租户更新任务镜像。Flow 服务控制器使用 {@code @IgnoreTenant}，
     * 因此写入条件必须显式包含可信租户。
     */
    int updateByTaskIdAndTenant(@Param("taskId") String taskId,
                                @Param("tenantId") Long tenantId,
                                @Param("task") FlowTask task);

    int countProcessParticipant(@Param("processInstanceId") String processInstanceId,
                                @Param("userId") String userId,
                                @Param("tenantId") Long tenantId);

    /**
     * 批量读取监控页流程实例的活动任务摘要，避免按实例逐条查询。
     */
    List<Map<String, Object>> selectActiveTaskSummaries(@Param("processInstanceIds") Collection<String> processInstanceIds,
                                                         @Param("tenantId") Long tenantId);

    /**
     * 按租户分页读取流程审批时间轴对应的本地任务快照。
     */
    IPage<FlowTask> selectHistoryTasks(Page<FlowTask> page,
                                       @Param("processInstanceId") String processInstanceId,
                                       @Param("tenantId") Long tenantId);

    /**
     * 按本地任务快照的截止时间和主键稳定分页，供超时扫描使用。
     * 该查询返回租户信息，调用方需在处理每条任务时恢复对应租户上下文。
     */
    List<FlowTask> selectTimeoutCandidates(@Param("now") LocalDateTime now,
                                           @Param("cursorDueDate") LocalDateTime cursorDueDate,
                                           @Param("cursorId") String cursorId,
                                           @Param("limit") int limit);

    List<FlowTask> selectDueDateBackfillCandidates(@Param("now") LocalDateTime now,
                                                   @Param("cursorCreateTime") LocalDateTime cursorCreateTime,
                                                   @Param("cursorId") String cursorId,
                                                   @Param("limit") int limit);

    int updateDueDateByTaskIdAndTenant(@Param("taskId") String taskId,
                                       @Param("tenantId") Long tenantId,
                                       @Param("dueDate") LocalDateTime dueDate);

    IPage<FlowTask> selectAdminTasksByProcessInstance(Page<FlowTask> page,
                                                       @Param("processInstanceId") String processInstanceId,
                                                       @Param("tenantId") Long tenantId);

    List<FlowTask> selectAdminTaskTreeByProcessInstance(@Param("processInstanceId") String processInstanceId,
                                                        @Param("tenantId") Long tenantId,
                                                        @Param("limit") int limit);

    /**
     * 分页查询待办任务（带分类关联）
     */
    IPage<FlowTask> selectTodoTasks(Page<FlowTask> page, @Param("userId") String userId,
                                     @Param("title") String title, @Param("category") String category,
                                     @Param("status") Integer status, @Param("tenantId") Long tenantId,
                                     @Param("activeOrgId") Long activeOrgId);

    /**
     * 分页查询已办任务（带分类关联）
     */
    IPage<FlowTask> selectDoneTasks(Page<FlowTask> page, @Param("userId") String userId,
                                      @Param("title") String title, @Param("category") String category,
                                      @Param("status") Integer status, @Param("tenantId") Long tenantId,
                                      @Param("activeOrgId") Long activeOrgId);

    /**
     * 分页查询我发起的任务（带分类关联）
     */
    IPage<FlowTask> selectStartedTasks(Page<FlowTask> page, @Param("userId") String userId,
                                         @Param("title") String title, @Param("category") String category,
                                         @Param("status") Integer status, @Param("tenantId") Long tenantId);

    /**
     * 分页查询未签收的候选任务
     */
    IPage<FlowTask> selectCandidateTasks(Page<FlowTask> page, @Param("userId") String userId,
                                          @Param("groupId") String groupId, @Param("title") String title,
                                          @Param("tenantId") Long tenantId);

    /**
     * 分页查询已逾期但仍未完成的任务。
     */
    IPage<FlowTask> selectOverduePendingTasks(Page<FlowTask> page, @Param("now") LocalDateTime now);

    Long countOverduePending(@Param("tenantId") Long tenantId,
                             @Param("now") LocalDateTime now);

    /**
     * 统计工作台待办数。
     */
    Long countWorkspaceTodo(@Param("userId") String userId, @Param("tenantId") Long tenantId,
                            @Param("activeOrgId") Long activeOrgId);

    /**
     * 统计指定时间后的已办数。
     */
    Long countWorkspaceDoneSince(@Param("userId") String userId, @Param("since") LocalDateTime since,
                                 @Param("tenantId") Long tenantId);

    /**
     * 统计我发起且仍在流转中的流程数。
     */
    Long countWorkspaceStartedRunning(@Param("userId") String userId, @Param("tenantId") Long tenantId);

    /**
     * 将流程终结后由 Flowable 删除事件标记为取消的活动任务统一修正为终结。
     */
    int updateProcessTaskStatusByTaskIds(@Param("taskIds") Collection<String> taskIds,
                                         @Param("tenantId") Long tenantId,
                                         @Param("status") Integer status,
                                         @Param("completeTime") LocalDateTime completeTime);

    int deleteByProcessInstanceIdPhysically(@Param("processInstanceId") String processInstanceId,
                                            @Param("tenantId") Long tenantId);
}
