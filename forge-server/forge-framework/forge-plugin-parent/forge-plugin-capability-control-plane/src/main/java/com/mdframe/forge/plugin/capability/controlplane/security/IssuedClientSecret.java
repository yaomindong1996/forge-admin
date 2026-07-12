package com.mdframe.forge.plugin.capability.controlplane.security;

public record IssuedClientSecret(
        String rawSecret,
        String keyId,
        String keyPrefix,
        String keyHash) {
}
