package com.mdframe.forge.plugin.job.manager;

import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.entity.SysJobApiIdempotency;
import com.mdframe.forge.plugin.job.model.JobApiPrincipal;
import com.mdframe.forge.plugin.job.model.JobApiTriggerTarget;
import com.mdframe.forge.plugin.job.service.JobApiReservationService;
import com.mdframe.forge.plugin.job.support.JobOpenApiException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Component
public class JobApiIdempotencyManager {

    private static final Pattern IDEMPOTENCY_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{8,128}$");
    private static final String LOCK_KEY_PREFIX = "forge:job:openapi:idempotency:";

    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final JobApiReservationService reservationService;
    private final JobProperties jobProperties;

    public JobApiIdempotencyManager(
            ObjectProvider<RedissonClient> redissonClientProvider,
            JobApiReservationService reservationService,
            JobProperties jobProperties) {
        this.redissonClientProvider = redissonClientProvider;
        this.reservationService = reservationService;
        this.jobProperties = jobProperties;
    }

    public JobApiReservationService.Reservation reserve(
            JobApiPrincipal principal,
            JobApiTriggerTarget target,
            String idempotencyKey) {
        if (idempotencyKey == null || !IDEMPOTENCY_KEY_PATTERN.matcher(idempotencyKey).matches()) {
            throw JobOpenApiException.badRequest("invalid_idempotency_key");
        }
        String keyHash = hash(idempotencyKey);
        String lockKey = LOCK_KEY_PREFIX + principal.tokenKeyId() + ":" + target.getId() + ":" + keyHash;
        RLock lock = null;
        try {
            RedissonClient client = redissonClientProvider.getIfAvailable();
            if (client == null) {
                throw JobOpenApiException.unavailable();
            }
            lock = client.getLock(lockKey);
            boolean acquired = lock.tryLock(
                    jobProperties.getOpenApi().getIdempotencyLockWaitMillis(),
                    jobProperties.getOpenApi().getIdempotencyLockLeaseMillis(),
                    TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw JobOpenApiException.conflict("idempotency_request_in_progress");
            }
            return reserveWithUniqueFallback(principal, target, keyHash);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw JobOpenApiException.unavailable();
        } catch (JobOpenApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("定时任务开放API幂等保护不可用: keyId={}, jobId={}, exceptionType={}",
                    principal.tokenKeyId(), target.getId(), exception.getClass().getSimpleName());
            throw JobOpenApiException.unavailable();
        } finally {
            unlock(lock, principal.tokenKeyId(), target.getId());
        }
    }

    private JobApiReservationService.Reservation reserveWithUniqueFallback(
            JobApiPrincipal principal,
            JobApiTriggerTarget target,
            String keyHash) {
        LocalDateTime now = LocalDateTime.now();
        try {
            return reservationService.reserve(principal, target, keyHash, now);
        } catch (DuplicateKeyException exception) {
            SysJobApiIdempotency existing = reservationService.findEffective(
                    principal, target, keyHash, now);
            if (existing != null) {
                return new JobApiReservationService.Reservation(existing.getExecutionId(), true);
            }
            throw JobOpenApiException.conflict("idempotency_conflict");
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前JDK不支持SHA-256", exception);
        }
    }

    private void unlock(RLock lock, String keyId, Long jobId) {
        if (lock == null) {
            return;
        }
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (RuntimeException exception) {
            log.warn("释放开放API幂等锁失败: keyId={}, jobId={}, exceptionType={}",
                    keyId, jobId, exception.getClass().getSimpleName());
        }
    }
}
