package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessProcessRun;
import com.mdframe.forge.plugin.generator.dto.businessprocess.BusinessProcessRunQueryDTO;
import com.mdframe.forge.plugin.generator.vo.businessprocess.BusinessProcessRunVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface BusinessProcessRunMapper extends BaseMapper<AiBusinessProcessRun> {

    Page<BusinessProcessRunVO> selectRunPage(Page<BusinessProcessRunVO> page,
                                              @Param("tenantId") Long tenantId,
                                              @Param("query") BusinessProcessRunQueryDTO query);

    AiBusinessProcessRun selectRunById(@Param("tenantId") Long tenantId,
                                        @Param("runId") Long runId);

    AiBusinessProcessRun selectByIdempotencyKey(@Param("tenantId") Long tenantId,
                                                 @Param("processVersionId") Long processVersionId,
                                                 @Param("idempotencyKey") String idempotencyKey);

    AiBusinessProcessRun selectWaitingByProcessInstanceId(
            @Param("tenantId") Long tenantId,
            @Param("processInstanceId") String processInstanceId);

    AiBusinessProcessRun selectByProcessInstanceId(@Param("tenantId") Long tenantId,
                                                    @Param("processInstanceId") String processInstanceId);

    AiBusinessProcessRun selectLatestByBusinessKey(
            @Param("tenantId") Long tenantId,
            @Param("businessKey") String businessKey);

    /** 列表/详情打印：按业务单据取最近一次已挂流程实例的运行记录。 */
    AiBusinessProcessRun selectLatestBySubject(
            @Param("tenantId") Long tenantId,
            @Param("applicationId") Long applicationId,
            @Param("objectCode") String objectCode,
            @Param("recordId") String recordId);

    List<AiBusinessProcessRun> selectActiveByBusinessKeys(
            @Param("tenantId") Long tenantId,
            @Param("businessKeys") Collection<String> businessKeys);

    List<AiBusinessProcessRun> selectStartedByBusinessKeys(
            @Param("tenantId") Long tenantId,
            @Param("businessKeys") Collection<String> businessKeys);

    List<AiBusinessProcessRun> selectRecoverableRuns(@Param("tenantId") Long tenantId,
                                                      @Param("before") LocalDateTime before,
                                                      @Param("limit") Integer limit);

    Long countByProcessId(@Param("tenantId") Long tenantId,
                          @Param("processId") Long processId);

    int compareAndSetStatus(@Param("tenantId") Long tenantId,
                            @Param("runId") Long runId,
                            @Param("expectedStatus") String expectedStatus,
                            @Param("expectedCurrentNodeId") String expectedCurrentNodeId,
                            @Param("expectedProcessInstanceId") String expectedProcessInstanceId,
                            @Param("nextStatus") String nextStatus,
                            @Param("currentNodeId") String currentNodeId,
                            @Param("processInstanceId") String processInstanceId,
                            @Param("nextRetryTime") LocalDateTime nextRetryTime,
                            @Param("errorCode") String errorCode,
                            @Param("errorSummary") String errorSummary);

    int retryFailed(@Param("tenantId") Long tenantId,
                    @Param("runId") Long runId,
                    @Param("maxRetryCount") Integer maxRetryCount,
                    @Param("updateBy") Long updateBy);
}
