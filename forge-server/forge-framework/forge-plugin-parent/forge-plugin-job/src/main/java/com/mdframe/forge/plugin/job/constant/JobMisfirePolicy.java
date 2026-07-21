package com.mdframe.forge.plugin.job.constant;

import java.util.Set;

/**
 * 定时任务错过触发策略。
 */
public final class JobMisfirePolicy {

    public static final String FIRE_ONCE_NOW = "FIRE_ONCE_NOW";
    public static final String DO_NOTHING = "DO_NOTHING";
    public static final String DEFAULT = DO_NOTHING;

    private static final Set<String> SUPPORTED_POLICIES = Set.of(FIRE_ONCE_NOW, DO_NOTHING);

    private JobMisfirePolicy() {
    }

    public static boolean isSupported(String policy) {
        return SUPPORTED_POLICIES.contains(policy);
    }
}
