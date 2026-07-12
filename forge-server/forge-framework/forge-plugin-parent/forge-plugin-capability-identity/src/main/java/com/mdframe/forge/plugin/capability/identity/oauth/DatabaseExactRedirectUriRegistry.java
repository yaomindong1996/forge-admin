package com.mdframe.forge.plugin.capability.identity.oauth;

import com.mdframe.forge.plugin.capability.identity.mapper.AiCapabilityOAuthRedirectUriMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class DatabaseExactRedirectUriRegistry implements ExactRedirectUriRegistry {

    private final AiCapabilityOAuthRedirectUriMapper redirectUriMapper;

    public DatabaseExactRedirectUriRegistry(AiCapabilityOAuthRedirectUriMapper redirectUriMapper) {
        this.redirectUriMapper = redirectUriMapper;
    }

    @Override
    public boolean contains(Long tenantId, Long clientId, String redirectUri) {
        if (tenantId == null || clientId == null || redirectUri == null || redirectUri.isBlank()) {
            return false;
        }
        return redirectUriMapper.selectExact(
                tenantId, clientId, sha256(redirectUri), redirectUri) != null;
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }
}
