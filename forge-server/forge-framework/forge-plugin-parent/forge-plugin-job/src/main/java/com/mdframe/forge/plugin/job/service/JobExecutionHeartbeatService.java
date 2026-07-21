package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 为运行中任务维护数据库心跳，供集群启动恢复识别真正失联的执行。
 */
@Slf4j
@Service
public class JobExecutionHeartbeatService {

    private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger();

    private final SysJobLogMapper jobLogMapper;
    private final JobProperties jobProperties;
    private final ScheduledExecutorService executor;

    @Autowired
    public JobExecutionHeartbeatService(SysJobLogMapper jobLogMapper, JobProperties jobProperties) {
        this(jobLogMapper, jobProperties, createExecutor());
    }

    JobExecutionHeartbeatService(SysJobLogMapper jobLogMapper,
                                 JobProperties jobProperties,
                                 ScheduledExecutorService executor) {
        this.jobLogMapper = jobLogMapper;
        this.jobProperties = jobProperties;
        this.executor = executor;
    }

    public HeartbeatHandle start(Long executionId) {
        if (executionId == null) {
            throw new IllegalArgumentException("任务执行ID不能为空");
        }
        refresh(executionId);
        Duration interval = jobProperties.validatedExecutionHeartbeatInterval();
        long intervalMillis = interval.toMillis();
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                () -> refresh(executionId), intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }

    private void refresh(Long executionId) {
        try {
            int updated = jobLogMapper.refreshHeartbeat(executionId);
            if (updated == 0) {
                log.debug("任务心跳未更新，执行可能已经结束: executionId={}", executionId);
            }
        } catch (RuntimeException exception) {
            log.warn("任务心跳更新失败: executionId={}, exceptionType={}",
                    executionId, exception.getClass().getSimpleName());
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private static ScheduledExecutorService createExecutor() {
        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor)
                Executors.newScheduledThreadPool(1, runnable -> {
                    Thread thread = new Thread(runnable,
                            "forge-job-heartbeat-" + THREAD_SEQUENCE.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    @FunctionalInterface
    public interface HeartbeatHandle extends AutoCloseable {

        @Override
        void close();
    }
}
