package com.mdframe.forge.starter.crypto.persistence;

import com.mdframe.forge.starter.crypto.crypto.CryptoAlgorithm;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 持久化密文格式解析结果。
 */
public record PersistentCiphertext(
        Format format,
        String algorithm,
        String keyId,
        String payload,
        String failureReason
) {

    public static final String VERSION = "FPC1";
    private static final Pattern KEY_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    public enum Format {
        EMPTY,
        LEGACY,
        ACTIVE,
        HISTORICAL,
        UNKNOWN_KEY,
        UNKNOWN
    }

    public static PersistentCiphertext inspect(String value,
                                               String legacyAlgorithm,
                                               String activeKeyId,
                                               Set<String> readableKeyIds) {
        if (!StringUtils.hasText(value)) {
            return new PersistentCiphertext(Format.EMPTY, null, null, null, null);
        }
        if (!value.startsWith(VERSION + ":")) {
            String algorithm = StringUtils.hasText(legacyAlgorithm) ? legacyAlgorithm : CryptoAlgorithm.SM4.getCode();
            try {
                CryptoAlgorithm.fromCode(algorithm);
                return new PersistentCiphertext(Format.LEGACY, algorithm, null, value, null);
            } catch (IllegalArgumentException e) {
                return new PersistentCiphertext(Format.UNKNOWN, algorithm, null, value, "不支持的旧密文算法");
            }
        }

        String[] parts = value.split(":", 4);
        if (parts.length != 4) {
            return new PersistentCiphertext(Format.UNKNOWN, null, null, null, "版本化密文格式不完整");
        }
        String algorithm = parts[1];
        String keyId = parts[2];
        String payload = parts[3];
        try {
            CryptoAlgorithm.fromCode(algorithm);
        } catch (IllegalArgumentException e) {
            return new PersistentCiphertext(Format.UNKNOWN, algorithm, keyId, payload, "不支持的密文算法");
        }
        if (!KEY_ID_PATTERN.matcher(keyId).matches()) {
            return new PersistentCiphertext(Format.UNKNOWN, algorithm, keyId, payload, "非法 keyId");
        }
        if (!StringUtils.hasText(payload)) {
            return new PersistentCiphertext(Format.UNKNOWN, algorithm, keyId, payload, "密文载荷为空");
        }
        if (keyId.equals(activeKeyId)) {
            return new PersistentCiphertext(Format.ACTIVE, algorithm, keyId, payload, null);
        }
        if (readableKeyIds != null && readableKeyIds.contains(keyId)) {
            return new PersistentCiphertext(Format.HISTORICAL, algorithm, keyId, payload, null);
        }
        return new PersistentCiphertext(Format.UNKNOWN_KEY, algorithm, keyId, payload, "未知 keyId");
    }

    public boolean versioned() {
        return format == Format.ACTIVE
                || format == Format.HISTORICAL
                || format == Format.UNKNOWN_KEY
                || (format == Format.UNKNOWN && keyId != null);
    }
}
