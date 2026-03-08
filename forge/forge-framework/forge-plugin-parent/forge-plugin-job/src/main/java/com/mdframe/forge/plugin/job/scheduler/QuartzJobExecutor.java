package com.mdframe.forge.plugin.job.scheduler;

import cn.hutool.extra.spring.SpringUtil;
import com.mdframe.forge.plugin.job.executor.JobExecutorRouterManager;
import com.mdframe.forge.plugin.job.monitor.JobMonitor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

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
        
        // 获取任务参数
        String executorHandler = context.getJobDetail().getJobDataMap().getString("executorHandler");
        String executorBean = context.getJobDetail().getJobDataMap().getString("executorBean");
        String executorMethod = context.getJobDetail().getJobDataMap().getString("executorMethod");
        String executorService = context.getJobDetail().getJobDataMap().getString("executorService");
        String jobParam = context.getJobDetail().getJobDataMap().getString("jobParam");
        String executeMode = context.getJobDetail().getJobDataMap().getString("executeMode");
        
        LocalDateTime startTime = LocalDateTime.now();
        String result = null;
        Exception exception = null;
        
        try {
            log.info("开始执行任务: {}.{}, 模式: {}, Handler: {}",
                    jobGroup, jobName, executeMode, executorHandler);
            JobExecutorRouterManager routerManager = SpringUtil.getBean(JobExecutorRouterManager.class);
            
            // 使用路由管理器执行任务
            result = routerManager.route(executeMode, executorBean, executorMethod,
                                        executorHandler, executorService, jobParam);
            
            log.info("任务执行成功: {}.{}, 结果: {}", jobGroup, jobName, result);
        } catch (Exception e) {
            log.error("任务执行失败: {}.{}", jobGroup, jobName, e);
            exception = e;
            result = "执行失败: " + e.getMessage();
        } finally {
            // 记录执行日志
            LocalDateTime endTime = LocalDateTime.now();
            JobMonitor jobMonitor = SpringUtil.getBean(JobMonitor.class);
            jobMonitor.recordLog(jobName, jobGroup, executorHandler, jobParam,
                    context.getFireTime(), startTime, endTime,
                    exception == null ? 1 : 0, result, exception);
        }
    }
}
