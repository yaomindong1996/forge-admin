package com.mdframe.forge.plugin.job.example;

import com.mdframe.forge.plugin.job.executor.IJobExecutor;
import com.mdframe.forge.starter.job.annotation.JobHandler;
import com.mdframe.forge.starter.job.annotation.ScheduledJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 任务使用示例
 *
 * 支持三种执行模式：
 * 1. BEAN模式：直接调用本地Bean方法（单体模式）
 * 2. HANDLER模式：调用本地Handler（单体模式）
 * 3. RPC模式：远程调用其他服务Handler（分布式模式）
 */
@Slf4j
@Component
public class JobExamples {
    
    // ============ 方式1：@ScheduledJob注解（Bean直连模式）============
    
    /**
     * 示例1：简单定时任务
     */
    @ScheduledJob(cron = "0/5 * * * * ?",
                  name = "simpleTask",
                  description = "每5秒执行一次")
    public void simpleTask() {
        log.info("执行简单定时任务");
    }
    
    /**
     * 示例2：带参数的任务
     */
    @ScheduledJob(cron = "0 0/10 * * * ?",
                  name = "paramTask",
                  description = "每10分钟执行")
    public String taskWithParam(String param) {
        log.info("执行参数任务: {}", param);
        return "SUCCESS";
    }
    
    // ============ 方式2：@JobHandler注解（Handler模式）============
    
    /**
     * Handler模式示例
     * 需要在管理界面或API中动态创建任务并绑定此Handler
     */
    @Component
    @JobHandler("dataCleanupHandler")
    public static class DataCleanupHandler implements IJobExecutor {
        
        @Override
        public String execute(String param) throws Exception {
            log.info("执行数据清理任务，参数: {}", param);
            // 业务逻辑
            return "清理完成";
        }
    }
    
    /**
     * 另一个Handler示例
     */
    @Component
    @JobHandler("reportGenerator")
    public static class ReportGeneratorHandler implements IJobExecutor {
        
        @Override
        public String execute(String param) throws Exception {
            log.info("生成报表，参数: {}", param);
            // 报表生成逻辑
            return "报表已生成";
        }
    }
    
    // ============ 方式3：RPC模式（分布式）============
    
    /**
     * RPC模式：远程调用其他服务
     * 适用场景：分布式部署，调度中心与执行器分离
     *
     * 配置方式：通过REST API或数据库添加任务
     * {
     *   "jobName": "remoteDataSync",
     *   "executeMode": "RPC",
     *   "executorService": "business-service",  // 远程服务名
     *   "executorHandler": "dataSyncHandler",   // 远程 Handler名称
     *   "cronExpression": "0 0/10 * * * ?"
     * }
     *
     * 远程服务(business-service)需要：
     * 1. 实现IJobExecutor接口的Handler
     * 2. 启用执行器端点：forge.job.executor-enabled=true
     */
}
