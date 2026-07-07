package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.RoleUserQuery;
import com.mdframe.forge.plugin.system.dto.SysRoleQuery;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

/**
 * 角色Mapper接口
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 分页查询角色列表
     */
    IPage<SysRole> selectRolePage(Page<SysRole> page, @Param("query") SysRoleQuery query);

    /**
     * 查询角色详情
     */
    SysRole selectRoleById(@Param("id") Long id);

    /**
     * 分页查询角色下的用户
     */
    IPage<SysUser> selectRoleUsers(Page<SysUser> page, @Param("query") RoleUserQuery query);

    /**
     * 查询角色绑定用户数量。
     */
    Long countUsersByRole(@Param("roleId") Long roleId, @Param("tenantId") Long tenantId);

    /**
     * 统计可用于角色分配的目标用户数量。
     */
    Long countAssignableTargetUsers(@Param("userIds") Collection<Long> userIds, @Param("tenantId") Long tenantId);

    /**
     * 统计非普通目标用户数量。
     */
    Long countNonNormalTargetUsers(@Param("userIds") Collection<Long> userIds, @Param("tenantId") Long tenantId);

    /**
     * 统计目标用户中超出指定数据范围上限的数量。
     */
    Long countUsersExceedingDataScope(@Param("dataScope") Integer dataScope,
            @Param("userIds") Collection<Long> userIds,
            @Param("tenantId") Long tenantId);

    /**
     * 统计角色已绑定用户中超出指定数据范围上限的数量。
     */
    Long countRoleUsersExceedingDataScope(@Param("dataScope") Integer dataScope,
            @Param("roleId") Long roleId,
            @Param("tenantId") Long tenantId);
}
