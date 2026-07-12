package com.mdframe.forge.plugin.capability.model;

public record CapabilityQuery(String capabilityCodePrefix, int pageSize, String cursor) {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public CapabilityQuery {
        capabilityCodePrefix = normalize(capabilityCodePrefix);
        cursor = normalize(cursor);
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize 必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
        }
    }

    public static CapabilityQuery all() {
        return new CapabilityQuery(null, MAX_PAGE_SIZE, null);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
