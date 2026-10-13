package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.dto.SysRoleQuery;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class SysRoleAccessPolicyTest {

    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final SysTenantMapper tenantMapper = mock(SysTenantMapper.class);
    private final SysOrgMapper orgMapper = mock(SysOrgMapper.class);
    private final SysRoleAccessPolicy policy = new SysRoleAccessPolicy(roleMapper, tenantMapper, orgMapper);

    @Test
    void normalizesQueryToLoginTenantAndDelegatedRolesForPermissionWildcard() {
        LoginUser user = user(SystemConstants.UserType.NORMAL_USER, 1L);
        user.setPermissions(Set.of("system:role:*"));
        user.setRoleIds(List.of(7L, 8L));
        when(tenantMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        SysRoleQuery query = new SysRoleQuery();

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(user);
            policy.normalizeRoleQueryTenant(query);
        }

        assertThat(query.getTenantId()).isEqualTo(1L);
        assertThat(query.getAccessibleRoleIds()).containsExactly(7L, 8L);
    }

    @Test
    void rejectsRoleOutsideTheLoginTenantEvenWhenPermissionIsGranted() {
        LoginUser user = user(SystemConstants.UserType.NORMAL_USER, 1L);
        user.setPermissions(Set.of("system:role:query"));
        user.setRoleIds(List.of(7L));
        SysRole foreignRole = role(7L, 2L);
        when(roleMapper.selectById(7L)).thenReturn(foreignRole);

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(user);
            assertThatThrownBy(() -> policy.loadRoleForAccess(7L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("非本租户");
        }
    }

    @Test
    void tenantAdminCannotGrantSystemWideDataScope() {
        LoginUser user = user(SystemConstants.UserType.TENANT_ADMIN, 1L);

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(user);
            assertThatThrownBy(() -> policy.validateDataScopeAllowedForCurrentUser(
                    SystemConstants.RoleDataScope.ALL))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("用户类型上限");
            policy.validateDataScopeAllowedForCurrentUser(SystemConstants.RoleDataScope.TENANT);
        }
    }

    @Test
    void rejectsOrganizationsOutsideTheRoleTenant() {
        when(orgMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> policy.validateOrgTenant(List.of(10L, 11L), 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("组织不属于");
    }

    @Test
    void rejectsDeletingBuiltInSystemRole() {
        SysRole systemRole = role(1L, 1L);
        systemRole.setIsSystem(1);

        assertThatThrownBy(() -> policy.validateRoleDeletable(systemRole))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("系统内置角色不能删除");
    }

    @Test
    void rejectsDisablingBuiltInSystemRole() {
        SysRole systemRole = role(1L, 1L);
        systemRole.setIsSystem(1);
        systemRole.setRoleKey("admin");

        assertThatThrownBy(() -> policy.assertSystemRoleFieldsMutable(systemRole, 0, "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("系统内置角色不能停用");
    }

    private LoginUser user(int userType, Long tenantId) {
        LoginUser user = new LoginUser();
        user.setUserId(100L);
        user.setUserType(userType);
        user.setTenantId(tenantId);
        return user;
    }

    private SysRole role(Long id, Long tenantId) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setTenantId(tenantId);
        return role;
    }
}
