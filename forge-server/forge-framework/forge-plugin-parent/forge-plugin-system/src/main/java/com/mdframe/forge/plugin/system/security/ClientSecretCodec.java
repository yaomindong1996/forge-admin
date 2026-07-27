package com.mdframe.forge.plugin.system.security;

import com.mdframe.forge.starter.auth.util.PasswordUtil;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ClientSecretCodec {

    static final String BCRYPT_PREFIX = "{bcrypt}";
    private static final int MIN_SECRET_LENGTH = 16;
    private static final int MAX_SECRET_BYTES = 72;
    private static final int MIN_BCRYPT_COST = 10;
    private static final int MAX_BCRYPT_COST = 12;
    private static final Pattern BCRYPT_PATTERN = Pattern.compile(
            "^\\$2[aby]\\$(\\d{2})\\$[./A-Za-z0-9]{53}$");

    public String encode(String rawSecret) {
        validateRawSecret(rawSecret);
        return encodeUnchecked(rawSecret);
    }

    public String encodeLegacyUpgrade(String rawSecret) {
        if (rawSecret == null || rawSecret.isEmpty()
                || rawSecret.getBytes(StandardCharsets.UTF_8).length > MAX_SECRET_BYTES) {
            throw new IllegalArgumentException("历史客户端密钥格式无效，必须由管理员轮换");
        }
        return encodeUnchecked(rawSecret);
    }

    public MatchResult verify(String rawSecret, String storedSecret, boolean allowLegacy) {
        if (rawSecret == null || storedSecret == null || storedSecret.isBlank()) {
            return MatchResult.NO_MATCH;
        }
        if (storedSecret.startsWith(BCRYPT_PREFIX)) {
            if (rawSecret.getBytes(StandardCharsets.UTF_8).length > MAX_SECRET_BYTES) {
                return MatchResult.NO_MATCH;
            }
            return new MatchResult(matchesBcrypt(rawSecret, storedSecret.substring(BCRYPT_PREFIX.length())), false);
        }
        if (storedSecret.startsWith("{")) {
            return MatchResult.NO_MATCH;
        }
        if (!allowLegacy) {
            return MatchResult.NO_MATCH;
        }
        return new MatchResult(constantTimeEquals(rawSecret, storedSecret), true);
    }

    public boolean isEncoded(String storedSecret) {
        return storedSecret != null && storedSecret.startsWith(BCRYPT_PREFIX);
    }

    private boolean matchesBcrypt(String rawSecret, String bcryptHash) {
        Matcher matcher = BCRYPT_PATTERN.matcher(bcryptHash);
        if (!matcher.matches()) {
            return false;
        }
        int cost = Integer.parseInt(matcher.group(1));
        if (cost < MIN_BCRYPT_COST || cost > MAX_BCRYPT_COST) {
            return false;
        }
        try {
            return PasswordUtil.matches(rawSecret, bcryptHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean constantTimeEquals(String actual, String expected) {
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private void validateRawSecret(String rawSecret) {
        int characterLength = rawSecret == null ? 0 : rawSecret.length();
        int byteLength = rawSecret == null ? 0 : rawSecret.getBytes(StandardCharsets.UTF_8).length;
        if (characterLength < MIN_SECRET_LENGTH || byteLength > MAX_SECRET_BYTES) {
            throw new IllegalArgumentException("客户端密钥必须至少16个字符且UTF-8不超过72字节");
        }
    }

    private String encodeUnchecked(String rawSecret) {
        return BCRYPT_PREFIX + PasswordUtil.encrypt(rawSecret);
    }

    public record MatchResult(boolean matched, boolean legacy) {
        private static final MatchResult NO_MATCH = new MatchResult(false, false);
    }
}
