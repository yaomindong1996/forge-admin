package com.mdframe.forge.plugin.capability.identity.token;

import com.mdframe.forge.plugin.capability.identity.config.CapabilityIdentityProperties;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CapabilityAccessTokenCodec {

    private static final Pattern RAW_TOKEN = Pattern.compile(
            "^fdu_([A-Za-z0-9_-]{22})_[A-Za-z0-9_-]{43}$");
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int KEY_ID_BYTES = 16;
    private static final int TOKEN_SECRET_BYTES = 32;

    private final CapabilityIdentityProperties properties;
    private final SecureRandom secureRandom;

    public CapabilityAccessTokenCodec(CapabilityIdentityProperties properties) {
        this(properties, new SecureRandom());
    }

    CapabilityAccessTokenCodec(CapabilityIdentityProperties properties, SecureRandom secureRandom) {
        this.properties = Objects.requireNonNull(properties, "properties 不能为空");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom 不能为空");
    }

    public IssuedCapabilityAccessToken issue() {
        requirePepper();
        String keyId = randomPart(KEY_ID_BYTES);
        String rawToken = "fdu_" + keyId + "_" + randomPart(TOKEN_SECRET_BYTES);
        return new IssuedCapabilityAccessToken(
                rawToken, keyId, "fdu_" + keyId, hash(rawToken));
    }

    public String extractKeyId(String rawToken) {
        if (rawToken == null) {
            return null;
        }
        Matcher matcher = RAW_TOKEN.matcher(rawToken);
        return matcher.matches() ? matcher.group(1) : null;
    }

    public boolean matches(String rawToken, String expectedHash) {
        requirePepper();
        byte[] candidate = HexFormat.of().parseHex(hash(rawToken == null ? "" : rawToken));
        byte[] expected = parseExpectedHash(expectedHash);
        return MessageDigest.isEqual(candidate, expected);
    }

    private String hash(String rawToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    properties.getTokenPepper().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("当前 JDK 不支持 MCP token HMAC 算法", exception);
        }
    }

    private String randomPart(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] parseExpectedHash(String expectedHash) {
        if (expectedHash == null || !expectedHash.matches("[0-9a-f]{64}")) {
            return new byte[32];
        }
        return HexFormat.of().parseHex(expectedHash);
    }

    private void requirePepper() {
        String pepper = properties.getTokenPepper();
        if (pepper == null || pepper.length() < 32) {
            throw new BusinessException("Forge MCP Token Pepper 未配置或长度不足 32 位");
        }
    }
}
