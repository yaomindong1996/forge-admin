package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_page_template")
public class AiPageTemplate extends BaseEntity {

    @TableId(value = "id")
    private Long id;

    /** 模板唯一标识（如 simple-crud / tree-crud） */
    private String templateKey;

    /** 模板显示名称 */
    private String templateName;

    /** 模板描述 */
    private String description;

    /** 图标（mdi: 前缀） */
    private String icon;

    /** 该模板专属的 AI system prompt 补充 */
    private String systemPrompt;

    /** 告诉 AI 该模板可用的字段约束（JSON） */
    private String schemaHint;

    /** 默认配置值（JSON，如 modalType/searchGridCols 等） */
    private String defaultConfig;

    /** 是否启用（1启用 0停用） */
    private Integer enabled;

    /** 排序 */
    private Integer sort;

    /** 是否内置（1内置不可删除） */
    private Integer isBuiltin;

    /**
     * 代码生成策略类型
     * - TEMPLATE（默认）：Velocity 模板生成，适用于简单 CRUD 等结构化组件
     * - AI：大模型生成，适用于复杂组件（主从表、看板、工作流等）
     */
    private String codegenType;
}
