package com.mdframe.forge.plugin.job.scheduler;

import cn.hutool.extra.spring.SpringUtil;
import com.mdframe.forge.plugin.job.executor.JobExecutorRouterManager;
import com.mdframe.forge.plugin.job.manager.JobExecutionLockManager;
import com.mdframe.forge.plugin.job.service.JobExecutionLifecycleService;
import com.mdframe.forge.plugin.job.service.JobExecutionHeartbeatService;
import com.mdframe.forge.plugin.job.service.JobFlowOrchestrationService;
import com.mdframe.forge.plugin.job.service.JobOnceCompletionService;
import com.mdframe.forge.plugin.job.service.JobRetryExecutor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.support.GenericApplicationContext;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuartzJobExecutorTest {

    private final RecordingRouterManager routerManager = new RecordingRouterManager();
    private final RecordingCompletionService completionService = new RecordingCompletionService();
    private final JobExecutionLifecycleService lifecycleService = mock(JobExecutionLifecycleService.class);
    private final JobExecutionHeartbeatService heartbeatService = mock(JobExecutionHeartbeatService.class);
    private final JobFlowOrchestrationService flowOrchestrationService =
            mock(JobFlowOrchestrationService.class);
    private GenericApplicationContext applicationContext;

    @BeforeAll
    @SuppressWarnings("unchecked")
    void setUpContext() {
        ObjectProvider<RedissonClient> redissonProvider = mock(ObjectProvider.class);
        when(redissonProvider.getIfAvailable()).thenReturn(null);
        applicationContext = new GenericApplicationContext();
        applicationContext.getBeanFactory().registerSingleton("jobExecutorRouterManager", routerManager);
        applicationContext.getBeanFactory().registerSingleton("jobExecutionLifecycleService", lifecycleService);
        applicationContext.getBeanFactory().registerSingleton("jobExecutionHeartbeatService", heartbeatService);
        applicationContext.getBeanFactory().registerSingleton(
                "jobFlowOrchestrationService", flowOrchestrationService);
        applicationContext.getBeanFactory().registerSingleton("jobExecutionLockManager",
                new JobExecutionLockManager(redissonProvider));
        applicationContext.getBeanFactory().registerSingleton("jobRetryExecutor", new JobRetryExecutor());
        applicationContext.getBeanFactory().registerSingleton("jobOnceCompletionService", completionService);
        applicationContext.refresh();
        new SpringUtil().setApplicationContext(applicationContext);
    }

    @BeforeEach
    void resetState() {
        routerManager.fail = false;
        routerManager.calls = 0;
        completionService.calls = 0;
        reset(lifecycleService);
        reset(heartbeatService);
        reset(flowOrchestrationService);
        when(lifecycleService.accept(any(JobExecutionContext.class), eq(1L))).thenReturn(41L);
        when(heartbeatService.start(41L)).thenReturn(() -> { });
        when(flowOrchestrationService.start(eq(1L), eq(41L), any(JobDataMap.class)))
                .thenReturn("process-41");
    }

    @AfterAll
    void tearDownContext() {
        applicationContext.close();
    }

    @Test
    void shouldCompletePlannedOnceAfterSuccessfulExecution() {
        new QuartzJobExecutor().execute(scheduledOnceContext("SCHEDULED"));

        assertEquals(1, completionService.calls);
        assertEquals(1, routerManager.calls);
        verify(lifecycleService).markSuccess(41L, "SUCCESS", 0);
    }

    @Test
    void shouldCompletePlannedOnceAfterFailedExecution() {
        routerManager.fail = true;

        new QuartzJobExecutor().execute(scheduledOnceContext("SCHEDULED"));

        assertEquals(1, completionService.calls);
        assertEquals(1, routerManager.calls);
        verify(lifecycleService).markFailed(eq(41L), any(IllegalStateException.class), eq(0));
    }

    @Test
    void shouldNotCompleteManualOnceExecution() {
        new QuartzJobExecutor().execute(scheduledOnceContext("MANUAL"));

        assertEquals(0, completionService.calls);
        verify(lifecycleService).markSuccess(41L, "SUCCESS", 0);
    }

    @Test
    void shouldFailClosedAndCompleteOnceWhenRedisIsUnavailable() {
        new QuartzJobExecutor().execute(scheduledOnceContext("SCHEDULED", "SKIP_IF_RUNNING"));

        assertEquals(0, routerManager.calls);
        assertEquals(1, completionService.calls);
        verify(lifecycleService).markSkipped(eq(41L), contains("Redis"));
    }

    @Test
    void shouldRouteFlowWithoutInvokingSingleExecutorAndLinkProcessInstance() {
        JobExecutionContext context = scheduledOnceContext("MANUAL");
        context.getMergedJobDataMap().put("invokeMode", "FLOW");
        context.getMergedJobDataMap().put("flowModelKey", "daily-settlement");
        context.getMergedJobDataMap().put("flowModelVersion", 7);
        context.getMergedJobDataMap().put("flowDeploymentId", "deployment-7");
        context.getMergedJobDataMap().put(
                "flowProcessDefinitionId", "daily-settlement:7:definition-7");

        new QuartzJobExecutor().execute(context);

        assertEquals(0, routerManager.calls);
        verify(flowOrchestrationService).start(eq(1L), eq(41L), any(JobDataMap.class));
        verify(lifecycleService).markFlowSuccess(41L, "process-41", 0);
    }

    private JobExecutionContext scheduledOnceContext(String triggerType) {
        return scheduledOnceContext(triggerType, "ALLOW");
    }

    private JobExecutionContext scheduledOnceContext(String triggerType, String concurrentPolicy) {
        Date scheduledFireTime = new Date(1_800_000_000_000L);
        JobKey jobKey = JobKey.jobKey("sampleJob", "DEFAULT");
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("jobConfigId", 1L);
        dataMap.put("scheduleType", "ONCE");
        dataMap.put("triggerType", triggerType);
        dataMap.put("plannedFireEpochMilli", scheduledFireTime.getTime());
        dataMap.put("plannedFireOnceTime", "2027-01-15T08:00:00");
        dataMap.put("timezone", "Asia/Shanghai");
        dataMap.put("executeMode", "BEAN");
        dataMap.put("concurrentPolicy", concurrentPolicy);
        dataMap.put("idempotentFlag", 0);
        dataMap.put("retryCount", 0);
        JobDetail jobDetail = JobBuilder.newJob(QuartzJobExecutor.class)
                .withIdentity(jobKey)
                .usingJobData(dataMap)
                .build();
        Trigger trigger = trigger(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));

        return (JobExecutionContext) Proxy.newProxyInstance(
                JobExecutionContext.class.getClassLoader(),
                new Class<?>[]{JobExecutionContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getJobDetail" -> jobDetail;
                    case "getMergedJobDataMap" -> dataMap;
                    case "getScheduledFireTime", "getFireTime" -> scheduledFireTime;
                    case "getTrigger" -> trigger;
                    case "toString" -> "QuartzJobExecutorTestContext";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private Trigger trigger(TriggerKey triggerKey) {
        return (Trigger) Proxy.newProxyInstance(
                Trigger.class.getClassLoader(),
                new Class<?>[]{Trigger.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getKey" -> triggerKey;
                    case "toString" -> "QuartzJobExecutorTestTrigger";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return 0;
    }

    private static final class RecordingRouterManager extends JobExecutorRouterManager {
        private boolean fail;
        private int calls;

        @Override
        public String route(String executeMode, String executorBean, String executorMethod,
                            String executorHandler, String executorService, String jobParam) {
            calls++;
            if (fail) {
                throw new IllegalStateException("failure");
            }
            return "SUCCESS";
        }
    }

    private static final class RecordingCompletionService extends JobOnceCompletionService {
        private int calls;

        private RecordingCompletionService() {
            super(null);
        }

        @Override
        public boolean markCompleted(Long jobConfigId, LocalDateTime plannedFireTime, String timezone) {
            calls++;
            assertEquals(1L, jobConfigId);
            assertEquals(LocalDateTime.of(2027, 1, 15, 8, 0), plannedFireTime);
            assertEquals("Asia/Shanghai", timezone);
            return true;
        }
    }
}
