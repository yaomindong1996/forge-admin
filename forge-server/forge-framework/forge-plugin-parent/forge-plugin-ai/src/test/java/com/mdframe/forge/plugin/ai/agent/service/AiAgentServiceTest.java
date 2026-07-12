package com.mdframe.forge.plugin.ai.agent.service;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.mapper.AiAgentMapper;
import com.mdframe.forge.plugin.ai.routing.domain.AiModelRoutePolicy;
import com.mdframe.forge.plugin.ai.routing.mapper.AiModelRoutePolicyMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAgentServiceTest {

    @Mock
    private AiAgentMapper agentMapper;

    @Mock
    private AiModelRoutePolicyMapper policyMapper;

    private AiAgentService service;

    @BeforeEach
    void setUp() {
        service = new AiAgentService(policyMapper);
        ReflectionTestUtils.setField(service, "baseMapper", agentMapper);
    }

    @Test
    void pinnedModeShouldRequireProviderAndClearPolicy() {
        AiAgent agent = agent("PINNED", 10L, 20L);
        when(agentMapper.insert(agent)).thenReturn(1);

        service.createAgent(agent);

        assertNull(agent.getRoutePolicyId());
        verify(policyMapper, never()).selectById(any());
    }

    @Test
    void policyModeShouldRequireEnabledPolicy() {
        AiAgent agent = agent("POLICY", null, 20L);
        AiModelRoutePolicy policy = new AiModelRoutePolicy();
        policy.setId(20L);
        policy.setStatus("0");
        when(policyMapper.selectById(20L)).thenReturn(policy);
        when(agentMapper.insert(agent)).thenReturn(1);

        service.createAgent(agent);

        assertEquals("POLICY", agent.getModelSelectionMode());
        verify(agentMapper).insert(agent);
    }

    @Test
    void policyModeShouldRejectMissingOrDisabledPolicy() {
        AiAgent missingPolicy = agent("POLICY", null, null);
        assertThrows(BusinessException.class, () -> service.createAgent(missingPolicy));

        AiAgent disabledPolicy = agent("POLICY", null, 20L);
        AiModelRoutePolicy policy = new AiModelRoutePolicy();
        policy.setId(20L);
        policy.setStatus("1");
        when(policyMapper.selectById(20L)).thenReturn(policy);

        assertThrows(BusinessException.class, () -> service.createAgent(disabledPolicy));
        verify(agentMapper, never()).insert(any(AiAgent.class));
    }

    private AiAgent agent(String mode, Long providerId, Long policyId) {
        AiAgent agent = new AiAgent();
        agent.setAgentCode("test-agent");
        agent.setModelSelectionMode(mode);
        agent.setProviderId(providerId);
        agent.setRoutePolicyId(policyId);
        return agent;
    }
}
