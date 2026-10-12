package com.mdframe.forge.plugin.ai.provider.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** 从 T0 协议 fixture 增量扩展，仅 loopback 合成响应；不代表真实供应商验收。 */
final class AiLoopbackFixture implements AutoCloseable {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final HttpServer server;
    private final BlockingQueue<Request> requests = new LinkedBlockingQueue<>();
    private final Queue<Response> overrides = new ConcurrentLinkedQueue<>();
    private final Queue<CountDownLatch> gates = new ConcurrentLinkedQueue<>();

    AiLoopbackFixture() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    Request request() {
        return requests.poll();
    }

    Request awaitRequest() throws InterruptedException {
        return requests.poll(3, TimeUnit.SECONDS);
    }

    void respondOnce(int status, String body, boolean sse) {
        overrides.add(new Response(status, body, sse));
    }

    void pauseOnce(CountDownLatch gate) {
        gates.add(gate);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            JsonNode body = contentType != null && contentType.startsWith("multipart/")
                    ? JSON.createObjectNode().put("rawPayload", raw).put("contentType", contentType)
                    : JSON.readTree(raw);
            requests.add(new Request(path, body, exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("X-DashScope-SSE")));
            awaitGate();
            Response response = overrides.poll();
            if (response == null) {
                response = defaultResponse(path, body);
            }
            exchange.getResponseHeaders().set("Content-Type", response.contentType());
            byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(response.status(), bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    private void awaitGate() throws IOException {
        CountDownLatch gate = gates.poll();
        if (gate == null) {
            return;
        }
        try {
            if (!gate.await(10, TimeUnit.SECONDS)) {
                throw new IOException("fixture gate timed out");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("fixture interrupted", ex);
        }
    }

    private Response defaultResponse(String path, JsonNode request) {
        if (path.endsWith("/images/generations")) {
            return new Response(200,
                    "{\"created\":1,\"data\":[{\"url\":\"https://fixture.invalid/image.png\"}]}", false);
        }
        if (path.endsWith("/audio/transcriptions")) {
            // 真接口按 response_format=text 返回纯文本，不能用 JSON fixture 伪装该响应。
            return new Response(200, "fixture transcription", false, "text/plain");
        }
        if (path.endsWith("/audio/speech")) {
            return new Response(200, "fixture audio", false, "audio/mpeg");
        }
        if (path.endsWith("/chat/completions")) {
            return request.path("stream").asBoolean() ? new Response(200, openAiStream(), true)
                    : new Response(200, """
                    {"id":"t1-chat","object":"chat.completion","created":1,"model":"t1-model",
                    "choices":[{"index":0,"message":{"role":"assistant","content":"t1-answer"},
                    "finish_reason":"stop"}],"usage":{"prompt_tokens":3,"completion_tokens":2,"total_tokens":5}}
                    """, false);
        }
        if (path.endsWith("/embeddings")) {
            return new Response(200, """
                    {"object":"list","data":[{"object":"embedding","index":0,"embedding":[0.25,0.5]}],
                    "model":"t1-embedding","usage":{"prompt_tokens":2,"total_tokens":2}}
                    """, false);
        }
        if (path.endsWith("/text-embedding/text-embedding")) {
            var response = JSON.createObjectNode();
            var embeddings = response.putObject("output").putArray("embeddings");
            // 故意倒序，验证适配器必须按 text_index 排序，不能相信返回数组顺序。
            for (int i = request.path("input").path("texts").size() - 1; i >= 0; i--) {
                embeddings.addObject().put("text_index", i).putArray("embedding").add(0.25).add(0.5);
            }
            response.putObject("usage").put("total_tokens", 2);
            return new Response(200, response.toString(), false);
        }
        if (path.endsWith("/generation")) {
            boolean stream = request.path("parameters").path("incremental_output").asBoolean();
            String response = nativeMessage("native-answer", "stop");
            return new Response(200, stream ? "data: " + response + "\n\n" : response, stream);
        }
        return new Response(404, "{}", false);
    }

    static String nativeMessage(String content, String finish) {
        return "{\"request_id\":\"t1-native\",\"output\":{\"choices\":[{\"finish_reason\":\""
                + finish + "\",\"message\":{\"role\":\"assistant\",\"content\":\"" + content
                + "\"}}]},\"usage\":{\"input_tokens\":3,\"output_tokens\":2,\"total_tokens\":5}}";
    }

    static String nativeToolFrame(String finish, String id, String name, String arguments) {
        return nativeToolFrame(0, finish, id, name, arguments);
    }

    static String nativeToolFrame(int index, String finish, String id, String name, String arguments) {
        var response = JSON.createObjectNode();
        var choice = response.putObject("output").putArray("choices").addObject().put("finish_reason", finish);
        var call = choice.putObject("message").put("content", "").putArray("tool_calls").addObject();
        call.put("index", index).put("id", id);
        call.putObject("function").put("name", name).put("arguments", arguments);
        return "data: " + response + "\n\n";
    }

    static String openAiToolFrame(int index, String finish, String id, String name, String arguments) {
        var response = JSON.createObjectNode().put("id", "fixture-tool-response")
                .put("object", "chat.completion.chunk").put("created", 1).put("model", "fixture-model");
        var choice = response.putArray("choices").addObject().put("index", 0);
        if (finish == null) {
            choice.putNull("finish_reason");
        } else {
            choice.put("finish_reason", finish);
        }
        var call = choice.putObject("delta").put("role", "assistant").putArray("tool_calls").addObject();
        call.put("index", index).put("id", id).put("type", "function");
        call.putObject("function").put("name", name).put("arguments", arguments);
        return "data: " + response + "\n\n";
    }

    private String openAiStream() {
        String prefix = "data: {\"id\":\"t1-stream\",\"object\":\"chat.completion.chunk\","
                + "\"created\":1,\"model\":\"t1-model\",\"choices\":[{\"index\":0,";
        return prefix + "\"delta\":{\"role\":\"assistant\",\"content\":\"t1-stream\"},\"finish_reason\":null}]}\n\n"
                + prefix + "\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n" + "data: [DONE]\n\n";
    }

    @Override
    public void close() {
        server.stop(0);
    }

    record Request(String path, JsonNode body, String authorization, String sseHeader) { }
    record Response(int status, String body, boolean sse, String contentType) {
        Response(int status, String body, boolean sse) {
            this(status, body, sse, sse ? "text/event-stream" : "application/json");
        }
    }
}
