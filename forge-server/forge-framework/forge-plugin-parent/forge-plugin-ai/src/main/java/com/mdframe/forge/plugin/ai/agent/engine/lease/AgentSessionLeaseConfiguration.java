package com.mdframe.forge.plugin.ai.agent.engine.lease;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AgentSessionLeaseProperties.class)
public class AgentSessionLeaseConfiguration {

    @Bean(value = "forgeAgentLeaseScheduler", initMethod = "start", destroyMethod = "close")
    AgentLeaseHeartbeatScheduler forgeAgentLeaseScheduler(AgentSessionLeaseProperties properties) {
        properties.validate();
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("forge-agent-lease-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        var sequence = new AtomicInteger();
        scheduler.setThreadFactory(task -> new Thread(null, task,
                scheduler.getThreadNamePrefix() + sequence.incrementAndGet(), 0, false));
        return new AgentLeaseHeartbeatScheduler(scheduler);
    }
}
