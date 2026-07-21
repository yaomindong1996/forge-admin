package com.mdframe.forge.plugin.job.manager;

import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.model.JobApiPrincipal;
import com.mdframe.forge.plugin.job.support.JobOpenApiException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class JobApiRateLimitManager {

    private static final String RATE_KEY_PREFIX = "forge:job:openapi:rate:";

    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final JobProperties jobProperties;

    public JobApiRateLimitManager(
            ObjectProvider<RedissonClient> redissonClientProvider,
            JobProperties jobProperties) {
        this.redissonClientProvider = redissonClientProvider;
        this.jobProperties = jobProperties;
    }

    public void acquireRead(JobApiPrincipal principal) {
        acquire(principal, "read", jobProperties.getOpenApi().validatedReadRateLimit());
    }

    public void acquireTrigger(JobApiPrincipal principal) {
        acquire(principal, "trigger", jobProperties.getOpenApi().validatedTriggerRateLimit());
    }

    private void acquire(JobApiPrincipal principal, String operation, int permitsPerMinute) {
        String keyId = principal == null ? null : principal.tokenKeyId();
        if (keyId == null || keyId.isBlank()) {
            throw JobOpenApiException.unauthorized();
        }
        String rateKey = RATE_KEY_PREFIX + operation + ":" + keyId;
        try {
            RedissonClient client = redissonClientProvider.getIfAvailable();
            if (client == null) {
                throw JobOpenApiException.unavailable();
            }
            RRateLimiter limiter = client.getRateLimiter(rateKey);
            limiter.trySetRate(RateType.OVERALL, permitsPerMinute, Duration.ofMinutes(1));
            if (!limiter.tryAcquire()) {
                throw JobOpenApiException.tooManyRequests();
            }
        } catch (JobOpenApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("定时任务开放API限流不可用: keyId={}, operation={}, exceptionType={}",
                    keyId, operation, exception.getClass().getSimpleName());
            throw JobOpenApiException.unavailable();
        }
    }
}
