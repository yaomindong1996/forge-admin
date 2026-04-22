package com.mdframe.forge.plugin.ai.client.controller;

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

@Slf4j
@RestController
@RequestMapping("/ai/client")
@RequiredArgsConstructor
public class AiClientController {

    private final AiClient aiClient;

    @PostMapping("/call")
    public RespInfo<AiClientResponse> call(@RequestBody AiClientRequest request) {
        AiClientResponse response = aiClient.call(request);
        return RespInfo.success(response);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestBody AiClientRequest request) {
        return aiClient.stream(request)
                .map(chunk -> ServerSentEvent.builder(chunk)
                        .event("message")
                        .build())
                .concatWith(Flux.just(ServerSentEvent.builder("[DONE]")
                        .event("done")
                        .build()))
                .onErrorResume(e -> {
                    log.error("AiClient 流式调用失败", e);
                    return Flux.just(ServerSentEvent.builder("错误: " + e.getMessage())
                            .event("error")
                            .build());
                });
    }
}
