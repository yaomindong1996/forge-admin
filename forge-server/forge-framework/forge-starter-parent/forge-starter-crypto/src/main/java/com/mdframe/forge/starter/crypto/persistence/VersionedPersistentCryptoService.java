package com.mdframe.forge.starter.crypto.persistence;

import com.mdframe.forge.starter.core.context.CryptoProperties;
import com.mdframe.forge.starter.crypto.crypto.CryptoAlgorithm;
import com.mdframe.forge.starter.crypto.crypto.Encryptor;
import com.mdframe.forge.starter.crypto.crypto.EncryptorFactory;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * FPC1 版本化持久化密文实现。
 */
public class VersionedPersistentCryptoService implements PersistentCryptoService {

    private final CryptoProperties properties;
    private final EncryptorFactory encryptorFactory;

    public VersionedPersistentCryptoService(CryptoProperties properties, EncryptorFactory encryptorFactory) {
        this.properties = properties;
        this.encryptorFactory = encryptorFactory;
    }

    @Override
    public String encrypt(String plaintext, String algorithm) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        CryptoProperties.PersistenceProperties persistence = persistenceProperties();
        CryptoAlgorithm cryptoAlgorithm = CryptoAlgorithm.fromCode(resolveAlgorithm(algorithm));
        if (Boolean.TRUE.equals(persistence.getWriteVersioned())) {
            String keyId = requiredActiveKeyId(persistence);
            String key = requiredActiveKey(persistence);
            String payload = encryptorFactory.getEncryptor(cryptoAlgorithm).encrypt(plaintext, key);
            return PersistentCiphertext.VERSION + ":"
                    + cryptoAlgorithm.getCode() + ":"
                    + keyId + ":"
                    + payload;
        }
        return encryptorFactory.getEncryptor(cryptoAlgorithm)
                .encrypt(plaintext, requiredLegacyKey(persistence));
    }

    @Override
    public String decrypt(String ciphertext, String legacyAlgorithm) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        PersistentCiphertext parsed = inspect(ciphertext, legacyAlgorithm);
        return switch (parsed.format()) {
            case ACTIVE, HISTORICAL -> decryptVersioned(parsed);
            case LEGACY -> decryptLegacy(parsed);
            case EMPTY -> ciphertext;
            case UNKNOWN_KEY -> throw failure("持久化密文 keyId 未配置");
            case UNKNOWN -> throw failure("持久化密文格式非法: " + parsed.failureReason());
        };
    }

    @Override
    public PersistentCiphertext inspect(String ciphertext, String legacyAlgorithm) {
        CryptoProperties.PersistenceProperties persistence = persistenceProperties();
        return PersistentCiphertext.inspect(
                ciphertext,
                resolveAlgorithm(legacyAlgorithm),
                persistence.getActiveKeyId(),
                readableKeyIds(persistence)
        );
    }

    @Override
    public String reencrypt(String ciphertext, String legacyAlgorithm) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        PersistentCiphertext parsed = inspect(ciphertext, legacyAlgorithm);
        if (parsed.format() == PersistentCiphertext.Format.ACTIVE) {
            return ciphertext;
        }
        String plaintext = decrypt(ciphertext, legacyAlgorithm);
        return encrypt(plaintext, parsed.algorithm());
    }

    private String decryptVersioned(PersistentCiphertext ciphertext) {
        String key = resolveKey(ciphertext.keyId());
        if (!StringUtils.hasText(key)) {
            throw failure("持久化密文 keyId 未配置");
        }
        Encryptor encryptor = encryptorFactory.getEncryptor(ciphertext.algorithm());
        return encryptor.decrypt(ciphertext.payload(), key);
    }

    private String decryptLegacy(PersistentCiphertext ciphertext) {
        CryptoProperties.PersistenceProperties persistence = persistenceProperties();
        if (!Boolean.TRUE.equals(persistence.getLegacyReadEnabled())) {
            throw failure("旧无版本密文读取已关闭");
        }
        return encryptorFactory.getEncryptor(ciphertext.algorithm())
                .decrypt(ciphertext.payload(), requiredLegacyKey(persistence));
    }

    private CryptoProperties.PersistenceProperties persistenceProperties() {
        CryptoProperties.PersistenceProperties persistence = properties.getPersistence();
        if (persistence == null || !Boolean.TRUE.equals(persistence.getEnabled())) {
            throw failure("持久化加密未启用");
        }
        return persistence;
    }

    private String resolveAlgorithm(String algorithm) {
        return StringUtils.hasText(algorithm) ? algorithm : properties.getAlgorithm();
    }

    private String requiredActiveKeyId(CryptoProperties.PersistenceProperties persistence) {
        if (!StringUtils.hasText(persistence.getActiveKeyId())) {
            throw failure("持久化加密活动 keyId 未配置");
        }
        return persistence.getActiveKeyId();
    }

    private String requiredActiveKey(CryptoProperties.PersistenceProperties persistence) {
        if (StringUtils.hasText(persistence.getActiveKey())) {
            return persistence.getActiveKey();
        }
        String activeKey = persistence.getKeys() == null ? null : persistence.getKeys().get(persistence.getActiveKeyId());
        if (!StringUtils.hasText(activeKey)) {
            throw failure("持久化加密活动密钥未配置");
        }
        return activeKey;
    }

    private String requiredLegacyKey(CryptoProperties.PersistenceProperties persistence) {
        if (StringUtils.hasText(persistence.getLegacyKey())) {
            return persistence.getLegacyKey();
        }
        if (StringUtils.hasText(properties.getSecretKey())) {
            return properties.getSecretKey();
        }
        throw failure("旧无版本持久化密文密钥未配置");
    }

    private String resolveKey(String keyId) {
        CryptoProperties.PersistenceProperties persistence = persistenceProperties();
        if (keyId.equals(persistence.getActiveKeyId()) && StringUtils.hasText(persistence.getActiveKey())) {
            return persistence.getActiveKey();
        }
        Map<String, String> keys = persistence.getKeys();
        return keys == null ? null : keys.get(keyId);
    }

    private Set<String> readableKeyIds(CryptoProperties.PersistenceProperties persistence) {
        Set<String> keyIds = new LinkedHashSet<>();
        if (StringUtils.hasText(persistence.getActiveKeyId())
                && (StringUtils.hasText(persistence.getActiveKey())
                || (persistence.getKeys() != null && StringUtils.hasText(persistence.getKeys().get(persistence.getActiveKeyId()))))) {
            keyIds.add(persistence.getActiveKeyId());
        }
        if (persistence.getKeys() != null) {
            persistence.getKeys().forEach((keyId, key) -> {
                if (StringUtils.hasText(key)) {
                    keyIds.add(keyId);
                }
            });
        }
        return keyIds;
    }

    private IllegalStateException failure(String message) {
        return new IllegalStateException(message);
    }
}
