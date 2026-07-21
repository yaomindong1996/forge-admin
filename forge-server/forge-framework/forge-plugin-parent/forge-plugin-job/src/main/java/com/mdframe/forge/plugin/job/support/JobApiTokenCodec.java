package com.mdframe.forge.plugin.job.support;

import com.mdframe.forge.plugin.job.config.JobProperties;
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

public final class JobApiTokenCodec {

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "^fja_([A-Za-z0-9_-]{22})_[A-Za-z0-9_-]{43}$");
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int KEY_ID_BYTES = 16;
    private static final int SECRET_BYTES = 32;

    private final JobProperties.OpenApi properties;
    private final SecureRandom secureRandom;

    public JobApiTokenCodec(JobProperties properties) {
        this(properties.getOpenApi(), new SecureRandom());
    }

    JobApiTokenCodec(JobProperties.OpenApi properties, SecureRandom secureRandom) {
        this.properties = Objects.requireNonNull(properties, "properties 不能为空");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom 不能为空");
    }

    public IssuedJobApiToken issue() {
        requirePepper();
        String keyId = randomPart(KEY_ID_BYTES);
        String token = "fja_" + keyId + "_" + randomPart(SECRET_BYTES);
        return new IssuedJobApiToken(token, keyId, "fja_" + keyId, hash(token));
    }

    public String extractKeyId(String token) {
        if (token == null) {
            return null;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(token);
        return matcher.matches() ? matcher.group(1) : null;
    }

    public boolean matches(String token, String expectedHash) {
        requirePepper();
        byte[] candidate = HexFormat.of().parseHex(hash(token == null ? "" : token));
        byte[] expected = parseExpectedHash(expectedHash);
        return MessageDigest.isEqual(candidate, expected);
    }

    private String hash(String token) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    properties.getTokenPepper().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(token.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("当前JDK不支持开放API Token HMAC算法", exception);
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
            throw new BusinessException("定时任务开放API Token Pepper未配置或长度不足32位");
        }
    }
}
