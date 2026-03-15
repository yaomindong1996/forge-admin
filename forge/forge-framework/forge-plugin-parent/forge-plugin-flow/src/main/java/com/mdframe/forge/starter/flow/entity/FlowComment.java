package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程审批意见实体
 */
@Data
@TableName("sys_flow_comment")
public class FlowComment {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 流程定义KEY
     */
    private String processDefKey;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 类型（comment-审批意见/event-流程事件）
     */
    private String type;

    /**
     * 意见内容
     */
    private String message;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户姓名
     */
    private String userName;

    /**
     * 完整消息（JSON）
     */
    private String fullMessage;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}