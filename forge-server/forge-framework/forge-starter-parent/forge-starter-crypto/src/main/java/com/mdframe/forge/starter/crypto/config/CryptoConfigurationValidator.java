package com.mdframe.forge.starter.crypto.config;

import com.mdframe.forge.starter.core.context.CryptoProperties;
import com.mdframe.forge.starter.crypto.crypto.CryptoAlgorithm;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 加密配置启动校验。
 */
public final class CryptoConfigurationValidator {

    private static final Pattern KEY_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    public void validate(CryptoProperties properties) {
        if (properties == null || !Boolean.TRUE.equals(properties.getEnabled())) {
            return;
        }
        validateDefaultAlgorithm(properties.getAlgorithm());
        validateTransportKey(properties);
        validatePersistence(properties);
    }

    private void validateDefaultAlgorithm(String algorithm) {
        CryptoAlgorithm.fromCode(algorithm);
    }

    private void validateTransportKey(CryptoProperties properties) {
        if (!Boolean.TRUE.equals(properties.getEnableApiCrypto())
                && !Boolean.TRUE.equals(properties.getEnableFieldCrypto())) {
            return;
        }
        assertBase64Key("FORGE_CRYPTO_SECRET_KEY", properties.getSecretKey(),
                CryptoAlgorithm.fromCode(properties.getAlgorithm()));
    }

    private void validatePersistence(CryptoProperties properties) {
        CryptoProperties.PersistenceProperties persistence = properties.getPersistence();
        if (persistence == null || !Boolean.TRUE.equals(persistence.getEnabled())) {
            return;
        }
        validateKeyMap(persistence.getKeys());

        if (Boolean.TRUE.equals(persistence.getWriteVersioned())) {
            assertSafeKeyId("forge.crypto.persistence.active-key-id", persistence.getActiveKeyId());
            assertBase64PersistenceKey("FORGE_CRYPTO_PERSISTENCE_ACTIVE_KEY",
                    resolveActiveKey(persistence));
        } else {
            assertBase64PersistenceKey("FORGE_CRYPTO_PERSISTENCE_LEGACY_KEY",
                    resolveLegacyKey(properties, persistence));
        }

        if (Boolean.TRUE.equals(persistence.getLegacyReadEnabled())) {
            assertBase64PersistenceKey("FORGE_CRYPTO_PERSISTENCE_LEGACY_KEY",
                    resolveLegacyKey(properties, persistence));
        }
    }

    private void validateKeyMap(Map<String, String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        keys.forEach((keyId, key) -> {
            if (!StringUtils.hasText(key)) {
                throw new IllegalStateException("forge.crypto.persistence.keys." + keyId + " 未配置");
            }
            assertSafeKeyId("forge.crypto.persistence.keys." + keyId, keyId);
            assertBase64PersistenceKey("forge.crypto.persistence.keys." + keyId, key);
        });
    }

    private String resolveActiveKey(CryptoProperties.PersistenceProperties persistence) {
        if (StringUtils.hasText(persistence.getActiveKey())) {
            return persistence.getActiveKey();
        }
        if (persistence.getKeys() == null || !StringUtils.hasText(persistence.getActiveKeyId())) {
            return null;
        }
        return persistence.getKeys().get(persistence.getActiveKeyId());
    }

    private String resolveLegacyKey(CryptoProperties properties,
                                    CryptoProperties.PersistenceProperties persistence) {
        if (StringUtils.hasText(persistence.getLegacyKey())) {
            return persistence.getLegacyKey();
        }
        return properties.getSecretKey();
    }

    private void assertSafeKeyId(String configName, String keyId) {
        if (!StringUtils.hasText(keyId) || !KEY_ID_PATTERN.matcher(keyId).matches()) {
            throw new IllegalStateException(configName + " 必须匹配 [A-Za-z0-9_-]{1,32}");
        }
    }

    private void assertBase64PersistenceKey(String configName, String key) {
        byte[] keyBytes = decodeBase64(configName, key);
        if (keyBytes.length != 16) {
            throw new IllegalStateException(configName + " 必须是 Base64 编码的 16 字节密钥");
        }
    }

    private void assertBase64Key(String configName, String key, CryptoAlgorithm algorithm) {
        byte[] keyBytes = decodeBase64(configName, key);
        if (algorithm == CryptoAlgorithm.SM4 && keyBytes.length != 16) {
            throw new IllegalStateException(configName + " 必须是 Base64 编码的 16 字节 SM4 密钥");
        }
        if (algorithm == CryptoAlgorithm.AES
                && keyBytes.length != 16
                && keyBytes.length != 24
                && keyBytes.length != 32) {
            throw new IllegalStateException(configName + " 必须是 Base64 编码的 16/24/32 字节 AES 密钥");
        }
    }

    private byte[] decodeBase64(String configName, String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalStateException(configName + " 未配置");
        }
        try {
            return Base64.getDecoder().decode(key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(configName + " 必须是合法 Base64 编码", e);
        }
    }
}
