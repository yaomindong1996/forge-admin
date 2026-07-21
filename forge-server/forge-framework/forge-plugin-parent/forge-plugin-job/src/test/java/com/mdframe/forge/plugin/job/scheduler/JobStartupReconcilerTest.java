package com.mdframe.forge.plugin.job.scheduler;

import com.mdframe.forge.plugin.job.manager.JobScheduleCoordinator;
import com.mdframe.forge.plugin.job.service.JobExecutionRecoveryService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobStartupReconcilerTest {

    @Test
    void shouldRecoverStaleExecutionsBeforeSynchronizingSchedules() {
        JobExecutionRecoveryService recoveryService = mock(JobExecutionRecoveryService.class);
        JobScheduleCoordinator coordinator = mock(JobScheduleCoordinator.class);
        JobStartupReconciler reconciler = new JobStartupReconciler(coordinator, recoveryService);

        reconciler.run(null);

        var ordered = inOrder(recoveryService, coordinator);
        ordered.verify(recoveryService).recoverStaleExecutions();
        ordered.verify(coordinator).reconcileOnStartup();
    }

    @Test
    void shouldStillSynchronizeSchedulesWhenExecutionRecoveryFails() {
        JobExecutionRecoveryService recoveryService = mock(JobExecutionRecoveryService.class);
        JobScheduleCoordinator coordinator = mock(JobScheduleCoordinator.class);
        when(recoveryService.recoverStaleExecutions()).thenThrow(new IllegalStateException("db unavailable"));
        JobStartupReconciler reconciler = new JobStartupReconciler(coordinator, recoveryService);

        reconciler.run(null);

        verify(coordinator).reconcileOnStartup();
    }
}
