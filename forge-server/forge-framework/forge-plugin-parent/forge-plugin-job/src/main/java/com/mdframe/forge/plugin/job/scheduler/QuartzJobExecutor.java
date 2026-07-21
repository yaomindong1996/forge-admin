package com.mdframe.forge.plugin.job.scheduler;

import cn.hutool.extra.spring.SpringUtil;
import com.mdframe.forge.plugin.job.constant.JobConcurrentPolicy;
import com.mdframe.forge.plugin.job.constant.JobInvokeMode;
import com.mdframe.forge.plugin.job.constant.JobScheduleType;
import com.mdframe.forge.plugin.job.executor.JobExecutorRouterManager;
import com.mdframe.forge.plugin.job.manager.JobExecutionLockManager;
import com.mdframe.forge.plugin.job.service.JobExecutionLifecycleService;
import com.mdframe.forge.plugin.job.service.JobExecutionHeartbeatService;
import com.mdframe.forge.plugin.job.service.JobFlowOrchestrationService;
import com.mdframe.forge.plugin.job.service.JobOnceCompletionService;
import com.mdframe.forge.plugin.job.service.JobRetryExecutor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.TriggerKey;

import java.util.Date;

/**
 * Quartz任务执行入口
 * 负责路由任务到合适的执行器（本地/远程）并记录日志
 */
@Slf4j
public class QuartzJobExecutor implements Job {
    
    @Override
    public void execute(JobExecutionContext context) {
        String jobName = context.getJobDetail().getKey().getName();
        String jobGroup = context.getJobDetail().getKey().getGroup();
        
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        Long jobConfigId = getLong(jobDataMap, "jobConfigId");
        String executorHandler = jobDataMap.getString("executorHandler");
        String executorBean = jobDataMap.getString("executorBean");
        String executorMethod = jobDataMap.getString("executorMethod");
        String executorService = jobDataMap.getString("executorService");
        String jobParam = jobDataMap.getString("jobParam");
        String executeMode = jobDataMap.getString("executeMode");
        
        try {
            JobExecutionLifecycleService lifecycleService = SpringUtil.getBean(JobExecutionLifecycleService.class);
            Long executionId = lifecycleService.accept(context, jobConfigId);
            JobExecutionHeartbeatService heartbeatService =
                    SpringUtil.getBean(JobExecutionHeartbeatService.class);
            try (JobExecutionHeartbeatService.HeartbeatHandle ignored = heartbeatService.start(executionId)) {
                executeAccepted(jobDataMap, executionId, jobConfigId, jobName, jobGroup,
                        executorHandler, executorBean, executorMethod, executorService,
                        jobParam, executeMode, lifecycleService);
            }
        } catch (RuntimeException exception) {
            log.error("任务执行入口失败: {}.{}, exceptionType={}",
                    jobGroup, jobName, exception.getClass().getName());
        } finally {
            completePlannedOnce(context, jobDataMap, jobConfigId, jobName, jobGroup);
        }
    }

    private void executeAccepted(JobDataMap jobDataMap, Long executionId, Long jobConfigId,
                                 String jobName, String jobGroup, String executorHandler,
                                 String executorBean, String executorMethod, String executorService,
                                 String jobParam, String executeMode,
                                 JobExecutionLifecycleService lifecycleService) {
        JobExecutionLockManager.LockHandle lockHandle = null;
        try {
            if (JobConcurrentPolicy.SKIP_IF_RUNNING.equals(
                    getStringOrDefault(jobDataMap, "concurrentPolicy", JobConcurrentPolicy.DEFAULT))) {
                JobExecutionLockManager lockManager = SpringUtil.getBean(JobExecutionLockManager.class);
                lockHandle = lockManager.tryAcquire(jobConfigId);
                if (!lockHandle.isAcquired()) {
                    lifecycleService.markSkipped(executionId, lockHandle.getReason());
                    log.info("任务执行已跳过: {}.{}, reason={}", jobGroup, jobName, lockHandle.getReason());
                    return;
                }
            }

            log.info("开始执行任务: {}.{}, 模式: {}, Handler: {}",
                    jobGroup, jobName, executeMode, executorHandler);
            JobRetryExecutor retryExecutor = SpringUtil.getBean(JobRetryExecutor.class);
            int configuredRetryCount = getIntValue(jobDataMap, "retryCount", 0);
            boolean idempotent = getIntValue(jobDataMap, "idempotentFlag", 0) == 1;
            String invokeMode = getStringOrDefault(
                    jobDataMap, "invokeMode", JobInvokeMode.SINGLE);
            JobRetryExecutor.RetryResult retryResult = retryExecutor.execute(
                    configuredRetryCount, idempotent,
                    () -> executeByInvokeMode(invokeMode, jobDataMap, executionId, jobConfigId,
                            executorHandler, executorBean, executorMethod,
                            executorService, jobParam, executeMode));
            if (retryResult.success()) {
                if (JobInvokeMode.FLOW.equals(invokeMode)) {
                    lifecycleService.markFlowSuccess(
                            executionId, retryResult.result(), retryResult.retryCount());
                } else {
                    lifecycleService.markSuccess(
                            executionId, retryResult.result(), retryResult.retryCount());
                }
                log.info("任务执行成功: {}.{}, retryCount={}",
                        jobGroup, jobName, retryResult.retryCount());
            } else {
                lifecycleService.markFailed(
                        executionId, retryResult.error(), retryResult.retryCount());
                log.error("任务执行失败: {}.{}, retryCount={}, exceptionType={}",
                        jobGroup, jobName, retryResult.retryCount(),
                        retryResult.error().getClass().getName());
            }
        } catch (RuntimeException exception) {
            lifecycleService.markFailed(executionId, exception, 0);
            log.error("任务执行治理失败: {}.{}, exceptionType={}",
                    jobGroup, jobName, exception.getClass().getName());
        } finally {
            if (lockHandle != null) {
                lockHandle.close();
            }
        }
    }

    private String executeByInvokeMode(String invokeMode, JobDataMap jobDataMap,
                                       Long executionId, Long jobConfigId,
                                       String executorHandler, String executorBean,
                                       String executorMethod, String executorService,
                                       String jobParam, String executeMode) throws Exception {
        if (JobInvokeMode.FLOW.equals(invokeMode)) {
            JobFlowOrchestrationService flowService =
                    SpringUtil.getBean(JobFlowOrchestrationService.class);
            return flowService.start(jobConfigId, executionId, jobDataMap);
        }
        if (!JobInvokeMode.SINGLE.equals(invokeMode)) {
            throw new IllegalArgumentException("不支持的任务调用方式: " + invokeMode);
        }
        JobExecutorRouterManager routerManager = SpringUtil.getBean(JobExecutorRouterManager.class);
        return routerManager.route(executeMode, executorBean, executorMethod,
                executorHandler, executorService, jobParam);
    }

    private void completePlannedOnce(JobExecutionContext context, JobDataMap jobDataMap,
                                     Long jobConfigId, String jobName, String jobGroup) {
        if (!isPlannedOnceTrigger(context, jobDataMap, jobName, jobGroup)) {
            return;
        }
        String plannedFireOnceTime = jobDataMap.getString("plannedFireOnceTime");
        String timezone = jobDataMap.getString("timezone");
        JobOnceCompletionService completionService = SpringUtil.getBean(JobOnceCompletionService.class);
        boolean completed = completionService.markCompleted(
                jobConfigId, java.time.LocalDateTime.parse(plannedFireOnceTime), timezone);
        if (!completed) {
            log.warn("一次性任务完成态未更新，任务可能已被重新配置: jobConfigId={}, jobKey={}.{}",
                    jobConfigId, jobGroup, jobName);
        }
    }

    static boolean isPlannedOnceTrigger(JobExecutionContext context, JobDataMap jobDataMap,
                                        String jobName, String jobGroup) {
        if (!JobScheduleType.ONCE.equals(jobDataMap.getString("scheduleType"))
                || !"SCHEDULED".equals(jobDataMap.getString("triggerType"))) {
            return false;
        }
        Date scheduledFireTime = context.getScheduledFireTime();
        Long plannedFireEpochMilli = getLongValue(jobDataMap, "plannedFireEpochMilli");
        TriggerKey expectedTriggerKey = TriggerKey.triggerKey(jobName, jobGroup);
        return scheduledFireTime != null
                && plannedFireEpochMilli != null
                && plannedFireEpochMilli == scheduledFireTime.getTime()
                && context.getTrigger() != null
                && expectedTriggerKey.equals(context.getTrigger().getKey());
    }

    private Long getLong(JobDataMap jobDataMap, String key) {
        return getLongValue(jobDataMap, key);
    }

    private static Long getLongValue(JobDataMap jobDataMap, String key) {
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

    private String getStringOrDefault(JobDataMap jobDataMap, String key, String defaultValue) {
        String value = jobDataMap.getString(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private int getIntValue(JobDataMap jobDataMap, String key, int defaultValue) {
        Object value = jobDataMap.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
