package com.mdframe.forge.plugin.ai.provider.vo;

import java.util.List;

/**
 * 内置供应商预设元数据，不包含任何密钥或连接凭据。
 */
public record AiProviderTemplateVO(
        String templateKey, String name, String providerType, String baseUrl,
        String defaultModel, String adapterCode, String connectionHint,
        String documentationUrl, boolean featured, List<String> providerTypeAliases,
        Long configuredCount) {
}
