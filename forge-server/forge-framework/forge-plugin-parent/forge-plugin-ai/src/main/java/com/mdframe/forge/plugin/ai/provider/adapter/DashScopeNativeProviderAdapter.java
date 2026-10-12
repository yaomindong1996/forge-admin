package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.plugin.ai.provider.dashscope.DashScopeNativeChatModel;
import com.mdframe.forge.plugin.ai.provider.dashscope.DashScopeNativeEmbeddingModel;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * DashScope 原生供应商适配器。
 */
@Component
public class DashScopeNativeProviderAdapter implements AiProviderAdapter {

    @Override
    public String adapterCode() {
        return AiProviderAdapterCode.DASHSCOPE_NATIVE.getCode();
    }

    @Override
    public void validate(AiProvider provider, AiModelRuntimeOptions options) {
        validateCommon(provider, options);
        AiProviderBaseUrlPolicy.normalizeAndValidate(adapterCode(), provider.getBaseUrl());
    }

    @Override
    public ChatModel createChatModel(AiProvider provider, AiModelRuntimeOptions options) {
        validate(provider, options);
        String baseUrl = AiProviderBaseUrlPolicy.normalizeAndValidate(adapterCode(), provider.getBaseUrl());
        var optionsBuilder = ToolCallingChatOptions.builder().model(options.model());
        if (options.temperature() != null) {
            optionsBuilder.temperature(options.temperature());
        }
        if (options.maxTokens() != null) {
            optionsBuilder.maxTokens(options.maxTokens());
        }
        // 使用原生 generation/SSE 协议；不依赖与 Spring AI 2 GA 不兼容的 Alibaba milestone。
        return new DashScopeNativeChatModel(baseUrl, provider.getApiKey(), optionsBuilder.build());
    }

    @Override
    public EmbeddingModel createEmbeddingModel(AiProvider provider, String model) {
        validateCommon(provider, new AiModelRuntimeOptions(model, null, null));
        String baseUrl = AiProviderBaseUrlPolicy.normalizeAndValidate(adapterCode(), provider.getBaseUrl());
        return new DashScopeNativeEmbeddingModel(baseUrl, provider.getApiKey(), model);
    }

    private void validateCommon(AiProvider provider, AiModelRuntimeOptions options) {
        if (provider == null) {
            throw new BusinessException("AI供应商配置不能为空");
        }
        if (!StringUtils.hasText(provider.getApiKey())) {
            throw new BusinessException("API Key不能为空");
        }
        if (options == null || !StringUtils.hasText(options.model())) {
            throw new BusinessException("模型标识不能为空");
        }
    }
}
