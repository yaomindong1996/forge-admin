package com.mdframe.forge.plugin.ai.routing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.health.*;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import com.mdframe.forge.plugin.ai.routing.constant.*;
import com.mdframe.forge.plugin.ai.routing.domain.AiModelRoutePolicy;
import com.mdframe.forge.plugin.ai.routing.mapper.AiModelRoutePolicyMapper;
import com.mdframe.forge.plugin.ai.routing.mapper.AiModelRoutingQueryMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

@Component
@RequiredArgsConstructor
public class PolicyBasedAiModelRouter implements AiModelRouter {
    private final AiProviderService providerService;
    private final AiModelMapper modelMapper;
    private final AiModelRoutePolicyMapper policyMapper;
    private final AiModelRoutingQueryMapper routingQueryMapper;
    private final AiModelHealthRegistry healthRegistry;
    private final ObjectMapper objectMapper;

    @Override public RoutedInvocation route(RouteRequest request) { return select(request, true); }
    @Override public RouteDecision preview(RouteRequest request) { return select(request, false).decision(); }

    private RoutedInvocation select(RouteRequest request, boolean acquire) {
        Long tenantId = requireTenantId(request.tenantId());
        AiAgent agent = Objects.requireNonNull(request.agent(), "agent");
        boolean explicitProvider = request.providerId() != null;
        boolean explicitModel = StringUtils.hasText(request.modelName());
        if (!explicitProvider && !explicitModel && AiModelSelectionMode.fromNullable(agent.getModelSelectionMode()) == AiModelSelectionMode.POLICY) {
            return selectPolicy(request, tenantId, acquire);
        }

        AiProvider provider;
        AiModelRouteSource source;
        AiModelRouteReason reason;
        if (explicitProvider) provider = requireProvider(request.providerId());
        else if (explicitModel && AiModelSelectionMode.fromNullable(agent.getModelSelectionMode()) == AiModelSelectionMode.POLICY) provider = providerService.requireEnabledDefaultProvider();
        else if (agent.getProviderId() != null) provider = requireProvider(agent.getProviderId());
        else provider = providerService.requireEnabledDefaultProvider();
        requireSameTenant(tenantId, provider.getTenantId());

        String providerModelId;
        if (explicitModel) {
            providerModelId = request.modelName().trim();
            source = AiModelRouteSource.REQUEST;
            reason = explicitProvider ? AiModelRouteReason.REQUEST_EXPLICIT_PAIR : AiModelRouteReason.REQUEST_MODEL_WITH_RESOLVED_PROVIDER;
        } else if (explicitProvider) {
            providerModelId = requireDefaultModel(provider.getId());
            source = AiModelRouteSource.REQUEST;
            reason = AiModelRouteReason.REQUEST_PROVIDER_DEFAULT;
        } else if (StringUtils.hasText(agent.getModelName())) {
            providerModelId = agent.getModelName().trim();
            source = AiModelRouteSource.PINNED;
            reason = AiModelRouteReason.PINNED_MODEL;
        } else {
            providerModelId = requireDefaultModel(provider.getId());
            source = AiModelRouteSource.PROVIDER_DEFAULT;
            reason = AiModelRouteReason.PROVIDER_DEFAULT;
        }
        AiModel model = modelMapper.selectEnabledByProviderAndModelId(provider.getId(), providerModelId);
        if (model == null) throw new BusinessException("模型不存在、已停用或不属于所选供应商: " + providerModelId);
        requireSameTenant(tenantId, model.getTenantId());
        RouteDecision decision = new RouteDecision(provider, model, source, reason, null, List.of());
        return withHealth(tenantId, decision, acquire, false);
    }

    private RoutedInvocation selectPolicy(RouteRequest request, Long tenantId, boolean acquire) {
        Long policyId = request.agent().getRoutePolicyId();
        if (policyId == null) throw new BusinessException("路由模式 Agent 未配置路由策略");
        AiModelRoutePolicy policy = policyMapper.selectById(policyId);
        if (policy == null || !"0".equals(policy.getStatus())) throw new BusinessException("模型路由策略不存在或已停用");
        requireSameTenant(tenantId, policy.getTenantId());
        Set<String> required = parseCapabilities(policy.getRequiredCapabilities());
        List<RouteCandidateSkip> skips = new ArrayList<>();
        for (AiModelRouteCandidate candidate : routingQueryMapper.selectPolicyCandidates(policyId)) {
            if (!tenantId.equals(candidate.getTargetTenantId())
                    || !tenantId.equals(candidate.getModelTenantId())
                    || !tenantId.equals(candidate.getProviderTenantId())) {
                skips.add(new RouteCandidateSkip(candidate.getModelPk(), "TENANT_MISMATCH"));
                continue;
            }
            if (!"0".equals(candidate.getProviderStatus()) || !"0".equals(candidate.getModelStatus())) { skips.add(new RouteCandidateSkip(candidate.getModelPk(), "DISABLED")); continue; }
            Set<String> capabilities = StringUtils.hasText(candidate.getCapabilityCodes()) ? Set.of(candidate.getCapabilityCodes().split(",")) : Set.of();
            if (!capabilities.containsAll(required)) { skips.add(new RouteCandidateSkip(candidate.getModelPk(), "CAPABILITY_MISMATCH")); continue; }
            AiProvider provider = toProvider(candidate, tenantId);
            AiModel model = toModel(candidate, tenantId);
            RouteDecision decision = new RouteDecision(provider, model, AiModelRouteSource.POLICY, AiModelRouteReason.POLICY_PRIORITY, policyId, List.copyOf(skips));
            try { return withHealth(tenantId, decision, acquire, true); }
            catch (UnavailableCandidateException e) { skips.add(new RouteCandidateSkip(candidate.getModelPk(), e.reason)); }
        }
        throw new BusinessException("没有满足路由策略的可用模型");
    }

    private RoutedInvocation withHealth(Long tenantId, RouteDecision decision, boolean acquire, boolean skippable) {
        AiModelHealthKey key = new AiModelHealthKey(tenantId, decision.provider().getId(), decision.model().getId());
        AiModelHealthSnapshot snapshot = healthRegistry.snapshot(key);
        if (!acquire) {
            if (snapshot.status() == AiModelHealthStatus.OPEN) {
                if (skippable) throw new UnavailableCandidateException("HEALTH_OPEN");
                throw new BusinessException("所选模型当前已熔断");
            }
            return new RoutedInvocation(decision, new PreviewLease(key));
        }
        Optional<AiModelHealthLease> lease = healthRegistry.tryAcquire(key);
        if (lease.isEmpty()) {
            if (skippable) throw new UnavailableCandidateException("HEALTH_UNAVAILABLE");
            throw new BusinessException("所选模型当前不可调用");
        }
        return new RoutedInvocation(decision, lease.get());
    }

    private AiProvider requireProvider(Long id) {
        AiProvider provider = providerService.getById(id);
        if (provider == null) throw new BusinessException("AI 供应商不存在");
        if (!"0".equals(provider.getStatus())) throw new BusinessException("AI 供应商已停用: " + provider.getProviderName());
        return provider;
    }
    private String requireDefaultModel(Long providerId) {
        String modelId = modelMapper.selectEnabledDefaultModelId(providerId);
        if (!StringUtils.hasText(modelId)) throw new BusinessException("请为供应商设置默认模型");
        return modelId.trim();
    }

    private Long requireTenantId(Long tenantId) {
        if (tenantId == null) throw new BusinessException("无法确定当前租户");
        return tenantId;
    }

    private void requireSameTenant(Long expectedTenantId, Long actualTenantId) {
        if (actualTenantId != null && !expectedTenantId.equals(actualTenantId)) {
            throw new BusinessException("模型路由资源不属于当前租户");
        }
    }
    private Set<String> parseCapabilities(String json) {
        if (!StringUtils.hasText(json)) return Set.of();
        try {
            List<String> values = objectMapper.readValue(json, new TypeReference<>() {});
            Set<String> result = new HashSet<>();
            for (String value : values) result.add(AiModelCapabilityCode.require(value));
            return result;
        } catch (Exception e) { throw new BusinessException("路由策略能力配置无效"); }
    }
    private AiProvider toProvider(AiModelRouteCandidate c, Long tenantId) { AiProvider p = new AiProvider(); p.setId(c.getProviderPk()); p.setTenantId(tenantId); p.setProviderName(c.getProviderName()); p.setProviderType(c.getProviderType()); p.setAdapterCode(c.getAdapterCode()); p.setApiKey(c.getApiKey()); p.setBaseUrl(c.getBaseUrl()); p.setStatus(c.getProviderStatus()); return p; }
    private AiModel toModel(AiModelRouteCandidate c, Long tenantId) { AiModel m = new AiModel(); m.setId(c.getModelPk()); m.setTenantId(tenantId); m.setProviderId(c.getProviderPk()); m.setModelId(c.getModelId()); m.setModelName(c.getModelName()); m.setModelType(c.getModelType()); m.setStatus(c.getModelStatus()); m.setMaxTokens(c.getMaxTokens()); m.setContextWindow(c.getContextWindow()); m.setInputPricePerMillionCent(c.getInputPricePerMillionCent()); m.setOutputPricePerMillionCent(c.getOutputPricePerMillionCent()); return m; }
    private static final class UnavailableCandidateException extends RuntimeException { private final String reason; private UnavailableCandidateException(String reason) { this.reason = reason; } }
    private record PreviewLease(AiModelHealthKey key) implements AiModelHealthLease { public void success() {} public void failure(AiModelFailureCategory c) {} public void cancel() {} public void abort() {} }
}
