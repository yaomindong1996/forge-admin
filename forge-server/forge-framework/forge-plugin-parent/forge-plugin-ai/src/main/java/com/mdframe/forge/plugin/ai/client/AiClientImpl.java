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
import com.mdframe.forge.plugin.ai.provider.adapter.AiModelRuntimeOptions;
import com.mdframe.forge.plugin.ai.provider.support.AiProviderFailureDiagnostics;
import com.mdframe.forge.plugin.ai.health.AiModelFailureCategory;
import com.mdframe.forge.plugin.ai.health.AiModelFailureClassifier;
import com.mdframe.forge.plugin.ai.invocation.AiInvocationObservation;
import com.mdframe.forge.plugin.ai.invocation.AiInvocationOutcome;
import com.mdframe.forge.plugin.ai.invocation.AiInvocationPhase;
import com.mdframe.forge.plugin.ai.invocation.service.AiModelInvocationRecorder;
import com.mdframe.forge.plugin.ai.routing.RouteDecision;
import com.mdframe.forge.plugin.ai.routing.RoutedInvocation;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.metadata.Usage;
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
    private final ContextInjector contextInjector;
    private final AiModelInvocationRecorder invocationRecorder;
    private final AiModelFailureClassifier failureClassifier;

    @Override
    public AiClientResponse call(AiClientRequest request) {
        String requestId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        AiInvocationResolver.ResolvedInvocation resolved = null;
        boolean dispatched = false;
        try {
            resolved = invocationResolver.resolve(
                    request.getAgentCode(), request.getProviderId(), request.getModelName(),
                    request.getTemperature(), request.getMaxTokens());
            String systemPrompt = buildSystemPrompt(resolved.agent(), request.getContextVars());
            String sessionId = resolveSessionId(request.getSessionId());
            String historySessionId = StringUtils.hasText(sessionId) ? sessionId : null;
            AiModelRuntimeOptions options = buildOptions(
                    resolved.model(), resolved.temperature(), resolved.maxTokens());
            ChatClient baseClient = chatClientCache.getOrCreateBase(resolved.provider(), options);
            ChatClient chatClient = chatClientCache.createSessionClient(
                    baseClient, historySessionId, dbChatMemory);
            log.info("[AiClient.call] 请求开始, requestId={}, agentCode={}, providerId={}, model={}, systemLength={}, userLength={}",
                    requestId, request.getAgentCode(), resolved.provider().getId(), resolved.model(), length(systemPrompt), length(request.getMessage()));
            dispatched = true;
            ChatResponse response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(request.getMessage())
                    .call()
                    .chatResponse();
            String content = response != null && response.getResult() != null && response.getResult().getOutput() != null
                    ? response.getResult().getOutput().getText() : "";
            completeLeaseSuccess(resolved);
            recordObservation(requestId, request, resolved, AiInvocationOutcome.SUCCESS, AiInvocationPhase.COMPLETED,
                    true, null, response == null ? null : response.getMetadata().getUsage(), elapsedMillis(startedAt));
            log.info("[AiClient.call] 请求完成, requestId={}, responseLength={}, latencyMs={}", requestId, length(content), elapsedMillis(startedAt));
            if (StringUtils.hasText(sessionId)) {
                ensureSession(sessionId, request.getAgentCode(), request.getUserInputOrMessage(),
                        SessionHelper.getUserId(), SessionHelper.getTenantId());
                persistConversation(sessionId, request.getAgentCode(),
                        request.getMessage(), content, SessionHelper.getUserId(), SessionHelper.getTenantId());
            }
            return AiClientResponse.success(content, sessionId);
        } catch (BusinessException e) {
            abortOrFailLease(resolved, dispatched, e);
            recordObservation(requestId, request, resolved, resolved == null ? AiInvocationOutcome.ROUTING_FAILED : AiInvocationOutcome.FAILED,
                    dispatched ? AiInvocationPhase.DISPATCHED : AiInvocationPhase.RESOLUTION, dispatched, e, null, elapsedMillis(startedAt));
            log.warn("[AiClient] 业务异常, requestId={}, agentCode={}, exceptionType={}", requestId, request.getAgentCode(), e.getClass().getSimpleName());
            return handleBusinessException(e, request);
        } catch (Exception e) {
            abortOrFailLease(resolved, dispatched, e);
            recordObservation(requestId, request, resolved, AiInvocationOutcome.FAILED,
                    dispatched ? AiInvocationPhase.DISPATCHED : AiInvocationPhase.PREPARATION, dispatched, e, null, elapsedMillis(startedAt));
            log.error("[AiClient] AI调用失败, requestId={}, agentCode={}, exceptionType={}", requestId, request.getAgentCode(), e.getClass().getSimpleName());
            return AiClientResponse.fallback(null, AiFallbackReason.API_ERROR, request.getSessionId());
        }
    }

    @Override
    public Flux<String> stream(AiClientRequest request) {
        String requestId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        AtomicReference<AiInvocationResolver.ResolvedInvocation> resolvedReference = new AtomicReference<>();
        AtomicBoolean dispatched = new AtomicBoolean(false);
        try {
            AiInvocationResolver.ResolvedInvocation resolved = invocationResolver.resolve(
                    request.getAgentCode(), request.getProviderId(), request.getModelName(),
                    request.getTemperature(), request.getMaxTokens());
            resolvedReference.set(resolved);

            String systemPrompt = buildSystemPrompt(resolved.agent(), request.getContextVars());
            String sessionId = resolveSessionId(request.getSessionId());
            String historySessionId = StringUtils.hasText(sessionId) ? sessionId : null;

            AiModelRuntimeOptions options = buildOptions(
                    resolved.model(), resolved.temperature(), resolved.maxTokens());
            ChatClient baseClient = chatClientCache.getOrCreateBase(resolved.provider(), options);
            ChatClient chatClient = chatClientCache.createSessionClient(
                    baseClient, historySessionId, dbChatMemory);

            Long userId = SessionHelper.getUserId();
            Long tenantId = SessionHelper.getTenantId();
            ensureSession(sessionId, request.getAgentCode(), request.getUserInputOrMessage(),
                    userId, tenantId);
            log.info("[AiClient.stream] 请求开始, requestId={}, agentCode={}, providerId={}, model={}, systemLength={}, userLength={}",
                    requestId, request.getAgentCode(), resolved.provider().getId(), resolved.model(), length(systemPrompt), length(request.getMessage()));
            StringBuilder reasoningBuilder = new StringBuilder();
            StringBuilder contentBuilder = new StringBuilder();
            AtomicBoolean persisted = new AtomicBoolean(false);
            AtomicBoolean hasReasoningStarted = new AtomicBoolean(false);
            AtomicBoolean hasContentStarted = new AtomicBoolean(false);
            AtomicBoolean finalized = new AtomicBoolean(false);
            AtomicReference<Usage> lastUsage = new AtomicReference<>();
            AtomicReference<Throwable> streamFailure = new AtomicReference<>();

            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(request.getMessage())
                    .stream()
                    .chatResponse()
                    .doOnSubscribe(ignored -> dispatched.set(true))
                    .concatMap(chatResponse -> {
                        if (chatResponse != null && chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) lastUsage.set(chatResponse.getMetadata().getUsage());
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
                        }

                        String content = assistantMessage.getText();
                        if (StringUtils.hasText(content)) {
                            if (!hasContentStarted.get()) {
                                hasContentStarted.set(true);
                                if (hasReasoningStarted.get()) {
                                    String answerDelimiter = "\n\n==================== 完整回复 ====================\n";
                                    outputFlux = outputFlux.concatWith(Flux.just(answerDelimiter));
                                }
                            }
                            contentBuilder.append(content);
                            outputFlux = outputFlux.concatWith(Flux.just(content));
                        }

                        return outputFlux;
                    })
                    .doOnError(streamFailure::set)
                    .doFinally(signal -> {
                        if (!finalized.compareAndSet(false, true)) return;
                        String fullReasoning = reasoningBuilder.toString();
                        String fullContent = contentBuilder.toString();
                        boolean hasReasoning = hasReasoningStarted.get();
                        AiInvocationOutcome outcome;
                        Throwable failure = streamFailure.get();
                        if (signal == SignalType.CANCEL) { outcome = AiInvocationOutcome.CANCELLED; cancelLease(resolved); }
                        else if (failure != null) { outcome = AiInvocationOutcome.FAILED; abortOrFailLease(resolved, dispatched.get(), failure); }
                        else { outcome = AiInvocationOutcome.SUCCESS; completeLeaseSuccess(resolved); }
                        recordObservation(requestId, request, resolved, outcome,
                                signal == SignalType.CANCEL ? AiInvocationPhase.STREAMING : AiInvocationPhase.COMPLETED,
                                dispatched.get(), failure, lastUsage.get(), elapsedMillis(startedAt),
                                tenantId, userId);
                        log.info("[AiClient.stream] 请求结束, requestId={}, signal={}, reasoningLength={}, responseLength={}, latencyMs={}",
                                requestId, signal, fullReasoning.length(), fullContent.length(), elapsedMillis(startedAt));
                        String finalContent = hasReasoning
                                ? "【思考过程】\n" + fullReasoning + "\n\n【回复内容】\n" + fullContent
                                : fullContent;
                        persistConversationAsync(sessionId, request.getAgentCode(),
                                request.getUserInput(), finalContent, persisted, signal, userId, tenantId);
                    });
        } catch (BusinessException e) {
            AiInvocationResolver.ResolvedInvocation resolved = resolvedReference.get();
            abortOrFailLease(resolved, dispatched.get(), e);
            recordObservation(requestId, request, resolved,
                    resolved == null ? AiInvocationOutcome.ROUTING_FAILED : AiInvocationOutcome.FAILED,
                    resolved == null ? AiInvocationPhase.RESOLUTION : AiInvocationPhase.PREPARATION,
                    dispatched.get(), e, null, elapsedMillis(startedAt));
            log.warn("[AiClient] 业务异常(流式), requestId={}, agentCode={}, exceptionType={}", requestId, request.getAgentCode(), e.getClass().getSimpleName());
            AiClientResponse fallbackResp = handleBusinessException(e, request);
            return Flux.just("{\"fallback\":true,\"reason\":\"" + fallbackResp.getFallbackReason() + "\"}");
        } catch (Exception e) {
            AiInvocationResolver.ResolvedInvocation resolved = resolvedReference.get();
            abortOrFailLease(resolved, dispatched.get(), e);
            recordObservation(requestId, request, resolved, AiInvocationOutcome.FAILED,
                    resolved == null ? AiInvocationPhase.RESOLUTION : AiInvocationPhase.PREPARATION,
                    dispatched.get(), e, null, elapsedMillis(startedAt));
            log.error("[AiClient] 流式初始化失败, requestId={}, agentCode={}, exceptionType={}", requestId, request.getAgentCode(), e.getClass().getSimpleName());
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

    private AiModelRuntimeOptions buildOptions(String model, Double temperature, Integer maxTokens) {
        return new AiModelRuntimeOptions(model, temperature, maxTokens);
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

    private void completeLeaseSuccess(AiInvocationResolver.ResolvedInvocation resolved) {
        if (resolved != null && resolved.routedInvocation() != null) resolved.routedInvocation().healthLease().success();
    }

    private void cancelLease(AiInvocationResolver.ResolvedInvocation resolved) {
        if (resolved != null && resolved.routedInvocation() != null) resolved.routedInvocation().healthLease().cancel();
    }

    private void abortOrFailLease(AiInvocationResolver.ResolvedInvocation resolved, boolean dispatched, Throwable failure) {
        if (resolved == null || resolved.routedInvocation() == null) return;
        if (!dispatched) { resolved.routedInvocation().healthLease().abort(); return; }
        AiModelFailureCategory category = failureClassifier.classify(failure);
        if (category == AiModelFailureCategory.VALIDATION || category == AiModelFailureCategory.CONTENT_POLICY || category == AiModelFailureCategory.CANCELLED) resolved.routedInvocation().healthLease().cancel();
        else resolved.routedInvocation().healthLease().failure(category);
    }

    private void recordObservation(String requestId, AiClientRequest request,
                                   AiInvocationResolver.ResolvedInvocation resolved,
                                   AiInvocationOutcome outcome, AiInvocationPhase phase,
                                   boolean dispatched, Throwable failure, Usage usage, long latencyMs) {
        recordObservation(requestId, request, resolved, outcome, phase, dispatched,
                failure, usage, latencyMs, null, null);
    }

    private void recordObservation(String requestId, AiClientRequest request,
                                   AiInvocationResolver.ResolvedInvocation resolved,
                                   AiInvocationOutcome outcome, AiInvocationPhase phase,
                                   boolean dispatched, Throwable failure, Usage usage, long latencyMs,
                                   Long capturedTenantId, Long capturedUserId) {
        try {
            RouteDecision decision = resolved != null && resolved.routedInvocation() != null ? resolved.routedInvocation().decision() : null;
            AiProviderFailureDiagnostics diagnostics = failure == null ? new AiProviderFailureDiagnostics(null, null) : AiProviderFailureDiagnostics.from(failure);
            AiModelFailureCategory category = failure == null ? null : failureClassifier.classify(failure);
            Long tenantId = capturedTenantId != null ? capturedTenantId : SessionHelper.getTenantId();
            Long userId = capturedUserId != null ? capturedUserId : SessionHelper.getUserId();
            invocationRecorder.record(new AiInvocationObservation(
                    requestId, tenantId, userId, request.getAgentCode(), request.getSessionId(),
                    phase, dispatched, outcome,
                    decision == null ? null : decision.source(), decision == null ? null : decision.reason(), decision == null ? null : decision.policyId(),
                    decision == null ? null : decision.provider().getId(), decision == null ? null : decision.model().getId(),
                    decision == null ? null : decision.model().getModelId(), decision == null ? null : decision.provider().getAdapterCode(),
                    category, diagnostics.httpStatus(), failure == null ? null : diagnostics.errorCode(), latencyMs,
                    usage == null ? null : toLong(usage.getPromptTokens()), usage == null ? null : toLong(usage.getCompletionTokens()), usage == null ? null : toLong(usage.getTotalTokens()),
                    decision == null ? null : decision.model().getInputPricePerMillionCent(), decision == null ? null : decision.model().getOutputPricePerMillionCent()));
        } catch (Exception e) {
            log.warn("[AI调用治理] 记录失败, requestId={}, exceptionType={}", requestId, e.getClass().getSimpleName());
        }
    }

    private Long toLong(Number number) { return number == null ? null : number.longValue(); }
    private long elapsedMillis(long startedAt) { return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L); }
    private int length(String value) { return value == null ? 0 : value.length(); }

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
