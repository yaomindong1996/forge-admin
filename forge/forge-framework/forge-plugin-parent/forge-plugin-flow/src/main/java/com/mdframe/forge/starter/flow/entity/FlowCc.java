package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.*;
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
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 流程定义KEY
     */
    private String processDefKey;

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
}