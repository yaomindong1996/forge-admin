package com.mdframe.forge.plugin.job.constant;

import java.util.Set;

/**
 * 定时任务上层调用方式。
 */
public final class JobInvokeMode {

    public static final String SINGLE = "SINGLE";
    public static final String FLOW = "FLOW";

    private static final Set<String> SUPPORTED = Set.of(SINGLE, FLOW);

    private JobInvokeMode() {
    }

    public static boolean isSupported(String value) {
        return SUPPORTED.contains(value);
    }
}
