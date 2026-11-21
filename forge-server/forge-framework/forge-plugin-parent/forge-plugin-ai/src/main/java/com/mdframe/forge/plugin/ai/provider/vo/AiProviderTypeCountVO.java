package com.mdframe.forge.plugin.ai.provider.vo;

import lombok.Data;

/**
 * 当前租户下各供应商类型的已配置连接数统计（不包含密钥）。
 */
@Data
public class AiProviderTypeCountVO {
    private String providerType;
    private Long configuredCount;
}
