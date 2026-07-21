package com.mdframe.forge.plugin.job.manager;

import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.scheduler.JobScheduleException;
import com.mdframe.forge.plugin.job.scheduler.JobScheduler;
import com.mdframe.forge.plugin.job.service.JobScheduleDomainService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobScheduleCoordinatorTest {

    private Scheduler quartzScheduler;
    private InMemoryJobConfigMapper mapperState;
    private JobScheduleCoordinator coordinator;

    @BeforeEach
    void setUp() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("org.quartz.scheduler.instanceName", "JobScheduleCoordinatorTest");
        properties.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        properties.setProperty("org.quartz.threadPool.threadCount", "1");
        properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        quartzScheduler = new StdSchedulerFactory(properties).getScheduler();
        mapperState = new InMemoryJobConfigMapper(jobEntity());
        coordinator = new JobScheduleCoordinator(mapperState.mapper(),
                new JobScheduler(quartzScheduler, new JobScheduleDomainService()), lockManager());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (!quartzScheduler.isShutdown()) {
            quartzScheduler.shutdown(true);
        }
    }

    @Test
    void shouldSynchronizeDesiredStateIdempotently() throws Exception {
        coordinator.synchronize(1L);

        assertEquals("SYNCED", mapperState.entity.getSyncStatus());
        assertNull(mapperState.entity.getSyncError());
        assertNotNull(mapperState.entity.getSyncTime());
        assertTrue(quartzScheduler.checkExists(JobKey.jobKey("sampleJob", "DEFAULT")));

        mapperState.entity.setCronExpression("0 0/10 * * * ?");
        mapperState.entity.setStatus(0);
        mapperState.entity.setSyncStatus("PENDING");
        coordinator.synchronize(1L);

        assertEquals("SYNCED", mapperState.entity.getSyncStatus());
        assertEquals(Trigger.TriggerState.PAUSED,
                quartzScheduler.getTriggerState(TriggerKey.triggerKey("sampleJob", "DEFAULT")));
    }

    @Test
    void shouldPersistFailureAndAllowRetry() throws Exception {
        quartzScheduler.shutdown(true);

        assertThrows(JobScheduleException.class, () -> coordinator.synchronize(1L));
        assertEquals("FAILED", mapperState.entity.getSyncStatus());
        assertNotNull(mapperState.entity.getSyncError());

        Properties properties = new Properties();
        properties.setProperty("org.quartz.scheduler.instanceName", "JobScheduleCoordinatorRetryTest");
        properties.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        properties.setProperty("org.quartz.threadPool.threadCount", "1");
        properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        quartzScheduler = new StdSchedulerFactory(properties).getScheduler();
        coordinator = new JobScheduleCoordinator(mapperState.mapper(),
                new JobScheduler(quartzScheduler, new JobScheduleDomainService()), lockManager());

        coordinator.retrySynchronization(1L);
        assertEquals("SYNCED", mapperState.entity.getSyncStatus());
        assertNull(mapperState.entity.getSyncError());
    }

    @Test
    void shouldDeleteQuartzAndDatabaseIdempotently() throws Exception {
        coordinator.synchronize(1L);
        mapperState.entity.setSyncStatus("DELETE_PENDING");

        coordinator.synchronize(1L);

        assertTrue(mapperState.deleted);
        assertFalse(quartzScheduler.checkExists(JobKey.jobKey("sampleJob", "DEFAULT")));
    }

    @Test
    void shouldRecoverDatabaseJobsWithoutRemovingExternalJobs() throws Exception {
        quartzScheduler.addJob(JobBuilder.newJob(ExternalJob.class)
                .withIdentity("externalJob", "OTHER")
                .storeDurably()
                .build(), false);
        mapperState.entity.setStatus(0);

        coordinator.reconcileOnStartup();

        assertTrue(quartzScheduler.checkExists(JobKey.jobKey("sampleJob", "DEFAULT")));
        assertEquals(Trigger.TriggerState.PAUSED,
                quartzScheduler.getTriggerState(TriggerKey.triggerKey("sampleJob", "DEFAULT")));
        assertTrue(quartzScheduler.checkExists(JobKey.jobKey("externalJob", "OTHER")));
    }

    @Test
    void shouldNotRecoverCompletedOnceJob() throws Exception {
        mapperState.entity.setScheduleType("ONCE");
        mapperState.entity.setCronExpression(null);
        mapperState.entity.setFireOnceTime(LocalDateTime.of(2026, 7, 19, 10, 0));
        mapperState.entity.setStatus(2);

        coordinator.reconcileOnStartup();

        assertFalse(quartzScheduler.checkExists(JobKey.jobKey("sampleJob", "DEFAULT")));
    }

    @Test
    void shouldMarkPastOnceAsCompletedWithoutCreatingQuartzTrigger() throws Exception {
        mapperState.entity.setScheduleType("ONCE");
        mapperState.entity.setCronExpression(null);
        mapperState.entity.setFireOnceTime(LocalDateTime.of(2020, 1, 1, 10, 0));
        mapperState.entity.setMisfirePolicy("DO_NOTHING");

        coordinator.synchronize(1L);

        assertEquals(2, mapperState.entity.getStatus());
        assertEquals("SYNCED", mapperState.entity.getSyncStatus());
        assertFalse(quartzScheduler.checkExists(JobKey.jobKey("sampleJob", "DEFAULT")));
    }

    @Test
    void shouldResynchronizeLatestVersionWhenConfigurationChangesDuringQuartzWrite() {
        JobScheduler scheduler = mock(JobScheduler.class);
        AtomicBoolean first = new AtomicBoolean(true);
        when(scheduler.synchronize(any())).thenAnswer(invocation -> {
            if (first.compareAndSet(true, false)) {
                mapperState.entity.setCronExpression("0 0/10 * * * ?");
                mapperState.entity.setVersion(mapperState.entity.getVersion() + 1);
                mapperState.entity.setSyncStatus("PENDING");
            }
            return JobScheduler.SynchronizeResult.SCHEDULED;
        });
        coordinator = new JobScheduleCoordinator(mapperState.mapper(), scheduler, lockManager());

        coordinator.synchronize(1L);

        verify(scheduler, times(2)).synchronize(any());
        assertEquals("SYNCED", mapperState.entity.getSyncStatus());
        assertEquals("0 0/10 * * * ?", mapperState.entity.getCronExpression());
    }

    @Test
    void shouldRemoveQuartzJobWhenConfigurationDisappearsDuringSynchronization() {
        JobScheduler scheduler = mock(JobScheduler.class);
        doAnswer(invocation -> {
            mapperState.deleted = true;
            return JobScheduler.SynchronizeResult.SCHEDULED;
        }).when(scheduler).synchronize(any());
        coordinator = new JobScheduleCoordinator(mapperState.mapper(), scheduler, lockManager());

        coordinator.synchronize(1L);

        verify(scheduler).deleteJob("sampleJob", "DEFAULT");
    }

    private JobScheduleSynchronizationLockManager lockManager() {
        JobScheduleSynchronizationLockManager lockManager =
                mock(JobScheduleSynchronizationLockManager.class);
        when(lockManager.acquire(anyString(), anyString())).thenReturn(
                mock(JobScheduleSynchronizationLockManager.LockHandle.class));
        return lockManager;
    }

    private SysJobConfig jobEntity() {
        SysJobConfig entity = new SysJobConfig();
        entity.setId(1L);
        entity.setJobName("sampleJob");
        entity.setJobGroup("DEFAULT");
        entity.setDescription("示例任务");
        entity.setExecutorBean("sampleJobExecutor");
        entity.setExecutorMethod("execute");
        entity.setScheduleType("CRON");
        entity.setCronExpression("0 0/5 * * * ?");
        entity.setTimezone("Asia/Shanghai");
        entity.setExecuteMode("BEAN");
        entity.setStatus(1);
        entity.setSyncStatus("PENDING");
        entity.setVersion(0);
        return entity;
    }

    private static final class InMemoryJobConfigMapper {
        private SysJobConfig entity;
        private boolean deleted;

        private InMemoryJobConfigMapper(SysJobConfig entity) {
            this.entity = entity;
        }

        private SysJobConfigMapper mapper() {
            return (SysJobConfigMapper) Proxy.newProxyInstance(
                    SysJobConfigMapper.class.getClassLoader(),
                    new Class<?>[]{SysJobConfigMapper.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "selectById" -> deleted ? null : entity;
                        case "selectRecoveryCandidates" -> deleted || Integer.valueOf(2).equals(entity.getStatus())
                                ? List.of() : List.of(entity);
                        case "updateSyncState" -> updateSyncState(args);
                        case "logicalDeleteByVersion" -> logicalDeleteByVersion(args);
                        case "markOnceMissedCompleted" -> markOnceMissedCompleted(args);
                        case "toString" -> "InMemoryJobConfigMapper";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private int updateSyncState(Object[] args) {
            if (deleted || !entity.getVersion().equals(args[1])) {
                return 0;
            }
            entity.setSyncStatus((String) args[2]);
            entity.setSyncError((String) args[3]);
            entity.setSyncTime((LocalDateTime) args[4]);
            entity.setVersion(entity.getVersion() + 1);
            return 1;
        }

        private int logicalDeleteByVersion(Object[] args) {
            if (deleted || !entity.getVersion().equals(args[1])) {
                return 0;
            }
            deleted = true;
            return 1;
        }

        private int markOnceMissedCompleted(Object[] args) {
            if (deleted || !entity.getVersion().equals(args[3])) {
                return 0;
            }
            entity.setStatus(2);
            entity.setSyncStatus("SYNCED");
            entity.setSyncError(null);
            entity.setSyncTime((LocalDateTime) args[4]);
            entity.setVersion(entity.getVersion() + 1);
            return 1;
        }
    }

    public static class ExternalJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            // Test-only external Quartz job.
        }
    }
}
