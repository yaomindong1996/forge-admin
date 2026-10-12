package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.mdframe.forge.plugin.system.dto.BatchUserRoleBindDTO;
import com.mdframe.forge.plugin.system.dto.BatchUserTenantBindDTO;
import com.mdframe.forge.plugin.system.dto.SysUserDTO;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.dto.UserTenantBindDTO;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.vo.UserOrgBindingVO;
import com.mdframe.forge.plugin.system.vo.SysUserTenantVO;

import java.util.List;

/**
 * 用户Service接口
 */
public interface ISysUserService extends IService<SysUser> {

    /**
     * 分页查询用户列表
     *
     * @param query 查询条件
     * @return 用户分页列表
     */
    IPage<SysUser> selectUserPage(SysUserQuery query);

    /**
     * 导出查询用户列表（不分页）
     *
     * @param query 查询条件
     * @return 用户列表
     */
    List<SysUser> selectExportList(SysUserQuery query);

    /**
     * 根据ID查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    SysUser selectUserById(Long id);

    /**
     * 新增用户
     *
     * @param dto 用户信息
     * @return 是否成功
     */
    boolean insertUser(SysUserDTO dto);

    /**
     * 修改用户
     *
     * @param dto 用户信息
     * @return 是否成功
     */
    boolean updateUser(SysUserDTO dto);

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否成功
     */
    boolean deleteUserById(Long id);

    /**
     * 批量删除用户
     *
     * @param ids 用户ID数组
     * @return 是否成功
     */
    boolean deleteUserByIds(Long[] ids);

    /**
     * 给用户绑定角色
     *
     * @param userId 用户ID
     * @param roleIds 角色ID数组
     * @return 是否成功
     */
    boolean bindUserRoles(Long userId, Long[] roleIds);

    /**
     * 给用户绑定指定租户下的角色
     *
     * @param userId 用户ID
     * @param roleIds 角色ID数组
     * @param tenantId 操作租户ID
     * @return 是否成功
     */
    boolean bindUserRoles(Long userId, Long[] roleIds, Long tenantId);

    /**
     * 批量给用户追加指定租户下的角色。
     */
    boolean batchBindUserRoles(BatchUserRoleBindDTO dto);

    /**
     * 解除用户角色
     *
     * @param userId 用户ID
     * @param roleIds 角色ID数组
     * @return 是否成功
     */
    boolean unbindUserRoles(Long userId, Long[] roleIds);

    /**
     * 给用户绑定组织
     *
     * @param userId 用户ID
     * @param orgId 组织ID
     * @param isMain 是否主组织
     * @return 是否成功
     */
    boolean bindUserOrg(Long userId, Long orgId, Integer isMain);

    /**
     * 解除用户组织
     *
     * @param userId 用户ID
     * @param orgId 组织ID
     * @return 是否成功
     */
    boolean unbindUserOrg(Long userId, Long orgId);

    /**
     * 查询用户的角色ID列表
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> selectUserRoleIds(Long userId);

    /**
     * 查询用户指定租户下的角色ID列表
     *
     * @param userId 用户ID
     * @param tenantId 操作租户ID
     * @return 角色ID列表
     */
    List<Long> selectUserRoleIds(Long userId, Long tenantId);

    /**
     * 查询用户的组织ID列表
     *
     * @param userId 用户ID
     * @return 组织ID列表
     */
    List<Long> selectUserOrgIds(Long userId);

    /**
     * 查询用户指定租户下的组织ID列表
     *
     * @param userId 用户ID
     * @param tenantId 操作租户ID
     * @return 组织ID列表
     */
    List<Long> selectUserOrgIds(Long userId, Long tenantId);

    /**
     * 查询用户组织绑定详情。
     *
     * @param userId 用户ID
     * @param tenantId 操作租户ID
     * @return 组织绑定详情
     */
    List<UserOrgBindingVO> selectUserOrgBindings(Long userId, Long tenantId);

    /**
     * 查询用户在指定组织下的角色。
     *
     * @param userId 用户ID
     * @param orgId 组织ID
     * @param tenantId 操作租户ID
     * @return 角色ID列表
     */
    List<Long> selectUserOrgRoleIds(Long userId, Long orgId, Long tenantId);

    /**
     * 保存用户在指定组织下的角色。
     *
     * @param userId 用户ID
     * @param orgId 组织ID
     * @param roleIds 角色ID列表
     * @param tenantId 操作租户ID
     * @return 是否成功
     */
    boolean bindUserOrgRoles(Long userId, Long orgId, List<Long> roleIds, Long tenantId);

    /**
     * 查询用户绑定租户
     */
    List<SysUserTenantVO> selectUserTenants(Long userId);

    /**
     * 批量绑定用户租户
     */
    boolean bindUserTenants(Long userId, UserTenantBindDTO dto);

    /**
     * 批量将用户加入目标租户。
     */
    boolean batchBindUserTenant(BatchUserTenantBindDTO dto);
    
    /**
     * 批量绑定用户组织
     *
     * @param userId 用户ID
     * @param orgIds 组织ID列表
     * @param mainOrgId 主组织ID
     * @return 是否成功
     */
    boolean bindUserOrgs(Long userId, List<Long> orgIds, Long mainOrgId);

    /**
     * 批量绑定用户指定租户下的组织
     *
     * @param userId 用户ID
     * @param orgIds 组织ID列表
     * @param mainOrgId 主组织ID
     * @param tenantId 操作租户ID
     * @return 是否成功
     */
    boolean bindUserOrgs(Long userId, List<Long> orgIds, Long mainOrgId, Long tenantId);
    
    /**
     * 用户解封
     *
     * @param userId 用户ID
     */
    void doUntieDisable(Long userId);

    /**
     * 重置用户密码
     *
     * @param userId      用户ID
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean resetPassword(Long userId, String newPassword);

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateUserStatus(Long userId, Integer status);

    /**
     * 更新用户基本资料
     *
     * @param dto 用户信息
     * @return 是否成功
     */
    boolean updateUserProfile(SysUserDTO dto);

    /**
     * 查询用户的岗位ID列表
     *
     * @param userId 用户ID
     * @return 岗位ID列表
     */
    List<Long> selectUserPostIds(Long userId);

    /**
     * 查询用户指定租户下的岗位ID列表
     *
     * @param userId 用户ID
     * @param tenantId 操作租户ID
     * @return 岗位ID列表
     */
    List<Long> selectUserPostIds(Long userId, Long tenantId);

    /**
     * 批量绑定用户岗位
     *
     * @param userId     用户ID
     * @param postIds    岗位ID列表
     * @param mainPostId 主岗位ID
     * @return 是否成功
     */
    boolean bindUserPosts(Long userId, List<Long> postIds, Long mainPostId);

    /**
     * 批量绑定用户指定租户下的岗位
     *
     * @param userId     用户ID
     * @param postIds    岗位ID列表
     * @param mainPostId 主岗位ID
     * @param tenantId   操作租户ID
     * @return 是否成功
     */
    boolean bindUserPosts(Long userId, List<Long> postIds, Long mainPostId, Long tenantId);
}
