package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 流程审批层级配置实体
 * 用于支持动态任意层级审批
 */
@Data
@TableName("sys_flow_approval_level")
public class FlowApprovalLevel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /**
     * 节点配置ID
     */
    private String nodeConfigId;

    /**
     * 层级序号（从1开始）
     */
    private Integer levelIndex;

    /**
     * 层级名称
     */
    private String levelName;

    /**
     * 审批人类型：
     * user-指定用户
     * role-按角色
     * dept-按部门
     * post-按岗位
     * leader-上级领导
     * deptManager-部门负责人
     * expr-表达式
     */
    private String assigneeType;

    /**
     * 审批人值（JSON格式）
     */
    private String assigneeValue;

    /**
     * 层级条件表达式（满足条件才执行此层级）
     */
    private String conditionExpr;

    /**
     * 跳过条件（满足条件跳过此层级）
     */
    private String skipCondition;

    /**
     * 是否必须审批
     */
    private Boolean required;

    /**
     * 超时时间（小时）
     */
    private Integer timeoutHours;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态：0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 删除标志
     */
    @TableLogic
    private Integer deleted;
}