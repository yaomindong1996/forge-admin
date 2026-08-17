package com.mdframe.forge.starter.cache.managed.store;

import com.mdframe.forge.starter.cache.managed.model.CacheLookup;
import com.mdframe.forge.starter.cache.managed.model.ManagedCacheValue;
import org.redisson.api.RLocalCachedMapCache;

import java.util.concurrent.TimeUnit;

public class MultiLevelCacheHandle implements ManagedCacheHandle {

    private final RLocalCachedMapCache<String, ManagedCacheValue> cache;

    public MultiLevelCacheHandle(RLocalCachedMapCache<String, ManagedCacheValue> cache) {
        this.cache = cache;
    }

    @Override
    public CacheLookup get(String key) {
        ManagedCacheValue value = cache.get(key);
        return value == null ? CacheLookup.miss() : CacheLookup.hit(value.value());
    }

    @Override
    public void put(String key, ManagedCacheValue value, long ttlSeconds) {
        cache.fastPut(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void evict(String key) {
        cache.fastRemove(key);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public void close() {
        cache.clearLocalCache();
    }
}
