package com.mdframe.forge.starter.cache.managed.model;

import com.mdframe.forge.starter.cache.managed.enums.CacheMode;
import com.mdframe.forge.starter.cache.managed.enums.CacheScope;

import java.io.Serializable;
import java.util.List;

public record CacheDefinition(
        String applicationCode,
        String cacheName,
        String description,
        CacheMode defaultMode,
        List<CacheMode> allowedModes,
        CacheScope scope,
        long localTtlSeconds,
        long redisTtlSeconds,
        int localMaxSize,
        boolean cacheNull,
        long nullTtlSeconds,
        String source
) implements Serializable {

    public CacheDefinition {
        allowedModes = List.copyOf(allowedModes);
    }

    public String identity() {
        return applicationCode + "::" + cacheName;
    }
}
