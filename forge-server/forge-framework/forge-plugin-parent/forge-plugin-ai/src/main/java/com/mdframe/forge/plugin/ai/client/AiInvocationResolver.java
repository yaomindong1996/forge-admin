package com.mdframe.forge.plugin.ai.client;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.plugin.ai.routing.AiModelRouter;
import com.mdframe.forge.plugin.ai.routing.RouteRequest;
import com.mdframe.forge.plugin.ai.routing.RoutedInvocation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AiInvocationResolver {

    private final AiAgentService agentService;
    private final AiModelRouter modelRouter;

    public ResolvedInvocation resolve(String agentCode, Long providerId, String modelName,
                                      Double temperature, Integer maxTokens) {
        AiAgent agent = resolveAgent(agentCode);
        Long tenantId = agent.getTenantId() != null ? agent.getTenantId() : SessionHelper.getTenantId();
        RoutedInvocation routed = modelRouter.route(new RouteRequest(tenantId, agent, providerId, modelName));
        return new ResolvedInvocation(agent, routed.decision().provider(), routed.decision().model().getModelId(),
                resolveTemperature(temperature, agent), resolveMaxTokens(maxTokens, agent), routed);
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
            , RoutedInvocation routedInvocation
    ) {
        public ResolvedInvocation(AiAgent agent, AiProvider provider, String model, Double temperature, Integer maxTokens) {
            this(agent, provider, model, temperature, maxTokens, null);
        }
    }
}
