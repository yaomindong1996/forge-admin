package com.mdframe.forge.plugin.job.manager;

import cn.hutool.core.bean.BeanUtil;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.model.JobConfig;
import com.mdframe.forge.plugin.job.scheduler.JobScheduleException;
import com.mdframe.forge.plugin.job.scheduler.JobScheduler;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据库任务期望状态与 Quartz 运行态协调器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduleCoordinator {

    public static final String SYNC_PENDING = "PENDING";
    public static final String SYNCED = "SYNCED";
    public static final String SYNC_FAILED = "FAILED";
    public static final String DELETE_PENDING = "DELETE_PENDING";

    private static final int MAX_SYNC_ERROR_LENGTH = 1000;
    private static final int MAX_CONVERGENCE_ATTEMPTS = 10;

    private final SysJobConfigMapper jobConfigMapper;
    private final JobScheduler jobScheduler;
    private final JobScheduleSynchronizationLockManager synchronizationLockManager;

    public void synchronize(Long jobConfigId) {
        SysJobConfig initial = findConfig(jobConfigId);
        if (initial == null) {
            return;
        }
        try (JobScheduleSynchronizationLockManager.LockHandle ignored =
                     synchronizationLockManager.acquire(initial.getJobName(), initial.getJobGroup())) {
            synchronizeUnderLock(jobConfigId, initial.getJobName(), initial.getJobGroup());
        }
    }

    public void retrySynchronization(Long jobConfigId) {
        SysJobConfig jobConfig = requireConfig(jobConfigId);
        if (!DELETE_PENDING.equals(jobConfig.getSyncStatus())) {
            jobConfigMapper.updateSyncState(jobConfigId, jobConfig.getVersion(),
                    SYNC_PENDING, null, null);
        }
        synchronize(jobConfigId);
    }

    public void reconcileOnStartup() {
        List<SysJobConfig> candidates = jobConfigMapper.selectRecoveryCandidates();
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        for (SysJobConfig candidate : candidates) {
            try {
                synchronize(candidate.getId());
            } catch (RuntimeException exception) {
                log.error("启动恢复任务失败: jobKey={}{}{}, exceptionType={}",
                        candidate.getJobGroup(), '.', candidate.getJobName(),
                        exception.getClass().getSimpleName());
            }
        }
    }

    private void synchronizeUnderLock(Long jobConfigId, String jobName, String jobGroup) {
        for (int attempt = 1; attempt <= MAX_CONVERGENCE_ATTEMPTS; attempt++) {
            SysJobConfig jobConfig = findConfig(jobConfigId);
            if (jobConfig == null) {
                jobScheduler.deleteJob(jobName, jobGroup);
                return;
            }
            Integer expectedVersion = jobConfig.getVersion();
            try {
                if (DELETE_PENDING.equals(jobConfig.getSyncStatus())) {
                    jobScheduler.deleteJob(jobConfig.getJobName(), jobConfig.getJobGroup());
                    if (jobConfigMapper.logicalDeleteByVersion(jobConfigId, expectedVersion) > 0) {
                        return;
                    }
                    continue;
                }

                JobScheduler.SynchronizeResult result = jobScheduler.synchronize(toJobConfig(jobConfig));
                LocalDateTime syncTime = LocalDateTime.now();
                int updated = result == JobScheduler.SynchronizeResult.ONCE_MISSED
                        ? jobConfigMapper.markOnceMissedCompleted(
                                jobConfigId, jobConfig.getFireOnceTime(), jobConfig.getTimezone(),
                                expectedVersion, syncTime)
                        : jobConfigMapper.updateSyncState(
                                jobConfigId, expectedVersion, SYNCED, null, syncTime);
                if (updated > 0) {
                    return;
                }
            } catch (JobScheduleException exception) {
                String failureStatus = DELETE_PENDING.equals(jobConfig.getSyncStatus())
                        ? DELETE_PENDING : SYNC_FAILED;
                int updated = jobConfigMapper.updateSyncState(
                        jobConfigId, expectedVersion, failureStatus,
                        resolveError(exception), LocalDateTime.now());
                if (updated > 0) {
                    throw exception;
                }
            }
            log.info("任务配置同步期间版本变化，重新收敛: jobConfigId={}, attempt={}",
                    jobConfigId, attempt);
        }
        throw new JobScheduleException("任务配置持续变化，无法完成调度同步: " + jobConfigId);
    }

    private SysJobConfig findConfig(Long jobConfigId) {
        if (jobConfigId == null) {
            throw new BusinessException("任务ID不能为空");
        }
        return jobConfigMapper.selectById(jobConfigId);
    }

    private SysJobConfig requireConfig(Long jobConfigId) {
        SysJobConfig jobConfig = findConfig(jobConfigId);
        if (jobConfig == null) {
            throw new BusinessException("定时任务不存在");
        }
        return jobConfig;
    }

    private JobConfig toJobConfig(SysJobConfig entity) {
        JobConfig config = new JobConfig();
        BeanUtil.copyProperties(entity, config);
        return config;
    }

    private String resolveError(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = StringUtils.defaultIfBlank(current.getMessage(), current.getClass().getSimpleName());
        return StringUtils.abbreviate(message, MAX_SYNC_ERROR_LENGTH);
    }
}
