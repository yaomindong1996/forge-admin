package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.support.AiProviderFailureDiagnostics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 OpenAI Compatible 实际适配器的 HTTP 契约。
 */
@Timeout(15)
class AiProviderHttpContractTest {

    private AiLoopbackFixture fixture;
    private final OpenAiCompatibleProviderAdapter openAi = new OpenAiCompatibleProviderAdapter();
    private final AiModelRuntimeOptions runtime = new AiModelRuntimeOptions("t1-model", 0.3, 128);

    @BeforeEach
    void start() throws IOException {
        fixture = new AiLoopbackFixture();
    }

    @AfterEach
    void stop() {
        fixture.close();
    }

    @Test
    void openAiChatUsesConfiguredVersionPathAndOptions() {
        var model = openAi.createChatModel(provider("openai_compatible", "/v1"), runtime);
        assertEquals("t1-answer", ChatClient.create(model).prompt().user("question").call().content());
        var request = assertRequest("/v1/chat/completions");
        assertEquals(0.3, request.body().path("temperature").asDouble());
        assertEquals(128, request.body().path("max_tokens").asInt());
    }

    @Test
    void openAiStreamsAndEmbedsThroughActualAdapter() {
        var provider = provider("openai_compatible", "");
        var chunks = openAi.createChatModel(provider, runtime).stream(new Prompt("question"))
                .map(response -> response.getResult().getOutput().getText())
                .collectList().block(Duration.ofSeconds(5));
        assertNotNull(chunks);
        assertEquals("t1-stream", String.join("", chunks));
        assertTrue(assertRequest("/v1/chat/completions").body().path("stream").asBoolean());
        assertArrayEquals(new float[]{0.25f, 0.5f},
                openAi.createEmbeddingModel(provider, "t1-embedding").embed("text"));
        assertEquals("t1-embedding", assertRequest("/v1/embeddings").body().path("model").asText());
    }

    @Test
    void openAiDeclaresToolsButDoesNotExecuteReactLoopTools() {
        AtomicInteger executions = new AtomicInteger();
        fixture.respondOnce(200, """
                {"id":"tools","object":"chat.completion","created":1,"model":"t1-model",
                "choices":[{"index":0,"finish_reason":"tool_calls","message":{"role":"assistant",
                "content":null,"tool_calls":[{"id":"call-1","type":"function","function":
                {"name":"lookup","arguments":"{}"}}]}}]}
                """, false);
        var options = OpenAiChatOptions.builder().toolCallbacks(callback(executions)).build();
        var response = openAi.createChatModel(provider("openai_compatible", ""), runtime)
                .call(new Prompt("question", options));
        assertTrue(response.hasToolCalls());
        assertEquals("lookup", response.getResult().getOutput().getToolCalls().get(0).name());
        assertEquals(0, executions.get());
        assertEquals("lookup", assertRequest("/v1/chat/completions")
                .body().path("tools").get(0).path("function").path("name").asText());
    }

    @Test
    void openAiUpstreamErrorIsPropagatedWithoutAutomaticRetries() {
        fixture.respondOnce(503,
                "{\"error\":{\"message\":\"synthetic unavailable\",\"type\":\"server_error\"}}", false);
        var model = openAi.createChatModel(provider("openai_compatible", ""), runtime);
        var error = assertThrows(RuntimeException.class, () -> model.call(new Prompt("question")));
        assertEquals(503, AiProviderFailureDiagnostics.from(error).httpStatus());
        assertRequest("/v1/chat/completions");
        assertNull(fixture.request());
    }

    @Test
    void endpointNormalizationDoesNotDuplicateApiVersions() {
        assertEquals("https://synthetic.example/v1", OpenAiSdkSupport.baseUrl("https://synthetic.example/v1/"));
        assertEquals("https://synthetic.example/api/paas/v4",
                OpenAiSdkSupport.baseUrl("https://synthetic.example/api/paas/v4"));
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1",
                OpenAiSdkSupport.baseUrl("https://dashscope.aliyuncs.com/compatible-mode"));
    }

    private AiLoopbackFixture.Request assertRequest(String path) {
        var request = fixture.request();
        assertNotNull(request);
        assertEquals(path, request.path());
        assertEquals("Bearer t1-synthetic-not-a-real-key", request.authorization());
        return request;
    }

    private AiProvider provider(String adapter, String suffix) {
        AiProvider provider = new AiProvider();
        provider.setAdapterCode(adapter);
        provider.setApiKey("t1-synthetic-not-a-real-key");
        provider.setBaseUrl(fixture.baseUrl() + suffix);
        return provider;
    }

    private ToolCallback callback(AtomicInteger executions) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("lookup").description("synthetic lookup")
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String input) {
                executions.incrementAndGet();
                return "result";
            }
        };
    }
}
