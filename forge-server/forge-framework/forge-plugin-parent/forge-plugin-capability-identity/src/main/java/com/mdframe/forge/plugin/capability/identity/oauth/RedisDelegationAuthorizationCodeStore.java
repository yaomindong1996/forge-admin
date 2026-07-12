package com.mdframe.forge.plugin.capability.identity.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.identity.config.CapabilityIdentityProperties;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public final class RedisDelegationAuthorizationCodeStore implements DelegationAuthorizationCodeStore {

    private static final String KEY_PREFIX = "forge:capability:oauth:code:";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final DefaultRedisScript<String> GET_AND_DELETE = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); "
                    + "if value then redis.call('DEL', KEYS[1]); end; return value;",
            String.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CapabilityIdentityProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RedisDelegationAuthorizationCodeStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            CapabilityIdentityProperties properties) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate 不能为空");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper 不能为空");
        this.properties = Objects.requireNonNull(properties, "properties 不能为空");
    }

    @Override
    public String issue(DelegationAuthorizationCode authorizationCode) {
        requirePepper();
        if (authorizationCode == null) {
            throw new IllegalArgumentException("authorizationCode 不能为空");
        }
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawCode = "fdc_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        try {
            redisTemplate.opsForValue().set(
                    key(rawCode),
                    objectMapper.writeValueAsString(authorizationCode),
                    properties.validatedAuthorizationCodeTtl());
            return rawCode;
        } catch (JsonProcessingException exception) {
            throw new BusinessException("授权请求暂时不可用");
        }
    }

    @Override
    public DelegationAuthorizationCode consume(String rawCode) {
        requirePepper();
        if (rawCode == null || !rawCode.matches("^fdc_[A-Za-z0-9_-]{43}$")) {
            return null;
        }
        String value = redisTemplate.execute(GET_AND_DELETE, List.of(key(rawCode)));
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, DelegationAuthorizationCode.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("授权码无效或已失效");
        }
    }

    private String key(String rawCode) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    properties.getAuthorizationCodePepper().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(rawCode.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("当前 JDK 不支持 authorization code HMAC 算法", exception);
        }
    }

    private void requirePepper() {
        String pepper = properties.getAuthorizationCodePepper();
        if (pepper == null || pepper.length() < 32) {
            throw new BusinessException("Forge MCP Authorization Code Pepper 未配置或长度不足 32 位");
        }
    }
}
