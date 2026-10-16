package com.mdframe.forge.plugin.ai.model.adapter;

import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import com.mdframe.forge.plugin.ai.provider.adapter.OpenAiSdkSupport;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI 兼容协议的 Embedding 模型适配器。
 * 覆盖 OpenAI / Azure / DashScope 兼容 / 硅基流动等供应商。
 */
@Slf4j
@Component
public class OpenAiCompatibleEmbeddingModelAdapter implements AiEmbeddingModelAdapter {

    /**
     * 每次批量嵌入的文本条数。
     * 大文档有数百个 chunk，逐条 HTTP 调用太慢，按批调用 OpenAI 兼容 embeddings 接口。
     */
    private static final int BATCH_SIZE = 20;

    @Override
    public String getSupportedProvider() {
        return "openai_compatible";
    }

    @Override
    public boolean supports(String modelKey) {
        if (modelKey == null) {
            return false;
        }
        String lower = modelKey.toLowerCase();
        return lower.startsWith("text-embedding")
                || lower.startsWith("embedding-")
                || lower.startsWith("bge-")
                || lower.contains("embed");
    }

    @Override
    public List<List<Float>> embed(String baseUrl, String apiKey, String model, List<String> texts) {
        String effectiveBaseUrl = normalizeDashScopeBaseUrl(baseUrl);
        try {
            OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                    .baseUrl(OpenAiSdkSupport.baseUrl(effectiveBaseUrl)).apiKey(apiKey).model(model)
                    .timeout(OpenAiSdkSupport.REQUEST_TIMEOUT).maxRetries(0)
                    .build();
            OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder().options(options).build();

            // 分批调用 embed(List<String>)，每批 BATCH_SIZE 条，返回顺序与输入一致
            List<List<Float>> result = new ArrayList<>(texts.size());
            for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
                List<String> batch = texts.subList(i, Math.min(i + BATCH_SIZE, texts.size()));
                List<float[]> vectors = embeddingModel.embed(batch);
                for (float[] vector : vectors) {
                    List<Float> floatList = new ArrayList<>(vector.length);
                    for (float v : vector) {
                        floatList.add(v);
                    }
                    result.add(floatList);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("[AI Embedding] 调用失败, baseUrl={}, model={}, error={}", effectiveBaseUrl, model, e.getMessage());
            throw new BusinessException("Embedding模型调用失败: " + e.getMessage());
        }
    }

    /**
     * DashScope 的 OpenAI 兼容 Embedding 端点是 {host}/compatible-mode/v1/embeddings，
     * 而供应商配置的 baseUrl 通常是原生域名（https://dashscope.aliyuncs.com）。
     * 命中 DashScope 域名且路径未带 /compatible-mode 时，先补前缀，再由 SDK 地址策略追加 /v1，
     * 避免将兼容嵌入请求发往原生路径导致 404。
     */
    private String normalizeDashScopeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return baseUrl;
        }
        try {
            java.net.URI uri = java.net.URI.create(baseUrl.trim());
            String host = uri.getHost();
            if (host != null
                    && (host.equals("dashscope.aliyuncs.com") || host.equals("dashscope-intl.aliyuncs.com"))
                    && (uri.getPath() == null || !uri.getPath().contains("/compatible-mode"))) {
                return "https://" + host + "/compatible-mode";
            }
        } catch (Exception ignored) {
            // 非法 URL 保持原样，交由 SDK 地址校验处理。
        }
        return baseUrl;
    }
}
