package com.mdframe.forge.plugin.job.registry;

import com.mdframe.forge.plugin.job.constant.JobConcurrentPolicy;
import com.mdframe.forge.plugin.job.constant.JobMisfirePolicy;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.plugin.job.constant.JobScheduleType;
import com.mdframe.forge.plugin.job.manager.JobScheduleCoordinator;
import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.service.JobExecutorCatalogService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.job.annotation.JobHandler;
import com.mdframe.forge.starter.job.annotation.ScheduledJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 任务注解自动注册处理器
 * 扫描@JobHandler和@ScheduledJob注解，自动注册到调度器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobAutoRegistrar implements BeanPostProcessor {

    private final SysJobConfigMapper sysJobConfigMapper;

    private final JobExecutorCatalogService executorCatalogService;
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);

        JobHandler classJobHandler = AnnotationUtils.findAnnotation(targetClass, JobHandler.class);
        if (classJobHandler != null) {
            registerJobHandler(beanName, null, classJobHandler);
        }

        // 扫描方法级别的注解
        for (Method method : targetClass.getDeclaredMethods()) {
            // 处理@JobHandler
            JobHandler methodJobHandler = AnnotationUtils.findAnnotation(method, JobHandler.class);
            if (methodJobHandler != null) {
                registerJobHandler(beanName, method.getName(), methodJobHandler);
            }
            
            // 处理@ScheduledJob
            ScheduledJob scheduledJob = AnnotationUtils.findAnnotation(method, ScheduledJob.class);
            if (scheduledJob != null) {
                registerScheduledJob(beanName, method.getName(), scheduledJob);
            }
        }
        
        return bean;
    }
    
    /**
     * 注册JobHandler
     */
    private void registerJobHandler(String beanName, String methodName, JobHandler jobHandler) {
        String jobName = jobHandler.value();
        executorCatalogService.registerHandler(beanName, methodName, jobHandler);
        log.info("注册任务Handler目录: {} -> {}{}", jobName, beanName,
                methodName == null ? "" : "#" + methodName);
    }
    
    /**
     * 注册ScheduledJob
     */
    private void registerScheduledJob(String beanName, String methodName, ScheduledJob scheduledJob) {
        executorCatalogService.registerScheduledJob(beanName, methodName, scheduledJob);
        if (!scheduledJob.enabled()) {
            log.info("任务未启用，跳过注册: {}", scheduledJob.name());
            return;
        }
        
        String jobName = scheduledJob.name();
        if (jobName == null || jobName.isEmpty()) {
            jobName = beanName + "." + methodName;
        }
        
        String jobGroup = scheduledJob.group();
        SysJobConfig existing = sysJobConfigMapper.selectByJobKey(jobName, jobGroup);
        if (existing != null) {
            log.debug("注解任务配置已存在，保留数据库配置: {}.{}", jobGroup, jobName);
            return;
        }

        SysJobConfig jobConfig = new SysJobConfig();
        jobConfig.setJobName(jobName);
        jobConfig.setJobGroup(jobGroup);
        jobConfig.setDescription(scheduledJob.description());
        jobConfig.setExecutorBean(beanName);
        jobConfig.setExecutorMethod(methodName);
        jobConfig.setScheduleType(JobScheduleType.CRON);
        jobConfig.setCronExpression(scheduledJob.cron());
        jobConfig.setTimezone(JobScheduleType.DEFAULT_TIMEZONE);
        jobConfig.setExecuteMode("BEAN");
        jobConfig.setStatus(1);
        jobConfig.setConcurrentPolicy(JobConcurrentPolicy.DEFAULT);
        jobConfig.setMisfirePolicy(JobMisfirePolicy.DEFAULT);
        jobConfig.setIdempotentFlag(0);
        jobConfig.setRetryCount(0);
        jobConfig.setSyncStatus(JobScheduleCoordinator.SYNC_PENDING);
        jobConfig.setVersion(0);
        if (sysJobConfigMapper.insert(jobConfig) <= 0) {
            throw new BusinessException("注解任务配置登记失败: " + jobGroup + "." + jobName);
        }
        log.info("登记注解任务期望配置: {}.{} -> {}#{}", jobGroup, jobName, beanName, methodName);
    }
}
