package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.constant.JobExecutionStatus;
import com.mdframe.forge.plugin.job.entity.SysJobLog;
import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import com.mdframe.forge.plugin.job.model.JobApiTriggerTarget;
import com.mdframe.forge.plugin.job.support.JobLogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 管理单次任务执行日志从接受到终态的生命周期。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionLifecycleService {

    private final SysJobLogMapper jobLogMapper;
    private final SysJobConfigMapper jobConfigMapper;
    private final JobLogSanitizer logSanitizer;
    private final JobFailureAlarmService failureAlarmService;

    public Long accept(JobExecutionContext context, Long jobConfigId) {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        Long reservedExecutionId = getLong(jobDataMap, "reservedExecutionId");
        if (reservedExecutionId != null) {
            int updated = jobLogMapper.startReservedExecution(
                    reservedExecutionId,
                    toLocalDateTime(context.getScheduledFireTime()),
                    context.getFireInstanceId(),
                    LocalDateTime.now(),
                    jobDataMap.getString("executorHandler"),
                    logSanitizer.sanitizeJobParam(jobDataMap.getString("jobParam")));
            if (updated == 0) {
                throw new IllegalStateException("预留任务执行记录不存在或状态已变化");
            }
            return reservedExecutionId;
        }
        SysJobLog execution = new SysJobLog();
        execution.setJobConfigId(jobConfigId);
        execution.setJobName(context.getJobDetail().getKey().getName());
        execution.setJobGroup(context.getJobDetail().getKey().getGroup());
        execution.setExecutorHandler(jobDataMap.getString("executorHandler"));
        execution.setJobParam(logSanitizer.sanitizeJobParam(jobDataMap.getString("jobParam")));
        execution.setTriggerTime(toLocalDateTime(context.getFireTime()));
        execution.setTriggerType(getStringOrDefault(jobDataMap, "triggerType", "SCHEDULED"));
        execution.setScheduledFireTime(toLocalDateTime(context.getScheduledFireTime()));
        execution.setFireInstanceId(context.getFireInstanceId());
        execution.setStartTime(LocalDateTime.now());
        execution.setHeartbeatTime(execution.getStartTime());
        execution.setStatus(JobExecutionStatus.RUNNING);
        execution.setRetryCount(0);
        execution.setDelFlag(0);
        if (jobLogMapper.insert(execution) <= 0 || execution.getId() == null) {
            throw new IllegalStateException("创建任务执行记录失败");
        }
        return execution.getId();
    }

    public Long reserveOpenApi(JobApiTriggerTarget target, LocalDateTime acceptedAt) {
        SysJobLog execution = new SysJobLog();
        execution.setJobConfigId(target.getId());
        execution.setJobName(target.getJobName());
        execution.setJobGroup(target.getJobGroup());
        execution.setTriggerTime(acceptedAt);
        execution.setTriggerType("OPEN_API");
        execution.setStatus(JobExecutionStatus.ACCEPTED);
        execution.setRetryCount(0);
        execution.setDelFlag(0);
        if (jobLogMapper.insert(execution) <= 0 || execution.getId() == null) {
            throw new IllegalStateException("预留任务执行记录失败");
        }
        return execution.getId();
    }

    public void failAccepted(Long executionId, String reason) {
        String safeReason = logSanitizer.sanitizeException(reason);
        if (jobLogMapper.failAcceptedExecution(executionId, safeReason) == 0) {
            log.warn("预留任务执行记录失败态更新被忽略: executionId={}", executionId);
        }
    }

    public void markSuccess(Long executionId, String result, int retryCount) {
        complete(executionId, JobExecutionStatus.SUCCESS,
                logSanitizer.sanitizeResult(result), null, retryCount);
    }

    public void markFlowSuccess(Long executionId, String processInstanceId, int retryCount) {
        if (processInstanceId == null || processInstanceId.isBlank()) {
            throw new IllegalArgumentException("流程实例ID不能为空");
        }
        int updated = jobLogMapper.completeRunningFlowExecution(
                executionId, JobExecutionStatus.SUCCESS, "流程实例已启动",
                processInstanceId, retryCount);
        afterCompletion(executionId, JobExecutionStatus.SUCCESS, updated);
    }

    public void markFailed(Long executionId, Throwable error, int retryCount) {
        String exceptionMessage = error == null ? "未知执行异常"
                : logSanitizer.sanitizeException(getStackTrace(error));
        complete(executionId, JobExecutionStatus.FAILED, null, exceptionMessage, retryCount);
    }

    public void markSkipped(Long executionId, String reason) {
        complete(executionId, JobExecutionStatus.SKIPPED,
                logSanitizer.sanitizeResult(reason), null, 0);
    }

    private void complete(Long executionId, int status, String result,
                          String exceptionMessage, int retryCount) {
        int updated = jobLogMapper.completeRunningExecution(
                executionId, status, result, exceptionMessage, retryCount);
        afterCompletion(executionId, status, updated);
    }

    private void afterCompletion(Long executionId, int status, int updated) {
        if (updated == 0) {
            log.warn("任务执行记录终态更新被忽略: executionId={}, targetStatus={}", executionId, status);
            return;
        }
        if (status == JobExecutionStatus.SKIPPED) {
            return;
        }
        int counterUpdated = jobConfigMapper.applyExecutionOutcome(executionId, status);
        if (counterUpdated == 0) {
            log.debug("较旧执行结果未推进连续失败状态: executionId={}, status={}", executionId, status);
        }
        if (status == JobExecutionStatus.FAILED) {
            failureAlarmService.notifyFinalFailure(executionId);
        }
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private String getStringOrDefault(JobDataMap jobDataMap, String key, String defaultValue) {
        String value = jobDataMap.getString(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private Long getLong(JobDataMap jobDataMap, String key) {
        Object value = jobDataMap.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String getStackTrace(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
