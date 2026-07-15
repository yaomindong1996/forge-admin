package com.mdframe.forge.plugin.generator.constant;

import java.util.Set;

/**
 * 业务扩展治理状态。
 */
public final class BusinessExtensionStatus {

    public static final String DRAFT = "DRAFT";
    public static final String TESTED = "TESTED";
    public static final String ENABLED = "ENABLED";
    public static final String DISABLED = "DISABLED";

    public static final Set<String> ALL = Set.of(DRAFT, TESTED, ENABLED, DISABLED);

    private BusinessExtensionStatus() {
    }
}
