package com.mdframe.forge.starter.core.util;

import java.util.Locale;
import java.util.Set;

/**
 * 部署级 crypto 密钥字段策略。
 * 这些值只能来自环境变量、挂载配置或 Secret Manager，不能从数据库配置中心读写。
 */
public final class CryptoDeploymentSecretPolicy {

    private static final Set<String> EXACT_PROPERTY_KEYS = Set.of(
            "forge.crypto.secretkey",
            "forge.crypto.rsapublickey",
            "forge.crypto.rsaprivatekey",
            "forge.crypto.persistence.activekey",
            "forge.crypto.persistence.legacykey",
            "forge.crypto.persistence.keys",
            "forge.crypto.persistence.historicalkeys"
    );

    private static final Set<String> PROPERTY_KEY_PREFIXES = Set.of(
            "forge.crypto.persistence.keys",
            "forge.crypto.persistence.historicalkeys"
    );

    private static final Set<String> JSON_SECRET_FIELDS = Set.of(
            "secretkey",
            "rsapublickey",
            "rsaprivatekey",
            "persistence",
            "activekey",
            "legacykey",
            "keys",
            "historicalkeys"
    );

    private CryptoDeploymentSecretPolicy() {
    }

    public static boolean isDeploymentSecretPropertyKey(String key) {
        String normalized = normalize(key);
        if (normalized.isEmpty()) {
            return false;
        }
        if (EXACT_PROPERTY_KEYS.contains(normalized)) {
            return true;
        }
        return PROPERTY_KEY_PREFIXES.stream().anyMatch(prefix ->
                normalized.startsWith(prefix + ".") || normalized.startsWith(prefix + "["));
    }

    public static boolean isDeploymentSecretJsonField(String fieldName) {
        String normalized = normalize(fieldName);
        return JSON_SECRET_FIELDS.contains(normalized);
    }

    public static boolean isCryptoGroup(String groupCode) {
        return groupCode != null && "crypto".equalsIgnoreCase(groupCode.trim());
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "");
    }
}
