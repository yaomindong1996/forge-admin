package com.mdframe.forge.plugin.ai.agent.engine.service;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentCallerGuard;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentToolBindingResolver;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.provider.adapter.AiModelRuntimeOptions;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterRegistry;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.file.core.FileManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** 初始与 HITL 恢复共用服务端运行时装配，模型凭据/工具实例永远不从 Redis 恢复。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRuntimeFactory {

    private static final int DEFAULT_MAX_ITERS = 10;
    private final AiAgentService agentService;
    private final AiProviderAdapterRegistry providerRegistry;
    private final AiProviderMapper providerMapper;
    private final FileManager fileManager;
    private final AgentToolBindingResolver bindings;

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
        AiProvider provider = providerMapper.selectById(agent.getProviderId());
        if (provider == null || !Objects.equals(provider.getTenantId(), context.getTenantId())) {
            throw new BusinessException("模型供应商不存在或不可访问");
        }
        var options = new AiModelRuntimeOptions(agent.getModelName(),
                agent.getTemperature() == null ? 0.7 : agent.getTemperature().doubleValue(), agent.getMaxTokens());
        context.setChatModel(providerRegistry.createChatModel(provider, options));
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

    private String resolveFileUrl(Long fileId) {
        try {
            return fileManager.getAccessUrl(String.valueOf(fileId), 3600);
        } catch (RuntimeException ex) {
            log.warn("[AgentRuntime] 文件链接不可用: fileId={}, exceptionType={}", fileId, ex.getClass().getSimpleName());
            return null;
        }
    }
}
