package com.mdframe.forge.plugin.generator.constant;

import java.util.Set;

/**
 * 应用设计状态。
 */
public final class BusinessApplicationDesignStatus {

    public static final String DRAFT = "DRAFT";
    public static final String READY = "READY";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String CHANGED = "CHANGED";

    private static final Set<String> SUPPORTED_STATUSES = Set.of(DRAFT, READY, PUBLISHED, CHANGED);

    private BusinessApplicationDesignStatus() {
    }

    public static Set<String> supportedStatuses() {
        return SUPPORTED_STATUSES;
    }
}
