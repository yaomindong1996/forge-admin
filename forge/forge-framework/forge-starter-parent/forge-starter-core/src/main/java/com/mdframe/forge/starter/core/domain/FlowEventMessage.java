package com.mdframe.forge.starter.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 流程事件消息 DTO
 *
 * <p>用于 Redis Pub/Sub 和 HTTP Webhook 两种回调方式的统一消息体。</p>
 *
 * <h3>事件类型（eventType）说明</h3>
 * <ul>
 *     <li>{@code PROCESS_STARTED}   - 流程已发起</li>
 *     <li>{@code PROCESS_COMPLETED} - 流程已通过（全部审批节点完成）</li>
 *     <li>{@code PROCESS_REJECTED}  - 流程已驳回</li>
 *     <li>{@code PROCESS_CANCELED}  - 流程已取消/撤回</li>
 *     <li>{@code TASK_CREATED}      - 审批任务已创建（待办产生）</li>
 *     <li>{@code TASK_COMPLETED}    - 审批任务已完成</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowEventMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ==================== 事件类型常量 ====================

    public static final String PROCESS_STARTED   = "PROCESS_STARTED";
    public static final String PROCESS_COMPLETED = "PROCESS_COMPLETED";
    public static final String PROCESS_REJECTED  = "PROCESS_REJECTED";
    public static final String PROCESS_CANCELED  = "PROCESS_CANCELED";
    public static final String TASK_CREATED      = "TASK_CREATED";
    public static final String TASK_COMPLETED    = "TASK_COMPLETED";
    /** 任务被分配/签收事件 */
    public static final String TASK_ASSIGNED     = "TASK_ASSIGNED";

    // ==================== 事件基础信息 ====================

    /**
     * 事件类型，见上方常量
     */
    private String eventType;

    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;

    // ==================== 流程信息 ====================

    /**
     * 流程实例 ID（Flowable processInstanceId）
     */
    private String processInstanceId;

    /**
     * 流程定义 Key（modelKey）
     */
    private String processDefKey;

    /**
     * 流程定义 ID
     */
    private String processDefId;

    // ==================== 业务信息 ====================

    /**
     * 业务 Key（与业务表主键或唯一标识关联）
     */
    private String businessKey;

    /**
     * 业务类型（对应 FlowBusiness.businessType）
     */
    private String businessType;

    /**
     * 流程标题（申请单名称）
     */
    private String title;

    /**
     * 申请人 ID
     */
    private String applyUserId;

    /**
     * 申请人姓名
     */
    private String applyUserName;

    /**
     * 申请部门 ID
     */
    private String applyDeptId;

    /**
     * 申请部门名称
     */
    private String applyDeptName;

    // ==================== 任务信息（TASK_* 事件时填充）====================

    /**
     * 任务 ID（Flowable taskId，TASK_* 事件时有值）
     */
    private String taskId;

    /**
     * 任务名称（节点名称）
     */
    private String taskName;
    
    /**
     * 任务定义key
     */
    private String taskDefKey;

    /**
     * 任务处理人 ID
     */
    private String assigneeId;

    /**
     * 任务处理人姓名
     */
    private String assigneeName;

    /**
     * 审批意见
     */
    private String comment;

    // ==================== 扩展信息 ====================

    /**
     * 流程变量快照（可选，根据需要携带）
     */
    private Map<String, Object> variables;

    /**
     * 租户 ID（多租户场景）
     */
    private String tenantId;

    // ==================== 工厂方法 ====================

    /**
     * 快速构建流程级事件（不含任务信息）
     */
    public static FlowEventMessage ofProcess(String eventType,
                                             String processInstanceId,
                                             String processDefKey,
                                             String businessKey,
                                             String businessType,
                                             String title,
                                             String applyUserId,
                                             String applyUserName) {
        return FlowEventMessage.builder()
                .eventType(eventType)
                .eventTime(LocalDateTime.now())
                .processInstanceId(processInstanceId)
                .processDefKey(processDefKey)
                .businessKey(businessKey)
                .businessType(businessType)
                .title(title)
                .applyUserId(applyUserId)
                .applyUserName(applyUserName)
                .build();
    }

    /**
     * 快速构建任务级事件（含完整业务信息）
     */
    public static FlowEventMessage ofTask(String eventType,
                                          String processInstanceId,
                                          String processDefKey,
                                          String businessKey,
                                          String title,
                                          String applyUserId,
                                          String applyUserName,
                                          String taskId,
                                          String taskDefKey,
                                          String taskName,
                                          String assigneeId,
                                          String assigneeName,
                                          String comment) {
        return FlowEventMessage.builder()
                .eventType(eventType)
                .eventTime(LocalDateTime.now())
                .processInstanceId(processInstanceId)
                .processDefKey(processDefKey)
                .businessKey(businessKey)
                .title(title)
                .applyUserId(applyUserId)
                .applyUserName(applyUserName)
                .taskId(taskId)
                .taskName(taskName)
                .taskDefKey(taskDefKey)
                .assigneeId(assigneeId)
                .assigneeName(assigneeName)
                .comment(comment)
                .build();
    }
}
