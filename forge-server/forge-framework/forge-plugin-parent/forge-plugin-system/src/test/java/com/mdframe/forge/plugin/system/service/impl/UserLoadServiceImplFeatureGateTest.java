package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.entity.SysRoleResource;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleResourceMapper;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.plugin.feature.CommunityFeatureGate;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static com.mdframe.forge.plugin.system.service.impl.ResourceFeatureTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class UserLoadServiceImplFeatureGateTest {

    private final SysResourceMapper resources = mock(SysResourceMapper.class);
    private final SysRoleResourceMapper bindings = mock(SysRoleResourceMapper.class);

    @BeforeAll
    static void metadata() {
        initializeMetadata(SysResource.class, SysRoleResource.class);
    }

    @Test
    void shouldFilterPermissionsBeforeSavingOrdinaryUserSnapshot() {
        SysResource hidden = resource(1, 2, null);
        hidden.setVisible(0);
        SysResource duplicate = resource(5, 3, "");
        duplicate.setPerms(hidden.getPerms());
        SysResource blank = resource(6, 3, null);
        blank.setPerms("  ");
        allowResources(List.of(hidden, resource(2, 3, "ee.button"), resource(3, 3, "community.hello"),
                resource(4, 4, "ee.api"), duplicate, blank));
        LoginUser user = user(false);
        loadButtons(user, new CommunityFeatureGate());
        assertThat(user.getPermissions()).containsExactlyInAnyOrder("test:resource:1", "test:resource:3");
        assertOriginalRoleScope();
        var captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(resources).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment())
                .contains("id IN", "perms IS NOT NULL", "min_user_type >=")
                .doesNotContain("visible");
    }

    @Test
    void shouldRemoveAllDeniedPermissionsFromSnapshot() {
        allowResources(List.of(resource(1, 3, "ee.only")));
        LoginUser user = user(false);
        user.setPermissions(Set.of("stale:permission"));
        loadButtons(user, new CommunityFeatureGate());
        assertThat(user.getPermissions()).isEmpty();
    }

    @Test
    void shouldQueryApiPatternsUsingOnlyEnabledIdsAndKeepUserTypeBoundary() {
        SysResource hidden = resource(1, 4, null);
        hidden.setVisible(0);
        SysResource restricted = resource(4, 4, "community.hello");
        restricted.setMinUserType(0);
        allowResources(List.of(hidden, resource(2, 4, "ee.api"), resource(3, 4, "community.hello"), restricted));
        when(resources.selectApiPermissionPatternsByResourceIds(List.of(1L, 3L)))
                .thenReturn(List.of("GET /plugin/hello/info", "", "  ", "GET /plugin/hello/info", "/public/**"));
        LoginUser user = user(false);
        loadApi(user, new CommunityFeatureGate());
        assertThat(user.getApiPermissions()).containsExactly("GET /plugin/hello/info", "/public/**");
        verify(resources).selectApiPermissionPatternsByResourceIds(List.of(1L, 3L));
        assertOriginalRoleScope();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ee.only", "  ee.api"})
    void shouldClearApiSnapshotAndAvoidEmptyInQueryWhenAllDisabled(String code) {
        allowResources(List.of(resource(1, 4, code)));
        LoginUser user = user(false);
        user.setApiPermissions(List.of("stale/**"));
        loadApi(user, new CommunityFeatureGate());
        assertThat(user.getApiPermissions()).isEmpty();
        verify(resources, never()).selectApiPermissionPatternsByResourceIds(anyList());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldLeaveNoApiPermissionsWhenPatternsAreEmptyOrNull(boolean returnsNull) {
        allowResources(List.of(resource(1, 4, null)));
        when(resources.selectApiPermissionPatternsByResourceIds(List.of(1L)))
                .thenReturn(returnsNull ? null : List.of());
        LoginUser user = user(false);
        loadApi(user, new CommunityFeatureGate());
        assertThat(user.getApiPermissions()).isEmpty();
    }

    @Test
    void shouldKeepAdministratorWildcardsWithoutQueryingFeatureResources() {
        FeatureGate gate = mock(FeatureGate.class);
        LoginUser user = user(true);
        loadButtons(user, gate);
        loadApi(user, gate);
        assertThat(user.getPermissions()).containsExactlyInAnyOrder("*", "*:*:*");
        assertThat(user.getApiPermissions()).containsExactly("/**");
        verifyNoInteractions(resources, bindings, gate);
    }

    @Test
    void shouldUseSameCustomerGateForButtonsAndApi() {
        FeatureGate gate = mock(FeatureGate.class);
        when(gate.isEnabled("ee.allowed")).thenReturn(true);
        allowResources(List.of(resource(1, 4, "ee.allowed"), resource(2, 4, "community.denied")));
        when(resources.selectApiPermissionPatternsByResourceIds(List.of(1L))).thenReturn(List.of("GET /ee/info"));
        LoginUser user = user(false);
        loadButtons(user, gate);
        loadApi(user, gate);
        assertThat(user.getPermissions()).containsExactly("test:resource:1");
        assertThat(user.getApiPermissions()).containsExactly("GET /ee/info");
        verify(resources).selectApiPermissionPatternsByResourceIds(List.of(1L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"loadUserPermissions", "loadApiPermissions"})
    void shouldPropagateGateFailureWithoutSavingAnAllowedSnapshot(String method) {
        FeatureGate gate = mock(FeatureGate.class);
        when(gate.isEnabled("ee.api")).thenThrow(new IllegalStateException("gate unavailable"));
        allowResources(List.of(resource(1, 4, "ee.api")));
        LoginUser user = user(false);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service(gate), method, user))
                .isInstanceOf(IllegalStateException.class).hasMessage("gate unavailable");
        assertThat(user.getPermissions()).isNull();
        assertThat(user.getApiPermissions()).isNull();
        verify(resources, never()).selectApiPermissionPatternsByResourceIds(anyList());
    }

    @Test
    void shouldNotLoadFeatureResourcesWithoutRolesOrBindings() {
        LoginUser user = user(false);
        user.setRoleIds(List.of());
        loadButtons(user, new CommunityFeatureGate());
        loadApi(user, new CommunityFeatureGate());
        assertThat(user.getApiPermissions()).isEmpty();
        verifyNoInteractions(resources, bindings);
        when(bindings.selectList(any(Wrapper.class))).thenReturn(List.of());
        loadButtons(user(false), new CommunityFeatureGate());
        loadApi(user(false), new CommunityFeatureGate());
        verifyNoInteractions(resources);
    }

    private UserLoadServiceImpl service(FeatureGate gate) {
        return new UserLoadServiceImpl(null, null, null, null, null, null, null, bindings, resources,
                null, null, null, gate);
    }

    private void loadButtons(LoginUser user, FeatureGate gate) {
        ReflectionTestUtils.invokeMethod(service(gate), "loadUserPermissions", user);
    }

    private void loadApi(LoginUser user, FeatureGate gate) {
        ReflectionTestUtils.invokeMethod(service(gate), "loadApiPermissions", user);
    }

    private void allowResources(List<SysResource> source) {
        when(resources.selectList(any(Wrapper.class))).thenReturn(source);
        when(bindings.selectList(any(Wrapper.class)))
                .thenReturn(source.stream().map(item -> binding(item.getId())).toList());
    }

    private void assertOriginalRoleScope() {
        var captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(bindings).selectList(captor.capture());
        assertRoleScope(captor.getValue());
    }
}
