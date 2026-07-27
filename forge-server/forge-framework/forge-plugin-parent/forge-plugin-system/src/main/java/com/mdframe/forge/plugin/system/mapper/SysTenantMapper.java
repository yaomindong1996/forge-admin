package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysTenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 租户Mapper接口
 */
@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenant> {

    SysTenant selectEnabledTenantById(@Param("tenantId") Long tenantId);

    /**
     * 查询租户下直接归属用户数量。
     */
    Long countUsersByTenant(@Param("tenantId") Long tenantId);

    /**
     * 查询租户成员绑定数量。
     */
    Long countUserTenantBindings(@Param("tenantId") Long tenantId);
}
