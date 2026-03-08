package com.mdframe.forge.plugin.job.scheduler;

import com.mdframe.forge.plugin.job.model.JobConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;

/**
 * 任务调度管理器
 * 封装Quartz核心操作，提供任务CRUD和执行控制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduler {
    
    private final Scheduler scheduler;
    
    /**
     * 添加任务
     */
    public boolean addJob(JobConfig jobConfig) {
        try {
            JobKey jobKey = JobKey.jobKey(jobConfig.getJobName(), jobConfig.getJobGroup());
            
            // 检查任务是否已存在
            if (scheduler.checkExists(jobKey)) {
                log.warn("任务已存在: {}", jobKey);
                return false;
            }
            
            // 创建JobDetail
            JobDetail jobDetail = JobBuilder.newJob(QuartzJobExecutor.class)
                    .withIdentity(jobKey)
                    .withDescription(jobConfig.getDescription())
                    .usingJobData("executorHandler", jobConfig.getExecutorHandler())
                    .usingJobData("executorBean", jobConfig.getExecutorBean())
                    .usingJobData("executorMethod", jobConfig.getExecutorMethod())
                    .usingJobData("executorService", jobConfig.getExecutorService())
                    .usingJobData("jobParam", jobConfig.getJobParam())
                    .usingJobData("executeMode", jobConfig.getExecuteMode())
                    .storeDurably()
                    .build();
            
            // 创建Trigger
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(jobConfig.getJobName(), jobConfig.getJobGroup())
                    .withSchedule(CronScheduleBuilder.cronSchedule(jobConfig.getCronExpression())
                            .withMisfireHandlingInstructionDoNothing())
                    .build();
            
            scheduler.scheduleJob(jobDetail, trigger);
            
            if (jobConfig.getStatus() == 0) {
                scheduler.pauseJob(jobKey);
            }
            
            log.info("添加任务成功: {}", jobKey);
            return true;
        } catch (Exception e) {
            log.error("添加任务失败", e);
            return false;
        }
    }
    
    /**
     * 更新任务
     */
    public boolean updateJob(JobConfig jobConfig) {
        try {
            JobKey jobKey = JobKey.jobKey(jobConfig.getJobName(), jobConfig.getJobGroup());
            
            if (!scheduler.checkExists(jobKey)) {
                log.warn("任务不存在: {}", jobKey);
                return false;
            }
            
            // 更新JobDetail
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            JobBuilder jobBuilder = jobDetail.getJobBuilder();
            JobDetail newJobDetail = jobBuilder
                    .withDescription(jobConfig.getDescription())
                    .usingJobData("executorHandler", jobConfig.getExecutorHandler())
                    .usingJobData("executorBean", jobConfig.getExecutorBean())
                    .usingJobData("executorMethod", jobConfig.getExecutorMethod())
                    .usingJobData("executorService", jobConfig.getExecutorService())
                    .usingJobData("jobParam", jobConfig.getJobParam())
                    .usingJobData("executeMode", jobConfig.getExecuteMode())
                    .build();
            
            scheduler.addJob(newJobDetail, true);
            
            // 更新Trigger
            TriggerKey triggerKey = TriggerKey.triggerKey(jobConfig.getJobName(), jobConfig.getJobGroup());
            Trigger newTrigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(jobConfig.getCronExpression())
                            .withMisfireHandlingInstructionDoNothing())
                    .build();
            
            scheduler.rescheduleJob(triggerKey, newTrigger);
            
            log.info("更新任务成功: {}", jobKey);
            return true;
        } catch (Exception e) {
            log.error("更新任务失败", e);
            return false;
        }
    }
    
    /**
     * 删除任务
     */
    public boolean deleteJob(String jobName, String jobGroup) {
        try {
            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.info("删除任务成功: {}", jobKey);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("删除任务失败", e);
            return false;
        }
    }
    
    /**
     * 暂停任务
     */
    public boolean pauseJob(String jobName, String jobGroup) {
        try {
            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            scheduler.pauseJob(jobKey);
            log.info("暂停任务成功: {}", jobKey);
            return true;
        } catch (Exception e) {
            log.error("暂停任务失败", e);
            return false;
        }
    }
    
    /**
     * 恢复任务
     */
    public boolean resumeJob(String jobName, String jobGroup) {
        try {
            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            scheduler.resumeJob(jobKey);
            log.info("恢复任务成功: {}", jobKey);
            return true;
        } catch (Exception e) {
            log.error("恢复任务失败", e);
            return false;
        }
    }
    
    /**
     * 立即触发任务
     */
    public boolean triggerJob(String jobName, String jobGroup) {
        try {
            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            scheduler.triggerJob(jobKey);
            log.info("触发任务成功: {}", jobKey);
            return true;
        } catch (Exception e) {
            log.error("触发任务失败", e);
            return false;
        }
    }
    
    /**
     * 更新Cron表达式（热更新）
     */
    public boolean updateCron(String jobName, String jobGroup, String cronExpression) {
        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            
            if (trigger == null) {
                return false;
            }
            
            String oldCron = trigger.getCronExpression();
            if (oldCron.equals(cronExpression)) {
                return true;
            }
            
            // 重新构建Trigger
            Trigger newTrigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                            .withMisfireHandlingInstructionDoNothing())
                    .build();
            
            scheduler.rescheduleJob(triggerKey, newTrigger);
            log.info("更新Cron表达式成功: {} -> {}", oldCron, cronExpression);
            return true;
        } catch (Exception e) {
            log.error("更新Cron表达式失败", e);
            return false;
        }
    }
    
    /**
     * 检查任务是否存在
     */
    public boolean exists(String jobName, String jobGroup) {
        try {
            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            return scheduler.checkExists(jobKey);
        } catch (Exception e) {
            return false;
        }
    }
}
