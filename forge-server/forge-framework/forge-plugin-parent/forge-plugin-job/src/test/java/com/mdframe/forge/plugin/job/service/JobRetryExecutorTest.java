package com.mdframe.forge.plugin.job.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobRetryExecutorTest {

    @Test
    void shouldReturnFirstSuccessfulAttemptWithoutDelay() {
        List<Long> delays = new ArrayList<>();
        JobRetryExecutor executor = new JobRetryExecutor(delays::add);

        JobRetryExecutor.RetryResult result = executor.execute(3, true, () -> "SUCCESS");

        assertTrue(result.success());
        assertEquals("SUCCESS", result.result());
        assertEquals(0, result.retryCount());
        assertTrue(delays.isEmpty());
    }

    @Test
    void shouldRetryWithFixedDelayAndReportActualRetryCount() {
        List<Long> delays = new ArrayList<>();
        AtomicInteger attempts = new AtomicInteger();
        JobRetryExecutor executor = new JobRetryExecutor(delays::add);

        JobRetryExecutor.RetryResult result = executor.execute(3, true, () -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IllegalStateException("temporary");
            }
            return "RECOVERED";
        });

        assertTrue(result.success());
        assertEquals("RECOVERED", result.result());
        assertEquals(2, result.retryCount());
        assertEquals(List.of(1000L, 1000L), delays);
    }

    @Test
    void shouldStopAfterBoundedRetriesAndReturnFinalError() {
        List<Long> delays = new ArrayList<>();
        AtomicInteger attempts = new AtomicInteger();
        JobRetryExecutor executor = new JobRetryExecutor(delays::add);

        JobRetryExecutor.RetryResult result = executor.execute(2, true, () -> {
            throw new IllegalStateException("failure-" + attempts.incrementAndGet());
        });

        assertFalse(result.success());
        assertEquals(3, attempts.get());
        assertEquals(2, result.retryCount());
        assertEquals("failure-3", result.error().getMessage());
        assertEquals(List.of(1000L, 1000L), delays);
    }

    @Test
    void shouldNeverRetryTaskWithoutExplicitIdempotency() {
        AtomicInteger attempts = new AtomicInteger();
        List<Long> delays = new ArrayList<>();
        JobRetryExecutor executor = new JobRetryExecutor(delays::add);

        JobRetryExecutor.RetryResult result = executor.execute(5, false, () -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("not-idempotent");
        });

        assertFalse(result.success());
        assertEquals(1, attempts.get());
        assertEquals(0, result.retryCount());
        assertTrue(delays.isEmpty());
    }
}
