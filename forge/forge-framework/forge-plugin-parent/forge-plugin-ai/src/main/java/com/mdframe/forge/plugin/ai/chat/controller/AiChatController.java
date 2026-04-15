package com.mdframe.forge.plugin.ai.chat.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatRecord;
import com.mdframe.forge.plugin.ai.chat.dto.AIGenerateRequest;
import com.mdframe.forge.plugin.ai.chat.dto.ChatRequest;
import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import com.mdframe.forge.plugin.ai.chat.service.AiChatService;
import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 对话 Controller
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService chatService;
    private final AiChatSessionService sessionService;
    private final AiChatRecordService recordService;

    /**
     * AI 生成大屏（非流式）
     */
    @PostMapping("/generate")
    public RespInfo<String> generate(@RequestBody AIGenerateRequest request) {
        try {
            String result = chatService.generateDashboard(request);
            return RespInfo.success(result);
        } catch (Exception e) {
            log.error("AI 生成大屏失败", e);
            return RespInfo.error("AI 生成失败: " + e.getMessage());
        }
    }

    /**
     * AI 生成大屏（SSE 流式输出）
     */
    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generateStream(@RequestBody AIGenerateRequest request) {
        return chatService.generateDashboardStream(request)
                .map(chunk -> ServerSentEvent.builder(chunk)
                        .event("message")
                        .build())
                .concatWith(Flux.just(ServerSentEvent.builder("[DONE]")
                        .event("done")
                        .build()))
                .onErrorResume(e -> {
                    log.error("AI 流式生成大屏失败", e);
                    return Flux.just(ServerSentEvent.builder("错误: " + e.getMessage())
                            .event("error")
                            .build());
                });
    }

    /**
     * AI 对话（SSE 流式输出，支持多轮上下文）
     * 请求中需传入 sessionId（同一会话始终发相同值，新对话可不传或传新UUID）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody ChatRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return chatService.chatStream(request.getContent(), request.getAgentCode(),
                        request.getSessionId(), userId, request.getProviderId(),
                        request.getModelName(), request.getTemperature(), request.getMaxTokens(),
                        request.getProjectName(), request.getCanvasContext())
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

    // ==================== 会话管理接口 ====================

    /**
     * 获取当前用户的历史会话列表
     */
    @GetMapping("/session/list")
    public RespInfo<List<AiChatSession>> sessionList() {
        Long userId = StpUtil.getLoginIdAsLong();
        return RespInfo.success(sessionService.listByUser(userId));
    }

    /**
     * 获取指定会话的对话明细
     */
    @GetMapping("/session/{sessionId}/messages")
    public RespInfo<List<AiChatRecord>> sessionMessages(@PathVariable String sessionId) {
        return RespInfo.success(recordService.listBySession(sessionId));
    }

    /**
     * 删除会话（软删除， status=1）
     */
    @DeleteMapping("/session/{sessionId}")
    public RespInfo<Void> deleteSession(@PathVariable String sessionId) {
        sessionService.deleteSession(sessionId);
        return RespInfo.success();
    }
}
