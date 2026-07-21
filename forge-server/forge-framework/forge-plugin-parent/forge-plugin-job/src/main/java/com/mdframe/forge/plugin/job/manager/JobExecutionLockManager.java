package com.mdframe.forge.plugin.job.manager;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 为禁止并发的任务提供集群级零等待执行锁。
 */
@Slf4j
@Component
public class JobExecutionLockManager {

    private static final String LOCK_KEY_PREFIX = "forge:job:execution:";

    private final ObjectProvider<RedissonClient> redissonClientProvider;

    public JobExecutionLockManager(ObjectProvider<RedissonClient> redissonClientProvider) {
        this.redissonClientProvider = redissonClientProvider;
    }

    public LockHandle tryAcquire(Long jobConfigId) {
        if (jobConfigId == null) {
            return LockHandle.unavailable("任务配置ID缺失，受保护任务已跳过");
        }
        String lockKey = LOCK_KEY_PREFIX + jobConfigId;
        try {
            RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
            if (redissonClient == null) {
                return LockHandle.unavailable("Redis未配置，受保护任务已跳过");
            }
            RLock lock = redissonClient.getLock(lockKey);
            if (!lock.tryLock(0L, TimeUnit.MILLISECONDS)) {
                return LockHandle.contended("任务正在运行，本次触发已跳过");
            }
            return LockHandle.acquired(lockKey, lock);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("获取任务执行锁被中断: lockKey={}", lockKey);
            return LockHandle.unavailable("获取执行锁被中断，受保护任务已跳过");
        } catch (RuntimeException exception) {
            log.error("获取任务执行锁失败: lockKey={}, error={}", lockKey, exception.getMessage());
            return LockHandle.unavailable("Redis不可用，受保护任务已跳过");
        }
    }

    public static final class LockHandle implements AutoCloseable {

        private final State state;
        private final String reason;
        private final String lockKey;
        private final RLock lock;

        private LockHandle(State state, String reason, String lockKey, RLock lock) {
            this.state = state;
            this.reason = reason;
            this.lockKey = lockKey;
            this.lock = lock;
        }

        private static LockHandle acquired(String lockKey, RLock lock) {
            return new LockHandle(State.ACQUIRED, null, lockKey, lock);
        }

        private static LockHandle contended(String reason) {
            return new LockHandle(State.CONTENDED, reason, null, null);
        }

        private static LockHandle unavailable(String reason) {
            return new LockHandle(State.UNAVAILABLE, reason, null, null);
        }

        public boolean isAcquired() {
            return state == State.ACQUIRED;
        }

        public boolean isContended() {
            return state == State.CONTENDED;
        }

        public boolean isUnavailable() {
            return state == State.UNAVAILABLE;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public void close() {
            if (!isAcquired() || lock == null) {
                return;
            }
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (RuntimeException exception) {
                log.warn("释放任务执行锁失败: lockKey={}, error={}", lockKey, exception.getMessage());
            }
        }

        private enum State {
            ACQUIRED,
            CONTENDED,
            UNAVAILABLE
        }
    }
}
