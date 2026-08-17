package com.mdframe.forge.starter.cache.managed.model;

public record CacheLookup(boolean hit, Object value) {

    public static CacheLookup miss() {
        return new CacheLookup(false, null);
    }

    public static CacheLookup hit(Object value) {
        return new CacheLookup(true, value);
    }
}
