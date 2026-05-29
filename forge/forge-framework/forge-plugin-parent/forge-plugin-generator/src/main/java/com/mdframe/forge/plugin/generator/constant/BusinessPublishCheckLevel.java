package com.mdframe.forge.plugin.generator.constant;

/**
 * 业务对象发布检查级别。
 */
public class BusinessPublishCheckLevel {

    public static final String PASS = "PASS";

    public static final String WARN = "WARN";

    public static final String BLOCK = "BLOCK";

    private BusinessPublishCheckLevel() {
    }

    public static boolean isBlocking(String level) {
        return BLOCK.equals(level);
    }
}
