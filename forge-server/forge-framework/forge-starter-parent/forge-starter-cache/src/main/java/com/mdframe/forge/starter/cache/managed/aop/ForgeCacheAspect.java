package com.mdframe.forge.starter.cache.managed.aop;

import com.mdframe.forge.starter.cache.managed.ForgeManagedCacheManager;
import com.mdframe.forge.starter.cache.managed.annotation.ForgeCacheEvict;
import com.mdframe.forge.starter.cache.managed.annotation.ForgeCachePut;
import com.mdframe.forge.starter.cache.managed.annotation.ForgeCacheable;
import com.mdframe.forge.starter.cache.managed.definition.CacheDefinitionResolver;
import com.mdframe.forge.starter.cache.managed.key.ForgeCacheKeyResolver;
import com.mdframe.forge.starter.cache.managed.model.CacheDefinition;
import com.mdframe.forge.starter.cache.managed.model.CacheLookup;
import com.mdframe.forge.starter.cache.managed.properties.ManagedCacheProperties;
import com.mdframe.forge.starter.cache.managed.transaction.CacheTransactionExecutor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.util.Optional;

@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@Slf4j
public class ForgeCacheAspect {

    private final ForgeManagedCacheManager cacheManager;
    private final CacheDefinitionResolver definitionResolver;
    private final ForgeCacheKeyResolver keyResolver;
    private final CacheTransactionExecutor transactionExecutor;
    private final ManagedCacheProperties properties;

    public ForgeCacheAspect(ForgeManagedCacheManager cacheManager,
                            CacheDefinitionResolver definitionResolver,
                            ForgeCacheKeyResolver keyResolver,
                            CacheTransactionExecutor transactionExecutor,
                            ManagedCacheProperties properties) {
        this.cacheManager = cacheManager;
        this.definitionResolver = definitionResolver;
        this.keyResolver = keyResolver;
        this.transactionExecutor = transactionExecutor;
        this.properties = properties;
    }

    @Around("@annotation(annotation)")
    public Object cacheable(ProceedingJoinPoint joinPoint, ForgeCacheable annotation) throws Throwable {
        if (!properties.isAnnotationEnabled()) {
            return joinPoint.proceed();
        }
        Invocation invocation;
        Optional<String> key;
        try {
            invocation = invocation(joinPoint, annotation.cacheName());
            key = keyResolver.resolve(
                    invocation.method(), joinPoint.getTarget(), joinPoint.getArgs(), annotation.key(),
                    invocation.definition().scope(), null);
        } catch (RuntimeException exception) {
            log.warn("准备受管缓存读取失败，已穿透: cache={}", annotation.cacheName(), exception);
            return joinPoint.proceed();
        }
        if (key.isEmpty()) {
            return joinPoint.proceed();
        }
        CacheLookup lookup;
        try {
            lookup = cacheManager.get(invocation.definition(), key.get());
        } catch (RuntimeException exception) {
            log.warn("读取受管缓存失败，已穿透: cache={}", annotation.cacheName(), exception);
            lookup = CacheLookup.miss();
        }
        if (lookup.hit()) {
            return lookup.value();
        }
        Object result = joinPoint.proceed();
        afterCommit(annotation.cacheName(), "写入", () ->
                cacheManager.put(invocation.definition(), key.get(), result));
        return result;
    }

    @Around("@annotation(annotation)")
    public Object put(ProceedingJoinPoint joinPoint, ForgeCachePut annotation) throws Throwable {
        Object result = joinPoint.proceed();
        if (!properties.isAnnotationEnabled()) {
            return result;
        }
        try {
            Invocation invocation = invocation(joinPoint, annotation.cacheName());
            Optional<String> key = keyResolver.resolve(
                    invocation.method(), joinPoint.getTarget(), joinPoint.getArgs(), annotation.key(),
                    invocation.definition().scope(), result);
            key.ifPresent(cacheKey -> afterCommit(annotation.cacheName(), "写入", () ->
                    cacheManager.put(invocation.definition(), cacheKey, result)));
        } catch (RuntimeException exception) {
            log.warn("准备受管缓存写入失败，业务结果保持不变: cache={}", annotation.cacheName(), exception);
        }
        return result;
    }

    @Around("@annotation(annotation)")
    public Object evict(ProceedingJoinPoint joinPoint, ForgeCacheEvict annotation) throws Throwable {
        Object result = joinPoint.proceed();
        if (!properties.isAnnotationEnabled()) {
            return result;
        }
        try {
            Invocation invocation = invocation(joinPoint, annotation.cacheName());
            if (annotation.allEntries()) {
                afterCommit(annotation.cacheName(), "清空", () -> cacheManager.clear(invocation.definition()));
                return result;
            }
            Optional<String> key = keyResolver.resolve(
                    invocation.method(), joinPoint.getTarget(), joinPoint.getArgs(), annotation.key(),
                    invocation.definition().scope(), result);
            key.ifPresent(cacheKey -> afterCommit(annotation.cacheName(), "删除", () ->
                    cacheManager.evict(invocation.definition(), cacheKey)));
        } catch (RuntimeException exception) {
            log.warn("准备受管缓存失效失败，业务结果保持不变: cache={}", annotation.cacheName(), exception);
        }
        return result;
    }

    private void afterCommit(String cacheName, String operation, Runnable action) {
        try {
            transactionExecutor.afterCommit(() -> {
                try {
                    action.run();
                } catch (RuntimeException exception) {
                    log.warn("受管缓存{}失败，业务结果保持不变: cache={}", operation, cacheName, exception);
                }
            });
        } catch (RuntimeException exception) {
            log.warn("注册受管缓存提交后{}失败，业务结果保持不变: cache={}", operation, cacheName, exception);
        }
    }

    private Invocation invocation(ProceedingJoinPoint joinPoint, String cacheName) {
        Method signatureMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Method method = AopUtils.getMostSpecificMethod(signatureMethod, joinPoint.getTarget().getClass());
        CacheDefinition definition = definitionResolver.resolve(joinPoint.getTarget().getClass(), cacheName);
        return new Invocation(method, definition);
    }

    private record Invocation(Method method, CacheDefinition definition) {
    }
}
