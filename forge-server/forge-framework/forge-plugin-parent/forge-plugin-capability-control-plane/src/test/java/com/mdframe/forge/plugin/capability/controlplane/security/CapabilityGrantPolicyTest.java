package com.mdframe.forge.plugin.capability.controlplane.security;

import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapability;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityGrant;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilityGrantPolicyTest {

    private final CapabilityGrantPolicy policy = new CapabilityGrantPolicy();
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 11, 22, 30);

    @Test
    void shouldAllowOnlyMatchingActiveReadGrant() {
        CapabilityGrantDecision decision = policy.evaluate(
                client(1L, 100L, "ENABLED", now.plusMinutes(10)),
                capability(1L, "READ_ONLY", "PUBLISHED", 1),
                grant(1L, "PINNED", "1.2.0", "ENABLED", now.plusMinutes(10)),
                1L, 100L, "1.2.0", now);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.resolvedVersion()).isEqualTo("1.2.0");
    }

    @Test
    void shouldRejectCrossTenantOrganizationExpiryAndWriteBehavior() {
        AiCapabilityClient client = client(1L, 100L, "ENABLED", now.plusMinutes(10));
        AiCapability capability = capability(1L, "READ_ONLY", "PUBLISHED", 1);
        AiCapabilityGrant grant = grant(1L, "PINNED", "1.2.0", "ENABLED", now.plusMinutes(10));

        assertThat(policy.evaluate(client, capability, grant, 2L, 100L, "1.2.0", now).errorCode())
                .isEqualTo("TENANT_SCOPE_VIOLATION");
        assertThat(policy.evaluate(client, capability, grant, 1L, 101L, "1.2.0", now).errorCode())
                .isEqualTo("ORG_SCOPE_VIOLATION");
        client.setExpiresAt(now.minusSeconds(1));
        assertThat(policy.evaluate(client, capability, grant, 1L, 100L, "1.2.0", now).errorCode())
                .isEqualTo("CLIENT_EXPIRED");
        client.setExpiresAt(now.plusMinutes(10));
        capability.setBehavior("ACTION");
        assertThat(policy.evaluate(client, capability, grant, 1L, 100L, "1.2.0", now).errorCode())
                .isEqualTo("CAPABILITY_NOT_READ_ONLY");
    }

    @Test
    void shouldResolveFollowMajorOnlyWithinRequestedMajor() {
        AiCapabilityGrant grant = grant(1L, "FOLLOW_MAJOR", "2.0.0", "ENABLED", null);

        assertThat(policy.evaluate(
                client(1L, 100L, "ENABLED", null),
                capability(1L, "READ_ONLY", "PUBLISHED", 1),
                grant, 1L, 100L, "2.4.1", now).allowed()).isTrue();
        assertThat(policy.evaluate(
                client(1L, 100L, "ENABLED", null),
                capability(1L, "READ_ONLY", "PUBLISHED", 1),
                grant, 1L, 100L, "3.0.0", now).errorCode()).isEqualTo("VERSION_NOT_GRANTED");
    }

    @Test
    void shouldRejectGrantThatReferencesAnotherClientOrCapability() {
        AiCapabilityClient client = client(1L, 100L, "ENABLED", null);
        AiCapability capability = capability(1L, "READ_ONLY", "PUBLISHED", 1);
        AiCapabilityGrant grant = grant(1L, "PINNED", "1.2.0", "ENABLED", null);
        grant.setClientId(999L);

        assertThat(policy.evaluate(
                client, capability, grant, 1L, 100L, "1.2.0", now).errorCode())
                .isEqualTo("GRANT_SCOPE_VIOLATION");
    }

    private AiCapabilityClient client(Long tenantId, Long orgId, String status, LocalDateTime expiresAt) {
        AiCapabilityClient client = new AiCapabilityClient();
        client.setId(10L);
        client.setTenantId(tenantId);
        client.setActiveOrgId(orgId);
        client.setServiceUserId(10L);
        client.setStatus(status);
        client.setExpiresAt(expiresAt);
        return client;
    }

    private AiCapability capability(Long tenantId, String behavior, String status, Integer enabled) {
        AiCapability capability = new AiCapability();
        capability.setId(20L);
        capability.setTenantId(tenantId);
        capability.setBehavior(behavior);
        capability.setPublishStatus(status);
        capability.setEnabled(enabled);
        return capability;
    }

    private AiCapabilityGrant grant(
            Long tenantId,
            String strategy,
            String fixedVersion,
            String status,
            LocalDateTime expiresAt) {
        AiCapabilityGrant grant = new AiCapabilityGrant();
        grant.setTenantId(tenantId);
        grant.setClientId(10L);
        grant.setCapabilityId(20L);
        grant.setVersionStrategy(strategy);
        grant.setFixedVersion(fixedVersion);
        grant.setStatus(status);
        grant.setExpiresAt(expiresAt);
        return grant;
    }
}
