package com.mdframe.forge.plugin.ai.provider.adapter;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.ai.chat.model.ChatModel;
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
        String baseUrl = AiProviderBaseUrlPolicy.normalizeAndValidate(adapterCode(), provider.getBaseUrl());
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .baseUrl(baseUrl)
                .apiKey(provider.getApiKey())
                .build();
        DashScopeChatOptions.DashScopeChatOptionsBuilder optionsBuilder =
                DashScopeChatOptions.builder().model(options.model());
        if (options.temperature() != null) {
            optionsBuilder.temperature(options.temperature());
        }
        if (options.maxTokens() != null) {
            optionsBuilder.maxToken(options.maxTokens());
        }
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
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
