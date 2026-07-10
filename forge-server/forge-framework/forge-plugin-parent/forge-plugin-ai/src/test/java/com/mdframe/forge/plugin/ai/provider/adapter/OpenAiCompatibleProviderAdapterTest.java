package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiCompatibleProviderAdapterTest {

    private final OpenAiCompatibleProviderAdapter adapter = new OpenAiCompatibleProviderAdapter();

    @Test
    void validateShouldRequireCompleteCompatibleConfiguration() {
        AiProvider provider = provider("https://api.example.com/v1");
        AiModelRuntimeOptions options = new AiModelRuntimeOptions("model-a", 0.5D, 256);

        adapter.validate(provider, options);

        provider.setBaseUrl(null);
        assertThrows(BusinessException.class, () -> adapter.validate(provider, options));
        provider.setBaseUrl("https://dashscope.aliyuncs.com");
        assertThrows(BusinessException.class, () -> adapter.validate(provider, options));
    }

    @Test
    void createChatModelShouldMapGenericOptions() {
        AiProvider provider = provider("https://api.example.com/v1");
        AiModelRuntimeOptions runtimeOptions = new AiModelRuntimeOptions("model-a", 0.5D, 256);

        ChatModel chatModel = adapter.createChatModel(provider, runtimeOptions);

        assertInstanceOf(OpenAiChatModel.class, chatModel);
        OpenAiChatOptions options = assertInstanceOf(OpenAiChatOptions.class, chatModel.getDefaultOptions());
        assertEquals("model-a", options.getModel());
        assertEquals(0.5D, options.getTemperature());
        assertEquals(256, options.getMaxTokens());
    }

    private AiProvider provider(String baseUrl) {
        AiProvider provider = new AiProvider();
        provider.setAdapterCode("openai_compatible");
        provider.setApiKey("test-openai-key");
        provider.setBaseUrl(baseUrl);
        return provider;
    }
}
