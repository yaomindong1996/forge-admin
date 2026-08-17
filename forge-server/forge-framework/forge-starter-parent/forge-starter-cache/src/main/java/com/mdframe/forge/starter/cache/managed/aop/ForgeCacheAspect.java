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
        Invocation invocation = invocation(joinPoint, annotation.cacheName());
        Optional<String> key = keyResolver.resolve(
                invocation.method(), joinPoint.getTarget(), joinPoint.getArgs(), annotation.key(),
                invocation.definition().scope(), null);
        if (key.isEmpty()) {
            return joinPoint.proceed();
        }
        CacheLookup lookup = cacheManager.get(invocation.definition(), key.get());
        if (lookup.hit()) {
            return lookup.value();
        }
        Object result = joinPoint.proceed();
        transactionExecutor.afterCommit(() -> cacheManager.put(invocation.definition(), key.get(), result));
        return result;
    }

    @Around("@annotation(annotation)")
    public Object put(ProceedingJoinPoint joinPoint, ForgeCachePut annotation) throws Throwable {
        Object result = joinPoint.proceed();
        if (!properties.isAnnotationEnabled()) {
            return result;
        }
        Invocation invocation = invocation(joinPoint, annotation.cacheName());
        Optional<String> key = keyResolver.resolve(
                invocation.method(), joinPoint.getTarget(), joinPoint.getArgs(), annotation.key(),
                invocation.definition().scope(), result);
        key.ifPresent(cacheKey -> transactionExecutor.afterCommit(
                () -> cacheManager.put(invocation.definition(), cacheKey, result)));
        return result;
    }

    @Around("@annotation(annotation)")
    public Object evict(ProceedingJoinPoint joinPoint, ForgeCacheEvict annotation) throws Throwable {
        Object result = joinPoint.proceed();
        if (!properties.isAnnotationEnabled()) {
            return result;
        }
        Invocation invocation = invocation(joinPoint, annotation.cacheName());
        if (annotation.allEntries()) {
            transactionExecutor.afterCommit(() -> cacheManager.clear(invocation.definition()));
            return result;
        }
        Optional<String> key = keyResolver.resolve(
                invocation.method(), joinPoint.getTarget(), joinPoint.getArgs(), annotation.key(),
                invocation.definition().scope(), result);
        key.ifPresent(cacheKey -> transactionExecutor.afterCommit(
                () -> cacheManager.evict(invocation.definition(), cacheKey)));
        return result;
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
