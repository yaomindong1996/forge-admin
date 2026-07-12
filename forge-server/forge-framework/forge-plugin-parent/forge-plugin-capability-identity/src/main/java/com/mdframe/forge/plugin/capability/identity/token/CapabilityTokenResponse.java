package com.mdframe.forge.plugin.capability.identity.token;

public record CapabilityTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String scope,
        String resource) {
}
