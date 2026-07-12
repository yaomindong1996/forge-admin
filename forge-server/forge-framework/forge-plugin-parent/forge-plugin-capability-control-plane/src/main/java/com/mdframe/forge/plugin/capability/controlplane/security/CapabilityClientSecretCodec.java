package com.mdframe.forge.plugin.capability.controlplane.security;

import com.mdframe.forge.plugin.capability.controlplane.config.CapabilityControlPlaneProperties;
import com.mdframe.forge.starter.core.exception.BusinessException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

public final class CapabilityClientSecretCodec {

    private static final Pattern CLIENT_CODE = Pattern.compile("^[a-z][a-z0-9_]{2,63}$");
    private static final Pattern RAW_SECRET = Pattern.compile(
            "^fcp_([A-Za-z0-9_-]{22})_[A-Za-z0-9_-]{43}$");
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int KEY_ID_BYTES = 16;
    private static final int SECRET_BYTES = 32;

    private final CapabilityControlPlaneProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public CapabilityClientSecretCodec(CapabilityControlPlaneProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties 不能为空");
    }

    public IssuedClientSecret issue(String clientCode) {
        requirePepper();
        if (clientCode == null || !CLIENT_CODE.matcher(clientCode).matches()) {
            throw new BusinessException("clientCode 必须以小写字母开头且只包含小写字母、数字和下划线");
        }
        String keyId = randomPart(KEY_ID_BYTES);
        String rawSecret = "fcp_" + keyId + "_" + randomPart(SECRET_BYTES);
        return new IssuedClientSecret(
                rawSecret,
                keyId,
                "fcp_" + keyId,
                hash(rawSecret));
    }

    public String extractKeyId(String rawSecret) {
        if (rawSecret == null) {
            return null;
        }
        java.util.regex.Matcher matcher = RAW_SECRET.matcher(rawSecret);
        return matcher.matches() ? matcher.group(1) : null;
    }

    public boolean matches(String rawSecret, String expectedHash) {
        requirePepper();
        String candidateHash = hash(rawSecret == null ? "" : rawSecret);
        byte[] candidate = HexFormat.of().parseHex(candidateHash);
        byte[] expected = parseExpectedHash(expectedHash);
        return MessageDigest.isEqual(candidate, expected);
    }

    private String hash(String rawSecret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    properties.getClientPepper().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(rawSecret.getBytes(StandardCharsets.UTF_8)));
        }
        catch (GeneralSecurityException exception) {
            throw new IllegalStateException("当前 JDK 不支持客户端密钥算法", exception);
        }
    }

    private String randomPart(int byteLength) {
        byte[] randomBytes = new byte[byteLength];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private byte[] parseExpectedHash(String expectedHash) {
        if (expectedHash == null || !expectedHash.matches("[0-9a-f]{64}")) {
            return new byte[32];
        }
        return HexFormat.of().parseHex(expectedHash);
    }

    private void requirePepper() {
        String pepper = properties.getClientPepper();
        if (pepper == null || pepper.length() < 16) {
            throw new BusinessException("Forge Capability Client Pepper 未配置或长度不足");
        }
    }
}
