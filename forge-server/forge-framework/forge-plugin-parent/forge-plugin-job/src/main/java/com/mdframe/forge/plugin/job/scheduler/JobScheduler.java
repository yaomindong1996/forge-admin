package com.mdframe.forge.plugin.job.scheduler;

import com.mdframe.forge.plugin.job.constant.JobMisfirePolicy;
import com.mdframe.forge.plugin.job.constant.JobScheduleType;
import com.mdframe.forge.plugin.job.model.JobConfig;
import com.mdframe.forge.plugin.job.service.JobScheduleDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

/**
 * 任务调度管理器
 * 封装Quartz核心操作，提供任务CRUD和执行控制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduler {
    
    private final Scheduler scheduler;

    private final JobScheduleDomainService jobScheduleDomainService;
    
    /**
     * 添加任务
     */
    public void addJob(JobConfig jobConfig) {
        JobKey jobKey = jobKey(jobConfig);
        try {
            if (scheduler.checkExists(jobKey)) {
                throw new JobScheduleException("Quartz任务已存在: " + jobKey);
            }
            scheduler.scheduleJob(buildJobDetail(jobConfig), buildTrigger(jobConfig));
            applyStatus(jobConfig);
            log.info("添加任务成功: {}", jobKey);
        } catch (JobScheduleException exception) {
            throw exception;
        } catch (Exception e) {
            throw scheduleFailure("添加", jobKey, e);
        }
    }
    
    /**
     * 更新任务
     */
    public void updateJob(JobConfig jobConfig) {
        JobKey jobKey = jobKey(jobConfig);
        try {
            if (!scheduler.checkExists(jobKey)) {
                throw new JobScheduleException("Quartz任务不存在: " + jobKey);
            }
            replaceJob(jobConfig);
            applyStatus(jobConfig);
            log.info("更新任务成功: {}", jobKey);
        } catch (JobScheduleException exception) {
            throw exception;
        } catch (Exception e) {
            throw scheduleFailure("更新", jobKey, e);
        }
    }

    /**
     * 幂等同步数据库期望状态到 Quartz。
     */
    public SynchronizeResult synchronize(JobConfig jobConfig) {
        JobKey jobKey = jobKey(jobConfig);
        try {
            if (isMissedOnceWithoutCompensation(jobConfig)) {
                if (scheduler.checkExists(jobKey) && !scheduler.deleteJob(jobKey)) {
                    throw new JobScheduleException("Quartz错过的一次性任务删除未生效: " + jobKey);
                }
                log.info("一次性任务已错过且不补偿: {}", jobKey);
                return SynchronizeResult.ONCE_MISSED;
            }
            if (scheduler.checkExists(jobKey)) {
                replaceJob(jobConfig);
            } else {
                scheduler.scheduleJob(buildJobDetail(jobConfig), buildTrigger(jobConfig));
            }
            applyStatus(jobConfig);
            log.info("同步任务成功: {}", jobKey);
            return SynchronizeResult.SCHEDULED;
        } catch (Exception e) {
            throw scheduleFailure("同步", jobKey, e);
        }
    }

    private boolean isMissedOnceWithoutCompensation(JobConfig jobConfig) {
        if (!JobScheduleType.ONCE.equals(jobConfig.getScheduleType())
                || !JobMisfirePolicy.DO_NOTHING.equals(jobConfig.getMisfirePolicy())) {
            return false;
        }
        ZoneId zoneId = jobScheduleDomainService.requireZoneId(jobConfig.getTimezone());
        Instant fireTime = jobScheduleDomainService.resolveOnceInstant(jobConfig.getFireOnceTime(), zoneId);
        return !fireTime.isAfter(Instant.now());
    }
    
    /**
     * 删除任务
     */
    public void deleteJob(String jobName, String jobGroup) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        try {
            if (scheduler.checkExists(jobKey)) {
                if (!scheduler.deleteJob(jobKey)) {
                    throw new JobScheduleException("Quartz任务删除未生效: " + jobKey);
                }
                log.info("删除任务成功: {}", jobKey);
            }
        } catch (JobScheduleException exception) {
            throw exception;
        } catch (Exception e) {
            throw scheduleFailure("删除", jobKey, e);
        }
    }
    
    /**
     * 暂停任务
     */
    public void pauseJob(String jobName, String jobGroup) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        try {
            assertExists(jobKey);
            scheduler.pauseJob(jobKey);
            log.info("暂停任务成功: {}", jobKey);
        } catch (JobScheduleException exception) {
            throw exception;
        } catch (Exception e) {
            throw scheduleFailure("暂停", jobKey, e);
        }
    }
    
    /**
     * 恢复任务
     */
    public void resumeJob(String jobName, String jobGroup) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        try {
            assertExists(jobKey);
            scheduler.resumeJob(jobKey);
            log.info("恢复任务成功: {}", jobKey);
        } catch (JobScheduleException exception) {
            throw exception;
        } catch (Exception e) {
            throw scheduleFailure("恢复", jobKey, e);
        }
    }
    
    /**
     * 立即触发任务
     */
    public void triggerJob(String jobName, String jobGroup, Long jobConfigId) {
        triggerJob(jobName, jobGroup, jobConfigId, "MANUAL", null);
    }

    public void triggerJob(String jobName, String jobGroup, Long jobConfigId,
                           String triggerType, Long reservedExecutionId) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        try {
            assertExists(jobKey);
            JobDataMap triggerData = new JobDataMap();
            putIfNotNull(triggerData, "jobConfigId", jobConfigId);
            putIfNotNull(triggerData, "reservedExecutionId", reservedExecutionId);
            triggerData.put("triggerType", triggerType);
            scheduler.triggerJob(jobKey, triggerData);
            log.info("触发任务成功: {}", jobKey);
        } catch (JobScheduleException exception) {
            throw exception;
        } catch (Exception e) {
            throw scheduleFailure("触发", jobKey, e);
        }
    }
    
    /**
     * 更新Cron表达式（热更新）
     */
    public void updateCron(String jobName, String jobGroup, String cronExpression) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
        try {
            assertExists(jobKey);
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            if (trigger == null) {
                throw new JobScheduleException("Quartz触发器不存在: " + triggerKey);
            }
            String oldCron = trigger.getCronExpression();
            if (oldCron.equals(cronExpression)) {
                return;
            }
            Trigger newTrigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(jobKey)
                    .withSchedule(applyCronMisfirePolicy(
                            CronScheduleBuilder.cronSchedule(cronExpression)
                                    .inTimeZone(trigger.getTimeZone()),
                            trigger.getMisfireInstruction() == CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
                                    ? JobMisfirePolicy.FIRE_ONCE_NOW : JobMisfirePolicy.DO_NOTHING))
                    .build();
            scheduler.rescheduleJob(triggerKey, newTrigger);
            log.info("更新Cron表达式成功: {} -> {}", oldCron, cronExpression);
        } catch (JobScheduleException exception) {
            throw exception;
        } catch (Exception e) {
            throw scheduleFailure("更新Cron", jobKey, e);
        }
    }
    
    /**
     * 检查任务是否存在
     */
    public boolean exists(String jobName, String jobGroup) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        try {
            return scheduler.checkExists(jobKey);
        } catch (Exception e) {
            throw scheduleFailure("检查", jobKey, e);
        }
    }

    /**
     * 查询Quartz当前记录的下一次触发时间。列表展示失败时不阻断配置查询。
     */
    public LocalDateTime nextFireTime(String jobName, String jobGroup) {
        return nextFireTime(jobName, jobGroup, JobScheduleType.DEFAULT_TIMEZONE);
    }

    public LocalDateTime nextFireTime(String jobName, String jobGroup, String timezone) {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
        try {
            Trigger trigger = scheduler.getTrigger(triggerKey);
            Date nextFireTime = trigger == null ? null : trigger.getNextFireTime();
            ZoneId zoneId = jobScheduleDomainService.requireZoneId(timezone);
            return nextFireTime == null ? null
                    : LocalDateTime.ofInstant(nextFireTime.toInstant(), zoneId);
        } catch (Exception exception) {
            log.warn("读取Quartz任务下次触发时间失败: triggerKey={}, error={}",
                    triggerKey, exception.getMessage());
            return null;
        }
    }

    private void replaceJob(JobConfig jobConfig) throws SchedulerException {
        JobKey jobKey = jobKey(jobConfig);
        scheduler.addJob(buildJobDetail(jobConfig), true, true);
        TriggerKey triggerKey = triggerKey(jobConfig);
        if (scheduler.checkExists(triggerKey)) {
            scheduler.rescheduleJob(triggerKey, buildTrigger(jobConfig));
        } else {
            scheduler.scheduleJob(buildTrigger(jobConfig));
        }
    }

    private JobDetail buildJobDetail(JobConfig jobConfig) {
        JobDataMap jobDataMap = new JobDataMap();
        putIfNotNull(jobDataMap, "executorHandler", jobConfig.getExecutorHandler());
        putIfNotNull(jobDataMap, "executorBean", jobConfig.getExecutorBean());
        putIfNotNull(jobDataMap, "executorMethod", jobConfig.getExecutorMethod());
        putIfNotNull(jobDataMap, "executorService", jobConfig.getExecutorService());
        putIfNotNull(jobDataMap, "jobParam", jobConfig.getJobParam());
        putIfNotNull(jobDataMap, "executeMode", jobConfig.getExecuteMode());
        putIfNotNull(jobDataMap, "invokeMode", jobConfig.getInvokeMode());
        putIfNotNull(jobDataMap, "flowModelKey", jobConfig.getFlowModelKey());
        putIfNotNull(jobDataMap, "flowModelVersion", jobConfig.getFlowModelVersion());
        putIfNotNull(jobDataMap, "flowDeploymentId", jobConfig.getFlowDeploymentId());
        putIfNotNull(jobDataMap, "flowProcessDefinitionId", jobConfig.getFlowProcessDefinitionId());
        putIfNotNull(jobDataMap, "jobConfigId", jobConfig.getId());
        putIfNotNull(jobDataMap, "scheduleType", jobConfig.getScheduleType());
        putIfNotNull(jobDataMap, "concurrentPolicy", jobConfig.getConcurrentPolicy());
        putIfNotNull(jobDataMap, "misfirePolicy", jobConfig.getMisfirePolicy());
        putIfNotNull(jobDataMap, "idempotentFlag", jobConfig.getIdempotentFlag());
        putIfNotNull(jobDataMap, "retryCount", jobConfig.getRetryCount());
        if (JobScheduleType.ONCE.equals(jobConfig.getScheduleType())) {
            ZoneId zoneId = jobScheduleDomainService.requireZoneId(jobConfig.getTimezone());
            putIfNotNull(jobDataMap, "plannedFireEpochMilli", jobScheduleDomainService.resolveOnceInstant(
                    jobConfig.getFireOnceTime(), zoneId).toEpochMilli());
            putIfNotNull(jobDataMap, "plannedFireOnceTime", jobConfig.getFireOnceTime().toString());
            putIfNotNull(jobDataMap, "timezone", zoneId.getId());
        }
        jobDataMap.put("triggerType", "SCHEDULED");
        return JobBuilder.newJob(QuartzJobExecutor.class)
                .withIdentity(jobKey(jobConfig))
                .withDescription(jobConfig.getDescription())
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildTrigger(JobConfig jobConfig) {
        if (JobScheduleType.ONCE.equals(jobConfig.getScheduleType())) {
            ZoneId zoneId = jobScheduleDomainService.requireZoneId(jobConfig.getTimezone());
            Date fireTime = Date.from(jobScheduleDomainService.resolveOnceInstant(
                    jobConfig.getFireOnceTime(), zoneId));
            return TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey(jobConfig))
                    .forJob(jobKey(jobConfig))
                    .startAt(fireTime)
                    .withSchedule(applySimpleMisfirePolicy(
                            SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0),
                            jobConfig.getMisfirePolicy()))
                    .build();
        }
        ZoneId zoneId = jobScheduleDomainService.requireZoneId(jobConfig.getTimezone());
        return TriggerBuilder.newTrigger()
                .withIdentity(triggerKey(jobConfig))
                .forJob(jobKey(jobConfig))
                .withSchedule(applyCronMisfirePolicy(
                        CronScheduleBuilder.cronSchedule(jobConfig.getCronExpression())
                                .inTimeZone(TimeZone.getTimeZone(zoneId)),
                        jobConfig.getMisfirePolicy()))
                .build();
    }

    private CronScheduleBuilder applyCronMisfirePolicy(CronScheduleBuilder scheduleBuilder,
                                                        String misfirePolicy) {
        if (JobMisfirePolicy.FIRE_ONCE_NOW.equals(misfirePolicy)) {
            return scheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
        }
        return scheduleBuilder.withMisfireHandlingInstructionDoNothing();
    }

    private SimpleScheduleBuilder applySimpleMisfirePolicy(SimpleScheduleBuilder scheduleBuilder,
                                                            String misfirePolicy) {
        if (JobMisfirePolicy.FIRE_ONCE_NOW.equals(misfirePolicy)) {
            return scheduleBuilder.withMisfireHandlingInstructionFireNow();
        }
        return scheduleBuilder.withMisfireHandlingInstructionNextWithRemainingCount();
    }

    private void applyStatus(JobConfig jobConfig) throws SchedulerException {
        if (Integer.valueOf(0).equals(jobConfig.getStatus())) {
            scheduler.pauseJob(jobKey(jobConfig));
        } else {
            scheduler.resumeJob(jobKey(jobConfig));
        }
    }

    private void assertExists(JobKey jobKey) throws SchedulerException {
        if (!scheduler.checkExists(jobKey)) {
            throw new JobScheduleException("Quartz任务不存在: " + jobKey);
        }
    }

    public enum SynchronizeResult {
        SCHEDULED,
        ONCE_MISSED
    }

    private JobKey jobKey(JobConfig jobConfig) {
        return JobKey.jobKey(jobConfig.getJobName(), jobConfig.getJobGroup());
    }

    private TriggerKey triggerKey(JobConfig jobConfig) {
        return TriggerKey.triggerKey(jobConfig.getJobName(), jobConfig.getJobGroup());
    }

    private void putIfNotNull(JobDataMap dataMap, String key, Object value) {
        if (value != null) {
            dataMap.put(key, value);
        }
    }

    private JobScheduleException scheduleFailure(String operation, JobKey jobKey, Exception cause) {
        log.error("Quartz任务{}失败: key={}, error={}", operation, jobKey, cause.getMessage(), cause);
        return new JobScheduleException("Quartz任务" + operation + "失败: " + jobKey, cause);
    }
}
