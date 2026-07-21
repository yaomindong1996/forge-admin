package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.manager.JobApiIdempotencyManager;
import com.mdframe.forge.plugin.job.manager.JobScheduleCoordinator;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import com.mdframe.forge.plugin.job.model.JobApiPrincipal;
import com.mdframe.forge.plugin.job.model.JobApiTriggerTarget;
import com.mdframe.forge.plugin.job.scheduler.JobScheduler;
import com.mdframe.forge.plugin.job.support.JobOpenApiException;
import com.mdframe.forge.plugin.job.vo.JobOpenApiExecutionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobApiExecutionService {

    private final JobApiIdempotencyManager idempotencyManager;
    private final JobScheduler jobScheduler;
    private final JobExecutionLifecycleService lifecycleService;
    private final SysJobLogMapper jobLogMapper;

    public JobOpenApiExecutionVO trigger(
            JobApiPrincipal principal,
            JobApiTriggerTarget target,
            String idempotencyKey) {
        if (!Integer.valueOf(1).equals(target.getStatus())
                || !JobScheduleCoordinator.SYNCED.equals(target.getSyncStatus())) {
            throw JobOpenApiException.conflict("job_not_triggerable");
        }
        JobApiReservationService.Reservation reservation = idempotencyManager.reserve(
                principal, target, idempotencyKey);
        if (!reservation.reused()) {
            submit(target, reservation.executionId());
        }
        JobOpenApiExecutionVO execution = jobLogMapper.selectOpenExecutionById(
                reservation.executionId(), principal.jobIds(), principal.jobGroups());
        if (execution == null) {
            throw JobOpenApiException.unavailable();
        }
        return execution;
    }

    private void submit(JobApiTriggerTarget target, Long executionId) {
        try {
            jobScheduler.triggerJob(
                    target.getJobName(), target.getJobGroup(), target.getId(), "OPEN_API", executionId);
        } catch (RuntimeException exception) {
            lifecycleService.failAccepted(executionId, "调度提交失败");
            throw JobOpenApiException.unavailable();
        }
    }
}
