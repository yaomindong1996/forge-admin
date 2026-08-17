package com.mdframe.forge.starter.cache.managed.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

public record ManagedCacheValue(
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class") Object value,
        boolean nullValue,
        long localTtlNanos
) implements Serializable {
}
