package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobExecutionHeartbeatServiceTest {

    @Test
    void shouldCreateServiceWithoutScheduledExecutorBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(SysJobLogMapper.class, () -> mock(SysJobLogMapper.class));
            context.registerBean(JobProperties.class, JobProperties::new);
            context.register(JobExecutionHeartbeatService.class);

            context.refresh();

            assertNotNull(context.getBean(JobExecutionHeartbeatService.class));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRefreshImmediatelyAndCancelScheduledHeartbeatOnClose() {
        SysJobLogMapper mapper = mock(SysJobLogMapper.class);
        when(mapper.refreshHeartbeat(55L)).thenReturn(1);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ScheduledFuture<Object> future = mock(ScheduledFuture.class);
        doReturn(future).when(executor).scheduleAtFixedRate(any(Runnable.class),
                org.mockito.ArgumentMatchers.eq(30000L),
                org.mockito.ArgumentMatchers.eq(30000L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.MILLISECONDS));
        JobProperties properties = new JobProperties();
        properties.setExecutionHeartbeatInterval(Duration.ofSeconds(30));
        JobExecutionHeartbeatService service = new JobExecutionHeartbeatService(mapper, properties, executor);

        JobExecutionHeartbeatService.HeartbeatHandle handle = service.start(55L);
        handle.close();

        verify(mapper).refreshHeartbeat(55L);
        verify(future).cancel(false);
    }
}
