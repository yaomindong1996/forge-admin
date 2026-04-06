package com.mdframe.forge.starter.idempotent.strategy;

import com.mdframe.forge.starter.idempotent.annotation.Idempotent;
import com.mdframe.forge.starter.idempotent.dto.IdempotentResult;
import com.mdframe.forge.starter.idempotent.exception.IdempotentException;
import com.mdframe.forge.starter.idempotent.lock.LockManager;
import com.mdframe.forge.starter.idempotent.properties.LockProperties;
import com.mdframe.forge.starter.idempotent.service.ResultCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;

@Slf4j
@RequiredArgsConstructor
public class ReturnCacheStrategyHandler implements IdempotentStrategyHandler {
    
    private final LockManager lockManager;
    private final ResultCacheService resultCacheService;
    private final LockProperties lockProperties;
    
    @Override
    public Object handle(ProceedingJoinPoint joinPoint, Idempotent annotation, String idempotentKey) throws Throwable {
        IdempotentResult cachedResult = resultCacheService.getCachedResult(idempotentKey);
        
        if (cachedResult != null && IdempotentResult.STATUS_SUCCESS.equals(cachedResult.getStatus())) {
            log.info("缓存模式: 返回缓存结果, key={}", idempotentKey);
            return cachedResult.getResult();
        }
        
        if (!lockManager.tryLock(idempotentKey, lockProperties.getWaitTime(), lockProperties.getLeaseTime())) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            cachedResult = resultCacheService.getCachedResult(idempotentKey);
            if (cachedResult != null && IdempotentResult.STATUS_SUCCESS.equals(cachedResult.getStatus())) {
                log.info("缓存模式: 并发等待后返回缓存结果, key={}", idempotentKey);
                return cachedResult.getResult();
            }
            
            log.warn("缓存模式: 并发冲突, key={}", idempotentKey);
            throw new IdempotentException("并发冲突，请稍后重试");
        }
        
        try {
            Object result = joinPoint.proceed();
            log.debug("缓存模式: 业务执行成功, key={}", idempotentKey);
            
            if (annotation.cacheResult()) {
                resultCacheService.cacheResult(idempotentKey, result, annotation.cacheExpire());
            }
            
            if (annotation.deleteKeyAfterSuccess()) {
                lockManager.unlock(idempotentKey);
                resultCacheService.deleteCachedResult(idempotentKey);
            }
            
            return result;
        } catch (Throwable e) {
            lockManager.unlock(idempotentKey);
            log.error("缓存模式: 业务执行失败, key={}, error={}", idempotentKey, e.getMessage(), e);
            throw e;
        }
    }
}