package com.mdframe.forge.plugin.ai.provider.adapter;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 内存模拟 Redis 的原子 compare-and-delete；不执行真实 Lua，不代表真实 Redis 联调。 */
final class AgentRedisFixture {

    final StringRedisTemplate redis = mock(StringRedisTemplate.class, RETURNS_DEEP_STUBS);
    final Map<String, String> data = new ConcurrentHashMap<>();
    final AtomicInteger consumes = new AtomicInteger();
    volatile CountDownLatch readBarrier;

    AgentRedisFixture() {
        var values = redis.opsForValue();
        doAnswer(invocation -> {
            data.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString(), eq(10L), eq(TimeUnit.MINUTES));
        when(values.get(anyString())).thenAnswer(invocation -> {
            String value = data.get(invocation.getArgument(0));
            CountDownLatch barrier = readBarrier;
            if (barrier != null) {
                barrier.countDown();
                if (!barrier.await(3, TimeUnit.SECONDS)) {
                    throw new AssertionError("fixture interrupt read barrier timed out");
                }
            }
            return value;
        });
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList(), anyString()))
                .thenAnswer(invocation -> {
                    List<String> keys = invocation.getArgument(1);
                    String expected = invocation.getArgument(2);
                    if (data.remove(keys.get(0), expected)) {
                        consumes.incrementAndGet();
                        return 1L;
                    }
                    return 0L;
                });
        // 断言业务调用时不计入 fixture 自身获取 opsForValue 的初始化操作。
        clearInvocations(redis);
    }
}
