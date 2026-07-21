package com.mdframe.forge.plugin.job.scheduler;

import com.mdframe.forge.starter.core.exception.BusinessException;

/**
 * Quartz 调度操作失败。
 */
public class JobScheduleException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public JobScheduleException(String message) {
        super(message);
    }

    public JobScheduleException(String message, Throwable cause) {
        super(message, cause);
    }
}
