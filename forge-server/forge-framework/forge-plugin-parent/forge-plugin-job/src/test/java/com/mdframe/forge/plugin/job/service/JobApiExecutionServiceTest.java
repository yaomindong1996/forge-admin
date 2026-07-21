package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.manager.JobApiIdempotencyManager;
import com.mdframe.forge.plugin.job.manager.JobScheduleCoordinator;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import com.mdframe.forge.plugin.job.model.JobApiPrincipal;
import com.mdframe.forge.plugin.job.model.JobApiTriggerTarget;
import com.mdframe.forge.plugin.job.scheduler.JobScheduler;
import com.mdframe.forge.plugin.job.support.JobOpenApiException;
import com.mdframe.forge.plugin.job.vo.JobOpenApiExecutionVO;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JobApiExecutionServiceTest {

    @Test
    void shouldSubmitNewReservationAndReturnSafeExecutionProjection() {
        Fixture fixture = fixture();
        JobOpenApiExecutionVO expected = execution(88L);
        when(fixture.idempotencyManager.reserve(fixture.principal, fixture.target, "request-key-001"))
                .thenReturn(new JobApiReservationService.Reservation(88L, false));
        when(fixture.jobLogMapper.selectOpenExecutionById(
                88L, fixture.principal.jobIds(), fixture.principal.jobGroups())).thenReturn(expected);

        JobOpenApiExecutionVO result = fixture.service.trigger(
                fixture.principal, fixture.target, "request-key-001");

        assertSame(expected, result);
        verify(fixture.jobScheduler).triggerJob(
                "sampleJob", "OPS", 7L, "OPEN_API", 88L);
    }

    @Test
    void shouldReuseExistingReservationWithoutSubmittingQuartzAgain() {
        Fixture fixture = fixture();
        JobOpenApiExecutionVO expected = execution(88L);
        when(fixture.idempotencyManager.reserve(fixture.principal, fixture.target, "request-key-001"))
                .thenReturn(new JobApiReservationService.Reservation(88L, true));
        when(fixture.jobLogMapper.selectOpenExecutionById(
                88L, fixture.principal.jobIds(), fixture.principal.jobGroups())).thenReturn(expected);

        assertSame(expected, fixture.service.trigger(
                fixture.principal, fixture.target, "request-key-001"));

        verifyNoInteractions(fixture.jobScheduler);
    }

    @Test
    void shouldFailAcceptedExecutionWhenQuartzSubmissionFails() {
        Fixture fixture = fixture();
        when(fixture.idempotencyManager.reserve(fixture.principal, fixture.target, "request-key-001"))
                .thenReturn(new JobApiReservationService.Reservation(88L, false));
        doThrow(new IllegalStateException("scheduler unavailable"))
                .when(fixture.jobScheduler)
                .triggerJob("sampleJob", "OPS", 7L, "OPEN_API", 88L);

        JobOpenApiException exception = assertThrows(JobOpenApiException.class,
                () -> fixture.service.trigger(fixture.principal, fixture.target, "request-key-001"));

        assertEquals(503, exception.getStatus());
        verify(fixture.lifecycleService).failAccepted(88L, "调度提交失败");
    }

    @Test
    void shouldRejectStoppedOrUnsynchronizedJobBeforeReservation() {
        Fixture fixture = fixture();
        fixture.target.setSyncStatus(JobScheduleCoordinator.SYNC_FAILED);

        JobOpenApiException exception = assertThrows(JobOpenApiException.class,
                () -> fixture.service.trigger(fixture.principal, fixture.target, "request-key-001"));

        assertEquals(409, exception.getStatus());
        verifyNoInteractions(fixture.idempotencyManager, fixture.jobScheduler, fixture.lifecycleService,
                fixture.jobLogMapper);
    }

    @Test
    void shouldFailClosedWhenReservedExecutionCannotBeReadBack() {
        Fixture fixture = fixture();
        when(fixture.idempotencyManager.reserve(fixture.principal, fixture.target, "request-key-001"))
                .thenReturn(new JobApiReservationService.Reservation(88L, true));

        JobOpenApiException exception = assertThrows(JobOpenApiException.class,
                () -> fixture.service.trigger(fixture.principal, fixture.target, "request-key-001"));

        assertEquals(503, exception.getStatus());
        verify(fixture.jobScheduler, never()).triggerJob(
                "sampleJob", "OPS", 7L, "OPEN_API", 88L);
    }

    private Fixture fixture() {
        JobApiIdempotencyManager idempotencyManager = mock(JobApiIdempotencyManager.class);
        JobScheduler jobScheduler = mock(JobScheduler.class);
        JobExecutionLifecycleService lifecycleService = mock(JobExecutionLifecycleService.class);
        SysJobLogMapper jobLogMapper = mock(SysJobLogMapper.class);
        JobApiExecutionService service = new JobApiExecutionService(
                idempotencyManager, jobScheduler, lifecycleService, jobLogMapper);
        JobApiPrincipal principal = new JobApiPrincipal(
                1L, 1L, "trusted-key", "caller",
                Set.of("jobs:trigger"), Set.of(7L), Set.of("OPS"));
        JobApiTriggerTarget target = new JobApiTriggerTarget();
        target.setId(7L);
        target.setJobName("sampleJob");
        target.setJobGroup("OPS");
        target.setStatus(1);
        target.setSyncStatus(JobScheduleCoordinator.SYNCED);
        return new Fixture(service, idempotencyManager, jobScheduler, lifecycleService,
                jobLogMapper, principal, target);
    }

    private JobOpenApiExecutionVO execution(Long id) {
        JobOpenApiExecutionVO execution = new JobOpenApiExecutionVO();
        execution.setId(id);
        execution.setJobId(7L);
        execution.setStatus(4);
        return execution;
    }

    private record Fixture(
            JobApiExecutionService service,
            JobApiIdempotencyManager idempotencyManager,
            JobScheduler jobScheduler,
            JobExecutionLifecycleService lifecycleService,
            SysJobLogMapper jobLogMapper,
            JobApiPrincipal principal,
            JobApiTriggerTarget target) {
    }
}
