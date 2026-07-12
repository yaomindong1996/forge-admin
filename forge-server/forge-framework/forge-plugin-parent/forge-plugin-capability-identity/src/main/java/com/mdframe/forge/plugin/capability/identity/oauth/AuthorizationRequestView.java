package com.mdframe.forge.plugin.capability.identity.oauth;

import java.util.Set;

public record AuthorizationRequestView(
        String clientId,
        String clientName,
        Set<String> scopes,
        Long tenantId,
        String tenantName,
        Long activeOrgId,
        String activeOrgName,
        long expiresInSeconds) {
}
