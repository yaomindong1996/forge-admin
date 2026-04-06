package com.mdframe.forge.starter.idempotent.strategy;

import com.mdframe.forge.starter.idempotent.annotation.Idempotent;
import com.mdframe.forge.starter.idempotent.exception.IdempotentException;
import com.mdframe.forge.starter.idempotent.lock.LockManager;
import com.mdframe.forge.starter.idempotent.properties.LockProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;

@Slf4j
@RequiredArgsConstructor
public class StrictStrategyHandler implements IdempotentStrategyHandler {
    
    private final LockManager lockManager;
    private final LockProperties lockProperties;
    
    @Override
    public Object handle(ProceedingJoinPoint joinPoint, Idempotent annotation, String idempotentKey) throws Throwable {
        int expire = annotation.expire();
        
        if (!lockManager.tryLock(idempotentKey, lockProperties.getWaitTime(), lockProperties.getLeaseTime())) {
            log.warn("严格模式: 拒绝重复请求, key={}", idempotentKey);
            throw new IdempotentException(annotation.message());
        }
        
        try {
            Object result = joinPoint.proceed();
            log.debug("严格模式: 业务执行成功, key={}", idempotentKey);
            
            if (annotation.deleteKeyAfterSuccess()) {
                lockManager.unlock(idempotentKey);
            }
            
            return result;
        } catch (Throwable e) {
            lockManager.unlock(idempotentKey);
            log.error("严格模式: 业务执行失败, key={}, error={}", idempotentKey, e.getMessage(), e);
            throw e;
        }
    }
}