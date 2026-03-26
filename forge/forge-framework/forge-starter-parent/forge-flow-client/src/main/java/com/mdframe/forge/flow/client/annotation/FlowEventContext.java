package com.mdframe.forge.flow.client.annotation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 流程事件上下文（业务方回调的入参）
 * <p>
 * 由 flow-server 通过 Redis Pub/Sub 或 Webhook 推送，
 * {@link com.mdframe.forge.flow.client.helper.FlowEventSubscriber} 反序列化后
 * 传递给 {@link FlowCallback} 标注的方法。
 *
 * @author forge
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowEventContext {

    /** 事件类型：PROCESS_COMPLETED / PROCESS_REJECTED / PROCESS_CANCELED
     *  兼容 FlowEventMessage.eventType 字段名 */
    @JsonAlias("eventType")
    private String event;

    /** 业务唯一标识（发起时传入的 businessKey） */
    private String businessKey;

    /** 流程实例 ID */
    private String processInstanceId;

    /** 流程模型 Key */
    private String processDefKey;

    /** 流程标题 */
    private String title;

    /** 发起人 ID，兼容 FlowEventMessage.applyUserId */
    @JsonAlias("applyUserId")
    private String startUserId;

    /** 发起人姓名，兼容 FlowEventMessage.applyUserName */
    @JsonAlias("applyUserName")
    private String startUserName;

    /** 事件发生时间 */
    private LocalDateTime eventTime;

    /** 最终审批意见 */
    private String lastComment;

    // ==================== 任务信息（TASK_* 事件时填充）====================

    /** 任务 ID（Flowable taskId），兼容 FlowEventMessage.taskId */
    private String taskId;

    /** 任务名称（审批节点名），兼容 FlowEventMessage.taskName */
    private String taskName;

    /** 任务处理人 ID，兼容 FlowEventMessage.assigneeId */
    @JsonAlias("assigneeId")
    private String assigneeId;

    /** 任务处理人姓名，兼容 FlowEventMessage.assigneeName */
    private String assigneeName;

    /** 审批意见（TASK_COMPLETED 时就是该节点的意见）兼容 FlowEventMessage.comment */
    private String comment;

    /** 流程变量（可选） */
    private Map<String, Object> variables;
}
