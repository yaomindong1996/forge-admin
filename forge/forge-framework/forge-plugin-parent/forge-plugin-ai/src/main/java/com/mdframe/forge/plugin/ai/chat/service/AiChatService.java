package com.mdframe.forge.plugin.ai.chat.service;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatRecord;
import com.mdframe.forge.plugin.ai.chat.memory.DbChatMemory;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Builder;
import lombok.Data;

/**
 * AI 对话服务（使用标准 OpenAI 兼容接口，支持多供应商扩展）
 *
 * 支持的供应商：
 * - 阿里百炼：https://dashscope.aliyuncs.com/compatible-mode
 * - OpenAI：https://api.openai.com
 * - 智谱 AI：https://open.bigmodel.cn/api/paas/v4
 * - Moonshot：https://api.moonshot.cn/v1
 * - 其他兼容 OpenAI 接口的供应商
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AiAgentService agentService;
    private final AiProviderService providerService;
    private final DbChatMemory dbChatMemory;
    private final AiChatRecordService recordService;
    private final AiChatSessionService sessionService;

    @Data
    @Builder
    private static class StreamChatContext {
        private String sessionId;
        private Long userId;
        private Long tenantId;
        private String agentCode;
        private String systemPrompt;
        private String userPrompt;
        private Long providerId;
        private String modelName;
        private Double temperature;
        private Integer maxTokens;
        private AiAgent agent;
        private boolean useHistory;
    }

    private AiProvider resolveProvider(Long providerId, AiAgent agent) {
        AiProvider provider = null;
        if (providerId != null) {
            provider = providerService.getById(providerId);
            if (provider == null) {
                throw new RuntimeException("所选 AI 供应商不存在: " + providerId);
            }
        }
        if (provider == null && agent != null && agent.getProviderId() != null) {
            provider = providerService.getById(agent.getProviderId());
        }
        if (provider == null) {
            provider = providerService.getDefaultProvider();
        }
        if (provider == null) {
            throw new RuntimeException("未配置可用的 AI 供应商");
        }
        if (StringUtils.hasText(provider.getStatus()) && !"0".equals(provider.getStatus())) {
            throw new RuntimeException("所选 AI 供应商已停用: " + provider.getProviderName());
        }
        if (!StringUtils.hasText(provider.getBaseUrl()) || !StringUtils.hasText(provider.getApiKey())) {
            throw new RuntimeException("AI 供应商配置不完整，请检查 Base URL 与 API Key");
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

    private ChatClient buildChatClient(AiProvider provider, String model, Double temperature, Integer maxTokens) {
        return buildChatClient(provider, model, temperature, maxTokens, null);
    }

    private ChatClient buildChatClient(AiProvider provider, String model, Double temperature, Integer maxTokens, String sessionId) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .build();

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(model);
        if (temperature != null) {
            optionsBuilder.temperature(temperature);
        }
        if (maxTokens != null) {
            optionsBuilder.maxTokens(maxTokens);
        }

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(optionsBuilder.build())
                .build();
        log.info("AI ChatClient 初始化成功, provider={}, baseUrl={}, model={}, temperature={}, maxTokens={}, sessionId={}",
                provider.getProviderName(), provider.getBaseUrl(), model, temperature, maxTokens, sessionId);
        
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        if (StringUtils.hasText(sessionId)) {
            builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(dbChatMemory)
                    .conversationId(sessionId)
                    .build());
        }
        return builder.build();
    }

    /**
     * 构建大屏生成用户提示词
     */
    private String buildDashboardPrompt(com.mdframe.forge.plugin.ai.chat.dto.AIGenerateRequest request) {
        StringBuilder userPrompt = new StringBuilder(request.getPrompt());
        if (StringUtils.hasText(request.getProjectName())) {
            userPrompt.append("\n\n当前项目：").append(request.getProjectName());
        }
        if (StringUtils.hasText(request.getCanvasContext())) {
            userPrompt.append("\n\n当前画布已有内容（请在此基础上增量修改，尽量保持现有布局和组件语义一致）：\n")
                    .append(request.getCanvasContext());
        }
        if (request.getCanvasWidth() != null && request.getCanvasHeight() != null) {
            userPrompt.append("\n\n画布尺寸：")
                    .append(request.getCanvasWidth())
                    .append("x")
                    .append(request.getCanvasHeight());
        }
        if (StringUtils.hasText(request.getStyle())) {
            userPrompt.append("\n风格要求：")
                    .append("light".equalsIgnoreCase(request.getStyle()) ? "浅色主题" : "深色主题");
        }
        return userPrompt.toString();
    }

    private String buildDashboardSystemPrompt(AiAgent agent, com.mdframe.forge.plugin.ai.chat.dto.AIGenerateRequest request) {
        return AiPromptTemplateRenderer.renderDashboardSystemPrompt(agent.getSystemPrompt(), request);
    }

    private String buildChatUserPrompt(String content, String projectName, String canvasContext) {
        StringBuilder prompt = new StringBuilder(content);
        if (StringUtils.hasText(projectName)) {
            prompt.append("\n\n当前项目：").append(projectName);
        }
        if (StringUtils.hasText(canvasContext)) {
            prompt.append("\n\n当前画布已有内容（请基于现有内容进行分析、修改或补充，而不是完全忽略已有内容）：\n")
                    .append(canvasContext);
        }
        return prompt.toString();
    }

    /**
     * 大屏生成（非流式）
     */
    public String generateDashboard(com.mdframe.forge.plugin.ai.chat.dto.AIGenerateRequest request) {
        AiAgent agent = agentService.getByCode("dashboard_generator");
        if (agent == null) {
            throw new RuntimeException("未找到大屏生成 Agent");
        }

        AiProvider provider = resolveProvider(request.getProviderId(), agent);
        String model = resolveModel(request.getModelName(), agent, provider);
        Double temperature = resolveTemperature(request.getTemperature(), agent);
        Integer maxTokens = resolveMaxTokens(request.getMaxTokens(), agent);

        return buildChatClient(provider, model, temperature, maxTokens).prompt()
                .system(buildDashboardSystemPrompt(agent, request))
                .user(buildDashboardPrompt(request))
                .call()
                .content();
    }

    /**
     * 大屏生成（流式）
     */
    public Flux<String> generateDashboardStream(com.mdframe.forge.plugin.ai.chat.dto.AIGenerateRequest request) {
        AiAgent agent = agentService.getByCode("dashboard_generator");
        if (agent == null) {
            throw new RuntimeException("未找到大屏生成 Agent");
        }

        LoginUser loginUser = SessionHelper.getLoginUser();
        Long userId = loginUser != null ? loginUser.getUserId() : 1L;
        Long tenantId = loginUser != null ? loginUser.getTenantId() : 1L;
        String sessionId = StringUtils.hasText(request.getSessionId()) 
                ? request.getSessionId() 
                : UUID.randomUUID().toString();

        String systemPrompt = buildDashboardSystemPrompt(agent, request);
        String userPrompt = buildDashboardPrompt(request);

        StreamChatContext context = StreamChatContext.builder()
                .sessionId(sessionId)
                .userId(userId)
                .tenantId(tenantId)
                .agentCode("dashboard_generator")
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .providerId(request.getProviderId())
                .modelName(request.getModelName())
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .agent(agent)
                .useHistory(false)
                .build();

        return executeStreamChat(context);
    }

    /**
     * 流式对话（真正的 SSE 流式输出，支持多轮上下文）
     */
    public Flux<String> chatStream(String content, String agentCode, String sessionId, Long userId,
                                   Long providerId, String modelName, Double temperature, Integer maxTokens,
                                   String projectName, String canvasContext) {
        AiAgent agent = StringUtils.hasText(agentCode) ? agentService.getByCode(agentCode) : null;
        if (StringUtils.hasText(agentCode) && agent == null) {
            throw new RuntimeException("Agent 不存在: " + agentCode);
        }

        LoginUser loginUser = SessionHelper.getLoginUser();
        Long tenantId = loginUser != null ? loginUser.getTenantId() : 1L;
        String sid = StringUtils.hasText(sessionId) ? sessionId : UUID.randomUUID().toString();

        String systemPrompt = agent != null && StringUtils.hasText(agent.getSystemPrompt())
                ? agent.getSystemPrompt()
                : "你是 GoView 数据大屏平台的 AI 助手，可以回答关于数据可视化、大屏设计、ECharts 图表配置和 AI 供应商使用的问题。回答要简洁、实用、准确。";
        String userPrompt = buildChatUserPrompt(content, projectName, canvasContext);

        StreamChatContext context = StreamChatContext.builder()
                .sessionId(sid)
                .userId(userId)
                .tenantId(tenantId)
                .agentCode(agentCode)
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .providerId(providerId)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .agent(agent)
                .useHistory(true)
                .build();

        return executeStreamChat(context);
    }

    /**
     * 公共流式对话处理方法
     */
    private Flux<String> executeStreamChat(StreamChatContext context) {
        AiProvider provider = resolveProvider(context.getProviderId(), context.getAgent());
        String model = resolveModel(context.getModelName(), context.getAgent(), provider);
        Double temperature = resolveTemperature(context.getTemperature(), context.getAgent());
        Integer maxTokens = resolveMaxTokens(context.getMaxTokens(), context.getAgent());
        
        String sessionId = context.getSessionId();
        String historySessionId = context.isUseHistory() ? sessionId : null;
        ChatClient chatClient = buildChatClient(provider, model, temperature, maxTokens, historySessionId);

        sessionService.getOrCreate(sessionId, context.getUserId(), context.getTenantId(), 
                context.getAgentCode(), context.getUserPrompt());

        StringBuilder assistantBuilder = new StringBuilder();
        AtomicBoolean persisted = new AtomicBoolean(false);

        log.info("[AiChatService] 开始 AI 流式对话, sessionId={}, userId={}, agentCode={}, model={}, useHistory={}",
                sessionId, context.getUserId(), context.getAgentCode(), model, context.isUseHistory());

        return chatClient.prompt()
                .system(context.getSystemPrompt())
                .user(context.getUserPrompt())
                .stream()
                .content()
                .doOnNext(chunk -> {
                    assistantBuilder.append(chunk);
                    log.info("[AiChatService] 流式分片, sessionId={}, chunkLen={}, preview={}",
                            sessionId, chunk == null ? 0 : chunk.length(), truncateForLog(chunk));
                })
                .doOnComplete(() -> log.info("[AiChatService] 流式完成, sessionId={}, answerLen={}", 
                        sessionId, assistantBuilder.length()))
                .doOnError(e -> log.error("[AiChatService] 流式异常, sessionId={}", sessionId, e))
                .doFinally(signalType -> persistChatConversation(
                        sessionId,
                        context.getUserId(),
                        context.getTenantId(),
                        context.getAgentCode(),
                        context.getUserPrompt(),
                        assistantBuilder.toString(),
                        persisted,
                        signalType
                ));
    }

    private void persistChatConversation(String sessionId, Long userId, Long tenantId, String agentCode,
                                         String userPrompt, String assistantContent,
                                         AtomicBoolean persisted, SignalType signalType) {
        if (!persisted.compareAndSet(false, true)) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            recordService.save(AiChatRecord.builder()
                    .tenantId(tenantId)
                    .sessionId(sessionId)
                    .userId(userId)
                    .agentCode(agentCode)
                    .role("user")
                    .content(userPrompt)
                    .createTime(now)
                    .build());
            if (StringUtils.hasText(assistantContent)) {
                recordService.save(AiChatRecord.builder()
                        .tenantId(tenantId)
                        .sessionId(sessionId)
                        .userId(userId)
                        .agentCode(agentCode)
                        .role("assistant")
                        .content(assistantContent)
                        .createTime(now)
                        .build());
            }
            sessionService.touchSession(sessionId);
            log.info("[AiChatService] 对话记录已写入, sessionId={}, signalType={}, userLen={}, answerLen={}",
                    sessionId, signalType, userPrompt == null ? 0 : userPrompt.length(), assistantContent == null ? 0 : assistantContent.length());
        } catch (Exception e) {
            log.warn("[AiChatService] 写入对话记录失败, sessionId={}, signalType={}", sessionId, signalType, e);
        }
    }

    private String truncateForLog(String chunk) {
        if (!StringUtils.hasText(chunk)) {
            return "";
        }
        String normalized = chunk.replaceAll("\\s+", " ").trim();
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }
}
