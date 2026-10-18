package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.dto.SysRoleQuery;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.entity.SysTenant;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.enums.DataScopeType;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Access and assignment policy for role-management operations.
 */
@RequiredArgsConstructor
class SysRoleAccessPolicy {

    private static final String[] ROLE_MANAGEMENT_PERMISSIONS = {
            "system:role:list", "system:role:query", "system:role:add", "system:role:edit", "system:role:remove"
    };

    private final SysRoleMapper roleMapper;
    private final SysTenantMapper tenantMapper;
    private final SysOrgMapper orgMapper;

    void normalizeRoleQueryTenant(SysRoleQuery query) {
        LoginUser loginUser = requireLoginUser();
        query.setTenantId(resolveWriteTenantId(null));
        // 租户管理员维护本租户全部角色；普通授权用户只能看到已委派给自己的角色。
        if (!loginUser.isAdmin() && !loginUser.isTenantAdmin()) {
            query.setAccessibleRoleIds(loginUser.getRoleIds() == null
                    ? Collections.emptyList()
                    : loginUser.getRoleIds());
        }
    }

    void assertRoleManagementAllowed() {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin() || loginUser.isTenantAdmin()) {
            return;
        }
        if (!hasAnyPermission(loginUser, ROLE_MANAGEMENT_PERMISSIONS)) {
            throw new RuntimeException("无权访问角色管理功能");
        }
    }

    LoginUser requireLoginUser() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        return loginUser;
    }

    Long resolveWriteTenantId(Long requestedTenantId) {
        LoginUser loginUser = requireLoginUser();
        Long tenantId = loginUser.getTenantId();
        validateTenantEnabled(tenantId);
        return tenantId;
    }

    SysRole loadRoleForAccess(Long roleId) {
        assertRoleManagementAllowed();
        if (roleId == null) {
            throw new RuntimeException("角色ID不能为空");
        }
        SysRole role = TenantContextHolder.executeIgnore(() -> roleMapper.selectById(roleId));
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin() && !Objects.equals(role.getTenantId(), loginUser.getTenantId())) {
            throw new RuntimeException("无权操作非本租户角色");
        }
        if (!loginUser.isAdmin() && !loginUser.isTenantAdmin()
                && (loginUser.getRoleIds() == null || !loginUser.getRoleIds().contains(roleId))) {
            throw new RuntimeException("无权操作未委派给自己的角色");
        }
        return role;
    }

    void validateRoleDeletable(SysRole role) {
        if (role != null && role.getIsSystem() != null && role.getIsSystem() == 1) {
            throw new RuntimeException("系统内置角色不能删除");
        }
        Long userCount = TenantContextHolder.executeIgnore(() ->
                roleMapper.countUsersByRole(role.getId(), role.getTenantId()));
        if (userCount != null && userCount > 0) {
            throw new RuntimeException("当前角色已绑定用户，不能删除");
        }
    }

    /**
     * 内置角色禁止停用、改编码；其它字段仍可由超管维护。
     */
    void assertSystemRoleFieldsMutable(SysRole existing, Integer nextRoleStatus, String nextRoleKey) {
        if (existing == null || existing.getIsSystem() == null || existing.getIsSystem() != 1) {
            return;
        }
        if (nextRoleStatus != null && !EnableStatus.ENABLED.matches(nextRoleStatus)) {
            throw new RuntimeException("系统内置角色不能停用");
        }
        if (StringUtils.isNotBlank(nextRoleKey) && !Objects.equals(nextRoleKey, existing.getRoleKey())) {
            throw new RuntimeException("系统内置角色标识不能修改");
        }
    }

    void assertCanMaintainRole(SysRole role) {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            return;
        }
        if (role.getIsSystem() != null && role.getIsSystem() == 1) {
            throw new RuntimeException("系统内置角色只能由超级管理员维护");
        }
        if (loginUser.getRoleIds() != null && loginUser.getRoleIds().contains(role.getId())) {
            throw new RuntimeException("不能维护自己当前绑定的角色");
        }
    }

    void validateDataScopeAllowedForCurrentUser(Integer dataScope) {
        LoginUser loginUser = requireLoginUser();
        if (!isDataScopeAllowedForUserType(dataScope, normalizeUserType(loginUser.getUserType()))) {
            throw new RuntimeException("不能设置超过当前用户类型上限的数据范围");
        }
    }

    void validateDataScopeAllowedForBoundUsers(SysRole role, Integer dataScope) {
        if (dataScope == null) {
            return;
        }
        Long exceedCount = TenantContextHolder.executeIgnore(() ->
                roleMapper.countRoleUsersExceedingDataScope(dataScope, role.getId(), role.getTenantId()));
        if (exceedCount != null && exceedCount > 0) {
            throw new RuntimeException("角色数据范围超过目标用户类型上限");
        }
    }

    void validateSupportedDataScope(Integer dataScope) {
        if (DataScopeType.getByRoleDataScope(dataScope, false) == null) {
            throw new RuntimeException("不支持的数据权限范围");
        }
    }

    void validateOrgTenant(List<Long> orgIds, Long tenantId) {
        if (CollUtil.isEmpty(orgIds)) {
            return;
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                orgMapper.selectCount(new LambdaQueryWrapper<SysOrg>()
                        .in(SysOrg::getId, orgIds)
                        .eq(SysOrg::getTenantId, tenantId)));
        if (count == null || count != orgIds.size()) {
            throw new RuntimeException("组织不属于当前角色租户");
        }
    }

    private void validateTenantEnabled(Long tenantId) {
        if (tenantId == null) {
            throw new RuntimeException("租户不能为空");
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
                        .eq(SysTenant::getId, tenantId)
                        .eq(SysTenant::getTenantStatus, 1)));
        if (count == null || count == 0) {
            throw new RuntimeException("租户不存在或已禁用");
        }
    }

    private boolean hasAnyPermission(LoginUser loginUser, String... permissions) {
        Set<String> userPermissions = loginUser == null ? null : loginUser.getPermissions();
        if (userPermissions == null || userPermissions.isEmpty() || permissions == null) {
            return false;
        }
        if (userPermissions.contains("*") || userPermissions.contains("*:*:*")) {
            return true;
        }
        for (String permission : permissions) {
            if (hasPermission(userPermissions, permission)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPermission(Set<String> userPermissions, String permission) {
        if (permission == null || userPermissions.contains(permission)) {
            return permission != null;
        }
        int splitIndex = permission.lastIndexOf(':');
        while (splitIndex > 0) {
            String wildcardPermission = permission.substring(0, splitIndex) + ":*";
            if (userPermissions.contains(wildcardPermission)) {
                return true;
            }
            splitIndex = permission.lastIndexOf(':', splitIndex - 1);
        }
        return false;
    }

    private boolean isDataScopeAllowedForUserType(Integer dataScope, int userType) {
        if (dataScope == null || userType == SystemConstants.UserType.SYSTEM_ADMIN) {
            return true;
        }
        if (userType == SystemConstants.UserType.TENANT_ADMIN) {
            return dataScope != SystemConstants.RoleDataScope.ALL;
        }
        return dataScope != SystemConstants.RoleDataScope.ALL
                && dataScope != SystemConstants.RoleDataScope.TENANT;
    }

    private int normalizeUserType(Integer userType) {
        if (userType == null) {
            return SystemConstants.UserType.NORMAL_USER;
        }
        if (userType < SystemConstants.UserType.SYSTEM_ADMIN || userType > SystemConstants.UserType.NORMAL_USER) {
            return SystemConstants.UserType.NORMAL_USER;
        }
        return userType;
    }
}
