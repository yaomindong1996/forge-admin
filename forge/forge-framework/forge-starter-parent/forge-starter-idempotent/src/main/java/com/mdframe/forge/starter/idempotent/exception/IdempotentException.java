package com.mdframe.forge.starter.idempotent.exception;

public class IdempotentException extends RuntimeException {
    public IdempotentException(String message) {
        super(message);
    }

    public IdempotentException(String message, Throwable cause) {
        super(message, cause);
    }
}
