package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.dto.SysUserDTO;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.entity.SysTenant;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserTenant;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理访问与租户边界策略。
 */
@RequiredArgsConstructor
final class SysUserAccessPolicy {

    private static final String[] USER_MANAGEMENT_PERMISSIONS = {
        "system:user:list", "system:user:query", "system:user:add", "system:user:edit", "system:user:remove",
        "system:org:list", "system:org:query", "system:role:list", "system:role:query"
    };

    private final SysUserMapper userMapper;
    private final SysUserTenantMapper userTenantMapper;
    private final SysTenantMapper tenantMapper;
    private final SysUserAssignmentPolicy assignmentPolicy;

    void normalizeUserQueryTenant(SysUserQuery query) {
        LoginUser loginUser = requireLoginUser();
        assertUserManagementAllowed(loginUser);
        query.setTenantId(resolveWriteTenantId(null));
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

    Long resolveCurrentTenantIdForNonAdmin() {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.getTenantId() == null) {
            throw new RuntimeException("用户未登录");
        }
        return loginUser.getTenantId();
    }

    List<Long> resolveWriteTenantIds(List<Long> requestedTenantIds,
                                     Long defaultTenantId,
                                     LoginUser loginUser) {
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        if (!loginUser.isAdmin()) {
            return List.of(resolveCurrentTenantIdForNonAdmin());
        }
        List<Long> tenantIds = normalizeTenantIdList(requestedTenantIds);
        if (tenantIds.isEmpty()) {
            tenantIds.add(defaultTenantId != null ? defaultTenantId : loginUser.getTenantId());
        }
        if (defaultTenantId != null && !tenantIds.contains(defaultTenantId)) {
            throw new RuntimeException("默认租户必须包含在所属租户中");
        }
        tenantIds.forEach(this::validateTenantEnabled);
        return tenantIds;
    }

    List<Long> normalizeTenantIdList(List<Long> tenantIds) {
        if (tenantIds == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(tenantIds.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    Long resolveDefaultTenantId(List<Long> tenantIds, Long requestedDefaultTenantId) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            throw new RuntimeException("租户不能为空");
        }
        if (requestedDefaultTenantId != null) {
            if (!tenantIds.contains(requestedDefaultTenantId)) {
                throw new RuntimeException("默认租户必须包含在所属租户中");
            }
            return requestedDefaultTenantId;
        }
        return tenantIds.get(0);
    }

    Long resolveRoleBindTenantId(SysUser user, Long requestedTenantId) {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            Long tenantId = requestedTenantId != null
                ? requestedTenantId
                : (user.getTenantId() != null ? user.getTenantId() : loginUser.getTenantId());
            validateTenantEnabled(tenantId);
            return tenantId;
        }
        return resolveCurrentTenantIdForNonAdmin();
    }

    void validateUserTypeForWrite(SysUserDTO dto) {
        LoginUser loginUser = requireLoginUser();
        Integer userType = dto.getUserType();
        if (!loginUser.isAdmin() && userType != null && userType != SystemConstants.UserType.NORMAL_USER) {
            throw new RuntimeException("租户管理员只能维护普通用户");
        }
    }

    Integer resolveWriteUserType(Integer requestedUserType) {
        LoginUser loginUser = requireLoginUser();
        return loginUser.isAdmin()
            ? (requestedUserType != null ? requestedUserType : SystemConstants.UserType.NORMAL_USER)
            : SystemConstants.UserType.NORMAL_USER;
    }

    void validateTenantEnabled(Long tenantId) {
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

    void assertCanManageUser(Long userId) {
        assertUserAccess(userId, false);
    }

    void assertCanReadUser(Long userId) {
        assertUserAccess(userId, true);
    }

    Long resolveTenantScopedOperationTenantId(Long userId, Long requestedTenantId) {
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return resolveTenantScopedOperationTenantId(user, requestedTenantId);
    }

    Long resolveTenantScopedOperationTenantId(SysUser user, Long requestedTenantId) {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            Long tenantId = requestedTenantId != null
                ? requestedTenantId
                : (user.getTenantId() != null ? user.getTenantId() : loginUser.getTenantId());
            validateTenantEnabled(tenantId);
            return tenantId;
        }
        return resolveCurrentTenantIdForNonAdmin();
    }

    boolean isUserInTenant(Long userId, Long tenantId) {
        if (userId == null || tenantId == null) {
            return false;
        }
        Long count = TenantContextHolder.executeIgnore(() ->
            userTenantMapper.selectCount(new LambdaQueryWrapper<SysUserTenant>()
                .eq(SysUserTenant::getUserId, userId)
                .eq(SysUserTenant::getTenantId, tenantId)
                .eq(SysUserTenant::getStatus, 1)));
        return count != null && count > 0;
    }

    boolean hasEnabledTenantMembership(Long userId) {
        if (userId == null) {
            return false;
        }
        Long count = TenantContextHolder.executeIgnore(() ->
            userTenantMapper.selectCount(new LambdaQueryWrapper<SysUserTenant>()
                .eq(SysUserTenant::getUserId, userId)
                .eq(SysUserTenant::getStatus, 1)));
        return count != null && count > 0;
    }

    void assertUserManagementAllowed() {
        assertUserManagementAllowed(requireLoginUser());
    }

    void assertNotSelfManagementUnlessAdmin(Long userId) {
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin() && isCurrentLoginUser(userId, loginUser)) {
            throw new RuntimeException("不能在用户管理中维护当前登录用户");
        }
    }

    /**
     * 删除用户红线：禁止删当前登录账号、禁止删系统管理员（userType=0）。
     */
    void assertCanDeleteUser(Long userId) {
        LoginUser loginUser = requireLoginUser();
        assertCanManageUser(userId);
        if (isCurrentLoginUser(userId, loginUser)) {
            throw new RuntimeException("不能删除当前登录用户");
        }
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (Objects.equals(user.getUserType(), SystemConstants.UserType.SYSTEM_ADMIN)) {
            throw new RuntimeException("不能删除系统管理员");
        }
    }

    boolean isCurrentLoginUser(Long userId, LoginUser loginUser) {
        return userId != null && loginUser != null && Objects.equals(userId, loginUser.getUserId());
    }

    private void assertUserAccess(Long userId, boolean readOnly) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        LoginUser loginUser = requireLoginUser();
        assertUserManagementAllowed(loginUser);
        if (loginUser.isAdmin()) {
            return;
        }
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (Objects.equals(user.getUserType(), SystemConstants.UserType.SYSTEM_ADMIN)) {
            throw new RuntimeException("无权操作超级管理员");
        }
        if (!isUserInTenant(userId, loginUser.getTenantId())) {
            throw new RuntimeException("无权操作非本租户用户");
        }
        if (readOnly && isCurrentLoginUser(userId, loginUser)) {
            return;
        }
        if (assignmentPolicy.resolveEffectiveUserType(userId, loginUser.getTenantId())
            != SystemConstants.UserType.NORMAL_USER) {
            throw new RuntimeException(readOnly ? "租户管理员只能查看普通用户" : "租户管理员只能维护普通用户");
        }
    }

    private void assertUserManagementAllowed(LoginUser loginUser) {
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        if (loginUser.isAdmin() || loginUser.isTenantAdmin()) {
            return;
        }
        if (!hasAnyPermission(loginUser, USER_MANAGEMENT_PERMISSIONS)) {
            throw new RuntimeException("无权访问用户组织管理功能");
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
            if (userPermissions.contains(permission.substring(0, splitIndex) + ":*")) {
                return true;
            }
            splitIndex = permission.lastIndexOf(':', splitIndex - 1);
        }
        return false;
    }
}
