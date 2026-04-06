package com.mdframe.forge.starter.idempotent.exception;

import com.mdframe.forge.starter.core.exception.BusinessException;

public class IdempotentException extends BusinessException {
    public IdempotentException(String message) {
        super(429, message);
    }

    public IdempotentException(String message, Throwable cause) {
        super(429, message, cause);
    }
}
