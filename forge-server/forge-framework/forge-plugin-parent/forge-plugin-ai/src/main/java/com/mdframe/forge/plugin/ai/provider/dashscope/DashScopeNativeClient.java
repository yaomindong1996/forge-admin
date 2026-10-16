package com.mdframe.forge.plugin.ai.provider.dashscope;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.ai.provider.adapter.OpenAiSdkSupport;
import com.mdframe.forge.plugin.ai.provider.support.AiProviderRequestException;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/** 只调用通义原生协议，隔离 Spring AI Alibaba 里程碑依赖；错误正文不泄露到日志或客户端。 */
final class DashScopeNativeClient {

    static final ObjectMapper JSON = new ObjectMapper();
    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE =
            new ParameterizedTypeReference<>() { };
    private final RestClient rest;
    private final WebClient web;

    DashScopeNativeClient(String baseUrl, String apiKey) {
        String endpoint = baseUrl.replaceAll("/+$", "");
        if (!endpoint.endsWith("/api/v1")) {
            endpoint += "/api/v1";
        }
        HttpClient http = HttpClient.newBuilder().connectTimeout(OpenAiSdkSupport.REQUEST_TIMEOUT).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(OpenAiSdkSupport.REQUEST_TIMEOUT);
        rest = RestClient.builder().baseUrl(endpoint).requestFactory(factory)
                .defaultHeaders(headers -> headers.setBearerAuth(apiKey)).build();
        // 使用同一个有连接超时的 JDK 客户端，避免 WebClient 默认无限等待连接。
        web = WebClient.builder().baseUrl(endpoint)
                .clientConnector(new org.springframework.http.client.reactive.JdkClientHttpConnector(http))
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .defaultHeaders(headers -> headers.setBearerAuth(apiKey)).build();
    }

    JsonNode post(String path, JsonNode body) {
        try {
            String response = rest.post().uri(path).contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString()).retrieve()
                    .onStatus(status -> status.isError(), (request, result) -> {
                        throw new AiProviderRequestException("通义原生接口调用失败",
                                result.getStatusCode().value(), "http_error");
                    }).body(String.class);
            return parse(response);
        } catch (RestClientException ex) {
            throw transportFailure(ex);
        }
    }

    Flux<JsonNode> stream(String path, JsonNode body) {
        return web.post().uri(path).contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
                .header("X-DashScope-SSE", "enable").bodyValue(body.toString()).retrieve()
                .onStatus(status -> status.isError(), result -> result.releaseBody()
                        .thenReturn(new AiProviderRequestException("通义原生流式接口调用失败",
                                result.statusCode().value(), "http_error")))
                .bodyToFlux(SSE).timeout(OpenAiSdkSupport.REQUEST_TIMEOUT)
                .filter(event -> event.data() != null && !event.data().isBlank())
                .filter(event -> !"[DONE]".equals(event.data()))
                .map(event -> parse(event.data()))
                .onErrorMap(ex -> !(ex instanceof BusinessException),
                        DashScopeNativeClient::transportFailure);
    }

    private static AiProviderRequestException transportFailure(Throwable failure) {
        Throwable current = failure;
        for (int depth = 0; current != null && depth < 8; depth++, current = current.getCause()) {
            if (current instanceof TimeoutException || current instanceof HttpTimeoutException
                    || current instanceof SocketTimeoutException) {
                return new AiProviderRequestException("通义原生接口连接失败或超时", null, "timeout");
            }
        }
        return new AiProviderRequestException("通义原生接口连接失败或超时", null, "connection_error");
    }

    static JsonNode parse(String response) {
        try {
            JsonNode json = JSON.readTree(response == null ? "" : response);
            if (json == null || !json.isObject()) {
                throw new BusinessException("通义原生接口返回空结果");
            }
            if (!json.path("code").asText("").isBlank()
                    || json.path("status_code").asInt(200) >= 400) {
                throw new AiProviderRequestException("通义原生接口返回业务错误",
                        json.path("status_code").asInt(400), json.path("code").asText("unknown"));
            }
            return json;
        } catch (java.io.IOException ex) {
            throw new BusinessException("通义原生接口返回非法 JSON");
        }
    }
}
