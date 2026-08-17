package com.mdframe.forge.starter.cache.managed.store;

import com.mdframe.forge.starter.cache.managed.model.CacheLookup;
import com.mdframe.forge.starter.cache.managed.model.ManagedCacheValue;

public interface ManagedCacheHandle {

    CacheLookup get(String key);

    void put(String key, ManagedCacheValue value, long ttlSeconds);

    void evict(String key);

    void clear();

    default void close() {
    }
}
