package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 流程任务逾期提醒发送记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_flow_overdue_reminder_record")
public class FlowOverdueReminderRecord extends TenantEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * Flowable任务ID。
     */
    private String taskId;

    /**
     * 流程实例ID。
     */
    private String processInstanceId;

    /**
     * 流程定义KEY。
     */
    private String processDefKey;

    /**
     * 任务定义Key。
     */
    private String taskDefKey;

    /**
     * 提醒批次键，包含任务ID和提醒序号。
     */
    private String reminderKey;

    /**
     * 推送渠道。
     */
    private String channel;

    /**
     * 消息模板编码。
     */
    private String templateCode;

    /**
     * 接收人用户ID集合，逗号分隔。
     */
    private String receiverUserIds;

    /**
     * 消息ID。
     */
    private Long messageId;

    /**
     * 发送状态：0-待发送/1-成功/2-失败。
     */
    private Integer sendStatus;

    /**
     * 发送时间。
     */
    private LocalDateTime sendTime;

    /**
     * 失败原因。
     */
    private String errorMessage;

    /** 本次提醒发送失败后的重试次数。 */
    private Integer retryCount;

    /** 下次允许重试的时间，避免失败后每轮扫描立即重发。 */
    private LocalDateTime nextRetryTime;
}
