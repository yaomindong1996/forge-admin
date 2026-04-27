package com.mdframe.forge.plugin.ai.client;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiInvocationResolverTest {

    @Mock
    private AiAgentService agentService;

    @Mock
    private AiProviderService providerService;

    @Test
    void resolveShouldPreferExplicitOverrides() {
        AiInvocationResolver resolver = new AiInvocationResolver(agentService, providerService);
        AiAgent agent = buildAgent(2L, "agent-model", new BigDecimal("0.6"), 256);
        AiProvider provider = buildProvider(9L, "provider-model");

        when(agentService.getByCode("copilot")).thenReturn(agent);
        when(providerService.getById(9L)).thenReturn(provider);

        AiInvocationResolver.ResolvedInvocation resolved = resolver.resolve("copilot", 9L,
                "custom-model", 0.2D, 512);

        assertEquals(agent, resolved.agent());
        assertEquals(provider, resolved.provider());
        assertEquals("custom-model", resolved.model());
        assertEquals(0.2D, resolved.temperature());
        assertEquals(512, resolved.maxTokens());
    }

    @Test
    void resolveShouldFallbackToAgentAndProviderDefaults() {
        AiInvocationResolver resolver = new AiInvocationResolver(agentService, providerService);
        AiAgent agent = buildAgent(2L, null, null, 300);
        AiProvider provider = buildProvider(2L, "provider-default-model");

        when(agentService.getByCode("copilot")).thenReturn(agent);
        when(providerService.getById(2L)).thenReturn(provider);

        AiInvocationResolver.ResolvedInvocation resolved = resolver.resolve("copilot", null,
                null, null, null);

        assertEquals("provider-default-model", resolved.model());
        assertEquals(0.7D, resolved.temperature());
        assertEquals(300, resolved.maxTokens());
    }

    private AiAgent buildAgent(Long providerId, String modelName, BigDecimal temperature, Integer maxTokens) {
        AiAgent agent = new AiAgent();
        agent.setAgentCode("copilot");
        agent.setProviderId(providerId);
        agent.setModelName(modelName);
        agent.setTemperature(temperature);
        agent.setMaxTokens(maxTokens);
        return agent;
    }

    private AiProvider buildProvider(Long id, String defaultModel) {
        AiProvider provider = new AiProvider();
        provider.setId(id);
        provider.setProviderName("OpenAI");
        provider.setProviderType("openai");
        provider.setBaseUrl("https://api.example.com");
        provider.setApiKey("test-key");
        provider.setDefaultModel(defaultModel);
        provider.setStatus("0");
        return provider;
    }
}
