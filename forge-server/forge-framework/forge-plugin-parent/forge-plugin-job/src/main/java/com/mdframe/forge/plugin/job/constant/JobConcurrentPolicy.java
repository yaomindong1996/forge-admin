package com.mdframe.forge.plugin.job.constant;

import java.util.Set;

/**
 * 定时任务并发策略。
 */
public final class JobConcurrentPolicy {

    public static final String ALLOW = "ALLOW";
    public static final String SKIP_IF_RUNNING = "SKIP_IF_RUNNING";
    public static final String DEFAULT = ALLOW;

    private static final Set<String> SUPPORTED_POLICIES = Set.of(ALLOW, SKIP_IF_RUNNING);

    private JobConcurrentPolicy() {
    }

    public static boolean isSupported(String policy) {
        return SUPPORTED_POLICIES.contains(policy);
    }
}
