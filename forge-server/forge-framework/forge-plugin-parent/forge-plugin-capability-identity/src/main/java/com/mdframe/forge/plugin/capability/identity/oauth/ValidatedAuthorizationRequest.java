package com.mdframe.forge.plugin.capability.identity.oauth;

import java.util.Set;

public record ValidatedAuthorizationRequest(
        Long clientId,
        String clientCode,
        Long tenantId,
        String redirectUri,
        String resource,
        Set<String> scopes,
        String codeChallenge,
        String state) {

    public ValidatedAuthorizationRequest {
        scopes = Set.copyOf(scopes);
    }
}
