package com.mdframe.forge.plugin.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.job.dto.JobLogQuery;
import com.mdframe.forge.plugin.job.entity.SysJobLog;
import com.mdframe.forge.plugin.job.vo.JobLogDetailVO;
import com.mdframe.forge.plugin.job.vo.JobLogExportVO;
import com.mdframe.forge.plugin.job.vo.JobLogVO;
import com.mdframe.forge.plugin.job.vo.JobMonitorSummaryVO;
import com.mdframe.forge.plugin.job.vo.JobFailureAlarmContextVO;
import com.mdframe.forge.plugin.job.vo.JobOpenApiExecutionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 任务日志Mapper
 */
@Mapper
public interface SysJobLogMapper extends BaseMapper<SysJobLog> {

    Page<JobLogVO> selectLogPage(Page<JobLogVO> page, @Param("query") JobLogQuery query);

    JobLogDetailVO selectLogDetail(@Param("id") Long id);

    List<JobLogExportVO> selectExportList(@Param("query") JobLogQuery query);

    List<JobLogVO> selectRecentExecutions(@Param("jobConfigId") Long jobConfigId,
                                          @Param("limit") int limit);

    JobMonitorSummaryVO selectMonitorSummary(@Param("windowStart") LocalDateTime windowStart,
                                             @Param("windowEnd") LocalDateTime windowEnd);

    Long selectJobConfigIdById(@Param("id") Long id);

    JobFailureAlarmContextVO selectFailureAlarmContext(@Param("id") Long id);

    int completeRunningExecution(@Param("id") Long id,
                                 @Param("status") Integer status,
                                 @Param("result") String result,
                                 @Param("exceptionMsg") String exceptionMsg,
                                 @Param("retryCount") Integer retryCount);

    int completeRunningFlowExecution(@Param("id") Long id,
                                     @Param("status") Integer status,
                                     @Param("result") String result,
                                     @Param("processInstanceId") String processInstanceId,
                                     @Param("retryCount") Integer retryCount);

    int cleanPhysicalBefore(@Param("beforeDate") LocalDateTime beforeDate);

    int refreshHeartbeat(@Param("id") Long id);

    int failStaleExecutions(@Param("cutoff") LocalDateTime cutoff,
                            @Param("exceptionMsg") String exceptionMsg);

    JobOpenApiExecutionVO selectOpenExecutionById(
            @Param("id") Long id,
            @Param("jobIds") Set<Long> jobIds,
            @Param("jobGroups") Set<String> jobGroups);

    int countOpenExecutionById(@Param("id") Long id);

    int startReservedExecution(
            @Param("id") Long id,
            @Param("scheduledFireTime") LocalDateTime scheduledFireTime,
            @Param("fireInstanceId") String fireInstanceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("executorHandler") String executorHandler,
            @Param("jobParam") String jobParam);

    int failAcceptedExecution(
            @Param("id") Long id,
            @Param("exceptionMsg") String exceptionMsg);
}
