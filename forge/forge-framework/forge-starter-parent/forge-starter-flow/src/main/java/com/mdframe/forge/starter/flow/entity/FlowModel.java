package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程模型实体
 */
@Data
@TableName("sys_flow_model")
public class FlowModel {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 模型标识（唯一）
     */
    private String modelKey;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 描述
     */
    private String description;

    /**
     * 分类
     */
    private String category;

    /**
     * 流程类型（leave-请假/expense-报销/approval-审批）
     */
    private String flowType;

    /**
     * 表单类型（dynamic-动态表单/custom-业务表单）
     */
    private String formType;

    /**
     * 表单ID（业务表单时使用）
     */
    private String formId;

    /**
     * 动态表单JSON配置
     */
    private String formJson;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * Flowable部署ID
     */
    private String deploymentId;

    /**
     * 部署KEY（发布后生成）
     */
    private String deploymentKey;

    /**
     * 状态（0-设计/1-已发布/2-禁用）
     */
    private Integer status;

    /**
     * 发布时间
     */
    private LocalDateTime deployTime;

    /**
     * 创建人
     */
    private String createBy;

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
     * 删除标志（0-正常/1-删除）
     */
    @TableLogic
    private Integer delFlag;
}