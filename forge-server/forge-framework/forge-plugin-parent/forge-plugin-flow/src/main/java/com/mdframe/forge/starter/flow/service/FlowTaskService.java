package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.dto.FlowApprovalPointResultDTO;
import com.mdframe.forge.starter.flow.dto.ProcessDiagramInfo;
import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.vo.FlowHistoryPageVO;
import com.mdframe.forge.starter.flow.vo.FlowTaskSignRelationVO;

import java.util.List;
import java.util.Map;

/**
 * 流程任务服务接口
 */
public interface FlowTaskService {

    /**
     * 我的待办任务
     */
    IPage<FlowTask> todoTasks(Page<FlowTask> page, String userId, String title, String category, Integer status);

    /**
     * 我的已办任务
     */
    IPage<FlowTask> doneTasks(Page<FlowTask> page, String userId, String title, String category, Integer status);

    /**
     * 我发起的流程
     */
    IPage<FlowTask> startedTasks(Page<FlowTask> page, String userId, String title, String category, Integer status);

    /**
     * 候选任务（未签收的任务）
     * 支持按用户ID和/或组ID查询
     *
     * @param page    分页参数
     * @param userId  用户ID（可选）
     * @param groupId 组ID（可选）
     * @param title   标题过滤
     * @return 候选任务列表
     */
    IPage<FlowTask> candidateTasks(Page<FlowTask> page, String userId, String groupId, String title);

    /**
     * 签收任务
     */
    void claimTask(String taskId, String userId);

    /**
     * 审批通过
     */
    default void approve(String taskId, String userId, String comment, Map<String, Object> variables) {
        approve(taskId, userId, comment, null, variables);
    }

    /**
     * 审批通过
     */
    void approve(String taskId, String userId, String comment, String signature, Map<String, Object> variables);

    /**
     * 带可信租户与远程幂等凭证的审批入口。
     */
    default void approve(String taskId, String userId, String comment, String signature,
                         Map<String, Object> variables, Long tenantId,
                         String idempotencyKey, String requestDigest) {
        approve(taskId, userId, comment, signature, variables, tenantId, idempotencyKey, requestDigest, null);
    }

    /**
     * 带审批要点勾选结果的审批入口。
     */
    void approve(String taskId, String userId, String comment, String signature,
                 Map<String, Object> variables, Long tenantId,
                 String idempotencyKey, String requestDigest,
                 List<FlowApprovalPointResultDTO> approvalPointResults);

    /**
     * 审批驳回
     */
    default void reject(String taskId, String userId, String comment) {
        reject(taskId, userId, comment, null);
    }

    /**
     * 审批驳回
     */
    void reject(String taskId, String userId, String comment, String signature);

    /**
     * 带可信租户与远程幂等凭证的驳回入口。
     */
    void reject(String taskId, String userId, String comment, String signature,
                Long tenantId, String idempotencyKey, String requestDigest);

    /**
     * 驳回至流程设计中标记的发起人修改路径。具体目标节点由 BPMN 网关/回路决定，
     * 服务端通过 rejectToStart 变量保留动作语义，不强行改写用户设计的流程图。
     */
    void rejectToStart(String taskId, String userId, String comment, String signature,
                       Long tenantId, String idempotencyKey, String requestDigest);

    /**
     * 转办
     */
    default void delegate(String taskId, String userId, String targetUserId, String comment) {
        delegate(taskId, userId, targetUserId, comment, null);
    }

    /**
     * 转办
     */
    void delegate(String taskId, String userId, String targetUserId, String comment, String signature);

    /**
     * 带可信租户与远程幂等凭证的转办入口。
     */
    default void delegate(String taskId, String userId, String targetUserId, String comment,
                          String signature, Long tenantId, String idempotencyKey,
                          String requestDigest) {
        delegate(taskId, userId, targetUserId, comment, signature);
    }

    /**
     * 退回上一审批节点
     */
    void returnTask(String taskId, String userId, String comment, String signature);

    /**
     * 退回指定历史用户任务节点。targetActivityId 为空时等同于上一节点退回。
     */
    void returnTask(String taskId, String userId, String comment, String signature,
                   String targetActivityId);

    /**
     * 由当前处理人、任务拥有人或流程发起人改派任务。
     */
    void reassignByInitiator(String taskId, String userId, String targetUserId, String reason);

    /**
     * 终结流程
     */
    void terminateTask(String taskId, String userId, String comment, String signature);

    /**
     * 委派
     */
    void delegateTask(String taskId, String userId, String delegateUserId, String comment);

    /** 动态加签：将目标用户加入当前任务的候选办理人集合。 */
    void addSign(String taskId, String userId, String targetUserId, String reason);

    /** 带关系模式的动态加签；模式写入关系审计并保留 Flowable 候选任务兼容语义。 */
    default void addSign(String taskId, String userId, String targetUserId, String reason, String signMode) {
        addSign(taskId, userId, targetUserId, reason);
    }

    /** 带幂等凭证的动态加签入口。 */
    default void addSign(String taskId, String userId, String targetUserId, String reason,
                         String signMode, Long tenantId, String idempotencyKey,
                         String requestDigest) {
        addSign(taskId, userId, targetUserId, reason, signMode);
    }

    /** 动态减签：从当前任务的候选办理人集合移除目标用户。 */
    void reduceSign(String taskId, String userId, String targetUserId, String reason);

    /** 带关系模式的动态减签；模式用于关系审计。 */
    default void reduceSign(String taskId, String userId, String targetUserId, String reason, String signMode) {
        reduceSign(taskId, userId, targetUserId, reason);
    }

    /** 带幂等凭证的动态减签入口。 */
    default void reduceSign(String taskId, String userId, String targetUserId, String reason,
                            String signMode, Long tenantId, String idempotencyKey,
                            String requestDigest) {
        reduceSign(taskId, userId, targetUserId, reason, signMode);
    }

    /** 查询当前任务的动态加签关系，结果按关系创建时间排序并限制返回量。 */
    List<FlowTaskSignRelationVO> getSignRelations(String taskId, String userId);

    /**
     * 撤回流程
     */
    void withdraw(String processInstanceId, String userId);

    /**
     * 获取任务详情
     */
    FlowTask getTaskDetail(String taskId);

    /**
     * 获取流程图（高亮当前节点）
     *
     * @param processInstanceId 流程实例ID
     * @return PNG图片字节数组
     */
    byte[] getProcessDiagram(String processInstanceId);

    /**
     * 获取流程图详情（包含节点信息，用于交互式展示）
     *
     * @param processInstanceId 流程实例ID
     * @return 流程图详情
     */
    ProcessDiagramInfo getProcessDiagramInfo(String processInstanceId);

    /**
     * 获取流程图详情
     *
     * @param processInstanceId 流程实例ID
     * @param includeImage 是否返回 Base64 图片。默认查看器使用 BPMN XML 渲染，图片按需生成。
     * @return 流程图详情
     */
    ProcessDiagramInfo getProcessDiagramInfo(String processInstanceId, boolean includeImage);

    /**
     * 催办
     */
    void remind(String taskId);

    /**
     * 获取流程审批时间轴
     * 返回按时间排序的审批节点列表，每个元素包含：taskName, assigneeName, action, comment, createTime, completeTime
     *
     * @param processInstanceId 流程实例 ID
     * @return 审批历史列表
     */
    List<Map<String, Object>> getProcessHistory(String processInstanceId);

    /** 分页获取类型化流程审批时间轴。 */
    FlowHistoryPageVO getProcessHistoryPage(String processInstanceId, Integer pageNum, Integer pageSize);

    /**
     * 获取任务表单信息
     * 包含表单类型、表单配置、流程变量等
     *
     * @param taskId 任务ID
     * @return 任务表单信息
     */
    TaskFormInfo getTaskFormInfo(String taskId);

    /**
     * 获取流程关联表单信息。
     * 用于抄送、已完成流程等没有运行中任务的只读场景。
     *
     * @param processInstanceId 流程实例ID
     * @param businessKey 业务Key
     * @param processDefKey 流程定义Key
     * @param taskId 任务ID（可选）
     * @param taskDefKey 任务定义Key（可选）
     * @return 表单信息
     */
    TaskFormInfo getProcessFormInfo(String processInstanceId, String businessKey, String processDefKey,
                                    String taskId, String taskDefKey);
}
