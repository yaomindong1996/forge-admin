package com.mdframe.forge.starter.idempotent.lock;

public interface LockManager {
    
    boolean tryLock(String lockKey, long waitTime, long leaseTime);
    
    void unlock(String lockKey);
    
    boolean isLocked(String lockKey);
}