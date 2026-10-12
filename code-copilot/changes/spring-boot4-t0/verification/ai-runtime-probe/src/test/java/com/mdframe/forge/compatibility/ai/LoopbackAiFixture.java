package com.mdframe.forge.compatibility.ai;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** 只绑定 loopback 的协议桩。响应是合成数据，不调用真实 AI 服务。 */
final class LoopbackAiFixture implements AutoCloseable {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpServer server;
    private final Queue<CapturedRequest> requests = new ConcurrentLinkedQueue<>();

    LoopbackAiFixture() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    CapturedRequest request() {
        return requests.poll();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            JsonNode body = JSON.readTree(exchange.getRequestBody().readAllBytes());
            requests.add(new CapturedRequest(path, body));
            boolean stream = body.path("stream").asBoolean(false);
            String response = response(path, stream);
            boolean sse = path.equals("/v1/chat/completions") && stream;
            exchange.getResponseHeaders().set("Content-Type", sse ? "text/event-stream" : "application/json");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(response.equals("{}") ? 404 : 200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    private String response(String path, boolean stream) {
        return switch (path) {
            case "/v1/chat/completions" -> stream ? streamResponse() : """
                    {"id":"t0-chat","object":"chat.completion","created":1,"model":"t0-model",
                    "choices":[{"index":0,"message":{"role":"assistant","content":"t0-answer"},
                    "finish_reason":"stop"}],"usage":{"prompt_tokens":3,"completion_tokens":2,"total_tokens":5}}
                    """;
            case "/v1/embeddings" -> """
                    {"object":"list","data":[{"object":"embedding","index":0,"embedding":[0.25,0.5]}],
                    "model":"t0-embedding","usage":{"prompt_tokens":2,"total_tokens":2}}
                    """;
            case "/api/v1/services/aigc/text-generation/generation" -> """
                    {"request_id":"t0-dashscope","output":{"choices":[{"finish_reason":"stop",
                    "message":{"role":"assistant","content":"dashscope-answer"}}]},
                    "usage":{"input_tokens":3,"output_tokens":2,"total_tokens":5}}
                    """;
            default -> "{}";
        };
    }

    private String streamResponse() {
        String prefix = "data: {\"id\":\"t0-stream\",\"object\":\"chat.completion.chunk\","
                + "\"created\":1,\"model\":\"t0-model\",\"choices\":[{\"index\":0,";
        return prefix + "\"delta\":{\"role\":\"assistant\",\"content\":\"t0-stream\"},\"finish_reason\":null}]}\n\n"
                + prefix + "\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n"
                + "data: [DONE]\n\n";
    }

    @Override
    public void close() {
        server.stop(0);
    }

    record CapturedRequest(String path, JsonNode body) { }
}
