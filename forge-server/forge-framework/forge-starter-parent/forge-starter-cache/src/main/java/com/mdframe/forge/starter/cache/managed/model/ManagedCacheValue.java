package com.mdframe.forge.starter.cache.managed.model;

import java.io.Serializable;

public record ManagedCacheValue(Object value, boolean nullValue, long localTtlNanos) implements Serializable {
}
