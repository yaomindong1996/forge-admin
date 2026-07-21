package com.mdframe.forge.plugin.job.manager;

import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.scheduler.JobScheduleException;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobScheduleSynchronizationLockManagerTest {

    @Test
    void shouldAcquireAndReleaseDistributedScheduleLock() throws Exception {
        RLock lock = mock(RLock.class);
        when(lock.tryLock(5000L, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        RedissonClient client = mock(RedissonClient.class);
        when(client.getLock(anyString())).thenReturn(lock);
        JobScheduleSynchronizationLockManager manager = manager(provider(client));

        try (JobScheduleSynchronizationLockManager.LockHandle ignored =
                     manager.acquire("sampleJob", "DEFAULT")) {
            verify(lock).tryLock(5000L, TimeUnit.MILLISECONDS);
        }

        verify(lock).unlock();
    }

    @Test
    void shouldFailClosedWhenRedisIsUnavailable() {
        JobScheduleSynchronizationLockManager manager = manager(provider(null));

        assertThrows(JobScheduleException.class,
                () -> manager.acquire("sampleJob", "DEFAULT"));
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<RedissonClient> provider(RedissonClient client) {
        ObjectProvider<RedissonClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(client);
        return provider;
    }

    private JobScheduleSynchronizationLockManager manager(ObjectProvider<RedissonClient> provider) {
        JobProperties properties = new JobProperties();
        properties.setScheduleSyncLockWaitMillis(5000L);
        return new JobScheduleSynchronizationLockManager(provider, properties);
    }
}
