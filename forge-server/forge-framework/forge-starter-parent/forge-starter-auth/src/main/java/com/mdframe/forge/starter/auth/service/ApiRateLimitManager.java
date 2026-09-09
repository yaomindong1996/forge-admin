package com.mdframe.forge.starter.auth.service;

import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/** 普通 API 的 Redis 分布式限流器。桶键只保留接口模板和主体摘要。 */
@Slf4j
@Component
public class ApiRateLimitManager {

    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final boolean enabled;
    private final int permitsPerMinute;
    private final String keyPrefix;
    private final boolean enforce;

    @Autowired
    public ApiRateLimitManager(
            ObjectProvider<RedissonClient> redissonClientProvider,
            @Value("${forge.api-config.rate-limit.enabled:true}") boolean enabled,
            @Value("${forge.api-config.rate-limit.permits-per-minute:120}") int permitsPerMinute,
            @Value("${forge.api-config.rate-limit.key-prefix:forge:api:rate}") String keyPrefix,
            @Value("${forge.api-config.rate-limit.mode:enforce}") String mode) {
        this(redissonClientProvider, enabled, permitsPerMinute, keyPrefix, "enforce".equalsIgnoreCase(mode));
    }

    public ApiRateLimitManager(ObjectProvider<RedissonClient> redissonClientProvider,
                               boolean enabled, int permitsPerMinute, String keyPrefix) {
        this(redissonClientProvider, enabled, permitsPerMinute, keyPrefix, true);
    }

    private ApiRateLimitManager(ObjectProvider<RedissonClient> redissonClientProvider,
                                boolean enabled, int permitsPerMinute, String keyPrefix, boolean enforce) {
        this.redissonClientProvider = redissonClientProvider;
        this.enabled = enabled;
        this.permitsPerMinute = permitsPerMinute;
        this.keyPrefix = keyPrefix;
        this.enforce = enforce;
    }

    public void acquire(String scopeKey, String requestKey) {
        if (!enabled) {
            return;
        }
        if (!StringUtils.hasText(scopeKey) || !StringUtils.hasText(requestKey)) {
            throw new BusinessException(400, "限流主体缺失");
        }
        String prefix = StringUtils.hasText(keyPrefix) ? keyPrefix.trim() : "forge:api:rate";
        String rateKey = prefix + ":" + requestKey + ":" + digest(scopeKey);
        try {
            RedissonClient client = redissonClientProvider.getIfAvailable();
            if (client == null) {
                throw new IllegalStateException("REDIS_CLIENT_UNAVAILABLE");
            }
            RRateLimiter limiter = client.getRateLimiter(rateKey);
            limiter.trySetRate(RateType.OVERALL, Math.max(1, permitsPerMinute), Duration.ofMinutes(1));
            limiter.expire(Duration.ofMinutes(2));
            if (!limiter.tryAcquire()) {
                if (enforce) {
                    throw new BusinessException(429, "请求过于频繁，请稍后再试");
                }
                log.warn("普通 API 限流观察到超额请求: requestKey={}, scopeKey={}", requestKey, scopeKey);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("普通 API 限流不可用: requestKey={}, exceptionType={}",
                    requestKey, exception.getClass().getSimpleName());
            if (enforce) {
                throw unavailable();
            }
            log.warn("普通 API 限流观察模式忽略 Redis 故障: requestKey={}", requestKey);
        }
    }

    private BusinessException unavailable() {
        return new BusinessException(503, "限流服务暂不可用");
    }

    private String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("RATE_LIMIT_KEY_DIGEST_FAILED", exception);
        }
    }
}
