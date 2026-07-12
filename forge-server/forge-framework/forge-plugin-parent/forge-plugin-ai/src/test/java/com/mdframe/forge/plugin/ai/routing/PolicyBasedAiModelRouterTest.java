package com.mdframe.forge.plugin.ai.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.health.*;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import com.mdframe.forge.plugin.ai.routing.constant.*;
import com.mdframe.forge.plugin.ai.routing.mapper.AiModelRoutePolicyMapper;
import com.mdframe.forge.plugin.ai.routing.mapper.AiModelRoutingQueryMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PolicyBasedAiModelRouterTest {
    @Test void explicitPairShouldBeDeterministicAndAuditable(){
        AiProviderService providers=mock(AiProviderService.class); AiModelMapper models=mock(AiModelMapper.class); AiModelRoutePolicyMapper policies=mock(AiModelRoutePolicyMapper.class); AiModelRoutingQueryMapper queries=mock(AiModelRoutingQueryMapper.class); AiModelHealthRegistry health=mock(AiModelHealthRegistry.class);
        AiProvider provider=new AiProvider();provider.setId(10L);provider.setStatus("0");AiModel model=new AiModel();model.setId(20L);model.setProviderId(10L);model.setModelId("qwen-plus");model.setStatus("0");AiAgent agent=new AiAgent();agent.setModelSelectionMode("PINNED");
        AiModelHealthKey key=new AiModelHealthKey(1L,10L,20L);AiModelHealthLease lease=mock(AiModelHealthLease.class);
        when(providers.getById(10L)).thenReturn(provider);when(models.selectEnabledByProviderAndModelId(10L,"qwen-plus")).thenReturn(model);when(health.snapshot(key)).thenReturn(new AiModelHealthSnapshot(AiModelHealthStatus.HEALTHY,0,null,false));when(health.tryAcquire(key)).thenReturn(Optional.of(lease));
        RoutedInvocation result=new PolicyBasedAiModelRouter(providers,models,policies,queries,health,new ObjectMapper()).route(new RouteRequest(1L,agent,10L,"qwen-plus"));
        assertEquals(AiModelRouteSource.REQUEST,result.decision().source());assertEquals(AiModelRouteReason.REQUEST_EXPLICIT_PAIR,result.decision().reason());assertSame(lease,result.healthLease());verifyNoInteractions(policies,queries);
    }
    @Test void previewShouldNotAcquireLease(){
        AiProviderService providers=mock(AiProviderService.class); AiModelMapper models=mock(AiModelMapper.class); AiModelRoutePolicyMapper policies=mock(AiModelRoutePolicyMapper.class); AiModelRoutingQueryMapper queries=mock(AiModelRoutingQueryMapper.class); AiModelHealthRegistry health=mock(AiModelHealthRegistry.class);
        AiProvider provider=new AiProvider();provider.setId(10L);provider.setStatus("0");AiModel model=new AiModel();model.setId(20L);model.setModelId("m");AiAgent agent=new AiAgent();agent.setModelSelectionMode("PINNED");AiModelHealthKey key=new AiModelHealthKey(1L,10L,20L);
        when(providers.getById(10L)).thenReturn(provider);when(models.selectEnabledByProviderAndModelId(10L,"m")).thenReturn(model);when(health.snapshot(key)).thenReturn(new AiModelHealthSnapshot(AiModelHealthStatus.HALF_OPEN,3,null,false));
        RouteDecision decision=new PolicyBasedAiModelRouter(providers,models,policies,queries,health,new ObjectMapper()).preview(new RouteRequest(1L,agent,10L,"m"));
        assertEquals("m",decision.model().getModelId());verify(health,never()).tryAcquire(any());
    }

    @Test
    void policyCandidateShouldCarryTenantAndSkipCrossTenantRows() {
        AiProviderService providers = mock(AiProviderService.class);
        AiModelMapper models = mock(AiModelMapper.class);
        AiModelRoutePolicyMapper policies = mock(AiModelRoutePolicyMapper.class);
        AiModelRoutingQueryMapper queries = mock(AiModelRoutingQueryMapper.class);
        AiModelHealthRegistry health = mock(AiModelHealthRegistry.class);
        AiAgent agent = new AiAgent();
        agent.setModelSelectionMode("POLICY");
        agent.setRoutePolicyId(30L);
        com.mdframe.forge.plugin.ai.routing.domain.AiModelRoutePolicy policy =
                new com.mdframe.forge.plugin.ai.routing.domain.AiModelRoutePolicy();
        policy.setId(30L);
        policy.setTenantId(1L);
        policy.setStatus("0");
        policy.setRequiredCapabilities("[]");
        AiModelRouteCandidate foreign = candidate(40L, 20L, 2L);
        AiModelRouteCandidate local = candidate(41L, 21L, 1L);
        AiModelHealthKey key = new AiModelHealthKey(1L, 41L, 21L);
        AiModelHealthLease lease = mock(AiModelHealthLease.class);
        when(policies.selectById(30L)).thenReturn(policy);
        when(queries.selectPolicyCandidates(30L)).thenReturn(List.of(foreign, local));
        when(health.snapshot(key)).thenReturn(new AiModelHealthSnapshot(
                AiModelHealthStatus.HEALTHY, 0, null, false));
        when(health.tryAcquire(key)).thenReturn(Optional.of(lease));

        RoutedInvocation result = new PolicyBasedAiModelRouter(
                providers, models, policies, queries, health, new ObjectMapper())
                .route(new RouteRequest(1L, agent, null, null));

        assertEquals(1L, result.decision().provider().getTenantId());
        assertEquals(1L, result.decision().model().getTenantId());
        assertEquals("TENANT_MISMATCH", result.decision().skippedCandidates().get(0).reason());
    }

    @Test
    void policyShouldSkipOpenCandidateAndUseNextHealthyCandidate() {
        AiProviderService providers = mock(AiProviderService.class);
        AiModelMapper models = mock(AiModelMapper.class);
        AiModelRoutePolicyMapper policies = mock(AiModelRoutePolicyMapper.class);
        AiModelRoutingQueryMapper queries = mock(AiModelRoutingQueryMapper.class);
        AiModelHealthRegistry health = mock(AiModelHealthRegistry.class);
        AiAgent agent = new AiAgent();
        agent.setModelSelectionMode("POLICY");
        agent.setRoutePolicyId(30L);
        com.mdframe.forge.plugin.ai.routing.domain.AiModelRoutePolicy policy =
                new com.mdframe.forge.plugin.ai.routing.domain.AiModelRoutePolicy();
        policy.setId(30L);
        policy.setTenantId(1L);
        policy.setStatus("0");
        policy.setRequiredCapabilities("[]");
        AiModelRouteCandidate first = candidate(40L, 20L, 1L);
        AiModelRouteCandidate second = candidate(41L, 21L, 1L);
        AiModelHealthKey firstKey = new AiModelHealthKey(1L, 40L, 20L);
        AiModelHealthKey secondKey = new AiModelHealthKey(1L, 41L, 21L);
        AiModelHealthLease secondLease = mock(AiModelHealthLease.class);
        when(policies.selectById(30L)).thenReturn(policy);
        when(queries.selectPolicyCandidates(30L)).thenReturn(List.of(first, second));
        when(health.snapshot(firstKey)).thenReturn(new AiModelHealthSnapshot(
                AiModelHealthStatus.OPEN, 3, null, false));
        when(health.snapshot(secondKey)).thenReturn(new AiModelHealthSnapshot(
                AiModelHealthStatus.HEALTHY, 0, null, false));
        when(health.tryAcquire(firstKey)).thenReturn(Optional.empty());
        when(health.tryAcquire(secondKey)).thenReturn(Optional.of(secondLease));

        RoutedInvocation result = new PolicyBasedAiModelRouter(
                providers, models, policies, queries, health, new ObjectMapper())
                .route(new RouteRequest(1L, agent, null, null));

        assertEquals(21L, result.decision().model().getId());
        assertEquals("HEALTH_UNAVAILABLE",
                result.decision().skippedCandidates().get(0).reason());
        verify(health).tryAcquire(firstKey);
        verify(health).tryAcquire(secondKey);
    }

    private AiModelRouteCandidate candidate(Long providerId, Long modelId, Long tenantId) {
        AiModelRouteCandidate candidate = new AiModelRouteCandidate();
        candidate.setTargetId(modelId);
        candidate.setTargetTenantId(tenantId);
        candidate.setPolicyId(30L);
        candidate.setModelPk(modelId);
        candidate.setModelTenantId(tenantId);
        candidate.setProviderPk(providerId);
        candidate.setProviderTenantId(tenantId);
        candidate.setProviderStatus("0");
        candidate.setModelStatus("0");
        candidate.setModelId("model-" + modelId);
        candidate.setPriority(modelId.intValue());
        return candidate;
    }
}
