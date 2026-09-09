package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mdframe.forge.plugin.system.dto.RoleDataScopeSettingsDTO;
import com.mdframe.forge.plugin.system.dto.RoleModuleDataScopeDTO;
import com.mdframe.forge.plugin.system.dto.ScopedRolePermissionDTO;
import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.entity.SysRoleResource;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.plugin.system.service.ISysResourceService;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.entity.SysRoleModuleDataScope;
import com.mdframe.forge.starter.datascope.mapper.SysDataScopeConfigMapper;
import com.mdframe.forge.starter.datascope.mapper.SysRoleModuleDataScopeMapper;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysRoleServiceImplScopedPermissionTest {

    private static final Long ROLE_ID = 7L;
    private static final Long TENANT_ID = 1L;

    @BeforeAll
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"),
                SysRoleResource.class);
    }

    @Test
    void scopedSaveShouldPreserveResourcesAndDataScopesOutsideTheApplication() {
        Dependencies dependencies = new Dependencies();
        SysRoleServiceImpl service = dependencies.service();
        when(dependencies.roleMapper.selectById(ROLE_ID)).thenReturn(role());
        when(dependencies.resourceService.listByIds(anyCollection())).thenAnswer(invocation ->
                resources(invocation.getArgument(0)));
        when(dependencies.roleResourceMapper.selectResourceIdsByRole(TENANT_ID, ROLE_ID))
                .thenReturn(List.of(100L, 200L, 900L));

        ScopedRolePermissionDTO settings = settings(Set.of(100L, 200L), Set.of(200L));
        settings.setScopeModuleCodes(Set.of("ai:business:ORDER"));
        RoleModuleDataScopeDTO moduleScope = new RoleModuleDataScopeDTO();
        moduleScope.setModuleCode("ai:business:ORDER");
        moduleScope.setDataScope(3);
        settings.setModuleScopes(List.of(moduleScope));

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(admin());
            assertThat(service.saveScopedRolePermissions(ROLE_ID, settings)).isTrue();
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<SysRoleResource>> deleteCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(dependencies.roleResourceMapper).delete(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().getSqlSegment()).contains("resource_id IN");

        ArgumentCaptor<List<SysRoleResource>> resourceCaptor = ArgumentCaptor.forClass(List.class);
        verify(dependencies.roleResourceMapper).insertBatch(resourceCaptor.capture());
        assertThat(resourceCaptor.getValue())
                .extracting(SysRoleResource::getResourceId)
                .containsExactly(200L);
        assertThat(resourceCaptor.getValue())
                .extracting(SysRoleResource::getResourceId)
                .doesNotContain(900L);

        verify(dependencies.roleModuleDataScopeMapper)
                .deleteByRoleAndModules(TENANT_ID, ROLE_ID, Set.of("ai:business:ORDER"));
        ArgumentCaptor<List<SysRoleModuleDataScope>> scopeCaptor = ArgumentCaptor.forClass(List.class);
        verify(dependencies.roleModuleDataScopeMapper).insertBatch(scopeCaptor.capture());
        assertThat(scopeCaptor.getValue()).singleElement().satisfies(scope -> {
            assertThat(scope.getModuleCode()).isEqualTo("ai:business:ORDER");
            assertThat(scope.getDataScope()).isEqualTo(3);
        });
        verify(dependencies.dataScopeService).refreshDataScopeCache();
    }

    @Test
    void scopedSaveShouldAllowModuleAllWhenRoleDefaultIsPersonal() {
        Dependencies dependencies = new Dependencies();
        SysRoleServiceImpl service = dependencies.service();
        when(dependencies.roleMapper.selectById(ROLE_ID)).thenReturn(role());
        when(dependencies.roleMapper.countRoleUsersExceedingDataScope(1, ROLE_ID, TENANT_ID)).thenReturn(3L);
        when(dependencies.resourceService.listByIds(anyCollection())).thenAnswer(invocation ->
                resources(invocation.getArgument(0)));
        when(dependencies.roleResourceMapper.selectResourceIdsByRole(TENANT_ID, ROLE_ID))
                .thenReturn(List.of(100L));

        ScopedRolePermissionDTO settings = settings(Set.of(100L), Set.of(100L));
        settings.setScopeModuleCodes(Set.of("ai:business:INSPECTION"));
        RoleModuleDataScopeDTO moduleScope = new RoleModuleDataScopeDTO();
        moduleScope.setModuleCode("ai:business:INSPECTION");
        moduleScope.setDataScope(1);
        settings.setModuleScopes(List.of(moduleScope));

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(admin());
            assertThat(service.saveScopedRolePermissions(ROLE_ID, settings)).isTrue();
        }

        verify(dependencies.roleMapper, never()).countRoleUsersExceedingDataScope(any(), any(), any());
        ArgumentCaptor<List<SysRoleModuleDataScope>> scopeCaptor = ArgumentCaptor.forClass(List.class);
        verify(dependencies.roleModuleDataScopeMapper).insertBatch(scopeCaptor.capture());
        assertThat(scopeCaptor.getValue()).singleElement().satisfies(scope -> {
            assertThat(scope.getModuleCode()).isEqualTo("ai:business:INSPECTION");
            assertThat(scope.getDataScope()).isEqualTo(1);
        });
    }

    @Test
    void dataScopeSaveShouldAllowModuleAllWhenRoleDefaultIsPersonal() {
        Dependencies dependencies = new Dependencies();
        SysRoleServiceImpl service = dependencies.service();
        when(dependencies.roleMapper.selectById(ROLE_ID)).thenReturn(role());
        when(dependencies.roleMapper.countRoleUsersExceedingDataScope(5, ROLE_ID, TENANT_ID)).thenReturn(0L);
        when(dependencies.roleMapper.countRoleUsersExceedingDataScope(1, ROLE_ID, TENANT_ID)).thenReturn(3L);
        when(dependencies.roleMapper.updateById(any(SysRole.class))).thenReturn(1);
        SysDataScopeConfig config = new SysDataScopeConfig();
        config.setResourceCode("ai:business:INSPECTION");
        when(dependencies.dataScopeConfigMapper.selectEnabledModuleConfigs(TENANT_ID)).thenReturn(List.of(config));

        RoleDataScopeSettingsDTO settings = new RoleDataScopeSettingsDTO();
        settings.setDefaultDataScope(5);
        RoleModuleDataScopeDTO moduleScope = new RoleModuleDataScopeDTO();
        moduleScope.setModuleCode("ai:business:INSPECTION");
        moduleScope.setDataScope(1);
        settings.setModuleScopes(List.of(moduleScope));

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(admin());
            assertThat(service.saveRoleDataScopeSettings(ROLE_ID, settings)).isTrue();
        }

        verify(dependencies.roleMapper).countRoleUsersExceedingDataScope(5, ROLE_ID, TENANT_ID);
        verify(dependencies.roleMapper, never()).countRoleUsersExceedingDataScope(eq(1), any(), any());
        ArgumentCaptor<SysRoleModuleDataScope> scopeCaptor = ArgumentCaptor.forClass(SysRoleModuleDataScope.class);
        verify(dependencies.roleModuleDataScopeMapper).insert(scopeCaptor.capture());
        assertThat(scopeCaptor.getValue().getModuleCode()).isEqualTo("ai:business:INSPECTION");
        assertThat(scopeCaptor.getValue().getDataScope()).isEqualTo(1);
    }

    @Test
    void scopedSaveShouldRejectAResourceOutsideTheDeclaredApplicationScope() {
        Dependencies dependencies = new Dependencies();
        SysRoleServiceImpl service = dependencies.service();
        when(dependencies.roleMapper.selectById(ROLE_ID)).thenReturn(role());
        ScopedRolePermissionDTO settings = settings(Set.of(100L), Set.of(999L));

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(admin());
            assertThatThrownBy(() -> service.saveScopedRolePermissions(ROLE_ID, settings))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("授权范围以外");
        }

        verify(dependencies.roleResourceMapper, never()).delete(any());
        verify(dependencies.roleResourceMapper, never()).insertBatch(any());
        verify(dependencies.roleModuleDataScopeMapper, never()).deleteByRoleAndModules(any(), any(), any());
    }

    private static ScopedRolePermissionDTO settings(Set<Long> scopeIds, Set<Long> selectedIds) {
        ScopedRolePermissionDTO settings = new ScopedRolePermissionDTO();
        settings.setClientCode("pc");
        settings.setScopeResourceIds(scopeIds);
        settings.setSelectedResourceIds(selectedIds);
        return settings;
    }

    private static List<SysResource> resources(Collection<Long> ids) {
        return ids.stream().map(id -> {
            SysResource resource = new SysResource();
            resource.setId(id);
            resource.setParentId(0L);
            resource.setClientCode("pc");
            return resource;
        }).toList();
    }

    private static SysRole role() {
        SysRole role = new SysRole();
        role.setId(ROLE_ID);
        role.setTenantId(TENANT_ID);
        role.setDataScope(5);
        return role;
    }

    private static LoginUser admin() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setTenantId(TENANT_ID);
        loginUser.setUserType(0);
        return loginUser;
    }

    private static final class Dependencies {
        private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        private final SysRoleOrgMapper roleOrgMapper = mock(SysRoleOrgMapper.class);
        private final SysRoleResourceMapper roleResourceMapper = mock(SysRoleResourceMapper.class);
        private final SysTenantMapper tenantMapper = mock(SysTenantMapper.class);
        private final SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        private final SysUserOrgRoleMapper userOrgRoleMapper = mock(SysUserOrgRoleMapper.class);
        private final SysOrgMapper orgMapper = mock(SysOrgMapper.class);
        private final SysUserMapper userMapper = mock(SysUserMapper.class);
        private final SysUserTenantMapper userTenantMapper = mock(SysUserTenantMapper.class);
        private final SysResourceMapper resourceMapper = mock(SysResourceMapper.class);
        private final SysDataScopeConfigMapper dataScopeConfigMapper = mock(SysDataScopeConfigMapper.class);
        private final SysRoleModuleDataScopeMapper roleModuleDataScopeMapper =
                mock(SysRoleModuleDataScopeMapper.class);
        private final IDataScopeService dataScopeService = mock(IDataScopeService.class);
        private final ISysResourceService resourceService = mock(ISysResourceService.class);

        private SysRoleServiceImpl service() {
            return new SysRoleServiceImpl(
                    roleMapper,
                    roleOrgMapper,
                    roleResourceMapper,
                    tenantMapper,
                    userRoleMapper,
                    userOrgRoleMapper,
                    orgMapper,
                    userMapper,
                    userTenantMapper,
                    resourceMapper,
                    dataScopeConfigMapper,
                    roleModuleDataScopeMapper,
                    dataScopeService,
                    resourceService);
        }
    }
}
