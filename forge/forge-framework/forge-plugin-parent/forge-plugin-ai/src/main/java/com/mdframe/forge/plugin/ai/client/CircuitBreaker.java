package com.mdframe.forge.plugin.ai.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class CircuitBreaker {

    private static final int FAILURE_THRESHOLD = 3;
    private static final long RECOVERY_MILLIS = 5 * 60 * 1000L;

    private final ConcurrentHashMap<String, AtomicInteger> failureCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> openTime = new ConcurrentHashMap<>();

    public boolean isOpen(String key) {
        Long openAt = openTime.get(key);
        if (openAt == null) {
            return false;
        }
        if (System.currentTimeMillis() - openAt >= RECOVERY_MILLIS) {
            reset(key);
            log.info("[CircuitBreaker] 熔断恢复, key={}", key);
            return false;
        }
        return true;
    }

    public void recordFailure(String key) {
        AtomicInteger count = failureCount.computeIfAbsent(key, k -> new AtomicInteger(0));
        int current = count.incrementAndGet();
        if (current >= FAILURE_THRESHOLD) {
            openTime.put(key, System.currentTimeMillis());
            log.warn("[CircuitBreaker] 熔断触发, key={}, failures={}", key, current);
        }
    }

    public void recordSuccess(String key) {
        reset(key);
    }

    private void reset(String key) {
        failureCount.remove(key);
        openTime.remove(key);
    }
}
