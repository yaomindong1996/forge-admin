package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mdframe.forge.plugin.system.entity.*;
import com.mdframe.forge.plugin.system.mapper.*;
import com.mdframe.forge.starter.auth.service.ICaptchaService;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.plugin.autoconfigure.PluginAutoConfiguration;
import com.mdframe.forge.starter.plugin.feature.CommunityFeatureGate;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static com.mdframe.forge.plugin.system.service.impl.ResourceFeatureTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 真实 Spring 装配和公开用户加载路径；Mapper 全部为桩，不使用 Sa-Token/Redis 或真实库。 */
class UserLoadServiceImplFeatureGateIntegrationTest {

    @BeforeAll
    static void metadata() {
        initializeMetadata(SysResource.class, SysRoleResource.class, SysUserTenant.class,
                SysUserOrg.class, SysRole.class);
    }

    @Test
    void shouldWireDefaultGateIntoBothServicesAndFilterPublicLoginPath() {
        Fixture fixture = new Fixture();
        fixture.runner().run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(FeatureGate.class);
            FeatureGate gate = context.getBean(FeatureGate.class);
            assertThat(gate).isInstanceOf(CommunityFeatureGate.class);
            UserLoadServiceImpl loader = context.getBean(UserLoadServiceImpl.class);
            assertThat(ReflectionTestUtils.getField(loader, "featureGate")).isSameAs(gate);
            assertThat(ReflectionTestUtils.getField(context.getBean(SysResourceServiceImpl.class), "featureGate"))
                    .isSameAs(gate);
            LoginUser login = loader.loadUserByUserId(91L, 17L);
            assertThat(login.getPermissions()).containsExactly("test:resource:1");
            assertThat(login.getApiPermissions()).containsExactly("GET /community/info");
            assertThat(login.getTenantId()).isEqualTo(17L);
            assertThat(login.getRoleIds()).containsExactly(30L);
            assertThat(login.getActiveOrgId()).isEqualTo(41L);
        });
        verify(fixture.resources).selectApiPermissionPatternsByResourceIds(List.of(3L));
    }

    @Test
    void shouldUseCustomerGateForBothServicesWithoutDuplicateDefaultBean() {
        Fixture fixture = new Fixture();
        FeatureGate gate = mock(FeatureGate.class);
        when(gate.isEnabled("ee.button")).thenReturn(true);
        when(gate.isEnabled("ee.api")).thenReturn(true);
        fixture.runner().withBean(FeatureGate.class, () -> gate).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(FeatureGate.class);
            assertThat(context.getBean(FeatureGate.class)).isSameAs(gate);
            LoginUser login = context.getBean(UserLoadServiceImpl.class).loadUserByUserId(91L, 17L);
            assertThat(login.getPermissions()).containsExactly("test:resource:2");
            assertThat(login.getApiPermissions()).containsExactly("GET /ee/info");
        });
        verify(fixture.resources).selectApiPermissionPatternsByResourceIds(List.of(4L));
    }

    @Test
    void shouldStopLoginWhenGateFailsAndRestorePreviousTenantContext() {
        Fixture fixture = new Fixture();
        FeatureGate gate = mock(FeatureGate.class);
        when(gate.isEnabled("community.button")).thenThrow(new IllegalStateException("gate unavailable"));
        Long previousTenant = TenantContextHolder.getTenantId();
        TenantContextHolder.executeWithTenant(73L, () -> {
            fixture.runner().withBean(FeatureGate.class, () -> gate).run(context -> {
                assertThat(context).hasNotFailed();
                assertThatThrownBy(() -> context.getBean(UserLoadServiceImpl.class).loadUserByUserId(91L, 17L))
                        .isInstanceOf(IllegalStateException.class).hasMessage("gate unavailable");
                assertThat(TenantContextHolder.getTenantId()).isEqualTo(73L);
            });
        });
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(previousTenant);
    }

    @Configuration(proxyBeanMethods = false)
    @Import({SysResourceServiceImpl.class, UserLoadServiceImpl.class, PermissionServiceImpl.class})
    static class Services {
    }

    private static final class Fixture {
        private final SysUserMapper users = mock(SysUserMapper.class);
        private final SysUserOrgRoleMapper orgRoles = mock(SysUserOrgRoleMapper.class);
        private final SysRoleMapper roles = mock(SysRoleMapper.class);
        private final SysUserOrgMapper orgMemberships = mock(SysUserOrgMapper.class);
        private final SysUserTenantMapper memberships = mock(SysUserTenantMapper.class);
        private final SysTenantMapper tenants = mock(SysTenantMapper.class);
        private final SysRoleResourceMapper bindings = mock(SysRoleResourceMapper.class);
        private final SysResourceMapper resources = mock(SysResourceMapper.class);
        private final SysOrgMapper orgs = mock(SysOrgMapper.class);

        Fixture() {
            stubIdentityAndWorkspace();
            stubResourceSnapshots();
        }

        ApplicationContextRunner runner() {
            return new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(PluginAutoConfiguration.class))
                    .withUserConfiguration(Services.class)
                    .withBean(SysUserMapper.class, () -> users)
                    .withBean(SysUserRoleMapper.class, () -> mock(SysUserRoleMapper.class))
                    .withBean(SysUserOrgRoleMapper.class, () -> orgRoles)
                    .withBean(SysRoleMapper.class, () -> roles)
                    .withBean(SysUserOrgMapper.class, () -> orgMemberships)
                    .withBean(SysUserTenantMapper.class, () -> memberships)
                    .withBean(SysTenantMapper.class, () -> tenants)
                    .withBean(SysRoleResourceMapper.class, () -> bindings)
                    .withBean(SysResourceMapper.class, () -> resources)
                    .withBean(ICaptchaService.class, () -> mock(ICaptchaService.class))
                    .withBean(SysOrgMapper.class, () -> orgs)
                    .withBean(SysRegionMapper.class, () -> mock(SysRegionMapper.class));
        }

        private void stubIdentityAndWorkspace() {
            SysUser user = new SysUser();
            user.setId(91L);
            user.setUserType(2);
            user.setTenantId(17L);
            when(users.selectById(91L)).thenReturn(user);
            SysTenant tenant = new SysTenant();
            tenant.setId(17L);
            tenant.setTenantStatus(1);
            when(tenants.selectById(17L)).thenReturn(tenant);
            SysUserTenant member = new SysUserTenant();
            member.setTenantId(17L);
            member.setMemberType(2);
            when(memberships.selectOne(any(Wrapper.class))).thenReturn(member);
            when(memberships.selectList(any(Wrapper.class))).thenReturn(List.of(member));
            SysUserOrg orgMember = new SysUserOrg();
            orgMember.setOrgId(41L);
            orgMember.setIsMain(1);
            when(orgMemberships.selectList(any(Wrapper.class))).thenReturn(List.of(orgMember));
            SysOrg org = new SysOrg();
            org.setId(41L);
            when(orgs.selectById(41L)).thenReturn(org);
            when(orgRoles.selectActiveRoleIdsByUserOrg(17L, 91L, 41L)).thenReturn(List.of(30L));
            SysRole role = new SysRole();
            role.setId(30L);
            role.setRoleKey("t3-reviewer");
            when(roles.selectList(any(Wrapper.class))).thenReturn(List.of(role));
        }

        private void stubResourceSnapshots() {
            when(bindings.selectList(any(Wrapper.class)))
                    .thenReturn(List.of(binding(1), binding(2), binding(3), binding(4)));
            when(resources.selectList(any(Wrapper.class))).thenReturn(
                    List.of(resource(1, 3, "community.button"), resource(2, 3, "ee.button")),
                    List.of(resource(3, 4, "community.api"), resource(4, 4, "ee.api")));
            when(resources.selectApiPermissionPatternsByResourceIds(List.of(3L)))
                    .thenReturn(List.of("GET /community/info"));
            when(resources.selectApiPermissionPatternsByResourceIds(List.of(4L)))
                    .thenReturn(List.of("GET /ee/info"));
        }
    }
}
