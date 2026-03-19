package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程表单定义实体
 * 
 * @author forge
 */
@Data
@TableName("sys_flow_form")
public class FlowForm {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 表单Key（唯一标识）
     */
    private String formKey;

    /**
     * 表单名称
     */
    private String formName;

    /**
     * 表单类型：dynamic-动态表单，external-外部表单，builtin-内置表单
     */
    private String formType;

    /**
     * 表单Schema（JSON格式）
     */
    private String formSchema;

    /**
     * 外部表单URL（formType为external时使用）
     */
    private String formUrl;

    /**
     * 内置表单组件路径（formType为builtin时使用）
     */
    private String componentPath;

    /**
     * 表单配置（JSON格式，包含校验规则、事件等）
     */
    private String formConfig;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 描述
     */
    private String description;

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 删除标志
     */
    @TableLogic
    private Integer deleted;
}