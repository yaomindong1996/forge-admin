package com.mdframe.forge.starter.auth.service;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiRateLimitManagerTest {
    @Test
    void shouldRejectWhenRedisLimiterHasNoPermit() {
        RedissonClient client = mock(RedissonClient.class);
        RRateLimiter limiter = mock(RRateLimiter.class);
        when(client.getRateLimiter("forge:api:rate:GET:/auth/login:d1TE5ijBiEqmABbcZV5rhKdphyRA0sbZQUgQQ6CA5p0")).thenReturn(limiter);
        when(limiter.tryAcquire()).thenReturn(false);
        ApiRateLimitManager manager = new ApiRateLimitManager(provider(client), true, 120, "forge:api:rate");
        assertThatThrownBy(() -> manager.acquire("ip-127.0.0.1", "GET:/auth/login"))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo(429);
        verify(limiter).trySetRate(eq(org.redisson.api.RateType.OVERALL), eq(120L), any());
    }

    @Test
    void shouldFailClosedWhenRedisIsUnavailable() {
        ApiRateLimitManager manager = new ApiRateLimitManager(provider(null), true, 120, "forge:api:rate");
        assertThatThrownBy(() -> manager.acquire("ip-127.0.0.1", "GET:/auth/login"))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo(503);
    }

    private ObjectProvider<RedissonClient> provider(RedissonClient client) {
        return new ObjectProvider<>() {
            public RedissonClient getObject(Object... args) { return client; }
            public RedissonClient getIfAvailable() { return client; }
            public RedissonClient getIfUnique() { return client; }
            public RedissonClient getIfAvailable(Supplier<RedissonClient> supplier) { return client == null ? supplier.get() : client; }
            public RedissonClient getIfUnique(Supplier<RedissonClient> supplier) { return client == null ? supplier.get() : client; }
            public Iterator<RedissonClient> iterator() { return Collections.emptyIterator(); }
        };
    }
}
