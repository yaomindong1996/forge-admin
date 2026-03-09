package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程任务实体（我的待办/已办）
 */
@Data
@TableName("sys_flow_task")
public class FlowTask {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * Flowable任务ID
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务定义Key
     */
    private String taskDefKey;

    /**
     * 任务定义ID
     */
    private String taskDefId;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 流程定义ID
     */
    private String processDefId;

    /**
     * 流程定义KEY
     */
    private String processDefKey;

    /**
     * 业务Key
     */
    private String businessKey;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 处理人（签收后）
     */
    private String assignee;

    /**
     * 处理人姓名
     */
    private String assigneeName;

    /**
     * 候选人（逗号分隔）
     */
    private String candidateUsers;

    /**
     * 候选组（逗号分隔）
     */
    private String candidateGroups;

    /**
     * 任务拥有人
     */
    private String owner;

    /**
     * 截止日期
     */
    private LocalDateTime dueDate;

    /**
     * 优先级（0-100）
     */
    private Integer priority;

    /**
     * 状态（0-待办/1-已签收/2-已通过/3-已驳回/4-已转办/5-已委派/6-已撤回）
     */
    private Integer status;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 附件URL（逗号分隔）
     */
    private String attachmentUrls;

    /**
     * 流程发起人
     */
    private String startUserId;

    /**
     * 发起人姓名
     */
    private String startUserName;

    /**
     * 发起部门ID
     */
    private String startDeptId;

    /**
     * 发起部门名称
     */
    private String startDeptName;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 签收时间
     */
    private LocalDateTime claimTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;
}