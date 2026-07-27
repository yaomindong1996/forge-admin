package com.mdframe.forge.plugin.system.security;

import org.springframework.stereotype.Component;

@Component
public class ClientCredentialPolicy {

    public static final String AUTH_METHOD_NONE = "none";
    public static final String AUTH_METHOD_CLIENT_SECRET = "client_secret";

    private final ClientSecretCodec clientSecretCodec;

    public ClientCredentialPolicy(ClientSecretCodec clientSecretCodec) {
        this.clientSecretCodec = clientSecretCodec;
    }

    public String normalizeAuthMethod(String authMethod) {
        String normalized = authMethod == null || authMethod.isBlank()
                ? AUTH_METHOD_CLIENT_SECRET
                : authMethod.trim();
        if (!AUTH_METHOD_NONE.equals(normalized) && !AUTH_METHOD_CLIENT_SECRET.equals(normalized)) {
            throw new IllegalArgumentException("不支持的客户端认证方式");
        }
        return normalized;
    }

    public boolean requiresSecret(String authMethod) {
        return AUTH_METHOD_CLIENT_SECRET.equals(normalizeAuthMethod(authMethod));
    }

    public String resolveCreateSecret(String authMethod, String rawSecret) {
        if (!requiresSecret(authMethod)) {
            return null;
        }
        if (rawSecret == null || rawSecret.isBlank()) {
            throw new IllegalArgumentException("机密客户端的应用密钥不能为空");
        }
        return clientSecretCodec.encode(rawSecret);
    }

    public String resolveUpdateSecret(String authMethod, String rawSecret, String existingStoredSecret) {
        if (!requiresSecret(authMethod)) {
            return null;
        }
        if (rawSecret == null || rawSecret.isBlank()) {
            if (existingStoredSecret == null || existingStoredSecret.isBlank()) {
                throw new IllegalArgumentException("切换为机密客户端时必须设置应用密钥");
            }
            return existingStoredSecret;
        }
        return clientSecretCodec.encode(rawSecret);
    }
}
