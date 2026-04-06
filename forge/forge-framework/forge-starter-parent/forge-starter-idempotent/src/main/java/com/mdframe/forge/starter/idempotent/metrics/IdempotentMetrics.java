package com.mdframe.forge.starter.idempotent.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class IdempotentMetrics {
    
    private final MeterRegistry meterRegistry;
    
    private Counter totalRequests;
    private Counter successRequests;
    private Counter duplicateRequests;
    private Counter cacheReturnedRequests;
    private Counter tokenInvalidRequests;
    private Counter failedRequests;
    
    private Timer executionTimer;
    private Timer lockAcquireTimer;
    private Timer cacheLookupTimer;
    
    private Gauge cacheHitRate;
    private AtomicLong cacheHits = new AtomicLong(0);
    private AtomicLong cacheMisses = new AtomicLong(0);
    
    public IdempotentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        init();
    }
    
    private void init() {
        totalRequests = Counter.builder("idempotent.requests.total")
            .description("幂等请求总数")
            .register(meterRegistry);
        
        successRequests = Counter.builder("idempotent.requests.success")
            .description("成功执行的幂等请求")
            .register(meterRegistry);
        
        duplicateRequests = Counter.builder("idempotent.requests.duplicate")
            .description("重复请求次数")
            .register(meterRegistry);
        
        cacheReturnedRequests = Counter.builder("idempotent.cache.returned")
            .description("返回缓存结果的次数")
            .register(meterRegistry);
        
        tokenInvalidRequests = Counter.builder("idempotent.token.invalid")
            .description("Token无效次数")
            .register(meterRegistry);
        
        failedRequests = Counter.builder("idempotent.requests.failed")
            .description("失败的幂等请求")
            .register(meterRegistry);
        
        executionTimer = Timer.builder("idempotent.execution.time")
            .description("幂等执行耗时")
            .register(meterRegistry);
        
        lockAcquireTimer = Timer.builder("idempotent.lock.acquire.time")
            .description("获取锁耗时")
            .register(meterRegistry);
        
        cacheLookupTimer = Timer.builder("idempotent.cache.lookup.time")
            .description("缓存查询耗时")
            .register(meterRegistry);
        
        cacheHitRate = Gauge.builder("idempotent.cache.hit.rate", 
            this, metrics -> calculateHitRate())
            .description("缓存命中率")
            .register(meterRegistry);
        
        log.info("Prometheus监控初始化完成");
    }
    
    public void recordRequest() {
        totalRequests.increment();
    }
    
    public void recordSuccess() {
        successRequests.increment();
    }
    
    public void recordDuplicate() {
        duplicateRequests.increment();
    }
    
    public void recordCacheReturned() {
        cacheReturnedRequests.increment();
        cacheHits.incrementAndGet();
    }
    
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }
    
    public void recordTokenInvalid() {
        tokenInvalidRequests.increment();
    }
    
    public void recordFailed() {
        failedRequests.increment();
    }
    
    public void recordExecutionTime(long elapsedMillis) {
        executionTimer.record(elapsedMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    public void recordLockAcquireTime(long elapsedMillis) {
        lockAcquireTimer.record(elapsedMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    public void recordCacheLookupTime(long elapsedMillis) {
        cacheLookupTimer.record(elapsedMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    private double calculateHitRate() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total == 0 ? 0.0 : (double) hits / total;
    }
}