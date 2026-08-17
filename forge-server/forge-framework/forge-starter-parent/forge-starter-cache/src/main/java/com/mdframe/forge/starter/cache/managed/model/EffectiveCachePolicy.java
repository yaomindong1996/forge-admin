package com.mdframe.forge.starter.cache.managed.model;

import com.mdframe.forge.starter.cache.managed.enums.CacheMode;

import java.io.Serializable;

public record EffectiveCachePolicy(
        boolean enabled,
        CacheMode cacheMode,
        long localTtlSeconds,
        long redisTtlSeconds,
        int localMaxSize,
        boolean cacheNull,
        long nullTtlSeconds,
        long policyVersion
) implements Serializable {

    public static EffectiveCachePolicy from(CacheDefinition definition, CachePolicyOverride override) {
        if (override == null) {
            return new EffectiveCachePolicy(
                    true,
                    definition.defaultMode(),
                    definition.localTtlSeconds(),
                    definition.redisTtlSeconds(),
                    definition.localMaxSize(),
                    definition.cacheNull(),
                    definition.nullTtlSeconds(),
                    0L);
        }
        return new EffectiveCachePolicy(
                override.enabled(),
                override.cacheMode(),
                override.localTtlSeconds(),
                override.redisTtlSeconds(),
                override.localMaxSize(),
                override.cacheNull(),
                override.nullTtlSeconds(),
                override.policyVersion());
    }
}
