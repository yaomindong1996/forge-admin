package com.mdframe.forge.plugin.ai.invocation;

import com.mdframe.forge.plugin.ai.health.AiModelFailureCategory;
import com.mdframe.forge.plugin.ai.routing.constant.AiModelRouteReason;
import com.mdframe.forge.plugin.ai.routing.constant.AiModelRouteSource;

public record AiInvocationObservation(
        String requestId, Long tenantId, Long userId, String agentCode, String sessionId,
        AiInvocationPhase phase, boolean dispatched, AiInvocationOutcome outcome,
        AiModelRouteSource routeSource, AiModelRouteReason routeReason, Long policyId,
        Long providerPk, Long modelPk, String providerModelId, String adapterCode,
        AiModelFailureCategory errorCategory, Integer httpStatus, String errorCode,
        long latencyMillis, Long promptTokens, Long completionTokens, Long totalTokens,
        Long inputPricePerMillionCent, Long outputPricePerMillionCent) { }
