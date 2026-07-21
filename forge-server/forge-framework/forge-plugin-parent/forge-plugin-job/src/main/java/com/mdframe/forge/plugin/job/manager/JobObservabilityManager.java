package com.mdframe.forge.plugin.job.manager;

import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import com.mdframe.forge.plugin.job.scheduler.JobScheduler;
import com.mdframe.forge.plugin.job.vo.JobConfigVO;
import com.mdframe.forge.plugin.job.vo.JobFailureTaskVO;
import com.mdframe.forge.plugin.job.vo.JobMonitorSummaryVO;
import com.mdframe.forge.plugin.job.vo.JobOverviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务运行概览和监控聚合。
 */
@Component
@RequiredArgsConstructor
public class JobObservabilityManager {

    private static final int RECENT_EXECUTION_LIMIT = 5;
    private static final int FAILURE_TASK_LIMIT = 10;

    private final SysJobConfigMapper jobConfigMapper;
    private final SysJobLogMapper jobLogMapper;
    private final JobScheduler jobScheduler;

    public JobOverviewVO getOverview(Long id) {
        JobConfigVO config = jobConfigMapper.selectJobDetail(id);
        if (config == null) {
            throw new BusinessException("定时任务不存在");
        }
        JobOverviewVO overview = new JobOverviewVO();
        overview.setJobConfigId(config.getId());
        overview.setJobName(config.getJobName());
        overview.setJobGroup(config.getJobGroup());
        overview.setLastExecutionStatus(config.getLastExecutionStatus());
        overview.setLastExecutionTime(config.getLastExecutionTime());
        overview.setConsecutiveFailures(defaultInteger(config.getConsecutiveFailures()));
        if (Integer.valueOf(1).equals(config.getStatus()) && "SYNCED".equals(config.getSyncStatus())) {
            overview.setNextFireTime(jobScheduler.nextFireTime(
                    config.getJobName(), config.getJobGroup(), config.getTimezone()));
        }
        overview.setRecentExecutions(jobLogMapper.selectRecentExecutions(id, RECENT_EXECUTION_LIMIT));
        return overview;
    }

    public JobMonitorSummaryVO getSummary() {
        LocalDateTime windowEnd = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.minusHours(24);
        JobMonitorSummaryVO summary = jobLogMapper.selectMonitorSummary(windowStart, windowEnd);
        if (summary == null) {
            summary = new JobMonitorSummaryVO();
        }
        normalizeCounts(summary);
        summary.setWindowStart(windowStart);
        summary.setWindowEnd(windowEnd);
        summary.setSuccessRate(toPercentage(summary.getSuccessCount(), summary.getTotalCount()));
        summary.setFailureRate(toPercentage(summary.getFailedCount(), summary.getTotalCount()));
        summary.setConsecutiveFailureTaskCount(jobConfigMapper.selectConsecutiveFailureTaskCount());
        List<JobFailureTaskVO> failureTasks = jobConfigMapper.selectConsecutiveFailureTasks(FAILURE_TASK_LIMIT);
        summary.setFailureTasks(failureTasks == null ? List.of() : failureTasks);
        return summary;
    }

    private void normalizeCounts(JobMonitorSummaryVO summary) {
        summary.setTotalCount(defaultLong(summary.getTotalCount()));
        summary.setSuccessCount(defaultLong(summary.getSuccessCount()));
        summary.setFailedCount(defaultLong(summary.getFailedCount()));
        summary.setRunningCount(defaultLong(summary.getRunningCount()));
        summary.setSkippedCount(defaultLong(summary.getSkippedCount()));
        summary.setAcceptedCount(defaultLong(summary.getAcceptedCount()));
    }

    private BigDecimal toPercentage(long count, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private int defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }
}
