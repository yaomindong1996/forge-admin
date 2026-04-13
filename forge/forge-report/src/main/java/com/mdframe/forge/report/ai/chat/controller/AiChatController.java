package com.mdframe.forge.report.ai.chat.controller;

import com.mdframe.forge.report.ai.chat.dto.AIGenerateRequest;
import com.mdframe.forge.report.ai.chat.dto.ChatRequest;
import com.mdframe.forge.report.ai.chat.service.AiChatService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * AI 对话 Controller
 */
@Slf4j
@RestController
@RequestMapping("/goview/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService chatService;

    /**
     * AI 生成大屏（非流式）
     */
    @PostMapping("/generate")
    public RespInfo<String> generate(@RequestBody AIGenerateRequest request) {
        try {
            String result = chatService.generateDashboard(request.getPrompt());
            return RespInfo.success(result);
        } catch (Exception e) {
            log.error("AI 生成大屏失败", e);
            return RespInfo.error("AI 生成失败: " + e.getMessage());
        }
    }

    /**
     * AI 对话（SSE 流式输出）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody ChatRequest request) {
        return chatService.chatStream(request.getContent(), request.getAgentCode())
                .map(chunk -> ServerSentEvent.builder(chunk)
                        .event("message")
                        .build())
                .concatWith(Flux.just(ServerSentEvent.builder("[DONE]")
                        .event("done")
                        .build()))
                .onErrorResume(e -> {
                    log.error("AI 对话流式输出失败", e);
                    return Flux.just(ServerSentEvent.builder("错误: " + e.getMessage())
                            .event("error")
                            .build());
                });
    }
}
