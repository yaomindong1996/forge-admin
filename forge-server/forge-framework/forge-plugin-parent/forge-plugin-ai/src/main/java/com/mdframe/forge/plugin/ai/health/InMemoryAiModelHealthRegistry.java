package com.mdframe.forge.plugin.ai.health;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class InMemoryAiModelHealthRegistry implements AiModelHealthRegistry {
    private static final int FAILURE_THRESHOLD = 3;
    private static final Duration RECOVERY_WINDOW = Duration.ofMinutes(5);
    private final ConcurrentHashMap<AiModelHealthKey, State> states = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryAiModelHealthRegistry() { this(Clock.systemUTC()); }
    InMemoryAiModelHealthRegistry(Clock clock) { this.clock = clock; }

    @Override public AiModelHealthSnapshot snapshot(AiModelHealthKey key) {
        State state = states.computeIfAbsent(key, ignored -> new State());
        synchronized (state) { promoteHalfOpen(state); return state.snapshot(); }
    }

    @Override public Optional<AiModelHealthLease> tryAcquire(AiModelHealthKey key) {
        State state = states.computeIfAbsent(key, ignored -> new State());
        synchronized (state) {
            promoteHalfOpen(state);
            if (state.status == AiModelHealthStatus.OPEN || (state.status == AiModelHealthStatus.HALF_OPEN && state.probeInProgress)) return Optional.empty();
            if (state.status == AiModelHealthStatus.HALF_OPEN) state.probeInProgress = true;
            return Optional.of(new Lease(key, state));
        }
    }

    @Override public AiModelHealthLease acquireManualProbe(AiModelHealthKey key) {
        State state = states.computeIfAbsent(key, ignored -> new State());
        synchronized (state) {
            if (state.probeInProgress) throw new IllegalStateException("模型已有健康试探正在执行");
            state.probeInProgress = true;
            return new Lease(key, state);
        }
    }

    @Override public void reset(AiModelHealthKey key) { states.remove(key); }
    @Override public void resetProvider(Long tenantId, Long providerPk) { states.keySet().removeIf(k -> k.tenantId().equals(tenantId) && k.providerPk().equals(providerPk)); }

    private void promoteHalfOpen(State state) {
        if (state.status == AiModelHealthStatus.OPEN && state.openedAt != null && !clock.instant().isBefore(state.openedAt.plus(RECOVERY_WINDOW))) state.status = AiModelHealthStatus.HALF_OPEN;
    }

    private final class Lease implements AiModelHealthLease {
        private final AiModelHealthKey key;
        private final State state;
        private final AtomicBoolean ended = new AtomicBoolean();
        private Lease(AiModelHealthKey key, State state) { this.key = key; this.state = state; }
        @Override public AiModelHealthKey key() { return key; }
        @Override public void success() { finish(() -> { state.status = AiModelHealthStatus.HEALTHY; state.failureCount = 0; state.openedAt = null; }); }
        @Override public void failure(AiModelFailureCategory category) { finish(() -> { state.failureCount++; if (state.failureCount >= FAILURE_THRESHOLD || state.status == AiModelHealthStatus.HALF_OPEN) { state.status = AiModelHealthStatus.OPEN; state.openedAt = clock.instant(); } else state.status = AiModelHealthStatus.DEGRADED; }); }
        @Override public void cancel() { finish(() -> { }); }
        @Override public void abort() { finish(() -> { }); }
        private void finish(Runnable action) { if (!ended.compareAndSet(false, true)) return; synchronized (state) { action.run(); state.probeInProgress = false; } }
    }

    private static final class State {
        private AiModelHealthStatus status = AiModelHealthStatus.UNKNOWN;
        private int failureCount;
        private Instant openedAt;
        private boolean probeInProgress;
        private AiModelHealthSnapshot snapshot() { return new AiModelHealthSnapshot(status, failureCount, openedAt, probeInProgress); }
    }
}
