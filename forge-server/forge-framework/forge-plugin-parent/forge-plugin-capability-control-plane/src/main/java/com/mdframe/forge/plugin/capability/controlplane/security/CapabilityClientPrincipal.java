package com.mdframe.forge.plugin.capability.controlplane.security;

public record CapabilityClientPrincipal(
        Long clientId,
        String clientCode,
        Long tenantId,
        Long serviceUserId,
        Long activeOrgId,
        Integer credentialVersion) {
}
