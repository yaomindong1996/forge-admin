package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.entity.SysJobApiIdempotency;
import com.mdframe.forge.plugin.job.mapper.SysJobApiIdempotencyMapper;
import com.mdframe.forge.plugin.job.model.JobApiPrincipal;
import com.mdframe.forge.plugin.job.model.JobApiTriggerTarget;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobApiReservationServiceTest {

    @Test
    void shouldReuseEffectiveReservationWithoutCreatingExecution() {
        SysJobApiIdempotencyMapper mapper = mock(SysJobApiIdempotencyMapper.class);
        SysJobApiIdempotency existing = new SysJobApiIdempotency();
        existing.setExecutionId(41L);
        when(mapper.selectEffective(any(), any(), any(), any(), any())).thenReturn(existing);
        JobExecutionLifecycleService lifecycle = mock(JobExecutionLifecycleService.class);
        JobApiReservationService service = new JobApiReservationService(
                mapper, lifecycle, new JobProperties());

        JobApiReservationService.Reservation result = service.reserve(
                principal(), target(), "a".repeat(64), LocalDateTime.now());

        assertEquals(41L, result.executionId());
        assertTrue(result.reused());
        verify(lifecycle, never()).reserveOpenApi(any(), any());
    }

    @Test
    void shouldCreateExecutionAndHashOnlyIdempotencyRow() {
        SysJobApiIdempotencyMapper mapper = mock(SysJobApiIdempotencyMapper.class);
        when(mapper.insertReservation(any())).thenReturn(1);
        JobExecutionLifecycleService lifecycle = mock(JobExecutionLifecycleService.class);
        when(lifecycle.reserveOpenApi(any(), any())).thenReturn(42L);
        JobApiReservationService service = new JobApiReservationService(
                mapper, lifecycle, new JobProperties());
        String keyHash = "b".repeat(64);

        JobApiReservationService.Reservation result = service.reserve(
                principal(), target(), keyHash, LocalDateTime.now());

        assertEquals(42L, result.executionId());
        assertFalse(result.reused());
        verify(mapper).insertReservation(org.mockito.ArgumentMatchers.argThat(entity ->
                keyHash.equals(entity.getIdempotencyKeyHash())
                        && entity.getExecutionId().equals(42L)
                        && entity.getExpiresAt() != null));
    }

    private JobApiPrincipal principal() {
        return new JobApiPrincipal(1L, 1L, "trusted-key", "caller",
                Set.of("jobs:trigger"), Set.of(7L), Set.of());
    }

    private JobApiTriggerTarget target() {
        JobApiTriggerTarget target = new JobApiTriggerTarget();
        target.setId(7L);
        target.setJobName("sampleJob");
        target.setJobGroup("DEFAULT");
        return target;
    }
}
