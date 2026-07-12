package com.mdframe.forge.plugin.capability.flowaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.capability.flowaction.domain.AiCapabilityFlowActionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FlowActionExecutionLogMapper extends BaseMapper<AiCapabilityFlowActionLog> {

    AiCapabilityFlowActionLog selectByIdempotency(
            @Param("tenantId") Long tenantId,
            @Param("clientId") Long clientId,
            @Param("capabilityId") Long capabilityId,
            @Param("operation") String operation,
            @Param("idempotencyKey") String idempotencyKey);

    int updateResultByIdentity(@Param("log") AiCapabilityFlowActionLog log);
}
