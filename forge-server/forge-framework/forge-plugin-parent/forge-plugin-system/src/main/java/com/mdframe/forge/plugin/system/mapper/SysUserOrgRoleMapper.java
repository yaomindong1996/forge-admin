package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysUserOrgRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户组织内角色授权 Mapper。
 */
@Mapper
public interface SysUserOrgRoleMapper extends BaseMapper<SysUserOrgRole> {

    /**
     * 查询用户在当前组织下仍启用且适用于该组织的角色。
     */
    List<Long> selectActiveRoleIdsByUserOrg(@Param("tenantId") Long tenantId,
                                            @Param("userId") Long userId,
                                            @Param("orgId") Long orgId);

    /**
     * 查询用户在指定组织下的角色ID。
     */
    List<Long> selectUserOrgRoleIds(@Param("tenantId") Long tenantId,
                                    @Param("userId") Long userId,
                                    @Param("orgId") Long orgId);

    /**
     * 查询当前组织内拥有指定角色的用户。
     */
    List<Long> selectUserIdsByRoleIds(@Param("tenantId") Long tenantId,
                                      @Param("orgId") Long orgId,
                                      @Param("roleIds") List<Long> roleIds);

    /** 流程候选组解析：跨当前活动组织读取租户内有效角色成员。 */
    List<Long> selectUserIdsByRoleIdsAcrossOrg(@Param("tenantId") Long tenantId,
                                               @Param("roleIds") List<Long> roleIds);

    /**
     * 查询用户组织角色名称摘要。
     */
    List<String> selectRoleNamesByUserOrg(@Param("tenantId") Long tenantId,
                                          @Param("userId") Long userId,
                                          @Param("orgId") Long orgId);
}
