package com.mdframe.forge.plugin.ai.provider.dto;

import lombok.Data;

/**
 * AI 供应商连接测试请求。
 *
 * <p>已保存配置仅提交 id；未保存配置不得提交 id，且必须提交完整连接参数。</p>
 */
@Data
public class AiProviderTestDTO {

    private Long id;

    private String providerName;

    private String providerType;

    private String adapterCode;

    private String apiKey;

    private String baseUrl;

    private String defaultModel;
}
