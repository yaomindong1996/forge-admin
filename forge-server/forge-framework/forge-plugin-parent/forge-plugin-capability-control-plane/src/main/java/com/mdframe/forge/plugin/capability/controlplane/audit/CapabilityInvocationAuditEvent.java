package com.mdframe.forge.plugin.capability.controlplane.audit;

import com.mdframe.forge.plugin.capability.model.CapabilityResultStatus;

public record CapabilityInvocationAuditEvent(
        String requestId,
        Long clientId,
        String clientCode,
        Long capabilityId,
        String capabilityCode,
        String capabilityVersion,
        CapabilityActorType actorType,
        Long actorUserId,
        Long serviceUserId,
        Long activeOrgId,
        CapabilityResultStatus resultStatus,
        String resultCode,
        String errorCode,
        String schemaPath,
        String traceId,
        long durationMs) {
}
