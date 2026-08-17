package com.mdframe.forge.starter.cache.managed.model;

import com.mdframe.forge.starter.cache.managed.enums.CacheMode;

import java.io.Serializable;

public record CachePolicyOverride(
        String applicationCode,
        String cacheName,
        boolean enabled,
        CacheMode cacheMode,
        long localTtlSeconds,
        long redisTtlSeconds,
        int localMaxSize,
        boolean cacheNull,
        long nullTtlSeconds,
        long policyVersion
) implements Serializable {

    public String identity() {
        return applicationCode + "::" + cacheName;
    }
}
