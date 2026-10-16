package com.mdframe.forge.plugin.ai.agent.engine.tool;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.engine.tool.registry.AgentToolRegistry;
import com.mdframe.forge.plugin.ai.agenttool.domain.AiAgentToolConfig;
import com.mdframe.forge.plugin.ai.agenttool.service.AgentToolService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentToolBindingResolverTest {

    private final AgentToolService service = mock(AgentToolService.class);
    private final AgentToolRegistry registry = mock(AgentToolRegistry.class);
    private final AgentToolBindingResolver resolver = new AgentToolBindingResolver(service, registry);

    @Test
    void should_resolveBySourceAndDeduplicate_when_enabledBindingsAreLoaded() {
        AiAgent agent = agent();
        AgentTool builtin = mock(AgentTool.class);
        AgentTool mcp = mock(AgentTool.class);
        AgentTool capability = mock(AgentTool.class);
        when(service.listEnabledByAgentId(21L)).thenReturn(List.of(config("builtin", "lookup"),
                config("builtin", "lookup"), config("mcp", "remote"), config("capability", "action"),
                config("builtin", "unregistered")));
        when(registry.getTool("builtin", "lookup")).thenReturn(builtin);
        when(registry.getTool("mcp", "remote")).thenReturn(mcp);
        when(registry.getTool("capability", "action")).thenReturn(capability);
        assertThat(resolver.resolve(agent)).containsExactly(builtin, mcp, capability);
        verify(registry, never()).getAllTools();
    }

    @Test
    void should_notGrantAllRegistryTools_when_agentHasNoBindings() {
        when(service.listEnabledByAgentId(21L)).thenReturn(List.of());
        assertThat(resolver.resolve(agent())).isEmpty();
        verify(registry, never()).getAllTools();
    }

    @Test
    void should_preserveExplicitKnowledgeFallback_when_ragIsNotDisabled() {
        AiAgent agent = agent();
        agent.setKnowledgeIds("[501]");
        AgentTool rag = mock(AgentTool.class);
        when(registry.getTool("builtin", "rag_search")).thenReturn(rag);
        assertThat(resolver.resolve(agent)).containsExactly(rag);
        agent.setRagMode("none");
        assertThat(resolver.resolve(agent)).isEmpty();
        agent.setRagMode(null);
        agent.setKnowledgeIds("invalid-json");
        assertThat(resolver.resolve(agent)).isEmpty();
    }

    private AiAgent agent() {
        AiAgent agent = new AiAgent();
        agent.setId(21L);
        return agent;
    }

    private AiAgentToolConfig config(String source, String key) {
        AiAgentToolConfig config = new AiAgentToolConfig();
        config.setToolSource(source);
        config.setToolKey(key);
        return config;
    }
}
