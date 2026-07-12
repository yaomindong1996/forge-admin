package com.mdframe.forge.plugin.ai.health;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryAiModelHealthRegistryTest {
    private static final AiModelHealthKey KEY = new AiModelHealthKey(1L, 2L, 3L);
    @Test void shouldOpenAfterThreeFailuresAndRecoverWithSingleProbe() {
        MutableClock clock=new MutableClock(); InMemoryAiModelHealthRegistry registry=new InMemoryAiModelHealthRegistry(clock);
        for(int i=0;i<3;i++) registry.tryAcquire(KEY).orElseThrow().failure(AiModelFailureCategory.NETWORK);
        assertEquals(AiModelHealthStatus.OPEN, registry.snapshot(KEY).status()); assertTrue(registry.tryAcquire(KEY).isEmpty());
        clock.advance(Duration.ofMinutes(5)); assertEquals(AiModelHealthStatus.HALF_OPEN, registry.snapshot(KEY).status());
        AiModelHealthLease lease=registry.tryAcquire(KEY).orElseThrow(); assertTrue(registry.tryAcquire(KEY).isEmpty()); lease.success();
        assertEquals(AiModelHealthStatus.HEALTHY, registry.snapshot(KEY).status());
    }
    @Test void closeShouldAbortHalfOpenProbeWithoutFailure() {
        MutableClock clock=new MutableClock(); InMemoryAiModelHealthRegistry registry=new InMemoryAiModelHealthRegistry(clock);
        for(int i=0;i<3;i++) registry.tryAcquire(KEY).orElseThrow().failure(AiModelFailureCategory.NETWORK);
        clock.advance(Duration.ofMinutes(5)); AiModelHealthLease lease=registry.tryAcquire(KEY).orElseThrow(); lease.close();
        assertEquals(3, registry.snapshot(KEY).failureCount()); assertTrue(registry.tryAcquire(KEY).isPresent());
    }
    @Test void cancelShouldNotIncreaseFailureCount() {
        InMemoryAiModelHealthRegistry registry=new InMemoryAiModelHealthRegistry(new MutableClock());
        registry.tryAcquire(KEY).orElseThrow().cancel();
        assertEquals(0, registry.snapshot(KEY).failureCount());
        assertEquals(AiModelHealthStatus.UNKNOWN, registry.snapshot(KEY).status());
    }
    @Test void failedHalfOpenProbeShouldReopenImmediately() {
        MutableClock clock=new MutableClock(); InMemoryAiModelHealthRegistry registry=new InMemoryAiModelHealthRegistry(clock);
        for(int i=0;i<3;i++) registry.tryAcquire(KEY).orElseThrow().failure(AiModelFailureCategory.NETWORK);
        clock.advance(Duration.ofMinutes(5)); registry.tryAcquire(KEY).orElseThrow().failure(AiModelFailureCategory.NETWORK);
        assertEquals(AiModelHealthStatus.OPEN, registry.snapshot(KEY).status());
        assertTrue(registry.tryAcquire(KEY).isEmpty());
    }
    @Test void resetProviderShouldOnlyRemoveMatchingTenantAndProvider() {
        InMemoryAiModelHealthRegistry registry=new InMemoryAiModelHealthRegistry(new MutableClock());
        AiModelHealthKey sameProvider=new AiModelHealthKey(1L,2L,4L); AiModelHealthKey otherTenant=new AiModelHealthKey(2L,2L,3L);
        registry.tryAcquire(KEY).orElseThrow().failure(AiModelFailureCategory.NETWORK);
        registry.tryAcquire(sameProvider).orElseThrow().failure(AiModelFailureCategory.NETWORK);
        registry.tryAcquire(otherTenant).orElseThrow().failure(AiModelFailureCategory.NETWORK);
        registry.resetProvider(1L,2L);
        assertEquals(0,registry.snapshot(KEY).failureCount()); assertEquals(0,registry.snapshot(sameProvider).failureCount()); assertEquals(1,registry.snapshot(otherTenant).failureCount());
    }
    private static final class MutableClock extends Clock { private Instant now=Instant.parse("2026-01-01T00:00:00Z"); void advance(Duration d){now=now.plus(d);} public ZoneId getZone(){return ZoneOffset.UTC;} public Clock withZone(ZoneId z){return this;} public Instant instant(){return now;} }
}
