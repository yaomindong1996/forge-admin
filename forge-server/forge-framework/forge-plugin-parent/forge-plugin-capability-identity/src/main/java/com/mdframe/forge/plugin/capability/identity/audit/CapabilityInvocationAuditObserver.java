package com.mdframe.forge.plugin.capability.identity.audit;

import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;
import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityInvocationAuditEvent;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapability;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityInvocationAuditService;
import com.mdframe.forge.plugin.capability.model.CapabilityInvocation;
import com.mdframe.forge.plugin.capability.model.CapabilityResult;
import com.mdframe.forge.plugin.capability.spi.CapabilityInvocationObserver;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "forge.capability.identity", name = "enabled", havingValue = "true")
public class CapabilityInvocationAuditObserver implements CapabilityInvocationObserver {

    private final CapabilityCatalogService catalogService;
    private final CapabilityInvocationAuditService auditService;

    public CapabilityInvocationAuditObserver(
            CapabilityCatalogService catalogService,
            CapabilityInvocationAuditService auditService) {
        this.catalogService = catalogService;
        this.auditService = auditService;
    }

    @Override
    public void onCompleted(
            CapabilityInvocation invocation,
            CapabilityResult result,
            String schemaPath) {
        ExecutionIdentity identity = ExecutionIdentityContextHolder.current()
                .orElseThrow(() -> new IllegalStateException("能力调用缺少受信执行身份"));
        CapabilityActorType actorType = CapabilityActorType.valueOf(identity.actorType());
        AiCapability capability = catalogService.getByCode(
                invocation.caller().tenantId(), invocation.capabilityCode());
        String errorCode = result.errorCode();
        String resultCode = errorCode == null ? result.status().name() : errorCode;
        auditService.record(invocation.caller().tenantId(), new CapabilityInvocationAuditEvent(
                result.requestId(), identity.clientId(), identity.clientCode(),
                capability == null ? null : capability.getId(), invocation.capabilityCode(),
                invocation.version(), actorType, identity.actorUserId(), identity.serviceUserId(),
                invocation.caller().activeOrgId(), result.status(), resultCode, errorCode,
                schemaPath, safeTraceId(), result.durationMs()));
    }

    private String safeTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null && traceId.matches("^[A-Za-z0-9_-]{1,64}$") ? traceId : null;
    }
}
