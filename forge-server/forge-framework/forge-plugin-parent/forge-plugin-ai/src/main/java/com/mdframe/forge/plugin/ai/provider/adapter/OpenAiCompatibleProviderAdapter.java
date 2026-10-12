package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Spring AI 2 使用官方 OpenAI SDK，不再存在 OpenAiApi；供应商配置仍使用原兼容协议。 */
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
        validate(provider, options);
        String baseUrl = AiProviderBaseUrlPolicy.normalizeAndValidate(adapterCode(), provider.getBaseUrl());
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                .baseUrl(OpenAiSdkSupport.baseUrl(baseUrl)).apiKey(provider.getApiKey()).model(options.model())
                .timeout(OpenAiSdkSupport.REQUEST_TIMEOUT).maxRetries(0);
        if (options.temperature() != null) {
            builder.temperature(options.temperature());
        }
        if (options.maxTokens() != null) {
            builder.maxTokens(options.maxTokens());
        }
        // 不记录请求/响应正文或鉴权头，避免提示词、工具参数和密钥进入应用日志。
        return OpenAiChatModel.builder().options(builder.build()).build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(AiProvider provider, String model) {
        validate(provider, new AiModelRuntimeOptions(model, null, null));
        String baseUrl = AiProviderBaseUrlPolicy.normalizeAndValidate(adapterCode(), provider.getBaseUrl());
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .baseUrl(OpenAiSdkSupport.baseUrl(baseUrl)).apiKey(provider.getApiKey()).model(model)
                .timeout(OpenAiSdkSupport.REQUEST_TIMEOUT).maxRetries(0).build();
        return OpenAiEmbeddingModel.builder().options(options).build();
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
