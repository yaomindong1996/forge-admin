package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationPublishRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BusinessApplicationPublishRunMapper extends BaseMapper<AiBusinessApplicationPublishRun> {

    Long lockApplication(@Param("tenantId") Long tenantId,
                         @Param("applicationId") Long applicationId);

    AiBusinessApplicationPublishRun selectByIdempotencyKey(@Param("tenantId") Long tenantId,
                                                            @Param("applicationId") Long applicationId,
                                                            @Param("idempotencyKey") String idempotencyKey);

    AiBusinessApplicationPublishRun selectRunById(@Param("tenantId") Long tenantId,
                                                   @Param("applicationId") Long applicationId,
                                                   @Param("runId") Long runId);

    List<AiBusinessApplicationPublishRun> selectRuns(@Param("tenantId") Long tenantId,
                                                     @Param("applicationId") Long applicationId,
                                                     @Param("limit") Integer limit);

    Integer selectMaxTargetVersionNo(@Param("tenantId") Long tenantId,
                                     @Param("applicationId") Long applicationId);

    int claimCreated(@Param("tenantId") Long tenantId,
                     @Param("applicationId") Long applicationId,
                     @Param("runId") Long runId,
                     @Param("startedBy") Long startedBy);

    int updateProgress(@Param("tenantId") Long tenantId,
                       @Param("applicationId") Long applicationId,
                       @Param("runId") Long runId,
                       @Param("runStatus") String runStatus,
                       @Param("currentStep") String currentStep,
                       @Param("stepResultsJson") String stepResultsJson,
                       @Param("snapshotJson") String snapshotJson,
                       @Param("snapshotHash") String snapshotHash,
                       @Param("resultVersionId") Long resultVersionId,
                       @Param("errorCode") String errorCode,
                       @Param("errorSummary") String errorSummary,
                       @Param("finishedTime") LocalDateTime finishedTime);

    int incrementAttempt(@Param("tenantId") Long tenantId,
                         @Param("applicationId") Long applicationId,
                         @Param("runId") Long runId,
                         @Param("startedBy") Long startedBy);
}
