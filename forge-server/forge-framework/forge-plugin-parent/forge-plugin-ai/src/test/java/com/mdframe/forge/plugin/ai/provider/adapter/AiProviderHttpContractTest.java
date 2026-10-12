package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.support.AiProviderFailureDiagnostics;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** 测试实际 Forge 适配器，而非重新构造一个看似兼容的 SDK PoC。 */
@Timeout(15)
class AiProviderHttpContractTest {

    private AiLoopbackFixture fixture;
    private final OpenAiCompatibleProviderAdapter openAi = new OpenAiCompatibleProviderAdapter();
    private final DashScopeNativeProviderAdapter nativeAdapter = new DashScopeNativeProviderAdapter();
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
    void nativeChatKeepsNativeProtocolAndUsage() {
        var response = nativeAdapter.createChatModel(provider("dashscope_native", ""), runtime)
                .call(new Prompt("question"));
        assertEquals("native-answer", response.getResult().getOutput().getText());
        assertEquals(5, response.getMetadata().getUsage().getTotalTokens());
        var request = assertRequest("/api/v1/services/aigc/text-generation/generation");
        assertEquals("question", request.body().path("input").path("messages").get(0).path("content").asText());
        assertEquals(128, request.body().path("parameters").path("max_tokens").asInt());
        assertEquals(0.3, request.body().path("parameters").path("temperature").asDouble());
        assertFalse(request.body().has("messages"));
    }

    @Test
    void nativeSseUsesIncrementalOutputAndNativeHeader() {
        var chunks = ChatClient.create(nativeAdapter.createChatModel(provider("dashscope_native", ""), runtime))
                .prompt().user("question").stream().content().collectList().block(Duration.ofSeconds(5));
        assertNotNull(chunks);
        assertEquals("native-answer", String.join("", chunks));
        var request = assertRequest("/api/v1/services/aigc/text-generation/generation");
        assertEquals("enable", request.sseHeader());
        assertTrue(request.body().path("parameters").path("incremental_output").asBoolean());
    }

    @Test
    void nativeEmbeddingBatchesAndSortsReturnedIndices() {
        var model = nativeAdapter.createEmbeddingModel(provider("dashscope_native", ""), "text-embedding-v4");
        var vectors = model.embed(IntStream.range(0, 12).mapToObj(i -> "text-" + i).toList());
        assertEquals(12, vectors.size());
        vectors.forEach(vector -> assertArrayEquals(new float[]{0.25f, 0.5f}, vector));
        assertEquals(10, assertRequest("/api/v1/services/embeddings/text-embedding/text-embedding")
                .body().path("input").path("texts").size());
        assertEquals(2, assertRequest("/api/v1/services/embeddings/text-embedding/text-embedding")
                .body().path("input").path("texts").size());
    }

    @Test
    void nativeEmbeddingRejectsDuplicateOrMissingIndices() {
        fixture.respondOnce(200, """
                {"output":{"embeddings":[{"text_index":0,"embedding":[1]},
                {"text_index":0,"embedding":[2]}]}}
                """, false);
        var model = nativeAdapter.createEmbeddingModel(provider("dashscope_native", ""), "text-embedding-v4");
        assertThrows(BusinessException.class, () -> model.embed(List.of("one", "two")));
    }

    @Test
    void nativeToolDeclarationsAndToolResultMessagesRemainIntact() {
        var options = ToolCallingChatOptions.builder().toolCallbacks(callback(new AtomicInteger())).build();
        var toolResult = ToolResponseMessage.builder().responses(List.of(
                new ToolResponseMessage.ToolResponse("call-1", "lookup", "result"))).build();
        nativeAdapter.createChatModel(provider("dashscope_native", ""), runtime)
                .call(new Prompt(List.of(new UserMessage("question"), toolResult), options));
        var request = assertRequest("/api/v1/services/aigc/text-generation/generation");
        assertEquals("lookup", request.body().path("parameters").path("tools").get(0)
                .path("function").path("name").asText());
        var result = request.body().path("input").path("messages").get(1);
        assertEquals("tool", result.path("role").asText());
        assertEquals("call-1", result.path("tool_call_id").asText());
    }

    @Test
    void nativeMediaUsesMultimodalEndpoint() {
        var message = UserMessage.builder().text("describe").media(new Media(MimeTypeUtils.IMAGE_PNG,
                URI.create("https://synthetic.example/fixture.png"))).build();
        nativeAdapter.createChatModel(provider("dashscope_native", ""), runtime).call(new Prompt(message));
        var request = assertRequest("/api/v1/services/aigc/multimodal-generation/generation");
        assertEquals("https://synthetic.example/fixture.png", request.body().path("input").path("messages")
                .get(0).path("content").get(1).path("image").asText());
    }

    @Test
    void nativeHttpAndBusinessErrorsDoNotExposeUpstreamSecrets() {
        var model = nativeAdapter.createChatModel(provider("dashscope_native", ""), runtime);
        fixture.respondOnce(401, "{\"message\":\"synthetic-sensitive-body\"}", false);
        assertFalse(assertThrows(BusinessException.class, () -> model.call(new Prompt("question")))
                .getMessage().contains("synthetic-sensitive-body"));
        fixture.respondOnce(200, "{\"code\":\"InvalidApiKey\",\"message\":\"synthetic-sensitive-body\"}", false);
        assertThrows(BusinessException.class, () -> model.call(new Prompt("question")));
    }

    @Test
    void nativeSseErrorIsNotTreatedAsSuccessfulEmptyOutput() {
        fixture.respondOnce(200, "data: {\"code\":\"RateLimit\",\"message\":\"synthetic\"}\n\n", true);
        var model = nativeAdapter.createChatModel(provider("dashscope_native", ""), runtime);
        assertThrows(BusinessException.class, () -> model.stream(new Prompt("question"))
                .collectList().block(Duration.ofSeconds(5)));
    }

    @Test
    void nativeToolFragmentsProduceOneCompleteCallPerIndex() {
        fixture.respondOnce(200, AiLoopbackFixture.nativeToolFrame("null", "call-1", "lookup", "{\"")
                + AiLoopbackFixture.nativeToolFrame("tool_calls", "", "", "x\":1}"), true);
        var model = nativeAdapter.createChatModel(provider("dashscope_native", ""), runtime);
        var calls = model.stream(new Prompt("question"))
                .flatMapIterable(response -> response.getResult().getOutput().getToolCalls())
                .collectList().block(Duration.ofSeconds(5));
        assertNotNull(calls);
        assertEquals(1, calls.size());
        assertEquals("call-1", calls.get(0).id());
        assertEquals("lookup", calls.get(0).name());
        assertEquals("{\"x\":1}", calls.get(0).arguments());
    }

    @Test
    void nativeModelTimesOutInsteadOfWaitingIndefinitely() {
        CountDownLatch release = new CountDownLatch(1);
        fixture.pauseOnce(release);
        try {
            var model = nativeAdapter.createChatModel(provider("dashscope_native", ""), runtime);
            BusinessException error = assertThrows(BusinessException.class, () -> model.call(new Prompt("question")));
            assertTrue(error.getMessage().contains("超时"));
        } finally {
            release.countDown();
        }
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
