package com.mdframe.forge.plugin.ai.invocation.service;

import com.mdframe.forge.plugin.ai.invocation.AiInvocationObservation;
import com.mdframe.forge.plugin.ai.invocation.domain.AiModelInvocationLog;
import com.mdframe.forge.plugin.ai.invocation.mapper.AiModelInvocationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j @Service @RequiredArgsConstructor
public class AiModelInvocationRecorder {
    private final AiModelInvocationLogMapper mapper;

    public void record(AiInvocationObservation o) {
        if (o == null || !StringUtils.hasText(o.requestId()) || o.tenantId() == null || !StringUtils.hasText(o.agentCode()) || o.phase() == null || o.outcome() == null || o.latencyMillis() < 0) throw new IllegalArgumentException("调用观察必填字段不完整");
        AiModelInvocationLog logEntity = new AiModelInvocationLog();
        logEntity.setTenantId(o.tenantId()); logEntity.setRequestId(o.requestId()); logEntity.setUserId(o.userId()); logEntity.setAgentCode(o.agentCode()); logEntity.setSessionId(o.sessionId());
        logEntity.setPhase(o.phase().name()); logEntity.setDispatched(o.dispatched()); logEntity.setOutcome(o.outcome().name());
        logEntity.setRouteSource(o.routeSource() == null ? null : o.routeSource().name()); logEntity.setRouteReason(o.routeReason() == null ? null : o.routeReason().name()); logEntity.setRoutePolicyId(o.policyId());
        logEntity.setProviderId(o.providerPk()); logEntity.setModelId(o.modelPk()); logEntity.setProviderModelId(o.providerModelId()); logEntity.setAdapterCode(o.adapterCode());
        logEntity.setErrorCategory(o.errorCategory() == null ? null : o.errorCategory().name()); logEntity.setHttpStatus(o.httpStatus()); logEntity.setErrorCode(normalizeErrorCode(o.errorCode())); logEntity.setLatencyMs(o.latencyMillis());
        boolean usage = o.promptTokens() != null && o.completionTokens() != null && o.totalTokens() != null;
        logEntity.setUsageAvailable(usage); logEntity.setPromptTokens(usage ? o.promptTokens() : null); logEntity.setCompletionTokens(usage ? o.completionTokens() : null); logEntity.setTotalTokens(usage ? o.totalTokens() : null);
        boolean cost = usage && o.inputPricePerMillionCent() != null && o.outputPricePerMillionCent() != null;
        logEntity.setCostAvailable(cost); logEntity.setInputPricePerMillionCent(o.inputPricePerMillionCent()); logEntity.setOutputPricePerMillionCent(o.outputPricePerMillionCent());
        try { mapper.insert(logEntity); } catch (DuplicateKeyException ignored) { log.debug("[AI调用治理] requestId已记录, requestId={}", o.requestId()); }
    }

    private String normalizeErrorCode(String code) { return StringUtils.hasText(code) && code.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}") ? code : null; }
}
