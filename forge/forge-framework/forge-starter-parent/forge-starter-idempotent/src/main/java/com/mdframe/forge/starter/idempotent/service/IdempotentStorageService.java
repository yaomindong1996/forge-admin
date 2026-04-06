package com.mdframe.forge.starter.idempotent.service;

public interface IdempotentStorageService {
    boolean tryAcquire(String key, int expireSeconds);
    void release(String key);
    boolean exists(String key);
}
