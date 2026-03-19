package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程模板实体
 * 用于存储预定义的流程模板，方便快速创建流程
 */
@Data
@TableName("sys_flow_template")
public class FlowTemplate {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 模板标识（唯一）
     */
    private String templateKey;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 分类编码
     */
    private String category;

    /**
     * 描述
     */
    private String description;

    /**
     * 图标
     */
    private String icon;

    /**
     * 表单类型（dynamic-动态表单/external-外置表单/none-无表单）
     */
    private String formType;

    /**
     * 表单JSON配置
     */
    private String formJson;

    /**
     * BPMN流程定义XML
     */
    private String bpmnXml;

    /**
     * 缩略图URL
     */
    private String thumbnail;

    /**
     * 流程变量定义（JSON格式）
     */
    private String variables;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 状态（0-禁用/1-启用）
     */
    private Integer status;

    /**
     * 使用次数
     */
    private Integer usageCount;

    /**
     * 是否系统内置（0-否/1-是）
     */
    private Integer isSystem;

    /**
     * 排序
     */
    private Integer sortOrder;

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