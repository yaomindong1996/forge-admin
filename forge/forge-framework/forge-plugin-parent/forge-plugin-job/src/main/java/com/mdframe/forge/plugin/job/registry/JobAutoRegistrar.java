package com.mdframe.forge.plugin.job.registry;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.model.JobConfig;
import com.mdframe.forge.plugin.job.scheduler.JobScheduler;
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
    
    private final JobScheduler jobScheduler;
    
    private final SysJobConfigMapper sysJobConfigMapper;
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
   
        // 扫描方法级别的注解
        for (Method method : targetClass.getDeclaredMethods()) {
            // 处理@JobHandler
            JobHandler methodJobHandler = AnnotationUtils.findAnnotation(method, JobHandler.class);
            if (methodJobHandler != null) {
                registerJobHandler(beanName, methodJobHandler);
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
    private void registerJobHandler(String beanName, JobHandler jobHandler) {
        String jobName = jobHandler.value();
        log.info("注册任务Handler: {} -> {}", jobName, beanName);
        // Handler在需要时动态创建任务，这里仅记录
    }
    
    /**
     * 注册ScheduledJob
     */
    private void registerScheduledJob(String beanName, String methodName, ScheduledJob scheduledJob) {
        if (!scheduledJob.enabled()) {
            log.info("任务未启用，跳过注册: {}", scheduledJob.name());
            return;
        }
        
        String jobName = scheduledJob.name();
        if (jobName == null || jobName.isEmpty()) {
            jobName = beanName + "." + methodName;
        }
        
        JobConfig jobConfig = new JobConfig();
        jobConfig.setJobName(jobName);
        jobConfig.setJobGroup(scheduledJob.group());
        jobConfig.setDescription(scheduledJob.description());
        jobConfig.setExecutorBean(beanName);
        jobConfig.setExecutorMethod(methodName);
        jobConfig.setCronExpression(scheduledJob.cron());
        jobConfig.setExecuteMode("BEAN");
        jobConfig.setStatus(1); // 运行状态
        
        LambdaQueryWrapper<SysJobConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysJobConfig::getJobName, jobName);
        SysJobConfig sysJobConfig = sysJobConfigMapper.selectOne(queryWrapper);
        if (sysJobConfig == null) {
            sysJobConfig = new SysJobConfig();
            BeanUtil.copyProperties(jobConfig, sysJobConfig);
            sysJobConfigMapper.insert(sysJobConfig);
            boolean success = jobScheduler.addJob(jobConfig);
            if (success) {
                log.info("自动注册定时任务: {} -> {}#{}", jobName, beanName, methodName);
            } else {
                log.warn("任务注册失败或已存在: {}", jobName);
            }
        }
    }
}
