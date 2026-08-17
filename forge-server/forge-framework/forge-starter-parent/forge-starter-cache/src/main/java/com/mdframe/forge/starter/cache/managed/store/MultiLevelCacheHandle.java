package com.mdframe.forge.starter.cache.managed.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.mdframe.forge.starter.cache.managed.model.CacheLookup;
import com.mdframe.forge.starter.cache.managed.model.ManagedCacheValue;
import org.redisson.api.RLocalCachedMapCache;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MultiLevelCacheHandle implements ManagedCacheHandle {

    private final RLocalCachedMapCache<String, ManagedCacheValue> cache;
    private final Cache<String, Long> localExpirations;
    private final Ticker ticker;

    public MultiLevelCacheHandle(RLocalCachedMapCache<String, ManagedCacheValue> cache, int maximumSize) {
        this(cache, maximumSize, Ticker.systemTicker());
    }

    MultiLevelCacheHandle(RLocalCachedMapCache<String, ManagedCacheValue> cache,
                          int maximumSize,
                          Ticker ticker) {
        this.cache = cache;
        this.localExpirations = Caffeine.newBuilder().maximumSize(maximumSize).build();
        this.ticker = ticker;
    }

    @Override
    public CacheLookup get(String key) {
        Map<String, ManagedCacheValue> localCache = cache.getCachedMap();
        ManagedCacheValue localValue = localCache.get(key);
        if (localValue != null && !isLocalExpired(key, localValue)) {
            return CacheLookup.hit(localValue.value());
        }
        if (localValue != null) {
            localCache.entrySet().remove(Map.entry(key, localValue));
            localExpirations.invalidate(key);
        }
        ManagedCacheValue value = cache.get(key);
        if (value == null) {
            localExpirations.invalidate(key);
            return CacheLookup.miss();
        }
        rememberLocalExpiry(key, value);
        return CacheLookup.hit(value.value());
    }

    @Override
    public void put(String key, ManagedCacheValue value, long ttlSeconds) {
        cache.fastPut(key, value, ttlSeconds, TimeUnit.SECONDS);
        rememberLocalExpiry(key, value);
    }

    @Override
    public void evict(String key) {
        cache.fastRemove(key);
        localExpirations.invalidate(key);
    }

    @Override
    public void clear() {
        cache.clear();
        localExpirations.invalidateAll();
    }

    @Override
    public void close() {
        cache.clearLocalCache();
        localExpirations.invalidateAll();
    }

    private boolean isLocalExpired(String key, ManagedCacheValue value) {
        Long expiresAt = localExpirations.getIfPresent(key);
        if (expiresAt == null) {
            rememberLocalExpiry(key, value);
            return false;
        }
        return ticker.read() >= expiresAt;
    }

    private void rememberLocalExpiry(String key, ManagedCacheValue value) {
        long now = ticker.read();
        long ttl = value.localTtlNanos();
        long expiresAt = now + ttl;
        if (ttl > 0 && expiresAt < now) {
            expiresAt = Long.MAX_VALUE;
        }
        localExpirations.put(key, expiresAt);
    }
}
