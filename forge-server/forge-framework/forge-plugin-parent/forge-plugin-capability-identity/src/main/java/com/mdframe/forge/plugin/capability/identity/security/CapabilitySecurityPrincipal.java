package com.mdframe.forge.plugin.capability.identity.security;

import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;

import java.util.Set;

public record CapabilitySecurityPrincipal(
        Long clientId,
        String clientCode,
        CapabilityActorType actorType,
        Long actorUserId,
        Long serviceUserId,
        Long tenantId,
        Long activeOrgId,
        Integer credentialVersion,
        String tokenId,
        String audience,
        Set<String> scopes) {

    public CapabilitySecurityPrincipal {
        scopes = Set.copyOf(scopes);
    }
}
