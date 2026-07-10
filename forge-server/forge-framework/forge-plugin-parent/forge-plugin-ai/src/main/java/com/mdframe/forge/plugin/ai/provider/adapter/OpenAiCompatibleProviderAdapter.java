package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * OpenAI Compatible 供应商适配器。
 */
@Component
public class OpenAiCompatibleProviderAdapter implements AiProviderAdapter {

    @Override
    public String adapterCode() {
        return AiProviderAdapterCode.OPENAI_COMPATIBLE.getCode();
    }

    @Override
    public void validate(AiProvider provider, AiModelRuntimeOptions options) {
        validateCommon(provider, options);
        AiProviderBaseUrlPolicy.normalizeAndValidate(adapterCode(), provider.getBaseUrl());
    }

    @Override
    public ChatModel createChatModel(AiProvider provider, AiModelRuntimeOptions options) {
        String baseUrl = AiProviderBaseUrlPolicy.normalizeAndValidate(adapterCode(), provider.getBaseUrl());
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(provider.getApiKey())
                .build();
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder().model(options.model());
        if (options.temperature() != null) {
            optionsBuilder.temperature(options.temperature());
        }
        if (options.maxTokens() != null) {
            optionsBuilder.maxTokens(options.maxTokens());
        }
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(optionsBuilder.build())
                .build();
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
