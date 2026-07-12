package com.mdframe.forge.plugin.capability.controlplane.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityGrant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiCapabilityGrantMapper extends BaseMapper<AiCapabilityGrant> {

    Page<AiCapabilityGrant> selectPage(Page<AiCapabilityGrant> page,
                                       @Param("tenantId") Long tenantId,
                                       @Param("clientId") Long clientId,
                                       @Param("capabilityId") Long capabilityId,
                                       @Param("status") String status);

    AiCapabilityGrant selectTenantById(@Param("tenantId") Long tenantId, @Param("id") Long id);

    AiCapabilityGrant selectActiveGrant(@Param("tenantId") Long tenantId,
                                        @Param("clientId") Long clientId,
                                        @Param("capabilityId") Long capabilityId);

    int logicallyRevoke(@Param("tenantId") Long tenantId, @Param("id") Long id);
}
