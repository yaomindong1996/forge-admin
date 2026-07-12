package com.mdframe.forge.plugin.capability.identity.token;

import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;

import java.util.Set;

public record CapabilityTokenIssueCommand(
        Long clientId,
        Integer credentialVersion,
        CapabilityActorType actorType,
        Long actorUserId,
        Long serviceUserId,
        Long tenantId,
        Long activeOrgId,
        String audience,
        Set<String> scopes) {

    public CapabilityTokenIssueCommand {
        scopes = Set.copyOf(scopes);
    }
}
