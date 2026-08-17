package com.mdframe.forge.starter.cache.managed.model;

public record ManagedCacheView(
        CacheDefinition definition,
        EffectiveCachePolicy policy,
        boolean overridden,
        long hitCount,
        long missCount,
        long putCount,
        long evictionCount,
        long failureCount
) {
}
