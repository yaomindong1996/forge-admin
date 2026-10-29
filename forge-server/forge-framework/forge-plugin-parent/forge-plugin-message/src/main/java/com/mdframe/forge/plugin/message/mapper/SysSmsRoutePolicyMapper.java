package com.mdframe.forge.plugin.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.message.domain.entity.SysSmsRoutePolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysSmsRoutePolicyMapper extends BaseMapper<SysSmsRoutePolicy> {

    @Select("SELECT tenant_id FROM sys_sms_route_policy WHERE ping_enabled = 1")
    List<Long> selectPingEnabledTenantIds();

    @Select("SELECT * FROM sys_sms_route_policy WHERE tenant_id = #{tenantId} LIMIT 1")
    SysSmsRoutePolicy selectByTenantId(@Param("tenantId") Long tenantId);
}
