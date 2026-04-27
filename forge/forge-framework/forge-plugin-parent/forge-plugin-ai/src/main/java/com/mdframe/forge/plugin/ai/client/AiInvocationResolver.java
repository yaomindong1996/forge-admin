package com.mdframe.forge.plugin.ai.client;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AiInvocationResolver {

    private final AiAgentService agentService;
    private final AiProviderService providerService;

    public ResolvedInvocation resolve(String agentCode, Long providerId, String modelName,
                                      Double temperature, Integer maxTokens) {
        AiAgent agent = resolveAgent(agentCode);
        AiProvider provider = resolveProvider(providerId, agent);
        return new ResolvedInvocation(
                agent,
                provider,
                resolveModel(modelName, agent, provider),
                resolveTemperature(temperature, agent),
                resolveMaxTokens(maxTokens, agent)
        );
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
        if (provider == null && agent.getProviderId() != null) {
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
        if (StringUtils.hasText(agent.getModelName())) {
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
        if (agent.getTemperature() != null) {
            return agent.getTemperature().doubleValue();
        }
        return 0.7D;
    }

    private Integer resolveMaxTokens(Integer maxTokens, AiAgent agent) {
        if (maxTokens != null) {
            return maxTokens;
        }
        return agent.getMaxTokens();
    }

    public record ResolvedInvocation(
            AiAgent agent,
            AiProvider provider,
            String model,
            Double temperature,
            Integer maxTokens
    ) {
    }
}
