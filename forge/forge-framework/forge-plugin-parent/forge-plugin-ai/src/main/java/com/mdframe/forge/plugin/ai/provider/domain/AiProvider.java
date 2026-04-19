package com.mdframe.forge.plugin.ai.provider.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * AI 供应商实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_provider")
public class AiProvider extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /**
     * 供应商名称
     */
    private String providerName;

    /**
     * 类型（openai/azure/dashscope/ollama）
     */
    private String providerType;

    /**
     * 供应商Logo（文件ID或URL）
     */
    private String logo;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * API Base URL
     */
    private String baseUrl;

    /**
     * 可用模型列表 JSON
     */
    private String models;

    /**
     * 默认使用的模型名称
     */
    private String defaultModel;

    /**
     * 是否默认供应商（0否 1是）
     */
    private String isDefault;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
