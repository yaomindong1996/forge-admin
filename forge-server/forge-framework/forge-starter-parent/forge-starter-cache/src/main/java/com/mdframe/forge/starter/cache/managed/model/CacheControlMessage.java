package com.mdframe.forge.starter.cache.managed.model;

import java.io.Serializable;

public record CacheControlMessage(
        CacheControlAction action,
        String applicationCode,
        String cacheName,
        CachePolicyOverride policy
) implements Serializable {
}
