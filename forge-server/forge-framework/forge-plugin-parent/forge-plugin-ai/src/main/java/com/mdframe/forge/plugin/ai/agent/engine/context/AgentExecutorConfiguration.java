package com.mdframe.forge.plugin.ai.agent.engine.context;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.atomic.AtomicInteger;

/** 有界专用池，饱和时拒绝，不能在 HTTP 请求线程执行长期 ReAct 循环。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AgentExecutorProperties.class)
public class AgentExecutorConfiguration {

    @Bean("forgeAgentTaskExecutor")
    ThreadPoolTaskExecutor forgeAgentTaskExecutor(AgentExecutorProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix("forge-agent-");
        AtomicInteger sequence = new AtomicInteger();
        // 租户 TTL 是可继承的；线程首次创建时不继承请求的临时租户，任务身份由装饰器显式传入。
        executor.setThreadFactory(task -> new Thread(null, task,
                executor.getThreadNamePrefix() + sequence.incrementAndGet(), 0, false));
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }
}
