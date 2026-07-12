package com.mdframe.forge.plugin.capability.controlplane.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityInvocationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiCapabilityInvocationLogMapper extends BaseMapper<AiCapabilityInvocationLog> {

    int insertIdempotent(@Param("log") AiCapabilityInvocationLog log);

    int updateResultByRequestIdentity(@Param("log") AiCapabilityInvocationLog log);

    Page<AiCapabilityInvocationLog> selectPage(Page<AiCapabilityInvocationLog> page,
                                               @Param("tenantId") Long tenantId,
                                               @Param("clientId") Long clientId,
                                               @Param("capabilityCode") String capabilityCode,
                                               @Param("resultCode") String resultCode);

    AiCapabilityInvocationLog selectByRequestId(@Param("tenantId") Long tenantId,
                                                @Param("requestId") String requestId);
}
