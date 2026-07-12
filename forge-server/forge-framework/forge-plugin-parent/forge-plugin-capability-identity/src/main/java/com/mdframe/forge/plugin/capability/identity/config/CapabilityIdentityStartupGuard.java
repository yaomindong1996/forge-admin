package com.mdframe.forge.plugin.capability.identity.config;

import com.mdframe.forge.plugin.capability.controlplane.config.CapabilityControlPlaneProperties;

import java.util.HashSet;
import java.util.Set;

public final class CapabilityIdentityStartupGuard {

    public CapabilityIdentityStartupGuard(
            CapabilityIdentityProperties identityProperties,
            CapabilityControlPlaneProperties controlPlaneProperties) {
        identityProperties.validatedIssuer();
        identityProperties.validatedResource();
        identityProperties.validatedAccessTokenTtl();
        identityProperties.validatedAuthorizationCodeTtl();
        identityProperties.validatedAccessTokenRetention();
        identityProperties.validatedLastUsedTouchInterval();

        String clientPepper = controlPlaneProperties.getClientPepper();
        String tokenPepper = identityProperties.getTokenPepper();
        String codePepper = identityProperties.getAuthorizationCodePepper();
        if (clientPepper == null || clientPepper.length() < 16) {
            throw new IllegalStateException("Forge Capability Client Pepper 未配置或长度不足 16 位");
        }
        if (tokenPepper == null || tokenPepper.length() < 32) {
            throw new IllegalStateException("Forge MCP Token Pepper 未配置或长度不足 32 位");
        }
        if (codePepper == null || codePepper.length() < 32) {
            throw new IllegalStateException("Forge MCP Authorization Code Pepper 未配置或长度不足 32 位");
        }
        Set<String> peppers = new HashSet<>(Set.of(clientPepper, tokenPepper, codePepper));
        if (peppers.size() != 3) {
            throw new IllegalStateException("Forge MCP 三类 Pepper 必须互不相同");
        }
    }
}
