package com.mdframe.forge.starter.cache.managed.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.mdframe.forge.starter.cache.managed.model.CacheLookup;
import com.mdframe.forge.starter.cache.managed.model.ManagedCacheValue;

public class LocalCacheHandle implements ManagedCacheHandle {

    private final Cache<String, ManagedCacheValue> cache;

    public LocalCacheHandle(int maximumSize) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfter(new Expiry<String, ManagedCacheValue>() {
                    @Override
                    public long expireAfterCreate(String key, ManagedCacheValue value, long currentTime) {
                        return value.localTtlNanos();
                    }

                    @Override
                    public long expireAfterUpdate(String key, ManagedCacheValue value,
                                                  long currentTime, long currentDuration) {
                        return value.localTtlNanos();
                    }

                    @Override
                    public long expireAfterRead(String key, ManagedCacheValue value,
                                                long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    @Override
    public CacheLookup get(String key) {
        ManagedCacheValue value = cache.getIfPresent(key);
        return value == null ? CacheLookup.miss() : CacheLookup.hit(value.value());
    }

    @Override
    public void put(String key, ManagedCacheValue value, long ttlSeconds) {
        cache.put(key, value);
    }

    @Override
    public void evict(String key) {
        cache.invalidate(key);
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }
}
