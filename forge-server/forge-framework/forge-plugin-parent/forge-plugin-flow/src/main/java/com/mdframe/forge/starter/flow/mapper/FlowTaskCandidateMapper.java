package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowTaskCandidate;
import com.mdframe.forge.starter.flow.vo.FlowTaskSignRelationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FlowTaskCandidateMapper extends BaseMapper<FlowTaskCandidate> {

    int insertIgnore(FlowTaskCandidate candidate);

    int activate(@Param("tenantId") Long tenantId,
                 @Param("taskId") String taskId,
                 @Param("candidateType") String candidateType,
                 @Param("candidateValue") String candidateValue,
                 @Param("source") String source,
                 @Param("updateTime") java.time.LocalDateTime updateTime);

    int deactivate(@Param("tenantId") Long tenantId,
                   @Param("taskId") String taskId,
                   @Param("candidateType") String candidateType,
                   @Param("candidateValue") String candidateValue,
                   @Param("updateTime") java.time.LocalDateTime updateTime);

    int deactivateWithAudit(@Param("tenantId") Long tenantId,
                            @Param("taskId") String taskId,
                            @Param("candidateType") String candidateType,
                            @Param("candidateValue") String candidateValue,
                            @Param("operatorId") String operatorId,
                            @Param("reason") String reason,
                            @Param("idempotencyKey") String idempotencyKey,
                            @Param("requestDigest") String requestDigest,
                            @Param("updateTime") java.time.LocalDateTime updateTime);

    FlowTaskCandidate selectByIdempotency(@Param("tenantId") Long tenantId,
                                          @Param("taskId") String taskId,
                                          @Param("candidateType") String candidateType,
                                          @Param("idempotencyKey") String idempotencyKey);

    int countActiveByTaskAndValue(@Param("tenantId") Long tenantId,
                                  @Param("taskId") String taskId,
                                  @Param("candidateType") String candidateType,
                                  @Param("candidateValue") String candidateValue);

    java.util.List<FlowTaskSignRelationVO> selectDynamicSignRelations(@Param("tenantId") Long tenantId,
                                                                       @Param("parentTaskId") String parentTaskId);

    FlowTaskCandidate selectActiveDynamicSignRelation(@Param("tenantId") Long tenantId,
                                                      @Param("parentTaskId") String parentTaskId,
                                                      @Param("candidateValue") String candidateValue);
}
