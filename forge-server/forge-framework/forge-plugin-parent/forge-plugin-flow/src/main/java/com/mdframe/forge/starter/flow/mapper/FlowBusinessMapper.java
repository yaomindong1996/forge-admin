package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.vo.FlowMonitorDailyStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 流程业务 Mapper
 */
@Mapper
public interface FlowBusinessMapper extends BaseMapper<FlowBusiness> {
    
    /**
     * 根据流程实例ID查询业务信息
     */
    FlowBusiness selectByProcessInstanceId(@Param("processInstanceId") String processInstanceId);

    FlowBusiness selectByProcessInstanceIdAndTenantId(@Param("processInstanceId") String processInstanceId,
                                                      @Param("tenantId") Long tenantId);

    FlowBusiness selectByProcessInstanceIdAndTenantIdForUpdate(
            @Param("processInstanceId") String processInstanceId,
            @Param("tenantId") Long tenantId);
    
    /**
     * 根据业务Key查询业务信息
     */
    FlowBusiness selectByBusinessKey(@Param("businessKey") String businessKey);

    /**
     * 根据租户和业务Key查询业务信息。
     */
    FlowBusiness selectByBusinessKeyAndTenantId(@Param("tenantId") Long tenantId,
                                                @Param("businessKey") String businessKey);

    IPage<FlowBusiness> selectBusinessPage(Page<FlowBusiness> page,
                                           @Param("processDefKey") String processDefKey,
                                           @Param("status") String status,
                                           @Param("title") String title,
                                           @Param("applyUserId") String applyUserId);

    IPage<FlowBusiness> selectMonitorBusinessPage(Page<FlowBusiness> page,
                                                  @Param("tenantId") Long tenantId,
                                                  @Param("processName") String processName,
                                                  @Param("initiator") String initiator,
                                                  @Param("status") String status,
                                                  @Param("modelKey") String modelKey,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    List<FlowBusiness> selectBusinessesForCleanup(@Param("tenantId") Long tenantId,
                                                  @Param("processName") String processName,
                                                  @Param("initiator") String initiator,
                                                  @Param("status") String status,
                                                  @Param("modelKey") String modelKey,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    List<FlowMonitorDailyStatVO> selectDailyTrend(@Param("tenantId") Long tenantId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    List<Map<String, Object>> selectProcessDistribution(@Param("tenantId") Long tenantId);

    Map<String, Object> selectMonitorStatistics(@Param("tenantId") Long tenantId,
                                                @Param("startOfDay") LocalDateTime startOfDay);

    int updateStatusByProcessInstanceId(@Param("processInstanceId") String processInstanceId,
                                        @Param("status") String status,
                                        @Param("tenantId") Long tenantId);

    int deleteByProcessInstanceIdPhysically(@Param("processInstanceId") String processInstanceId,
                                            @Param("tenantId") Long tenantId);

    int deleteBusinessRecordsWithoutProcessInstance(@Param("ids") Collection<String> ids,
                                                    @Param("tenantId") Long tenantId);
}
