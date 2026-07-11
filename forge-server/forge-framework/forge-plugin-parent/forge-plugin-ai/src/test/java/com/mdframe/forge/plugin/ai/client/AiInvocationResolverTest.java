package com.mdframe.forge.plugin.ai.client;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.health.AiModelHealthLease;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.routing.AiModelRouter;
import com.mdframe.forge.plugin.ai.routing.RouteDecision;
import com.mdframe.forge.plugin.ai.routing.RouteRequest;
import com.mdframe.forge.plugin.ai.routing.RoutedInvocation;
import com.mdframe.forge.plugin.ai.routing.constant.AiModelRouteReason;
import com.mdframe.forge.plugin.ai.routing.constant.AiModelRouteSource;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiInvocationResolverTest {

    @Mock
    private AiAgentService agentService;

    @Mock
    private AiModelRouter modelRouter;

    @Mock
    private AiModelHealthLease healthLease;

    @Test
    void resolveShouldAlwaysDelegateModelSelectionToRouter() {
        AiInvocationResolver resolver = new AiInvocationResolver(agentService, modelRouter);
        AiAgent agent = buildAgent();
        AiProvider provider = buildProvider();
        AiModel model = buildModel();
        when(agentService.getByCode("copilot")).thenReturn(agent);
        when(modelRouter.route(org.mockito.ArgumentMatchers.any(RouteRequest.class)))
                .thenReturn(new RoutedInvocation(new RouteDecision(
                        provider, model, AiModelRouteSource.REQUEST,
                        AiModelRouteReason.REQUEST_EXPLICIT_PAIR, null, List.of()), healthLease));

        AiInvocationResolver.ResolvedInvocation resolved = resolver.resolve(
                "copilot", 9L, "custom-model", 0.2D, 512);

        assertEquals(agent, resolved.agent());
        assertEquals(provider, resolved.provider());
        assertEquals("custom-model", resolved.model());
        assertEquals(0.2D, resolved.temperature());
        assertEquals(512, resolved.maxTokens());
        ArgumentCaptor<RouteRequest> requestCaptor = ArgumentCaptor.forClass(RouteRequest.class);
        verify(modelRouter).route(requestCaptor.capture());
        assertEquals(1L, requestCaptor.getValue().tenantId());
        assertEquals(9L, requestCaptor.getValue().providerId());
        assertEquals("custom-model", requestCaptor.getValue().modelName());
    }

    @Test
    void resolveShouldKeepAgentRuntimeDefaultsOutsideRoutingDecision() {
        AiInvocationResolver resolver = new AiInvocationResolver(agentService, modelRouter);
        AiAgent agent = buildAgent();
        AiProvider provider = buildProvider();
        AiModel model = buildModel();
        when(agentService.getByCode("copilot")).thenReturn(agent);
        when(modelRouter.route(org.mockito.ArgumentMatchers.any(RouteRequest.class)))
                .thenReturn(new RoutedInvocation(new RouteDecision(
                        provider, model, AiModelRouteSource.PINNED,
                        AiModelRouteReason.PINNED_MODEL, null, List.of()), healthLease));

        AiInvocationResolver.ResolvedInvocation resolved = resolver.resolve(
                "copilot", null, null, null, null);

        assertEquals(0.6D, resolved.temperature());
        assertEquals(256, resolved.maxTokens());
    }

    @Test
    void resolveShouldFailBeforeRoutingWhenAgentIsMissing() {
        AiInvocationResolver resolver = new AiInvocationResolver(agentService, modelRouter);
        when(agentService.getByCode("missing")).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> resolver.resolve("missing", null, null, null, null));

        assertEquals("Agent 不存在或已停用: missing", exception.getMessage());
    }

    private AiAgent buildAgent() {
        AiAgent agent = new AiAgent();
        agent.setTenantId(1L);
        agent.setAgentCode("copilot");
        agent.setTemperature(new BigDecimal("0.6"));
        agent.setMaxTokens(256);
        return agent;
    }

    private AiProvider buildProvider() {
        AiProvider provider = new AiProvider();
        provider.setId(9L);
        provider.setTenantId(1L);
        provider.setProviderName("OpenAI Compatible");
        return provider;
    }

    private AiModel buildModel() {
        AiModel model = new AiModel();
        model.setId(99L);
        model.setTenantId(1L);
        model.setProviderId(9L);
        model.setModelId("custom-model");
        return model;
    }
}
