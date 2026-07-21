package com.mdframe.forge.plugin.job.scheduler;

import com.mdframe.forge.plugin.job.model.JobConfig;
import com.mdframe.forge.plugin.job.service.JobScheduleDomainService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Properties;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobSchedulerTest {

    private Scheduler quartzScheduler;
    private JobScheduler jobScheduler;

    @BeforeEach
    void setUp() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("org.quartz.scheduler.instanceName", "JobSchedulerTest");
        properties.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        properties.setProperty("org.quartz.threadPool.threadCount", "1");
        properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        StdSchedulerFactory factory = new StdSchedulerFactory(properties);
        quartzScheduler = factory.getScheduler();
        jobScheduler = new JobScheduler(quartzScheduler, new JobScheduleDomainService());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (!quartzScheduler.isShutdown()) {
            quartzScheduler.shutdown(true);
        }
    }

    @Test
    void shouldAddUpdatePauseAndDeleteJob() throws Exception {
        JobConfig config = jobConfig("0 0/5 * * * ?", 1);

        jobScheduler.addJob(config);
        assertTrue(jobScheduler.exists(config.getJobName(), config.getJobGroup()));
        assertThrows(JobScheduleException.class, () -> jobScheduler.addJob(config));

        config.setCronExpression("0 0/10 * * * ?");
        config.setStatus(0);
        jobScheduler.synchronize(config);

        CronTrigger trigger = (CronTrigger) quartzScheduler.getTrigger(
                TriggerKey.triggerKey(config.getJobName(), config.getJobGroup()));
        assertEquals("0 0/10 * * * ?", trigger.getCronExpression());
        assertEquals(Trigger.TriggerState.PAUSED, quartzScheduler.getTriggerState(trigger.getKey()));

        jobScheduler.deleteJob(config.getJobName(), config.getJobGroup());
        assertFalse(quartzScheduler.checkExists(JobKey.jobKey(config.getJobName(), config.getJobGroup())));
        assertDoesNotThrow(() -> jobScheduler.deleteJob(config.getJobName(), config.getJobGroup()));
    }

    @Test
    void shouldExposeQuartzFailureAsBusinessException() throws Exception {
        quartzScheduler.shutdown(true);

        assertThrows(JobScheduleException.class,
                () -> jobScheduler.synchronize(jobConfig("0 0/5 * * * ?", 1)));
    }

    @Test
    void shouldBuildCronTriggerWithConfiguredTimezone() throws Exception {
        JobConfig config = jobConfig("0 0 2 * * ?", 1);
        config.setTimezone("UTC");

        jobScheduler.addJob(config);

        CronTrigger trigger = (CronTrigger) quartzScheduler.getTrigger(
                TriggerKey.triggerKey(config.getJobName(), config.getJobGroup()));
        assertEquals(TimeZone.getTimeZone("UTC"), trigger.getTimeZone());
    }

    @Test
    void shouldBuildSingleFireSimpleTrigger() throws Exception {
        JobConfig config = jobConfig(null, 1);
        config.setScheduleType("ONCE");
        config.setTimezone("Asia/Shanghai");
        config.setFireOnceTime(LocalDateTime.of(2099, 7, 19, 10, 0));

        jobScheduler.addJob(config);

        SimpleTrigger trigger = (SimpleTrigger) quartzScheduler.getTrigger(
                TriggerKey.triggerKey(config.getJobName(), config.getJobGroup()));
        assertEquals(0, trigger.getRepeatCount());
        assertEquals(Date.from(Instant.parse("2099-07-19T02:00:00Z")), trigger.getStartTime());
        assertEquals(config.getFireOnceTime(), jobScheduler.nextFireTime(
                config.getJobName(), config.getJobGroup(), config.getTimezone()));
    }

    @Test
    void shouldCarryExecutionPoliciesIntoJobDataMap() throws Exception {
        JobConfig config = jobConfig("0 0/5 * * * ?", 1);
        config.setConcurrentPolicy("SKIP_IF_RUNNING");
        config.setMisfirePolicy("FIRE_ONCE_NOW");
        config.setIdempotentFlag(1);
        config.setRetryCount(3);

        jobScheduler.addJob(config);

        JobDataMap dataMap = quartzScheduler.getJobDetail(
                JobKey.jobKey(config.getJobName(), config.getJobGroup())).getJobDataMap();
        assertEquals("SKIP_IF_RUNNING", dataMap.getString("concurrentPolicy"));
        assertEquals("FIRE_ONCE_NOW", dataMap.getString("misfirePolicy"));
        assertEquals(1, dataMap.getInt("idempotentFlag"));
        assertEquals(3, dataMap.getInt("retryCount"));
    }

    @Test
    void shouldCarryImmutableFlowBindingIntoJobDataMap() throws Exception {
        JobConfig config = jobConfig("0 0/5 * * * ?", 1);
        config.setInvokeMode("FLOW");
        config.setFlowModelKey("daily-settlement");
        config.setFlowModelVersion(7);
        config.setFlowDeploymentId("deployment-7");
        config.setFlowProcessDefinitionId("daily-settlement:7:definition-7");

        jobScheduler.addJob(config);

        JobDataMap dataMap = quartzScheduler.getJobDetail(
                JobKey.jobKey(config.getJobName(), config.getJobGroup())).getJobDataMap();
        assertEquals("FLOW", dataMap.getString("invokeMode"));
        assertEquals("daily-settlement", dataMap.getString("flowModelKey"));
        assertEquals(7, dataMap.getInt("flowModelVersion"));
        assertEquals("deployment-7", dataMap.getString("flowDeploymentId"));
        assertEquals("daily-settlement:7:definition-7",
                dataMap.getString("flowProcessDefinitionId"));
    }

    @Test
    void shouldMapCronMisfirePolicies() throws Exception {
        JobConfig config = jobConfig("0 0/5 * * * ?", 1);
        config.setMisfirePolicy("DO_NOTHING");
        jobScheduler.addJob(config);
        CronTrigger trigger = (CronTrigger) quartzScheduler.getTrigger(
                TriggerKey.triggerKey(config.getJobName(), config.getJobGroup()));
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, trigger.getMisfireInstruction());

        config.setMisfirePolicy("FIRE_ONCE_NOW");
        jobScheduler.synchronize(config);
        trigger = (CronTrigger) quartzScheduler.getTrigger(
                TriggerKey.triggerKey(config.getJobName(), config.getJobGroup()));
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, trigger.getMisfireInstruction());
    }

    @Test
    void shouldMapOnceMisfirePolicies() throws Exception {
        JobConfig config = jobConfig(null, 1);
        config.setScheduleType("ONCE");
        config.setFireOnceTime(LocalDateTime.of(2099, 7, 19, 10, 0));
        config.setMisfirePolicy("DO_NOTHING");
        jobScheduler.addJob(config);
        SimpleTrigger trigger = (SimpleTrigger) quartzScheduler.getTrigger(
                TriggerKey.triggerKey(config.getJobName(), config.getJobGroup()));
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT,
                trigger.getMisfireInstruction());

        config.setMisfirePolicy("FIRE_ONCE_NOW");
        jobScheduler.synchronize(config);
        trigger = (SimpleTrigger) quartzScheduler.getTrigger(
                TriggerKey.triggerKey(config.getJobName(), config.getJobGroup()));
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW, trigger.getMisfireInstruction());
    }

    @Test
    void shouldCompletePastOnceWithoutCreatingTriggerWhenMisfireIsDoNothing() throws Exception {
        JobConfig config = jobConfig(null, 1);
        config.setScheduleType("ONCE");
        config.setTimezone("Asia/Shanghai");
        config.setFireOnceTime(LocalDateTime.of(2020, 1, 1, 10, 0));
        config.setMisfirePolicy("DO_NOTHING");

        JobScheduler.SynchronizeResult result = jobScheduler.synchronize(config);

        assertEquals(JobScheduler.SynchronizeResult.ONCE_MISSED, result);
        assertFalse(quartzScheduler.checkExists(JobKey.jobKey("sampleJob", "DEFAULT")));
    }

    static JobConfig jobConfig(String cronExpression, int status) {
        JobConfig config = new JobConfig();
        config.setId(1L);
        config.setJobName("sampleJob");
        config.setJobGroup("DEFAULT");
        config.setDescription("示例任务");
        config.setExecutorBean("sampleJobExecutor");
        config.setExecutorMethod("execute");
        config.setScheduleType("CRON");
        config.setCronExpression(cronExpression);
        config.setTimezone("Asia/Shanghai");
        config.setExecuteMode("BEAN");
        config.setInvokeMode("SINGLE");
        config.setConcurrentPolicy("ALLOW");
        config.setMisfirePolicy("DO_NOTHING");
        config.setIdempotentFlag(0);
        config.setRetryCount(0);
        config.setStatus(status);
        return config;
    }
}
