package com.mdframe.forge.compatibility.ai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/** 新 SDK API 路线验证，不替代 Forge 当前适配器的编译测试。 */
@Timeout(15)
class OpenAiSdkCompatibilityTest {
    private LoopbackAiFixture fixture;

    @BeforeEach
    void startFixture() throws IOException {
        fixture = new LoopbackAiFixture();
    }

    @AfterEach
    void closeFixture() {
        fixture.close();
    }

    @Test
    void callsChatWithNewSdkAndPreservesRuntimeOptions() {
        assertEquals("t0-answer", ChatClient.create(model()).prompt().user("t0-question").call().content());
        var request = assertNotNullRequest();
        assertEquals("/v1/chat/completions", request.path());
        assertEquals("t0-model", request.body().path("model").asText());
        assertEquals(0.3, request.body().path("temperature").asDouble());
        assertEquals(128, request.body().path("max_tokens").asInt());
    }

    @Test
    void streamsChatThroughLoopback() {
        var chunks = ChatClient.create(model()).prompt().user("t0-stream-question")
                .stream().content().collectList().block(Duration.ofSeconds(5));
        assertNotNull(chunks);
        assertEquals("t0-stream", String.join("", chunks));
        assertTrue(assertNotNullRequest().body().path("stream").asBoolean());
    }

    @Test
    void callsEmbeddingThroughNewSdk() {
        var options = OpenAiEmbeddingOptions.builder().baseUrl(fixture.baseUrl() + "/v1")
                .apiKey("t0-synthetic-not-a-real-credential").model("t0-embedding")
                .timeout(Duration.ofSeconds(3)).maxRetries(0).build();
        var embedding = OpenAiEmbeddingModel.builder().options(options).build();
        assertArrayEquals(new float[]{0.25f, 0.5f}, embedding.embed("t0-text"));
        var request = assertNotNullRequest();
        assertEquals("/v1/embeddings", request.path());
        assertEquals("t0-embedding", request.body().path("model").asText());
    }

    @Test
    void retainsConversationAdvisorWithNewModel() {
        var memory = MessageWindowChatMemory.builder().build();
        var client = ChatClient.builder(model())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build()).build();
        assertEquals("t0-answer", client.prompt().user("t0-session-question")
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, "t0-session"))
                .call().content());
        assertEquals(2, memory.get("t0-session").size());
    }

    private OpenAiChatModel model() {
        // SDK 的 baseUrl 包含 /v1；旧 API 的拼接逻辑不能直接照搬。
        var options = OpenAiChatOptions.builder().baseUrl(fixture.baseUrl() + "/v1")
                .apiKey("t0-synthetic-not-a-real-credential").model("t0-model")
                .temperature(0.3).maxTokens(128).timeout(Duration.ofSeconds(3)).maxRetries(0).build();
        return OpenAiChatModel.builder().options(options).build();
    }

    private LoopbackAiFixture.CapturedRequest assertNotNullRequest() {
        var request = fixture.request();
        assertNotNull(request);
        return request;
    }
}
