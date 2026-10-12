package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.system.dto.RoleUserQuery;
import com.mdframe.forge.plugin.system.dto.RoleDataScopeSettingsDTO;
import com.mdframe.forge.plugin.system.dto.ScopedRolePermissionDTO;
import com.mdframe.forge.plugin.system.dto.SysRoleDTO;
import com.mdframe.forge.plugin.system.dto.SysRoleQuery;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.vo.RoleDataScopeSettingsVO;

import java.util.List;

/**
 * 角色Service接口
 */
public interface ISysRoleService extends IService<SysRole> {

    /**
     * 分页查询角色列表
     *
     * @param query 查询条件
     * @return 角色分页列表
     */
    IPage<SysRole> selectRolePage(SysRoleQuery query);

    /**
     * 根据ID查询角色详情
     *
     * @param id 角色ID
     * @return 角色详情
     */
    SysRole selectRoleById(Long id);

    /**
     * 新增角色
     *
     * @param dto 角色信息
     * @return 是否成功
     */
    boolean insertRole(SysRoleDTO dto);

    /**
     * 修改角色
     *
     * @param dto 角色信息
     * @return 是否成功
     */
    boolean updateRole(SysRoleDTO dto);

    /**
     * 删除角色
     *
     * @param id 角色ID
     * @return 是否成功
     */
    boolean deleteRoleById(Long id);

    /**
     * 批量删除角色
     *
     * @param ids 角色ID数组
     * @return 是否成功
     */
    boolean deleteRoleByIds(Long[] ids);

    /**
     * 给角色绑定资源（菜单/按钮/接口）
     *
     * @param roleId 角色ID
     * @param resourceIds 资源ID数组
     * @return 是否成功
     */
    boolean bindRoleResources(Long roleId, Long[] resourceIds);

    /**
     * 给角色绑定指定客户端下的资源（菜单/按钮/接口）
     *
     * @param roleId 角色ID
     * @param resourceIds 资源ID数组
     * @param clientCode 客户端编码
     * @return 是否成功
     */
    boolean bindRoleResources(Long roleId, Long[] resourceIds, String clientCode);

    /**
     * 解除角色资源
     *
     * @param roleId 角色ID
     * @param resourceIds 资源ID数组
     * @return 是否成功
     */
    boolean unbindRoleResources(Long roleId, Long[] resourceIds);

    /**
     * 查询角色的资源ID列表
     *
     * @param roleId 角色ID
     * @return 资源ID列表
     */
    List<Long> selectRoleResourceIds(Long roleId);

    /**
     * 查询角色指定客户端下的资源ID列表
     *
     * @param roleId 角色ID
     * @param clientCode 客户端编码
     * @return 资源ID列表
     */
    List<Long> selectRoleResourceIds(Long roleId, String clientCode);

    /**
     * 查询角色指定客户端下的资源ID列表。
     *
     * @param roleId 角色ID
     * @param clientCode 客户端编码
     * @param includeParents 是否包含父级资源ID
     * @return 资源ID列表
     */
    List<Long> selectRoleResourceIds(Long roleId, String clientCode, boolean includeParents);

    /**
     * 查询角色默认及业务模块数据范围。
     */
    RoleDataScopeSettingsVO getRoleDataScopeSettings(Long roleId);

    /**
     * 保存角色默认及业务模块数据范围。
     */
    boolean saveRoleDataScopeSettings(Long roleId, RoleDataScopeSettingsDTO settings);

    /**
     * 只替换指定资源和业务模块范围内的角色授权。
     */
    boolean saveScopedRolePermissions(Long roleId, ScopedRolePermissionDTO settings);

    /**
     * 查询角色适用组织ID列表。
     *
     * @param roleId 角色ID
     * @return 组织ID列表
     */
    List<Long> selectRoleOrgIds(Long roleId);

    /**
     * 保存角色适用组织。
     *
     * @param roleId 角色ID
     * @param orgIds 组织ID列表
     * @return 是否成功
     */
    boolean bindRoleOrgs(Long roleId, List<Long> orgIds);

    /**
     * 查询当前用户的角色ID列表
     *
     * @return 角色ID列表
     */
    List<Long> selectCurrentUserRoleIds();

    /**
     * 查询角色下的用户列表（分页）
     *
     * @param query 查询条件
     * @return 用户分页列表
     */
    IPage<SysUser> selectRoleUsers(RoleUserQuery query);

    /**
     * 移除角色用户
     *
     * @param roleId 角色ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean removeUserRole(Long roleId, Long userId);

    /**
     * 批量添加用户到角色
     *
     * @param roleId  角色ID
     * @param userIds 用户ID列表
     * @return 是否成功
     */
    boolean addUsersToRole(Long roleId, List<Long> userIds);
}
