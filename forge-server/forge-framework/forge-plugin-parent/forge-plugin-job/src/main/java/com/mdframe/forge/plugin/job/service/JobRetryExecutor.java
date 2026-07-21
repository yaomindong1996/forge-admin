package com.mdframe.forge.plugin.job.service;

import org.springframework.stereotype.Service;

/**
 * 定时任务顶层有限重试执行器。
 */
@Service
public class JobRetryExecutor {

    public static final int MAX_RETRY_COUNT = 5;
    public static final long RETRY_DELAY_MILLIS = 1000L;

    private final Sleeper sleeper;

    public JobRetryExecutor() {
        this(Thread::sleep);
    }

    JobRetryExecutor(Sleeper sleeper) {
        this.sleeper = sleeper;
    }

    public RetryResult execute(int configuredRetryCount, boolean idempotent, Attempt attempt) {
        int retryLimit = idempotent
                ? Math.min(Math.max(configuredRetryCount, 0), MAX_RETRY_COUNT)
                : 0;
        for (int retryCount = 0; retryCount <= retryLimit; retryCount++) {
            try {
                return RetryResult.success(attempt.execute(), retryCount);
            } catch (Exception exception) {
                if (retryCount >= retryLimit) {
                    return RetryResult.failure(exception, retryCount);
                }
                try {
                    sleeper.sleep(RETRY_DELAY_MILLIS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return RetryResult.failure(interruptedException, retryCount);
                }
            }
        }
        throw new IllegalStateException("任务重试状态异常");
    }

    @FunctionalInterface
    public interface Attempt {
        String execute() throws Exception;
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    public record RetryResult(boolean success, String result, Throwable error, int retryCount) {

        private static RetryResult success(String result, int retryCount) {
            return new RetryResult(true, result, null, retryCount);
        }

        private static RetryResult failure(Throwable error, int retryCount) {
            return new RetryResult(false, null, error, retryCount);
        }
    }
}
