package com.mdframe.forge.plugin.ai.provider.adapter;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DashScopeNativeProviderAdapterTest {

    private final DashScopeNativeProviderAdapter adapter = new DashScopeNativeProviderAdapter();

    @Test
    void validateShouldAllowDefaultBaseUrlAndRejectCompatibleMode() {
        AiProvider provider = provider(null);
        AiModelRuntimeOptions options = new AiModelRuntimeOptions("qwen-plus", 0.7D, 512);

        adapter.validate(provider, options);

        provider.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode");
        assertThrows(BusinessException.class, () -> adapter.validate(provider, options));
    }

    @Test
    void createChatModelShouldMapDashScopeOptions() {
        AiProvider provider = provider("https://dashscope.aliyuncs.com");
        AiModelRuntimeOptions runtimeOptions = new AiModelRuntimeOptions("qwen-plus", 0.7D, 512);

        ChatModel chatModel = adapter.createChatModel(provider, runtimeOptions);

        assertInstanceOf(DashScopeChatModel.class, chatModel);
        DashScopeChatOptions options = assertInstanceOf(DashScopeChatOptions.class,
                chatModel.getDefaultOptions());
        assertInstanceOf(ToolCallingChatOptions.class, options);
        assertEquals("qwen-plus", options.getModel());
        assertEquals(0.7D, options.getTemperature());
        assertEquals(512, options.getMaxTokens());
    }

    private AiProvider provider(String baseUrl) {
        AiProvider provider = new AiProvider();
        provider.setAdapterCode("dashscope_native");
        provider.setApiKey("test-dashscope-key");
        provider.setBaseUrl(baseUrl);
        return provider;
    }
}
