package com.mdframe.forge.starter.cache.managed.store;

import com.github.benmanes.caffeine.cache.Ticker;
import com.mdframe.forge.starter.cache.managed.model.ManagedCacheValue;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLocalCachedMapCache;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class MultiLevelCacheHandleTest {

    @Test
    void shouldApplyIndependentLocalTtlForRegularAndNullValues() {
        AtomicLong now = new AtomicLong();
        Map<String, ManagedCacheValue> local = new HashMap<>();
        Map<String, ManagedCacheValue> remote = new HashMap<>();
        ManagedCacheValue regular = new ManagedCacheValue("value", false, TimeUnit.SECONDS.toNanos(60));
        ManagedCacheValue nullValue = new ManagedCacheValue(null, true, TimeUnit.SECONDS.toNanos(10));
        local.put("regular", regular);
        local.put("null", nullValue);
        remote.putAll(local);

        MultiLevelCacheHandle handle = new MultiLevelCacheHandle(cache(local, remote), 100, now::get);
        assertThat(handle.get("regular").value()).isEqualTo("value");
        assertThat(handle.get("null").hit()).isTrue();

        now.set(TimeUnit.SECONDS.toNanos(11));

        assertThat(handle.get("regular").value()).isEqualTo("value");
        assertThat(handle.get("null").hit()).isTrue();
        assertThat(local).containsKeys("regular", "null");
    }

    @SuppressWarnings("unchecked")
    private RLocalCachedMapCache<String, ManagedCacheValue> cache(
            Map<String, ManagedCacheValue> local,
            Map<String, ManagedCacheValue> remote) {
        return (RLocalCachedMapCache<String, ManagedCacheValue>) Proxy.newProxyInstance(
                RLocalCachedMapCache.class.getClassLoader(),
                new Class<?>[]{RLocalCachedMapCache.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getCachedMap" -> local;
                    case "get" -> {
                        ManagedCacheValue value = remote.get(args[0]);
                        if (value != null) {
                            local.put((String) args[0], value);
                        }
                        yield value;
                    }
                    case "toString" -> "multi-level-cache-test";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
