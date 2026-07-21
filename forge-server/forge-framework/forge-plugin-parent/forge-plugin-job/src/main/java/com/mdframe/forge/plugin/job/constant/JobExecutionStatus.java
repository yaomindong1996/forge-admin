package com.mdframe.forge.plugin.job.constant;

/**
 * 定时任务执行状态，保留历史成功和失败值。
 */
public final class JobExecutionStatus {

    public static final int FAILED = 0;
    public static final int SUCCESS = 1;
    public static final int RUNNING = 2;
    public static final int SKIPPED = 3;
    public static final int ACCEPTED = 4;

    private JobExecutionStatus() {
    }
}
