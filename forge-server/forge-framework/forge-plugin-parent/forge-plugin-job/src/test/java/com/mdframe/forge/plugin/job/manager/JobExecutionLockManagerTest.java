package com.mdframe.forge.plugin.job.manager;

import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobExecutionLockManagerTest {

    @Test
    void shouldAcquireImmediatelyAndReleaseOwnedLock() throws Exception {
        RLock lock = mock(RLock.class);
        when(lock.tryLock(0L, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        RedissonClient redissonClient = mock(RedissonClient.class);
        when(redissonClient.getLock("forge:job:execution:7")).thenReturn(lock);

        JobExecutionLockManager.LockHandle handle = manager(provider(redissonClient)).tryAcquire(7L);

        assertTrue(handle.isAcquired());
        verify(lock).tryLock(0L, TimeUnit.MILLISECONDS);
        handle.close();
        verify(lock).unlock();
    }

    @Test
    void shouldAllowOnlyOneManagerToHoldSharedLock() throws Exception {
        AtomicBoolean locked = new AtomicBoolean(false);
        RLock lock = mock(RLock.class);
        when(lock.tryLock(0L, TimeUnit.MILLISECONDS))
                .thenAnswer(invocation -> locked.compareAndSet(false, true));
        when(lock.isHeldByCurrentThread()).thenAnswer(invocation -> locked.get());
        doAnswer(invocation -> {
            locked.set(false);
            return null;
        }).when(lock).unlock();
        RedissonClient redissonClient = mock(RedissonClient.class);
        when(redissonClient.getLock("forge:job:execution:8")).thenReturn(lock);
        JobExecutionLockManager firstManager = manager(provider(redissonClient));
        JobExecutionLockManager secondManager = manager(provider(redissonClient));

        JobExecutionLockManager.LockHandle first = firstManager.tryAcquire(8L);
        JobExecutionLockManager.LockHandle second = secondManager.tryAcquire(8L);

        assertTrue(first.isAcquired());
        assertTrue(second.isContended());
        assertFalse(second.isAcquired());
        second.close();
        first.close();

        JobExecutionLockManager.LockHandle afterRelease = secondManager.tryAcquire(8L);
        assertTrue(afterRelease.isAcquired());
        afterRelease.close();
        verify(lock, times(3)).tryLock(0L, TimeUnit.MILLISECONDS);
        verify(lock, times(2)).unlock();
    }

    @Test
    void shouldFailClosedWhenRedisIsMissingOrUnavailable() throws Exception {
        JobExecutionLockManager.LockHandle missing = manager(provider(null)).tryAcquire(9L);
        assertTrue(missing.isUnavailable());
        assertTrue(missing.getReason().contains("Redis"));

        RedissonClient redissonClient = mock(RedissonClient.class);
        when(redissonClient.getLock("forge:job:execution:10"))
                .thenThrow(new IllegalStateException("connection failed"));
        JobExecutionLockManager.LockHandle failed = manager(provider(redissonClient)).tryAcquire(10L);
        assertTrue(failed.isUnavailable());
        assertEquals("Redis不可用，受保护任务已跳过", failed.getReason());

        ObjectProvider<RedissonClient> failedProvider = provider(null);
        when(failedProvider.getIfAvailable()).thenThrow(new IllegalStateException("provider failed"));
        JobExecutionLockManager.LockHandle providerFailed = manager(failedProvider).tryAcquire(11L);
        assertTrue(providerFailed.isUnavailable());
        assertEquals("Redis不可用，受保护任务已跳过", providerFailed.getReason());
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<RedissonClient> provider(RedissonClient client) {
        ObjectProvider<RedissonClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(client);
        return provider;
    }

    private JobExecutionLockManager manager(ObjectProvider<RedissonClient> provider) {
        return new JobExecutionLockManager(provider);
    }
}
