package com.mdframe.forge.plugin.ai.client;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatRecord;
import com.mdframe.forge.plugin.ai.chat.memory.DbChatMemory;
import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import com.mdframe.forge.plugin.ai.chat.service.AiPromptTemplateRenderer;
import com.mdframe.forge.plugin.ai.client.dto.AiClientRequest;
import com.mdframe.forge.plugin.ai.client.dto.AiClientResponse;
import com.mdframe.forge.plugin.ai.client.dto.AiFallbackReason;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiClientImpl implements AiClient {

    private final AiAgentService agentService;
    private final AiProviderService providerService;
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
            AiAgent agent = resolveAgent(request.getAgentCode());
            AiProvider provider = resolveProvider(request.getProviderId(), agent);
            String model = resolveModel(request.getModelName(), agent, provider);
            Double temperature = resolveTemperature(request.getTemperature(), agent);
            Integer maxTokens = resolveMaxTokens(request.getMaxTokens(), agent);

            String systemPrompt = buildSystemPrompt(agent, request.getContextVars());
            String sessionId = resolveSessionId(request.getSessionId());
            String historySessionId = StringUtils.hasText(sessionId) ? sessionId : null;

            OpenAiChatOptions options = buildOptions(model, temperature, maxTokens);
            ChatClient chatClient = chatClientCache.getOrCreate(
                    provider.getId(), model, provider, options, historySessionId, dbChatMemory);

            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user(request.getMessage())
                    .call()
                    .content();

            circuitBreaker.recordSuccess(circuitKey);

            if (StringUtils.hasText(sessionId)) {
                persistConversation(sessionId, request.getAgentCode(),
                        request.getMessage(), content);
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
            AiAgent agent = resolveAgent(request.getAgentCode());
            AiProvider provider = resolveProvider(request.getProviderId(), agent);
            String model = resolveModel(request.getModelName(), agent, provider);
            Double temperature = resolveTemperature(request.getTemperature(), agent);
            Integer maxTokens = resolveMaxTokens(request.getMaxTokens(), agent);

            String systemPrompt = buildSystemPrompt(agent, request.getContextVars());
            String sessionId = resolveSessionId(request.getSessionId());
            String historySessionId = StringUtils.hasText(sessionId) ? sessionId : null;

            OpenAiChatOptions options = buildOptions(model, temperature, maxTokens);
            ChatClient chatClient = chatClientCache.getOrCreate(
                    provider.getId(), model, provider, options, historySessionId, dbChatMemory);

            sessionService.getOrCreate(sessionId, null, null,
                    request.getAgentCode(), request.getMessage());

            StringBuilder assistantBuilder = new StringBuilder();
            AtomicBoolean persisted = new AtomicBoolean(false);

            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(request.getMessage())
                    .stream()
                    .content()
                    .doOnNext(assistantBuilder::append)
                    .doFinally(signal -> {
                        circuitBreaker.recordSuccess(circuitKey);
                        persistConversationAsync(sessionId, request.getAgentCode(),
                                request.getMessage(), assistantBuilder.toString(), persisted, signal);
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

    private AiAgent resolveAgent(String agentCode) {
        if (!StringUtils.hasText(agentCode)) {
            throw new BusinessException("Agent编码不能为空");
        }
        AiAgent agent = agentService.getByCode(agentCode);
        if (agent == null) {
            throw new BusinessException("Agent 不存在或已停用: " + agentCode);
        }
        return agent;
    }

    private AiProvider resolveProvider(Long providerId, AiAgent agent) {
        AiProvider provider = null;
        if (providerId != null) {
            provider = providerService.getById(providerId);
        }
        if (provider == null && agent != null && agent.getProviderId() != null) {
            provider = providerService.getById(agent.getProviderId());
        }
        if (provider == null) {
            provider = providerService.getDefaultProvider();
        }
        if (provider == null) {
            throw new BusinessException("未配置可用的 AI 供应商");
        }
        if (StringUtils.hasText(provider.getStatus()) && !"0".equals(provider.getStatus())) {
            throw new BusinessException("AI 供应商已停用: " + provider.getProviderName());
        }
        if (!StringUtils.hasText(provider.getBaseUrl()) || !StringUtils.hasText(provider.getApiKey())) {
            throw new BusinessException("AI 供应商配置不完整");
        }
        return provider;
    }

    private String resolveModel(String modelName, AiAgent agent, AiProvider provider) {
        if (StringUtils.hasText(modelName)) {
            return modelName;
        }
        if (agent != null && StringUtils.hasText(agent.getModelName())) {
            return agent.getModelName();
        }
        if (StringUtils.hasText(provider.getDefaultModel())) {
            return provider.getDefaultModel();
        }
        return "gpt-3.5-turbo";
    }

    private Double resolveTemperature(Double temperature, AiAgent agent) {
        if (temperature != null) {
            return temperature;
        }
        if (agent != null && agent.getTemperature() != null) {
            return agent.getTemperature().doubleValue();
        }
        return 0.7D;
    }

    private Integer resolveMaxTokens(Integer maxTokens, AiAgent agent) {
        if (maxTokens != null) {
            return maxTokens;
        }
        if (agent != null && agent.getMaxTokens() != null) {
            return agent.getMaxTokens();
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
                                     String userPrompt, String assistantContent) {
        try {
            LocalDateTime now = LocalDateTime.now();
            recordService.save(AiChatRecord.builder()
                    .sessionId(sessionId)
                    .agentCode(agentCode)
                    .role("user")
                    .content(userPrompt)
                    .createTime(now)
                    .build());
            if (StringUtils.hasText(assistantContent)) {
                recordService.save(AiChatRecord.builder()
                        .sessionId(sessionId)
                        .agentCode(agentCode)
                        .role("assistant")
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
                                          AtomicBoolean persisted, SignalType signalType) {
        if (!persisted.compareAndSet(false, true)) {
            return;
        }
        persistConversation(sessionId, agentCode, userPrompt, assistantContent);
    }
}
