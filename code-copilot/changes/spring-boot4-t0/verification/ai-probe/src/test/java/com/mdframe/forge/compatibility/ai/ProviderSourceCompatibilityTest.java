package com.mdframe.forge.compatibility.ai;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.mdframe.forge.plugin.ai.provider.adapter.AiModelRuntimeOptions;
import com.mdframe.forge.plugin.ai.provider.adapter.DashScopeNativeProviderAdapter;
import com.mdframe.forge.plugin.ai.provider.adapter.OpenAiCompatibleProviderAdapter;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 编译和调用真实适配器；只构造模型，不访问任何供应商。 */
class ProviderSourceCompatibilityTest {

    @Test
    void constructsCurrentOpenAiAdapterWithItsActualOptions() {
        OpenAiCompatibleProviderAdapter adapter = new OpenAiCompatibleProviderAdapter();
        AiProvider provider = provider("http://127.0.0.1:1");
        ChatModel model = adapter.createChatModel(provider, new AiModelRuntimeOptions("t0-model", 0.3, 128));
        OpenAiChatOptions options = assertInstanceOf(OpenAiChatOptions.class, model.getDefaultOptions());
        assertEquals("t0-model", options.getModel());
        assertEquals(0.3, options.getTemperature());
        assertEquals(128, options.getMaxTokens());
        assertNotNull(adapter.createEmbeddingModel(provider, "t0-embedding"));
    }

    @Test
    void constructsCurrentDashScopeAdapterWithItsActualOptions() {
        DashScopeNativeProviderAdapter adapter = new DashScopeNativeProviderAdapter();
        AiProvider provider = provider("http://127.0.0.1:1");
        ChatModel model = adapter.createChatModel(provider, new AiModelRuntimeOptions("t0-model", 0.3, 128));
        DashScopeChatOptions options = assertInstanceOf(DashScopeChatOptions.class, model.getDefaultOptions());
        assertEquals("t0-model", options.getModel());
        assertEquals(0.3, options.getTemperature());
        assertEquals(128, options.getMaxTokens());
        assertNotNull(adapter.createEmbeddingModel(provider, "t0-embedding"));
    }

    @Test
    void rejectsDashScopeCompatibleUrlForNativeAdapter() {
        DashScopeNativeProviderAdapter adapter = new DashScopeNativeProviderAdapter();
        assertThrows(BusinessException.class, () -> adapter.validate(
                provider("https://dashscope.aliyuncs.com/compatible-mode"),
                new AiModelRuntimeOptions("t0-model", null, null)));
    }

    @Test
    void retainsChatClientAndConversationAdvisorApi() {
        // 合成内存模型只验证 ChatClient / advisor 交互，不冒充真实模型能力。
        ChatModel model = prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage("t0-answer"))));
        ChatClient client = ChatClient.builder(model).build();
        assertEquals("t0-answer", client.prompt().user("t0-question").call().content());
        var memory = MessageWindowChatMemory.builder().build();
        ChatClient session = client.mutate().defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build()).build();
        assertEquals("t0-answer", session.prompt().user("t0-session-question")
                .advisors(advisor -> advisor.param(
                        org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID, "t0-session"))
                .call().content());
        assertEquals(2, memory.get("t0-session").size());
    }

    private AiProvider provider(String baseUrl) {
        AiProvider provider = new AiProvider();
        provider.setBaseUrl(baseUrl);
        provider.setApiKey("t0-synthetic-not-a-real-credential");
        return provider;
    }
}
