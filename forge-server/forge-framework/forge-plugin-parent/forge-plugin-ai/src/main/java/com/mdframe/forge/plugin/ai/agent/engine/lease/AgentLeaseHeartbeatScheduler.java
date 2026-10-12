package com.mdframe.forge.plugin.ai.agent.engine.lease;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

/** 不暴露为通用 TaskScheduler Bean，避免 @Scheduled/Boot 自动配置把业务扫描放进续租池。 */
@RequiredArgsConstructor
public class AgentLeaseHeartbeatScheduler implements AutoCloseable {
    private final ThreadPoolTaskScheduler delegate;

    public void start() {
        delegate.initialize();
    }

    public ScheduledFuture<?> schedule(Runnable task, Duration interval) {
        return delegate.scheduleAtFixedRate(task, Instant.now().plus(interval), interval);
    }

    @Override
    public void close() {
        delegate.shutdown();
    }
}
