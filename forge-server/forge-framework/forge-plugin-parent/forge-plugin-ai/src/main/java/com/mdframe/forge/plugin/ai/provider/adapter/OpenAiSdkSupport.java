package com.mdframe.forge.plugin.ai.provider.adapter;

import java.net.URI;
import java.time.Duration;

/** 不同 OpenAI SDK 模型统一的端点和请求边界，不改写用户已配置的 v1/v4 等版本路径。 */
public final class OpenAiSdkSupport {

    public static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private OpenAiSdkSupport() {
    }

    public static String baseUrl(String configuredUrl) {
        if (configuredUrl == null || configuredUrl.isBlank()) {
            return configuredUrl;
        }
        String base = configuredUrl.trim().replaceAll("/+$", "");
        String path = URI.create(base).getPath();
        // 旧 OpenAiApi 自动拼 /v1；官方 SDK 的 baseUrl 则必须含 API 版本。
        return path != null && path.matches(".*/v\\d+$") ? base : base + "/v1";
    }
}
