package com.mdframe.forge.plugin.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.job.dto.JobConfigQuery;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.plugin.job.model.JobApiTriggerTarget;
import com.mdframe.forge.plugin.job.vo.JobApiResourceOptionVO;
import com.mdframe.forge.plugin.job.vo.JobFailureTaskVO;
import com.mdframe.forge.plugin.job.vo.JobConfigVO;
import com.mdframe.forge.plugin.job.vo.JobOpenApiSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 任务配置Mapper
 */
@Mapper
public interface SysJobConfigMapper extends BaseMapper<SysJobConfig> {

    Page<JobConfigVO> selectJobPage(Page<JobConfigVO> page, @Param("query") JobConfigQuery query);

    JobConfigVO selectJobDetail(@Param("id") Long id);

    List<SysJobConfig> selectRecoveryCandidates();

    SysJobConfig selectByJobKey(@Param("jobName") String jobName, @Param("jobGroup") String jobGroup);

    int updateSyncState(@Param("id") Long id,
                        @Param("expectedVersion") Integer expectedVersion,
                        @Param("syncStatus") String syncStatus,
                        @Param("syncError") String syncError,
                        @Param("syncTime") LocalDateTime syncTime);

    int logicalDeleteByVersion(@Param("id") Long id,
                               @Param("expectedVersion") Integer expectedVersion);

    int markOnceMissedCompleted(@Param("id") Long id,
                                @Param("fireOnceTime") LocalDateTime fireOnceTime,
                                @Param("timezone") String timezone,
                                @Param("expectedVersion") Integer expectedVersion,
                                @Param("syncTime") LocalDateTime syncTime);

    int markOnceCompleted(@Param("id") Long id,
                          @Param("fireOnceTime") LocalDateTime fireOnceTime,
                          @Param("timezone") String timezone);

    int applyExecutionOutcome(@Param("executionId") Long executionId,
                              @Param("status") Integer status);

    int selectConsecutiveFailureTaskCount();

    List<JobFailureTaskVO> selectConsecutiveFailureTasks(@Param("limit") int limit);

    Page<JobOpenApiSummaryVO> selectOpenJobPage(
            Page<JobOpenApiSummaryVO> page,
            @Param("jobIds") Set<Long> jobIds,
            @Param("jobGroups") Set<String> jobGroups);

    JobOpenApiSummaryVO selectOpenJobById(
            @Param("id") Long id,
            @Param("jobIds") Set<Long> jobIds,
            @Param("jobGroups") Set<String> jobGroups);

    JobApiTriggerTarget selectOpenTriggerTarget(
            @Param("id") Long id,
            @Param("jobIds") Set<Long> jobIds,
            @Param("jobGroups") Set<String> jobGroups);

    int countOpenJobById(@Param("id") Long id);

    List<JobApiResourceOptionVO> selectJobApiResourceOptions();
}
