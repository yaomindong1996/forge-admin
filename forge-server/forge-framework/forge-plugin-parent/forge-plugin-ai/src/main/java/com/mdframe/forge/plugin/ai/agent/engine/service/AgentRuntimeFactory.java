package com.mdframe.forge.plugin.ai.agent.engine.service;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentCallerGuard;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentToolBindingResolver;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.adapter.AiModelRuntimeOptions;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterRegistry;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.routing.AiModelRouter;
import com.mdframe.forge.plugin.ai.routing.RouteDecision;
import com.mdframe.forge.plugin.ai.routing.RouteRequest;
import com.mdframe.forge.plugin.ai.routing.constant.AiModelSelectionMode;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.file.core.FileManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/** 初始与 HITL 恢复共用服务端运行时装配，模型凭据/工具实例永远不从 Redis 恢复。 */
@Slf4j
@Component
public class AgentRuntimeFactory {

    private static final int DEFAULT_MAX_ITERS = 10;
    private static final String ENABLED_STATUS = "0";
    private static final String PROVIDER_UNAVAILABLE_MSG =
            "模型供应商不存在或不可访问，请先在「模型与服务配置」中添加并启用模型供应商";

    private final AiAgentService agentService;
    private final AiProviderAdapterRegistry providerRegistry;
    private final AiProviderMapper providerMapper;
    private final FileManager fileManager;
    private final AgentToolBindingResolver bindings;
    @Nullable
    private final AiModelRouter modelRouter;
    @Nullable
    private final AiModelMapper modelMapper;

    @Autowired
    public AgentRuntimeFactory(AiAgentService agentService,
                               AiProviderAdapterRegistry providerRegistry,
                               AiProviderMapper providerMapper,
                               FileManager fileManager,
                               AgentToolBindingResolver bindings,
                               @Nullable AiModelRouter modelRouter,
                               @Nullable AiModelMapper modelMapper) {
        this.agentService = agentService;
        this.providerRegistry = providerRegistry;
        this.providerMapper = providerMapper;
        this.fileManager = fileManager;
        this.bindings = bindings;
        this.modelRouter = modelRouter;
        this.modelMapper = modelMapper;
    }

    AgentRuntimeFactory(AiAgentService agentService,
                        AiProviderAdapterRegistry providerRegistry,
                        AiProviderMapper providerMapper,
                        FileManager fileManager,
                        AgentToolBindingResolver bindings) {
        this(agentService, providerRegistry, providerMapper, fileManager, bindings, null, null);
    }

    public AiAgent requireAgent(String code) {
        AiAgent agent = agentService.getByCode(code);
        if (agent == null) {
            throw new BusinessException("Agent 不存在或已停用");
        }
        if (!Objects.equals(agent.getTenantId(), AgentCallerGuard.requireCaller().getTenantId())) {
            throw new BusinessException(403, "无权访问该 Agent");
        }
        return agent;
    }

    public void attach(ReactContext context, AiAgent agent) {
        AgentCallerGuard.requireOwner(context);
        if (!Objects.equals(context.getAgentId(), agent.getId())
                || !Objects.equals(context.getTenantId(), agent.getTenantId())) {
            throw new BusinessException(403, "Agent 配置与会话不匹配");
        }
        ResolvedBinding resolved = resolveModelBinding(context, agent);
        var options = new AiModelRuntimeOptions(resolved.modelName(),
                agent.getTemperature() == null ? 0.7 : agent.getTemperature().doubleValue(), agent.getMaxTokens());
        context.setChatModel(providerRegistry.createChatModel(resolved.provider(), options));
        context.setAgent(agent);
        context.setBoundTools(bindings.resolve(agent));
        context.setFileUrlResolver(this::resolveFileUrl);
        context.setToolGroupMode(agent.getToolGroupMode() == null ? "all" : agent.getToolGroupMode());
    }

    public void restore(ReactContext context) {
        AiAgent agent = requireAgent(context.getAgentCode());
        attach(context, agent);
        context.setSystemPrompt(agent.getSystemPrompt());
        int currentLimit = agent.getMaxIters() == null ? DEFAULT_MAX_ITERS : agent.getMaxIters();
        // 不因确认重置预算，也不因为后台调大上限而扩大已开始运行的会话。
        context.setMaxIters(Math.min(context.getMaxIters(), currentLimit));
    }

    private ResolvedBinding resolveModelBinding(ReactContext context, AiAgent agent) {
        Long tenantId = context.getTenantId();
        if (AiModelSelectionMode.fromNullable(agent.getModelSelectionMode()) == AiModelSelectionMode.POLICY
                && modelRouter != null) {
            RouteDecision decision = modelRouter.preview(new RouteRequest(tenantId, agent, null, null));
            AiProvider routed = decision != null ? decision.provider() : null;
            if (routed == null || !Objects.equals(routed.getTenantId(), tenantId)) {
                throw new BusinessException(PROVIDER_UNAVAILABLE_MSG);
            }
            String routedModel = decision.model() != null && StringUtils.hasText(decision.model().getModelId())
                    ? decision.model().getModelId().trim()
                    : resolveDefaultModel(routed);
            return new ResolvedBinding(routed, routedModel);
        }

        if (agent.getProviderId() != null) {
            AiProvider pinned = providerMapper.selectById(agent.getProviderId());
            if (pinned != null) {
                if (!Objects.equals(pinned.getTenantId(), tenantId)) {
                    throw new BusinessException("模型供应商不存在或不可访问");
                }
                if (pinned.getStatus() == null || ENABLED_STATUS.equals(pinned.getStatus())) {
                    String modelName = StringUtils.hasText(agent.getModelName())
                            ? agent.getModelName().trim()
                            : resolveDefaultModel(pinned);
                    return new ResolvedBinding(pinned, modelName);
                }
                log.warn("[AgentRuntime] 智能体绑定供应商已停用，尝试回退租户默认供应商: agentCode={}, providerId={}",
                        agent.getAgentCode(), agent.getProviderId());
                return resolveFallbackBinding(tenantId, agent, true);
            }
            log.warn("[AgentRuntime] 智能体绑定供应商不存在，尝试回退租户默认供应商: agentCode={}, providerId={}",
                    agent.getAgentCode(), agent.getProviderId());
            return resolveFallbackBinding(tenantId, agent, true);
        }

        return resolveFallbackBinding(tenantId, agent, false);
    }

    private ResolvedBinding resolveFallbackBinding(Long tenantId, AiAgent agent, boolean stalePinnedProvider) {
        AiProvider fallback = providerMapper.selectDefaultProvider();
        if (fallback == null) {
            fallback = providerMapper.selectFirstEnabledProvider();
        }
        if (fallback == null || !Objects.equals(fallback.getTenantId(), tenantId)) {
            throw new BusinessException(PROVIDER_UNAVAILABLE_MSG);
        }
        String defaultModel = resolveDefaultModel(fallback);
        String modelName;
        if (stalePinnedProvider) {
            modelName = StringUtils.hasText(defaultModel) ? defaultModel : trimToNull(agent.getModelName());
        } else {
            modelName = StringUtils.hasText(agent.getModelName()) ? agent.getModelName().trim() : defaultModel;
        }
        log.info("[AgentRuntime] 智能体回退到可用供应商: agentCode={}, providerId={}, model={}",
                agent.getAgentCode(), fallback.getId(), modelName);
        return new ResolvedBinding(fallback, modelName);
    }

    private String resolveDefaultModel(AiProvider provider) {
        if (provider == null) {
            return null;
        }
        if (StringUtils.hasText(provider.getDefaultModel())) {
            return provider.getDefaultModel().trim();
        }
        if (modelMapper == null || provider.getId() == null) {
            return null;
        }
        String defaultModelId = modelMapper.selectEnabledDefaultModelId(provider.getId());
        if (StringUtils.hasText(defaultModelId)) {
            return defaultModelId.trim();
        }
        List<AiModel> enabled = modelMapper.selectEnabledModels(provider.getId());
        if (enabled != null) {
            for (AiModel item : enabled) {
                if (item != null && StringUtils.hasText(item.getModelId())) {
                    return item.getModelId().trim();
                }
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String resolveFileUrl(Long fileId) {
        try {
            return fileManager.getAccessUrl(String.valueOf(fileId), 3600);
        } catch (RuntimeException ex) {
            log.warn("[AgentRuntime] 文件链接不可用: fileId={}, exceptionType={}", fileId, ex.getClass().getSimpleName());
            return null;
        }
    }

    private record ResolvedBinding(AiProvider provider, String modelName) {
    }
}
