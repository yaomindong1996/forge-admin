package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysUserOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户-组织关联Mapper接口
 */
@Mapper
public interface SysUserOrgMapper extends BaseMapper<SysUserOrg> {

    /**
     * 查询用户在指定租户下显式绑定的组织ID。
     */
    List<Long> selectOrgIdsByUserTenant(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    /** 流程运行时读取用户主组织，显式绑定租户和有效组织。 */
    SysUserOrg selectFlowMainOrgByUser(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    /** 流程运行时读取用户任一有效组织，显式绑定租户和有效组织。 */
    SysUserOrg selectFlowAnyOrgByUser(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    /** 流程运行时判断用户是否属于租户内有效组织。 */
    Long countFlowUserOrg(@Param("tenantId") Long tenantId,
                          @Param("userId") Long userId,
                          @Param("orgId") Long orgId);

}
