package com.mdframe.forge.starter.openapi.security.idempotency;

import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 开放API通用幂等组件。模板方法：Idempotency-Key 格式校验 → SHA-256 哈希 →
 * Redisson 锁串行化 → 快照命中直接返回 → 执行业务 → 写快照（唯一约束回查兜底并发）。
 * 泛化自定时任务开放API的幂等实现；Redis 不可用时失败关闭（503）。
 */
@Slf4j
public class OpenApiIdempotencyManager {

    private static final Pattern IDEMPOTENCY_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{8,128}$");

    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final String keyPrefix;
    private final long lockWaitMillis;
    private final long lockLeaseMillis;

    public OpenApiIdempotencyManager(
            ObjectProvider<RedissonClient> redissonClientProvider,
            String keyPrefix,
            long lockWaitMillis,
            long lockLeaseMillis) {
        this.redissonClientProvider = redissonClientProvider;
        this.keyPrefix = StringUtils.defaultIfBlank(keyPrefix, "forge:openapi");
        this.lockWaitMillis = lockWaitMillis;
        this.lockLeaseMillis = lockLeaseMillis;
    }

    /**
     * 幂等执行：同一 scopeKey + Idempotency-Key 只执行一次业务，重试返回首次响应快照。
     */
    public <T> IdempotencyResult<T> execute(IdempotencyCommand<T> command, Supplier<T> action) {
        if (command == null || command.snapshotLoader() == null
                || command.snapshotWriter() == null || action == null) {
            throw new BusinessException(500, "幂等执行命令不完整");
        }
        if (StringUtils.isBlank(command.scopeKey())) {
            throw new BusinessException(401, "幂等作用域缺失");
        }
        String idempotencyKey = command.idempotencyKey();
        if (idempotencyKey == null || !IDEMPOTENCY_KEY_PATTERN.matcher(idempotencyKey).matches()) {
            throw new BusinessException(400, "Idempotency-Key 缺失或格式非法（8-128位字母数字._:-）");
        }
        String keyHash = hash(idempotencyKey);
        String lockKey = keyPrefix + ":idempotency:" + command.scopeKey() + ":" + keyHash;
        RLock lock = acquireLock(command.scopeKey(), lockKey);
        try {
            return executeLocked(command, action, keyHash);
        } finally {
            unlock(lock, command.scopeKey());
        }
    }

    private RLock acquireLock(String scopeKey, String lockKey) {
        try {
            RedissonClient client = redissonClientProvider.getIfAvailable();
            if (client == null) {
                log.error("[开放API幂等] 基础设施不可用: scopeKey={}, phase=REDIS_CLIENT, reason=RedissonClientMissing",
                        scopeKey);
                throw serviceUnavailable(null);
            }
            RLock lock = client.getLock(lockKey);
            boolean acquired = lock.tryLock(lockWaitMillis, lockLeaseMillis, TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new BusinessException(409, "相同幂等键的请求正在处理中");
            }
            return lock;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw infrastructureUnavailable(scopeKey, "LOCK_ACQUIRE", exception);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw infrastructureUnavailable(scopeKey, "LOCK_ACQUIRE", exception);
        }
    }

    private <T> IdempotencyResult<T> executeLocked(
            IdempotencyCommand<T> command, Supplier<T> action, String keyHash) {
        T existing = loadSnapshot(command, keyHash, "SNAPSHOT_LOAD");
        if (existing != null) {
            return IdempotencyResult.hit(existing);
        }

        // action 内的 Schema、授权和业务异常必须原样向上传递，不能误报为幂等基础设施故障。
        T fresh = action.get();
        try {
            command.snapshotWriter().accept(keyHash, fresh);
        } catch (DuplicateKeyException exception) {
            T concurrent = loadSnapshot(command, keyHash, "DUPLICATE_SNAPSHOT_LOAD");
            if (concurrent != null) {
                return IdempotencyResult.hit(concurrent);
            }
            throw new BusinessException(409, "幂等记录写入冲突");
        } catch (RuntimeException exception) {
            throw infrastructureUnavailable(command.scopeKey(), "SNAPSHOT_WRITE", exception);
        }
        return IdempotencyResult.fresh(fresh);
    }

    private <T> T loadSnapshot(
            IdempotencyCommand<T> command, String keyHash, String phase) {
        try {
            return command.snapshotLoader().apply(keyHash);
        } catch (RuntimeException exception) {
            throw infrastructureUnavailable(command.scopeKey(), phase, exception);
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

    private void unlock(RLock lock, String scopeKey) {
        if (lock == null) {
            return;
        }
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (RuntimeException exception) {
            log.warn("释放开放API幂等锁失败: scopeKey={}, exceptionType={}",
                    scopeKey, exception.getClass().getSimpleName(), exception);
        }
    }

    private BusinessException infrastructureUnavailable(
            String scopeKey, String phase, Throwable exception) {
        log.error("[开放API幂等] 基础设施异常: scopeKey={}, phase={}, exceptionType={}",
                scopeKey, phase, exception.getClass().getSimpleName(), exception);
        return serviceUnavailable(exception);
    }

    private BusinessException serviceUnavailable(Throwable cause) {
        return new BusinessException(503, "开放API幂等服务暂不可用", cause);
    }
}
