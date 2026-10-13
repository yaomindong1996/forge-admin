package com.mdframe.forge.starter.flow.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Boot 4 兼容：存在自定义 {@link java.util.concurrent.Executor}（如 flowEventExecutor、logTaskExecutor）时，
 * TaskExecutionAutoConfiguration 默认不再创建 {@code applicationTaskExecutor}，
 * 而 Flowable 8 {@code ProcessEngineAutoConfiguration} 强制按该 Qualifier 注入。
 * <p>
 * 本配置在 ProcessEngine 之前补齐缺失 Bean；也可配合 {@code spring.task.execution.mode=force}。
 */
@AutoConfiguration
@AutoConfigureBefore(name = "org.flowable.spring.boot.ProcessEngineAutoConfiguration")
public class FlowApplicationTaskExecutorConfiguration {

    public static final String APPLICATION_TASK_EXECUTOR_BEAN_NAME = "applicationTaskExecutor";

    @Bean(name = APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    @ConditionalOnMissingBean(name = APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("app-task-");
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }
}
