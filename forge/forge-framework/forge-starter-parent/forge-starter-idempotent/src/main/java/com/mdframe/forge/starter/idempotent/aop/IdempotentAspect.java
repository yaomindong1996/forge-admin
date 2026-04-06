package com.mdframe.forge.starter.idempotent.aop;

import com.mdframe.forge.starter.idempotent.annotation.Idempotent;
import com.mdframe.forge.starter.idempotent.exception.IdempotentException;
import com.mdframe.forge.starter.idempotent.generator.IdempotentKeyGenerator;
import com.mdframe.forge.starter.idempotent.properties.IdempotentProperties;
import com.mdframe.forge.starter.idempotent.service.IdempotentStorageService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

@Slf4j
@Aspect
public class IdempotentAspect {
    private final IdempotentKeyGenerator keyGenerator;
    private final IdempotentStorageService storageService;
    private final IdempotentProperties properties;

    public IdempotentAspect(IdempotentKeyGenerator keyGenerator, IdempotentStorageService storageService, IdempotentProperties properties) {
        this.keyGenerator = keyGenerator;
        this.storageService = storageService;
        this.properties = properties;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        String prefix = idempotent.prefix().isEmpty() ? properties.getPrefix() : idempotent.prefix();
        String key = keyGenerator.generate(joinPoint, prefix, idempotent.key());
        int expire = idempotent.expire() > 0 ? idempotent.expire() : properties.getExpire();
        String message = idempotent.message().isEmpty() ? properties.getMessage() : idempotent.message();

        log.debug("幂等校验，key: {}", key);

        if (!storageService.tryAcquire(key, expire)) {
            log.warn("重复请求，key: {}", key);
            throw new IdempotentException(message);
        }

        try {
            Object result = joinPoint.proceed();
            if (idempotent.deleteKeyAfterSuccess()) {
                storageService.release(key);
            }
            return result;
        } catch (Throwable e) {
            storageService.release(key);
            throw e;
        }
    }
}
