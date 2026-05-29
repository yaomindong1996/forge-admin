package com.mdframe.forge.plugin.generator.constant;

/**
 * 业务就绪度状态常量
 */
public class BusinessReadinessStatus {

    /**
     * 已登记，但不一定可运行
     */
    public static final String REGISTERED = "REGISTERED";

    /**
     * 已配置必要参数，但未验证运行
     */
    public static final String CONFIGURED = "CONFIGURED";

    /**
     * 可运行
     */
    public static final String RUNNABLE = "RUNNABLE";

    /**
     * 缺少必要配置
     */
    public static final String MISSING = "MISSING";

    /**
     * 配置存在但运行校验失败
     */
    public static final String ERROR = "ERROR";

    /**
     * 验收通过
     */
    public static final String PASSED = "PASSED";

    /**
     * 部分通过
     */
    public static final String PARTIAL = "PARTIAL";

    /**
     * 验收失败
     */
    public static final String FAILED = "FAILED";

    private BusinessReadinessStatus() {
    }
}
