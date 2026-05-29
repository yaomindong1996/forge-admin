package com.mdframe.forge.plugin.generator.constant;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * 业务对象设计状态常量。
 */
public class BusinessObjectDesignStatus {

    public static final String DRAFT = "DRAFT";

    public static final String DESIGNING = "DESIGNING";

    public static final String READY = "READY";

    public static final String PUBLISHED = "PUBLISHED";

    public static final String CHANGED = "CHANGED";

    public static final String DISABLED = "DISABLED";

    private static final Set<String> VALUES = Set.of(DRAFT, DESIGNING, READY, PUBLISHED, CHANGED, DISABLED);

    private BusinessObjectDesignStatus() {
    }

    public static boolean isValid(String status) {
        return VALUES.contains(normalize(status));
    }

    public static String normalize(String status) {
        return StringUtils.defaultIfBlank(status, DRAFT).trim().toUpperCase(Locale.ROOT);
    }
}
