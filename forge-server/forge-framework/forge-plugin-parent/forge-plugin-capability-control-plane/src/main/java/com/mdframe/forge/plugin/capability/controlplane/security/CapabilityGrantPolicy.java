package com.mdframe.forge.plugin.capability.controlplane.security;

import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapability;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityGrant;

import java.time.LocalDateTime;
import java.util.Objects;

public final class CapabilityGrantPolicy {

    public CapabilityGrantDecision evaluate(
            AiCapabilityClient client,
            AiCapability capability,
            AiCapabilityGrant grant,
            Long callerTenantId,
            Long callerActiveOrgId,
            String requestedVersion,
            LocalDateTime now) {
        if (client == null || capability == null || grant == null) {
            return CapabilityGrantDecision.deny("GRANT_NOT_FOUND");
        }
        if (!Objects.equals(callerTenantId, client.getTenantId())
                || !Objects.equals(client.getTenantId(), capability.getTenantId())
                || !Objects.equals(client.getTenantId(), grant.getTenantId())) {
            return CapabilityGrantDecision.deny("TENANT_SCOPE_VIOLATION");
        }
        if (client.getId() == null || capability.getId() == null
                || !Objects.equals(client.getId(), grant.getClientId())
                || !Objects.equals(capability.getId(), grant.getCapabilityId())) {
            return CapabilityGrantDecision.deny("GRANT_SCOPE_VIOLATION");
        }
        if (!Objects.equals(callerActiveOrgId, client.getActiveOrgId())) {
            return CapabilityGrantDecision.deny("ORG_SCOPE_VIOLATION");
        }
        if (client.getServiceUserId() == null || client.getServiceUserId() <= 0
                || client.getActiveOrgId() == null || client.getActiveOrgId() <= 0) {
            return CapabilityGrantDecision.deny("CLIENT_IDENTITY_INVALID");
        }
        if (!"ENABLED".equals(client.getStatus())) {
            return CapabilityGrantDecision.deny("CLIENT_DISABLED");
        }
        if (expired(client.getExpiresAt(), now)) {
            return CapabilityGrantDecision.deny("CLIENT_EXPIRED");
        }
        if (!"PUBLISHED".equals(capability.getPublishStatus())
                || !Integer.valueOf(1).equals(capability.getEnabled())) {
            return CapabilityGrantDecision.deny("CAPABILITY_DISABLED");
        }
        if (!"READ_ONLY".equals(capability.getBehavior())
                || "HIGH".equals(capability.getRiskLevel())) {
            return CapabilityGrantDecision.deny("CAPABILITY_NOT_READ_ONLY");
        }
        if (!"ENABLED".equals(grant.getStatus())) {
            return CapabilityGrantDecision.deny("GRANT_DISABLED");
        }
        if (expired(grant.getExpiresAt(), now)) {
            return CapabilityGrantDecision.deny("GRANT_EXPIRED");
        }
        if (requestedVersion == null || requestedVersion.isBlank()) {
            return CapabilityGrantDecision.deny("VERSION_REQUIRED");
        }
        if ("PINNED".equals(grant.getVersionStrategy())) {
            return requestedVersion.equals(grant.getFixedVersion())
                    ? CapabilityGrantDecision.allow(requestedVersion)
                    : CapabilityGrantDecision.deny("VERSION_NOT_GRANTED");
        }
        if ("FOLLOW_MAJOR".equals(grant.getVersionStrategy())) {
            String grantedMajor = major(grant.getFixedVersion());
            String requestedMajor = major(requestedVersion);
            return grantedMajor != null && grantedMajor.equals(requestedMajor)
                    ? CapabilityGrantDecision.allow(requestedVersion)
                    : CapabilityGrantDecision.deny("VERSION_NOT_GRANTED");
        }
        return CapabilityGrantDecision.deny("VERSION_STRATEGY_UNSUPPORTED");
    }

    private boolean expired(LocalDateTime expiresAt, LocalDateTime now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    private String major(String version) {
        if (version == null || !version.matches("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$")) {
            return null;
        }
        return version.substring(0, version.indexOf('.'));
    }
}
