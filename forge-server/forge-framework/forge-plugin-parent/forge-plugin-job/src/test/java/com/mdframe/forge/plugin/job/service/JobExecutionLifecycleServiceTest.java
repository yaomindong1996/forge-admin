package com.mdframe.forge.plugin.job.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.job.constant.JobExecutionStatus;
import com.mdframe.forge.plugin.job.entity.SysJobLog;
import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import com.mdframe.forge.plugin.job.scheduler.QuartzJobExecutor;
import com.mdframe.forge.plugin.job.support.JobLogSanitizer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JobExecutionLifecycleServiceTest {

    @Test
    void shouldAtomicallyCompleteFlowExecutionWithProcessInstanceId() {
        SysJobLogMapper mapper = mock(SysJobLogMapper.class);
        SysJobConfigMapper configMapper = mock(SysJobConfigMapper.class);
        when(mapper.completeRunningFlowExecution(
                15L, JobExecutionStatus.SUCCESS, "流程实例已启动", "process-15", 1))
                .thenReturn(1);
        when(configMapper.applyExecutionOutcome(15L, JobExecutionStatus.SUCCESS)).thenReturn(1);
        JobExecutionLifecycleService service = service(mapper, configMapper);

        service.markFlowSuccess(15L, "process-15", 1);

        verify(mapper).completeRunningFlowExecution(
                15L, JobExecutionStatus.SUCCESS, "流程实例已启动", "process-15", 1);
        verify(configMapper).applyExecutionOutcome(15L, JobExecutionStatus.SUCCESS);
    }

    @Test
    void shouldInsertOneRunningExecutionWithQuartzMetadata() {
        SysJobLogMapper mapper = mock(SysJobLogMapper.class);
        when(mapper.insert(any(SysJobLog.class))).thenAnswer(invocation -> {
            SysJobLog log = invocation.getArgument(0);
            log.setId(101L);
            return 1;
        });
        JobExecutionLifecycleService service = service(mapper);

        Long executionId = service.accept(context(), 7L);

        ArgumentCaptor<SysJobLog> captor = ArgumentCaptor.forClass(SysJobLog.class);
        verify(mapper).insert(captor.capture());
        SysJobLog log = captor.getValue();
        assertEquals(101L, executionId);
        assertEquals(JobExecutionStatus.RUNNING, log.getStatus());
        assertEquals(7L, log.getJobConfigId());
        assertEquals("sampleJob", log.getJobName());
        assertEquals("DEFAULT", log.getJobGroup());
        assertEquals("SCHEDULED", log.getTriggerType());
        assertEquals("fire-101", log.getFireInstanceId());
        assertTrue(log.getHeartbeatTime() != null);
        assertEquals(0, log.getRetryCount());
        assertFalse(log.getJobParam().contains("secret-token"));
    }

    @Test
    void shouldStartReservedOpenApiExecutionWithoutInsertingAnotherRow() {
        SysJobLogMapper mapper = mock(SysJobLogMapper.class);
        when(mapper.startReservedExecution(
                eq(88L), any(), eq("fire-101"), any(), eq("sampleHandler"), any()))
                .thenReturn(1);
        JobExecutionLifecycleService service = service(mapper);
        JobExecutionContext context = context();
        context.getMergedJobDataMap().put("reservedExecutionId", 88L);
        context.getMergedJobDataMap().put("triggerType", "OPEN_API");

        Long executionId = service.accept(context, 7L);

        assertEquals(88L, executionId);
        verify(mapper, never()).insert(any(SysJobLog.class));
        verify(mapper).startReservedExecution(
                eq(88L), any(), eq("fire-101"), any(), eq("sampleHandler"),
                argThat(value -> !value.contains("secret-token")));
    }

    @Test
    void shouldUpdateSameRunningRowToTerminalStates() {
        SysJobLogMapper mapper = mock(SysJobLogMapper.class);
        SysJobConfigMapper configMapper = mock(SysJobConfigMapper.class);
        when(mapper.completeRunningExecution(any(), any(), any(), any(), any())).thenReturn(1);
        when(configMapper.applyExecutionOutcome(11L, JobExecutionStatus.SUCCESS)).thenReturn(1);
        when(configMapper.applyExecutionOutcome(12L, JobExecutionStatus.FAILED)).thenReturn(1);
        JobExecutionLifecycleService service = service(mapper, configMapper);

        service.markSuccess(11L, "result-token", 2);
        service.markFailed(12L, new IllegalStateException("password=raw-password"), 1);
        service.markSkipped(13L, "任务正在运行");

        verify(mapper).completeRunningExecution(
                11L, JobExecutionStatus.SUCCESS, "result-token", null, 2);
        verify(mapper).completeRunningExecution(
                eq(12L), eq(JobExecutionStatus.FAILED), isNull(),
                argThat(message -> {
                    assertTrue(message.startsWith("java.lang.IllegalStateException: password=****"));
                    assertTrue(message.contains("JobExecutionLifecycleServiceTest"));
                    assertFalse(message.contains("raw-password"));
                    return true;
                }), eq(1));
        verify(mapper).completeRunningExecution(
                13L, JobExecutionStatus.SKIPPED, "任务正在运行", null, 0);
        verify(configMapper).applyExecutionOutcome(11L, JobExecutionStatus.SUCCESS);
        verify(configMapper).applyExecutionOutcome(12L, JobExecutionStatus.FAILED);
        verify(configMapper, never()).applyExecutionOutcome(13L, JobExecutionStatus.SKIPPED);
    }

    @Test
    void shouldNotChangeFailureCounterWhenTerminalUpdateWasIgnored() {
        SysJobLogMapper mapper = mock(SysJobLogMapper.class);
        SysJobConfigMapper configMapper = mock(SysJobConfigMapper.class);
        when(mapper.completeRunningExecution(any(), any(), any(), any(), any())).thenReturn(0);
        JobFailureAlarmService alarmService = mock(JobFailureAlarmService.class);
        JobExecutionLifecycleService service = service(mapper, configMapper, alarmService);

        service.markFailed(12L, new IllegalStateException("执行失败"), 1);

        verifyNoInteractions(configMapper);
        verifyNoInteractions(alarmService);
    }

    @Test
    void shouldNotifyOnlyAfterFinalFailureWasStored() {
        SysJobLogMapper mapper = mock(SysJobLogMapper.class);
        SysJobConfigMapper configMapper = mock(SysJobConfigMapper.class);
        JobFailureAlarmService alarmService = mock(JobFailureAlarmService.class);
        when(mapper.completeRunningExecution(any(), any(), any(), any(), any())).thenReturn(1, 0);
        when(configMapper.applyExecutionOutcome(12L, JobExecutionStatus.FAILED)).thenReturn(1);
        JobExecutionLifecycleService service = service(mapper, configMapper, alarmService);

        service.markFailed(12L, new IllegalStateException("执行失败"), 2);
        service.markFailed(12L, new IllegalStateException("重复回调"), 2);

        verify(alarmService).notifyFinalFailure(12L);
    }

    private JobExecutionLifecycleService service(SysJobLogMapper mapper) {
        return service(mapper, mock(SysJobConfigMapper.class));
    }

    private JobExecutionLifecycleService service(SysJobLogMapper mapper, SysJobConfigMapper configMapper) {
        return service(mapper, configMapper, mock(JobFailureAlarmService.class));
    }

    private JobExecutionLifecycleService service(SysJobLogMapper mapper, SysJobConfigMapper configMapper,
                                                  JobFailureAlarmService alarmService) {
        return new JobExecutionLifecycleService(mapper, configMapper,
                new JobLogSanitizer(new ObjectMapper()), alarmService);
    }

    private JobExecutionContext context() {
        Date fireTime = new Date(1_800_000_000_000L);
        Date scheduledFireTime = new Date(1_799_999_940_000L);
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("triggerType", "SCHEDULED");
        dataMap.put("executorHandler", "sampleHandler");
        dataMap.put("jobParam", "{\"token\":\"secret-token\"}");
        JobDetail jobDetail = JobBuilder.newJob(QuartzJobExecutor.class)
                .withIdentity("sampleJob", "DEFAULT")
                .usingJobData(dataMap)
                .build();
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getJobDetail()).thenReturn(jobDetail);
        when(context.getMergedJobDataMap()).thenReturn(dataMap);
        when(context.getFireTime()).thenReturn(fireTime);
        when(context.getScheduledFireTime()).thenReturn(scheduledFireTime);
        when(context.getFireInstanceId()).thenReturn("fire-101");
        return context;
    }
}
