package com.mdframe.forge.starter.idempotent.service;

import com.mdframe.forge.starter.idempotent.dto.IdempotentResult;

public interface ResultCacheService {
    
    void cacheResult(String key, Object result, int expireSeconds);
    
    void cacheException(String key, Throwable exception, int expireSeconds);
    
    IdempotentResult getCachedResult(String key);
    
    void deleteCachedResult(String key);
    
    boolean exists(String key);
    
    void markProcessing(String key, String requestId, int expireSeconds);
}