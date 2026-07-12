package com.mdframe.forge.plugin.capability.identity.oauth;

import java.util.Set;

public record DelegationAuthorizationCode(
        Long clientId,
        String clientCode,
        Integer credentialVersion,
        Long actorUserId,
        Long serviceUserId,
        Long tenantId,
        Long activeOrgId,
        String redirectUri,
        String resource,
        Set<String> scopes,
        String codeChallenge) {

    public DelegationAuthorizationCode {
        scopes = Set.copyOf(scopes);
    }
}
