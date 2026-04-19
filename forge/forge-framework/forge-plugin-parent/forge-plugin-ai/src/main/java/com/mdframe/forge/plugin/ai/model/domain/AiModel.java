package com.mdframe.forge.plugin.ai.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * AI 模型实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_model")
public class AiModel extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /**
     * 供应商ID
     */
    private Long providerId;

    /**
     * 模型类型（chat/embedding/image/audio）
     */
    private String modelType;

    /**
     * 模型标识（如 gpt-4o, text-embedding-3）
     */
    private String modelId;

    /**
     * 模型显示名称
     */
    private String modelName;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大Token数
     */
    private Integer maxTokens;

    /**
     * 模型图标（文件ID或URL）
     */
    private String icon;

    /**
     * 是否默认模型（0否 1是）
     */
    private String isDefault;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 备注
     */
    private String remark;
}
