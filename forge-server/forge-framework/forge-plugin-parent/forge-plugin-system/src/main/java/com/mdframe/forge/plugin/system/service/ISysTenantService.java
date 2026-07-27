package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.system.dto.SysTenantDTO;
import com.mdframe.forge.plugin.system.dto.SysTenantQuery;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.entity.SysTenant;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.vo.SysUserTenantVO;
import com.mdframe.forge.starter.core.session.LoginUser;

import java.util.List;

/**
 * 租户Service接口
 */
public interface ISysTenantService extends IService<SysTenant> {

    /**
     * 分页查询租户列表
     *
     * @param query 查询条件
     * @return 租户分页列表
     */
    IPage<SysTenant> selectTenantPage(SysTenantQuery query);

    /**
     * 根据ID查询租户详情
     *
     * @param id 租户ID
     * @return 租户详情
     */
    SysTenant selectTenantById(Long id);
    
    /**
     * 查询用户当前租户的配置
     * @param tenantId 租户ID
     * @return 租户配置
     */
    SysTenant selectUserTenantConfig(Long tenantId);

    /**
     * 查询登录页可公开展示的启用租户。
     */
    SysTenant selectEnabledTenantForLogin(Long tenantId);

    /**
     * 查询当前用户可访问租户
     */
    List<SysUserTenantVO> selectCurrentUserTenants();

    /**
     * 查询当前用户可分配租户选项
     */
    List<SysTenant> selectAssignableTenantOptions();

    /**
     * 分页查询租户下用户列表
     */
    IPage<SysUser> selectTenantUsers(Long tenantId, SysUserQuery query);

    /**
     * 将用户移出租户。
     */
    boolean removeTenantUser(Long tenantId, Long userId);

    /**
     * 切换当前登录租户
     */
    LoginUser switchTenant(Long tenantId);

    /**
     * 新增租户
     *
     * @param dto 租户信息
     * @return 是否成功
     */
    boolean insertTenant(SysTenantDTO dto);

    /**
     * 修改租户
     *
     * @param dto 租户信息
     * @return 是否成功
     */
    boolean updateTenant(SysTenantDTO dto);

    /**
     * 删除租户
     *
     * @param id 租户ID
     * @return 是否成功
     */
    boolean deleteTenantById(Long id);

    /**
     * 批量删除租户
     *
     * @param ids 租户ID数组
     * @return 是否成功
     */
    boolean deleteTenantByIds(Long[] ids);
}
