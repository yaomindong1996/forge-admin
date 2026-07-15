package com.mdframe.forge.plugin.generator.constant;

import java.util.Set;

/**
 * 应用协调发布运行与不可变版本状态。
 */
public final class BusinessApplicationPublishStatus {

    public static final String CREATED = "CREATED";
    public static final String RUNNING = "RUNNING";
    public static final String PARTIAL = "PARTIAL";
    public static final String FAILED = "FAILED";
    public static final String SUCCESS = "SUCCESS";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String ROLLBACK = "ROLLBACK";

    private static final Set<String> RUN_STATUSES = Set.of(CREATED, RUNNING, PARTIAL, FAILED, SUCCESS);
    private static final Set<String> VERSION_STATUSES = Set.of(PUBLISHED, ROLLBACK);

    private BusinessApplicationPublishStatus() {
    }

    public static Set<String> runStatuses() {
        return RUN_STATUSES;
    }

    public static Set<String> versionStatuses() {
        return VERSION_STATUSES;
    }
}
