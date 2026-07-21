package com.mdframe.forge.plugin.job.manager;

import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.scheduler.JobScheduleException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * 串行同一 Quartz Key 的配置同步，跨管理节点共享 Redis 锁。
 */
@Slf4j
@Component
public class JobScheduleSynchronizationLockManager {

    private static final String LOCK_KEY_PREFIX = "forge:job:schedule-sync:";

    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final JobProperties jobProperties;

    public JobScheduleSynchronizationLockManager(
            ObjectProvider<RedissonClient> redissonClientProvider,
            JobProperties jobProperties) {
        this.redissonClientProvider = redissonClientProvider;
        this.jobProperties = jobProperties;
    }

    public LockHandle acquire(String jobName, String jobGroup) {
        if (jobName == null || jobName.isBlank() || jobGroup == null || jobGroup.isBlank()) {
            throw new JobScheduleException("任务同步锁缺少Quartz任务标识");
        }
        String lockKey = LOCK_KEY_PREFIX + digest(jobGroup + '\0' + jobName);
        try {
            RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
            if (redissonClient == null) {
                throw new JobScheduleException("Redis未配置，无法安全同步Quartz任务");
            }
            RLock lock = redissonClient.getLock(lockKey);
            if (!lock.tryLock(jobProperties.validatedScheduleSyncLockWaitMillis(), TimeUnit.MILLISECONDS)) {
                throw new JobScheduleException("任务配置正在同步，请稍后重试");
            }
            return new LockHandle(lockKey, lock);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new JobScheduleException("获取任务同步锁被中断", exception);
        } catch (JobScheduleException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new JobScheduleException("Redis不可用，无法安全同步Quartz任务", exception);
        }
    }

    private String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM缺少SHA-256算法", exception);
        }
    }

    public static final class LockHandle implements AutoCloseable {

        private final String lockKey;
        private final RLock lock;

        private LockHandle(String lockKey, RLock lock) {
            this.lockKey = lockKey;
            this.lock = lock;
        }

        @Override
        public void close() {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (RuntimeException exception) {
                log.warn("释放任务同步锁失败: lockKey={}, exceptionType={}",
                        lockKey, exception.getClass().getSimpleName());
            }
        }
    }
}

