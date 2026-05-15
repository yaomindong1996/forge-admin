package com.mdframe.forge.plugin.ai.client.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.ai.client.AiClient;
import com.mdframe.forge.plugin.ai.client.dto.AiClientRequest;
import com.mdframe.forge.plugin.ai.client.dto.AiClientResponse;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai/client")
@RequiredArgsConstructor
public class AiClientController {

    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    @PostMapping("/call")
    public RespInfo<AiClientResponse> call(@RequestBody AiClientRequest request) {
        AiClientResponse response = aiClient.call(request);
        return RespInfo.success(response);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestBody AiClientRequest request) {
        return Flux.concat(
                Flux.just(buildProgressEvent("connecting", "正在连接模型...")),
                aiClient.stream(request).map(this::buildChunkEvent),
                Flux.just(buildCompleteEvent(request.getSessionId()))
        )
                .onErrorResume(e -> {
                    log.error("AiClient 流式调用失败", e);
                    return Flux.just(buildErrorEvent("错误: " + e.getMessage()));
                });
    }

    private ServerSentEvent<String> buildProgressEvent(String stage, String message) {
        Map<String, String> data = new HashMap<>();
        data.put("stage", stage);
        data.put("message", message);
        return ServerSentEvent.builder(toJson(data))
                .event("progress")
                .build();
    }

    private ServerSentEvent<String> buildChunkEvent(String chunk) {
        Map<String, String> data = new HashMap<>();
        data.put("content", chunk);
        return ServerSentEvent.builder(toJson(data))
                .event("chunk")
                .build();
    }

    private ServerSentEvent<String> buildCompleteEvent(String sessionId) {
        Map<String, String> data = new HashMap<>();
        data.put("sessionId", sessionId == null ? "" : sessionId);
        data.put("message", "生成完成");
        return ServerSentEvent.builder(toJson(data))
                .event("complete")
                .build();
    }

    private ServerSentEvent<String> buildErrorEvent(String message) {
        Map<String, String> data = new HashMap<>();
        data.put("message", message);
        return ServerSentEvent.builder(toJson(data))
                .event("error")
                .build();
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{\"message\":\"JSON序列化失败\"}";
        }
    }
}
