package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mdframe.forge.starter.flow.enums.FlowCcStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程抄送实体
 */
@Data
@TableName("sys_flow_cc")
public class FlowCc {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 流程定义KEY
     */
    private String processDefKey;

    /**
     * 流程名称（由业务侧列表展示扩展点补齐）
     */
    @TableField(exist = false)
    private String processName;

    /**
     * 流程定义名称（由业务侧列表展示扩展点补齐）
     */
    @TableField(exist = false)
    private String processDefinitionName;

    /**
     * 来源任务ID
     */
    private String taskId;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容摘要
     */
    private String content;

    /**
     * 业务Key
     */
    private String businessKey;

    /**
     * 业务对象编码（由业务侧列表展示扩展点补齐）
     */
    @TableField(exist = false)
    private String objectCode;

    /**
     * 业务记录ID（由业务侧列表展示扩展点补齐）
     */
    @TableField(exist = false)
    private Long recordId;

    /**
     * 业务对象名称（由业务侧列表展示扩展点补齐）
     */
    @TableField(exist = false)
    private String businessObjectName;

    /**
     * 业务摘要（由业务侧列表展示扩展点补齐）
     */
    @TableField(exist = false)
    private String businessSummary;

    /**
     * 抄送人ID
     */
    private String ccUserId;

    /**
     * 抄送人姓名
     */
    private String ccUserName;

    /**
     * 发送人ID
     */
    private String sendUserId;

    /**
     * 发送人姓名
     */
    private String sendUserName;

    /**
     * 抄送时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime ccTime;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    /**
     * 是否已读（0-未读/1-已读）
     */
    private Integer isRead;

    /** 抄送关系状态，撤回后接收人不可再查看。 */
    private Integer status;

    private String revokeBy;
    private LocalDateTime revokeTime;
    private String revokeReason;

    public boolean isActive() {
        return FlowCcStatus.ACTIVE.matches(status);
    }
}
