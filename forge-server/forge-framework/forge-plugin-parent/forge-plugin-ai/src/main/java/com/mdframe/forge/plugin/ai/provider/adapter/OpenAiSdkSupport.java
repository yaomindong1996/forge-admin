package com.mdframe.forge.plugin.ai.provider.adapter;

import java.net.URI;
import java.time.Duration;

/** 不同 OpenAI SDK 模型统一的端点和请求边界，不改写用户已配置的 v1/v4 等版本路径。 */
public final class OpenAiSdkSupport {

    /** 向量/语音/图像等单次同步调用超时（避免长响应被过短超时误杀） */
    public static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    /** 对话/流式生成超时（推理模型与长文本输出通常需要数十秒） */
    public static final Duration CHAT_TIMEOUT = Duration.ofSeconds(120);

    private OpenAiSdkSupport() {
    }

    public static String baseUrl(String configuredUrl) {
        if (configuredUrl == null || configuredUrl.isBlank()) {
            return configuredUrl;
        }
        String base = configuredUrl.trim().replaceAll("/+$", "");
        String path = URI.create(base).getPath();
        // 旧 OpenAiApi 自动拼 /v1；官方 SDK 的 baseUrl 则必须含 API 版本。
        return path != null && path.matches(".*/v[0-9]+(?:beta)?(?:/openai)?$") ? base : base + "/v1";
    }
}
