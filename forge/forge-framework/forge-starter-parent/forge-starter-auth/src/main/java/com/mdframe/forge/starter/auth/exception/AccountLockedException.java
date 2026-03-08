package com.mdframe.forge.starter.auth.exception;

import lombok.Getter;

/**
 * 账号锁定异常
 */
@Getter
public class AccountLockedException extends RuntimeException {

    /**
     * 剩余锁定时间（秒）
     */
    private final long remainingTime;

    public AccountLockedException(String message, long remainingTime) {
        super(message);
        this.remainingTime = remainingTime;
    }

    public AccountLockedException(long remainingTime) {
        super(String.format("账号已被锁定，请在 %d 秒后重试", remainingTime));
        this.remainingTime = remainingTime;
    }
}
