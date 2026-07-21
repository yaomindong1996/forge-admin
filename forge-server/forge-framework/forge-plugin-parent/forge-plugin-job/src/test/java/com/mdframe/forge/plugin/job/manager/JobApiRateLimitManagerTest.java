package com.mdframe.forge.plugin.job.manager;

import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.model.JobApiPrincipal;
import com.mdframe.forge.plugin.job.support.JobOpenApiException;
import org.junit.jupiter.api.Test;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JobApiRateLimitManagerTest {

    @Test
    void shouldUseAuthenticatedKeyIdAndRejectWhenLimitExceeded() {
        RRateLimiter limiter = mock(RRateLimiter.class);
        when(limiter.tryAcquire()).thenReturn(false);
        RedissonClient client = mock(RedissonClient.class);
        when(client.getRateLimiter("forge:job:openapi:rate:trigger:trusted-key-id")).thenReturn(limiter);
        JobApiRateLimitManager manager = manager(provider(client));

        JobOpenApiException exception = assertThrows(JobOpenApiException.class,
                () -> manager.acquireTrigger(principal()));

        assertEquals(429, exception.getStatus());
        verify(client).getRateLimiter("forge:job:openapi:rate:trigger:trusted-key-id");
    }

    @Test
    void shouldFailClosedWhenRedisIsMissing() {
        ObjectProvider<RedissonClient> provider = provider(null);
        JobApiRateLimitManager manager = manager(provider);

        JobOpenApiException exception = assertThrows(JobOpenApiException.class,
                () -> manager.acquireRead(principal()));

        assertEquals(503, exception.getStatus());
        verify(provider).getIfAvailable();
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<RedissonClient> provider(RedissonClient client) {
        ObjectProvider<RedissonClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(client);
        return provider;
    }

    private JobApiRateLimitManager manager(ObjectProvider<RedissonClient> provider) {
        return new JobApiRateLimitManager(provider, new JobProperties());
    }

    private JobApiPrincipal principal() {
        return new JobApiPrincipal(1L, 1L, "trusted-key-id", "caller",
                Set.of("jobs:trigger"), Set.of(7L), Set.of());
    }
}
