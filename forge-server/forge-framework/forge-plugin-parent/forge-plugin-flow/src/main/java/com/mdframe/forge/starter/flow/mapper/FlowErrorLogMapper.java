package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.entity.FlowErrorLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 流程运行错误日志 Mapper
 */
@Mapper
public interface FlowErrorLogMapper extends BaseMapper<FlowErrorLog> {

    IPage<FlowErrorLog> selectErrorLogPage(Page<FlowErrorLog> page,
                                           @Param("tenantId") Long tenantId,
                                           @Param("processInstanceId") String processInstanceId,
                                           @Param("activityId") String activityId,
                                           @Param("status") Integer status);

    List<FlowErrorLog> selectRecentByProcessInstanceId(@Param("tenantId") Long tenantId,
                                                       @Param("processInstanceId") String processInstanceId);

    FlowErrorLog selectLatestUnresolved(@Param("tenantId") Long tenantId,
                                        @Param("processInstanceId") String processInstanceId,
                                        @Param("activityId") String activityId);

    Long countUnresolvedByProcessInstanceId(@Param("tenantId") Long tenantId,
                                            @Param("processInstanceId") String processInstanceId);

    Map<String, Object> selectStatistics(@Param("tenantId") Long tenantId);

    FlowErrorLog selectByIdAndTenantId(@Param("logId") String logId,
                                       @Param("tenantId") Long tenantId);

    FlowErrorLog selectByIdAndTenantIdForUpdate(@Param("logId") String logId,
                                                @Param("tenantId") Long tenantId);

    int resolveByIdAndTenantId(@Param("logId") String logId,
                               @Param("tenantId") Long tenantId,
                               @Param("userId") String userId,
                               @Param("message") String message);

    int updateRetryState(@Param("logId") String logId,
                         @Param("tenantId") Long tenantId,
                         @Param("status") Integer status,
                         @Param("userId") String userId,
                         @Param("message") String message);

    int deleteByProcessInstanceIdPhysically(@Param("processInstanceId") String processInstanceId,
                                            @Param("tenantId") Long tenantId);
}
