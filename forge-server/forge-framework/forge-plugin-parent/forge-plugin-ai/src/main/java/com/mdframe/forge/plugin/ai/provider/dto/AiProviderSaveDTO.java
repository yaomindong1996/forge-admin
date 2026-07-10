package com.mdframe.forge.plugin.ai.provider.dto;

import lombok.Data;

/**
 * AI 供应商新增和修改请求。
 */
@Data
public class AiProviderSaveDTO {

    private Long id;

    private String providerName;

    private String providerType;

    private String adapterCode;

    private String logo;

    private String apiKey;

    private String baseUrl;

    private String models;

    private String defaultModel;

    private String isDefault;

    private String status;

    private String remark;
}
