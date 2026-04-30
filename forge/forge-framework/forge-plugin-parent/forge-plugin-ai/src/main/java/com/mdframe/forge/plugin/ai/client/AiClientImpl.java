package com.mdframe.forge.plugin.ai.client;

import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatRecord;
import com.mdframe.forge.plugin.ai.chat.memory.DbChatMemory;
import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import com.mdframe.forge.plugin.ai.chat.service.AiPromptTemplateRenderer;
import com.mdframe.forge.plugin.ai.client.dto.AiClientRequest;
import com.mdframe.forge.plugin.ai.client.dto.AiClientResponse;
import com.mdframe.forge.plugin.ai.client.dto.AiFallbackReason;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiClientImpl implements AiClient {

    private final AiInvocationResolver invocationResolver;
    private final DbChatMemory dbChatMemory;
    private final AiChatRecordService recordService;
    private final AiChatSessionService sessionService;
    private final ChatClientCache chatClientCache;
    private final CircuitBreaker circuitBreaker;
    private final ContextInjector contextInjector;

    @Override
    public AiClientResponse call(AiClientRequest request) {
        String circuitKey = request.getAgentCode();
        if (circuitBreaker.isOpen(circuitKey)) {
            log.warn("[AiClient] 熔断中, agentCode={}", request.getAgentCode());
            return AiClientResponse.fallback(null, AiFallbackReason.API_ERROR, request.getSessionId());
        }

        try {
            AiInvocationResolver.ResolvedInvocation resolved = invocationResolver.resolve(
                    request.getAgentCode(), request.getProviderId(), request.getModelName(),
                    request.getTemperature(), request.getMaxTokens());

            String systemPrompt = buildSystemPrompt(resolved.agent(), request.getContextVars());
            String sessionId = resolveSessionId(request.getSessionId());
            String historySessionId = StringUtils.hasText(sessionId) ? sessionId : null;

            OpenAiChatOptions options = buildOptions(resolved.model(), resolved.temperature(), resolved.maxTokens());
            ChatClient baseClient = chatClientCache.getOrCreateBase(
                    resolved.provider().getId(), resolved.model(), resolved.provider(), options);
            ChatClient chatClient = chatClientCache.createSessionClient(
                    baseClient, historySessionId, dbChatMemory);

            log.info("[AiClient.call] ===== 请求开始 =====");
            log.info("[AiClient.call] sessionId: {}", sessionId);
            log.info("[AiClient.call] agentCode: {}", request.getAgentCode());
            log.info("[AiClient.call] provider: {}({})", resolved.provider().getProviderName(), resolved.provider().getProviderType());
            log.info("[AiClient.call] model: {}", resolved.model());
            log.info("[AiClient.call] temperature: {}, maxTokens: {}", resolved.temperature(), resolved.maxTokens());
            log.info("[AiClient.call] systemPrompt: \n{}", systemPrompt);
            log.info("[AiClient.call] userPrompt: \n{}", request.getMessage());

            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user(request.getMessage())
                    .call()
                    .content();

            circuitBreaker.recordSuccess(circuitKey);

            log.info("[AiClient.call] ===== 响应结束 =====");
            log.info("[AiClient.call] assistantContent(length={}): \n{}", content.length(),
                    content.length() > 500 ? content.substring(0, 500) + "...(truncated)" : content);

            if (StringUtils.hasText(sessionId)) {
                ensureSession(sessionId, request.getAgentCode(), request.getUserInputOrMessage(),
                        SessionHelper.getUserId(), SessionHelper.getTenantId());
                persistConversation(sessionId, request.getAgentCode(),
                        request.getMessage(), content,SessionHelper.getUserId(),SessionHelper.getTenantId());
            }

            return AiClientResponse.success(content, sessionId);
        } catch (BusinessException e) {
            log.warn("[AiClient] 业务异常, agentCode={}, msg={}", request.getAgentCode(), e.getMessage());
            return handleBusinessException(e, request);
        } catch (Exception e) {
            log.error("[AiClient] AI调用失败, agentCode={}", request.getAgentCode(), e);
            circuitBreaker.recordFailure(circuitKey);
            return AiClientResponse.fallback(null, AiFallbackReason.API_ERROR, request.getSessionId());
        }
    }

@Override
    public Flux<String> stream(AiClientRequest request) {
        String circuitKey = request.getAgentCode();
        if (circuitBreaker.isOpen(circuitKey)) {
            log.warn("[AiClient] 熔断中(流式), agentCode={}", request.getAgentCode());
            return Flux.just("{\"fallback\":true,\"reason\":\"CIRCUIT_OPEN\"}");
        }

        try {
            AiInvocationResolver.ResolvedInvocation resolved = invocationResolver.resolve(
                    request.getAgentCode(), request.getProviderId(), request.getModelName(),
                    request.getTemperature(), request.getMaxTokens());

            String systemPrompt = buildSystemPrompt(resolved.agent(), request.getContextVars());
            String sessionId = resolveSessionId(request.getSessionId());
            String historySessionId = StringUtils.hasText(sessionId) ? sessionId : null;

            OpenAiChatOptions options = buildOptions(resolved.model(), resolved.temperature(), resolved.maxTokens());
            ChatClient baseClient = chatClientCache.getOrCreateBase(
                    resolved.provider().getId(), resolved.model(), resolved.provider(), options);
            ChatClient chatClient = chatClientCache.createSessionClient(
                    baseClient, historySessionId, dbChatMemory);
            
            Long userId = SessionHelper.getUserId();
            Long tenantId = SessionHelper.getTenantId();
            ensureSession(sessionId, request.getAgentCode(), request.getUserInputOrMessage(),
                    userId, tenantId);

            log.info("[AiClient.stream] ===== 请求开始 =====");
            log.info("[AiClient.stream] sessionId: {}", sessionId);
            log.info("[AiClient.stream] agentCode: {}", request.getAgentCode());
            log.info("[AiClient.stream] provider: {}({})", resolved.provider().getProviderName(), resolved.provider().getProviderType());
            log.info("[AiClient.stream] model: {}", resolved.model());
            log.info("[AiClient.stream] temperature: {}, maxTokens: {}", resolved.temperature(), resolved.maxTokens());
            log.info("[AiClient.stream] systemPrompt: \n{}", systemPrompt);
            log.info("[AiClient.stream] userPrompt: \n{}", request.getMessage());

            StringBuilder reasoningBuilder = new StringBuilder();
            StringBuilder contentBuilder = new StringBuilder();
            AtomicBoolean persisted = new AtomicBoolean(false);
            AtomicBoolean isInAnsweringPhase = new AtomicBoolean(false);
            AtomicBoolean hasReasoningStarted = new AtomicBoolean(false);
            AtomicBoolean hasContentStarted = new AtomicBoolean(false);

            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(request.getMessage())
                    .stream()
                    .chatResponse()
                    .flatMap(chatResponse -> {
                        log.info("AI返回:{}", JSONObject.toJSONString(chatResponse));
                        if (chatResponse == null || chatResponse.getResults().isEmpty()) {
                            return Flux.empty();
                        }
                        
                        Generation generation = chatResponse.getResults().get(0);
                        AssistantMessage assistantMessage = generation.getOutput();
                        
                        Flux<String> outputFlux = Flux.empty();
                        
                        String reasoningContent = extractReasoningContent(assistantMessage);
                        if (StringUtils.hasText(reasoningContent)) {
                            if (!hasReasoningStarted.get()) {
                                hasReasoningStarted.set(true);
                                String thinkingDelimiter = "==================== 思考过程 ====================\n";
                                outputFlux = outputFlux.concatWith(Flux.just(thinkingDelimiter));
                            }
                            reasoningBuilder.append(reasoningContent);
                            outputFlux = outputFlux.concatWith(Flux.just(reasoningContent));
                            log.debug("[AiClient.stream.reasoning] {}", reasoningContent);
                        }
                        
                        String content = assistantMessage.getText();
                        if (StringUtils.hasText(content)) {
                            if (!hasContentStarted.get()) {
                                hasContentStarted.set(true);
                                isInAnsweringPhase.set(true);
                                if (hasReasoningStarted.get()) {
                                    String answerDelimiter = "\n\n==================== 完整回复 ====================\n";
                                    outputFlux = outputFlux.concatWith(Flux.just(answerDelimiter));
                                }
                            }
                            contentBuilder.append(content);
                            outputFlux = outputFlux.concatWith(Flux.just(content));
                            log.debug("[AiClient.stream.content] {}", content);
                        }
                        
                        return outputFlux;
                    })
                    .doFinally(signal -> {
                        circuitBreaker.recordSuccess(circuitKey);
                        String fullReasoning = reasoningBuilder.toString();
                        String fullContent = contentBuilder.toString();
                        boolean hasReasoning = hasReasoningStarted.get();
                        
                        log.info("[AiClient.stream] ===== 响应结束({}) =====", signal);
                        if (hasReasoning) {
                            log.info("[AiClient.stream] reasoningContent(length={}): \n{}", fullReasoning.length(),
                                    fullReasoning.length() > 500 ? fullReasoning.substring(0, 500) + "...(truncated)" : fullReasoning);
                        }
                        log.info("[AiClient.stream] assistantContent(length={}): \n{}", fullContent.length(),
                                fullContent.length() > 500 ? fullContent.substring(0, 500) + "...(truncated)" : fullContent);
                        
                        String finalContent = hasReasoning
                                ? "【思考过程】\n" + fullReasoning + "\n\n【回复内容】\n" + fullContent
                                : fullContent;
                        persistConversationAsync(sessionId, request.getAgentCode(),
                                request.getUserInput(), finalContent, persisted, signal, userId, tenantId);
                    })
                    .doOnError(e -> {
                        log.error("[AiClient] 流式调用失败, agentCode={}", request.getAgentCode(), e);
                        circuitBreaker.recordFailure(circuitKey);
                    });
        } catch (BusinessException e) {
            log.warn("[AiClient] 业务异常(流式), agentCode={}, msg={}", request.getAgentCode(), e.getMessage());
            AiClientResponse fallbackResp = handleBusinessException(e, request);
            return Flux.just("{\"fallback\":true,\"reason\":\"" + fallbackResp.getFallbackReason() + "\"}");
        } catch (Exception e) {
            log.error("[AiClient] 流式初始化失败, agentCode={}", request.getAgentCode(), e);
            circuitBreaker.recordFailure(circuitKey);
            return Flux.just("{\"fallback\":true,\"reason\":\"API_ERROR\"}");
        }
    }
    
    private String extractReasoningContent(AssistantMessage message) {
        if (message == null) {
            return null;
        }
        Map<String, Object> metadata = message.getMetadata();
        if (metadata == null) {
            return null;
        }
        Object reasoning = metadata.get("reasoningContent");
        if (reasoning instanceof String) {
            return (String) reasoning;
        }
        reasoning = metadata.get("reasoning_content");
        if (reasoning instanceof String) {
            return (String) reasoning;
        }
        reasoning = metadata.get("reasoning");
        if (reasoning instanceof String) {
            return (String) reasoning;
        }
        return null;
    }

    private String resolveSessionId(String sessionId) {
        return StringUtils.hasText(sessionId) ? sessionId : UUID.randomUUID().toString();
    }

    private String buildSystemPrompt(AiAgent agent, Map<String, String> contextVars) {
        String prompt = agent.getSystemPrompt();
        if (contextVars != null && !contextVars.isEmpty()) {
            prompt = AiPromptTemplateRenderer.render(prompt, contextVars);
        }
        return contextInjector.injectContext(prompt, agent.getAgentCode());
    }

    private OpenAiChatOptions buildOptions(String model, Double temperature, Integer maxTokens) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder().model(model);
        if (temperature != null) {
            builder.temperature(temperature);
        }
        if (maxTokens != null) {
            builder.maxTokens(maxTokens);
        }
        return builder.build();
    }

    private AiClientResponse handleBusinessException(BusinessException e, AiClientRequest request) {
        AiFallbackReason reason;
        String msg = e.getMessage();
        if (msg.contains("未配置")) {
            reason = AiFallbackReason.PROVIDER_NOT_CONFIGURED;
        } else if (msg.contains("已停用")) {
            reason = AiFallbackReason.PROVIDER_DISABLED;
        } else {
            reason = AiFallbackReason.API_ERROR;
        }
        return AiClientResponse.fallback(null, reason, request.getSessionId());
    }

    private void persistConversation(String sessionId, String agentCode,
                                     String userPrompt, String assistantContent,Long userId,Long tenantId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            recordService.save(AiChatRecord.builder()
                    .sessionId(sessionId)
                    .agentCode(agentCode)
                    .userId(userId)
                    .tenantId(tenantId)
                    .role("user")
                    .content(userPrompt)
                    .createTime(now)
                    .build());
            if (StringUtils.hasText(assistantContent)) {
                recordService.save(AiChatRecord.builder()
                        .sessionId(sessionId)
                        .agentCode(agentCode)
                        .role("assistant")
                        .userId(userId)
                        .tenantId(tenantId)
                        .content(assistantContent)
                        .createTime(now)
                        .build());
            }
            sessionService.touchSession(sessionId);
        } catch (Exception e) {
            log.warn("[AiClient] 持久化对话记录失败, sessionId={}", sessionId, e);
        }
    }

    private void persistConversationAsync(String sessionId, String agentCode,
                                          String userPrompt, String assistantContent,
                                          AtomicBoolean persisted, SignalType signalType,Long userId,Long tenantId) {
        if (!persisted.compareAndSet(false, true)) {
            return;
        }
        persistConversation(sessionId, agentCode, userPrompt, assistantContent,userId,tenantId);
    }

    private void ensureSession(String sessionId, String agentCode, String firstMsg, Long userId, Long tenantId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        sessionService.getOrCreate(sessionId, userId, tenantId, agentCode, firstMsg);
    }
}
