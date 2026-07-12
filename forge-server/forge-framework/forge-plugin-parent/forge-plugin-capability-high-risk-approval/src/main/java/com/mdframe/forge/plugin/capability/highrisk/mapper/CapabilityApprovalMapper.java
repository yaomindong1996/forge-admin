package com.mdframe.forge.plugin.capability.highrisk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.capability.highrisk.domain.AiCapabilityApproval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CapabilityApprovalMapper extends BaseMapper<AiCapabilityApproval> {
    AiCapabilityApproval selectByIdempotency(@Param("tenantId") Long tenantId,
                                             @Param("clientId") Long clientId,
                                             @Param("capabilityId") Long capabilityId,
                                             @Param("idempotencyKey") String idempotencyKey);
    AiCapabilityApproval selectForUpdate(@Param("tenantId") Long tenantId,
                                         @Param("id") Long id);
    AiCapabilityApproval selectTenantById(@Param("tenantId") Long tenantId,
                                          @Param("id") Long id);
    AiCapabilityApproval selectOwned(@Param("tenantId") Long tenantId,
                                     @Param("id") Long id,
                                     @Param("clientId") Long clientId,
                                     @Param("actorUserId") Long actorUserId,
                                     @Param("serviceUserId") Long serviceUserId,
                                     @Param("activeOrgId") Long activeOrgId);
    int updateState(@Param("approval") AiCapabilityApproval approval);
}
