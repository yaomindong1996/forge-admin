package com.mdframe.forge.starter.log.config;

import com.mdframe.forge.starter.core.context.LogProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 日志线程池配置
 * 用于异步保存日志，避免阻塞业务线程
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class LogThreadPoolConfig {

    private final LogProperties logProperties;

    /**
     * 日志异步处理线程池
     */
    @Bean("logTaskExecutor")
    public Executor logTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(logProperties.getThreadPoolCoreSize());
        // 最大线程数
        executor.setMaxPoolSize(logProperties.getThreadPoolMaxSize());
        // 队列容量
        executor.setQueueCapacity(logProperties.getThreadPoolQueueCapacity());
        // 线程名称前缀
        executor.setThreadNamePrefix("log-async-");
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);
        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 关闭时等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("日志异步线程池初始化完成: coreSize={}, maxSize={}, queueCapacity={}",
                logProperties.getThreadPoolCoreSize(),
                logProperties.getThreadPoolMaxSize(),
                logProperties.getThreadPoolQueueCapacity());
        
        return executor;
    }
}
