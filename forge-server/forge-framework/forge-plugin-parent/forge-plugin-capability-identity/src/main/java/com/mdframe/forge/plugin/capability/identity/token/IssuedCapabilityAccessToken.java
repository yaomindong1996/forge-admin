package com.mdframe.forge.plugin.capability.identity.token;

public record IssuedCapabilityAccessToken(
        String rawToken,
        String keyId,
        String prefix,
        String tokenHash) {
}
