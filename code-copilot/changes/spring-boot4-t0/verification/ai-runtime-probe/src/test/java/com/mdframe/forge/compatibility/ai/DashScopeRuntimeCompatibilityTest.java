package com.mdframe.forge.compatibility.ai;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.mdframe.forge.plugin.ai.provider.adapter.AiModelRuntimeOptions;
import com.mdframe.forge.plugin.ai.provider.adapter.DashScopeNativeProviderAdapter;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.client.ChatClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/** 真实原生适配器与 AI 2 GA 的兼容性，不受旧 OpenAI API 编译错误遮挡。 */
@Timeout(15)
class DashScopeRuntimeCompatibilityTest {
    private final DashScopeNativeProviderAdapter adapter = new DashScopeNativeProviderAdapter();

    @Test
    void constructsActualNativeChatAndEmbeddingModels() {
        var provider = provider("http://127.0.0.1:1");
        var model = adapter.createChatModel(provider, new AiModelRuntimeOptions("t0-model", 0.3, 128));
        var options = assertInstanceOf(DashScopeChatOptions.class, model.getDefaultOptions());
        assertEquals("t0-model", options.getModel());
        assertEquals(0.3, options.getTemperature());
        assertEquals(128, options.getMaxTokens());
        assertNotNull(adapter.createEmbeddingModel(provider, "t0-embedding"));
    }

    @Test
    void callsActualNativeChatAgainstLocalProtocolFixture() throws IOException {
        try (var fixture = new LoopbackAiFixture()) {
            var model = adapter.createChatModel(provider(fixture.baseUrl()),
                    new AiModelRuntimeOptions("t0-model", 0.3, 128));
            assertEquals("dashscope-answer", ChatClient.create(model).prompt().user("t0-question").call().content());
            var request = fixture.request();
            assertNotNull(request);
            assertEquals("/api/v1/services/aigc/text-generation/generation", request.path());
            assertEquals("t0-model", request.body().path("model").asText());
        }
    }

    @Test
    void keepsNativeAndCompatibleModeSeparated() {
        assertThrows(BusinessException.class, () -> adapter.validate(
                provider("https://dashscope.aliyuncs.com/compatible-mode"),
                new AiModelRuntimeOptions("t0-model", null, null)));
    }

    private AiProvider provider(String baseUrl) {
        var provider = new AiProvider();
        provider.setBaseUrl(baseUrl);
        provider.setApiKey("t0-synthetic-not-a-real-credential");
        return provider;
    }
}
