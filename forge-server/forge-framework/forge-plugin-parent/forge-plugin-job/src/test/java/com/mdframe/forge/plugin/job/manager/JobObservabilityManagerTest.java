package com.mdframe.forge.plugin.job.manager;

import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import com.mdframe.forge.plugin.job.scheduler.JobScheduler;
import com.mdframe.forge.plugin.job.vo.JobConfigVO;
import com.mdframe.forge.plugin.job.vo.JobFailureTaskVO;
import com.mdframe.forge.plugin.job.vo.JobLogVO;
import com.mdframe.forge.plugin.job.vo.JobMonitorSummaryVO;
import com.mdframe.forge.plugin.job.vo.JobOverviewVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobObservabilityManagerTest {

    @Test
    void shouldBuildTaskOverviewWithRuntimeScheduleAndRecentExecutions() {
        SysJobConfigMapper configMapper = mock(SysJobConfigMapper.class);
        SysJobLogMapper logMapper = mock(SysJobLogMapper.class);
        JobScheduler scheduler = mock(JobScheduler.class);
        JobConfigVO config = new JobConfigVO();
        config.setId(7L);
        config.setJobName("inventoryClose");
        config.setJobGroup("BUSINESS");
        config.setTimezone("Asia/Shanghai");
        config.setStatus(1);
        config.setSyncStatus("SYNCED");
        config.setConsecutiveFailures(2);
        LocalDateTime nextFireTime = LocalDateTime.of(2026, 7, 21, 2, 0);
        List<JobLogVO> recent = List.of(new JobLogVO());
        when(configMapper.selectJobDetail(7L)).thenReturn(config);
        when(scheduler.nextFireTime("inventoryClose", "BUSINESS", "Asia/Shanghai"))
                .thenReturn(nextFireTime);
        when(logMapper.selectRecentExecutions(7L, 5)).thenReturn(recent);

        JobOverviewVO overview = manager(configMapper, logMapper, scheduler).getOverview(7L);

        assertEquals(7L, overview.getJobConfigId());
        assertEquals(2, overview.getConsecutiveFailures());
        assertEquals(nextFireTime, overview.getNextFireTime());
        assertEquals(recent, overview.getRecentExecutions());
    }

    @Test
    void shouldCalculateTwentyFourHourRatesAndAttachFailureTasks() {
        SysJobConfigMapper configMapper = mock(SysJobConfigMapper.class);
        SysJobLogMapper logMapper = mock(SysJobLogMapper.class);
        JobMonitorSummaryVO aggregate = new JobMonitorSummaryVO();
        aggregate.setTotalCount(8L);
        aggregate.setSuccessCount(5L);
        aggregate.setFailedCount(2L);
        aggregate.setRunningCount(0L);
        aggregate.setSkippedCount(0L);
        aggregate.setAcceptedCount(1L);
        JobFailureTaskVO failureTask = new JobFailureTaskVO();
        failureTask.setJobConfigId(7L);
        when(logMapper.selectMonitorSummary(any(), any())).thenReturn(aggregate);
        when(configMapper.selectConsecutiveFailureTaskCount()).thenReturn(1);
        when(configMapper.selectConsecutiveFailureTasks(10)).thenReturn(List.of(failureTask));

        JobMonitorSummaryVO summary = manager(configMapper, logMapper, mock(JobScheduler.class)).getSummary();

        assertEquals(new BigDecimal("62.50"), summary.getSuccessRate());
        assertEquals(new BigDecimal("25.00"), summary.getFailureRate());
        assertEquals(1L, summary.getAcceptedCount());
        assertEquals(summary.getTotalCount(), summary.getSuccessCount()
                + summary.getFailedCount()
                + summary.getRunningCount()
                + summary.getSkippedCount()
                + summary.getAcceptedCount());
        assertEquals(1, summary.getConsecutiveFailureTaskCount());
        assertEquals(List.of(failureTask), summary.getFailureTasks());
        assertNotNull(summary.getWindowStart());
        assertNotNull(summary.getWindowEnd());
        verify(logMapper).selectMonitorSummary(summary.getWindowStart(), summary.getWindowEnd());
    }

    @Test
    void shouldNormalizeAcceptedCountWhenAggregateIsEmpty() {
        SysJobConfigMapper configMapper = mock(SysJobConfigMapper.class);
        SysJobLogMapper logMapper = mock(SysJobLogMapper.class);
        when(logMapper.selectMonitorSummary(any(), any())).thenReturn(new JobMonitorSummaryVO());
        when(configMapper.selectConsecutiveFailureTaskCount()).thenReturn(0);

        JobMonitorSummaryVO summary = manager(
                configMapper, logMapper, mock(JobScheduler.class)).getSummary();

        assertEquals(0L, summary.getAcceptedCount());
        assertEquals(0L, summary.getTotalCount());
    }

    private JobObservabilityManager manager(SysJobConfigMapper configMapper,
                                            SysJobLogMapper logMapper,
                                            JobScheduler scheduler) {
        return new JobObservabilityManager(configMapper, logMapper, scheduler);
    }
}
