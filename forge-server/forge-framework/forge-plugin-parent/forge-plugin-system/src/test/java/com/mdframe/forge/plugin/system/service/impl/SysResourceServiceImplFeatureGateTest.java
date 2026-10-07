package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.entity.SysRoleResource;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.starter.auth.domain.UserResourceTreeVO;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.plugin.feature.CommunityFeatureGate;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;

import static com.mdframe.forge.plugin.system.service.impl.ResourceFeatureTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SysResourceServiceImplFeatureGateTest {

    private final SysResourceMapper resources = mock(SysResourceMapper.class);
    private final SysRoleResourceMapper bindings = mock(SysRoleResourceMapper.class);

    @BeforeAll
    static void metadata() {
        initializeMetadata(SysResource.class, SysRoleResource.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldFilterMenuForAdminAndNormalUserWithoutChangingOrder(boolean admin) {
        List<SysResource> source = List.of(resource(3, 2, null), resource(1, 2, "ee.workflow"),
                resource(5, 2, "community.hello"), resource(2, 2, ""), resource(4, 2, "  ee.report"));
        allowResources(source);
        try (MockedStatic<SessionHelper> session = session(user(admin))) {
            assertThat(service(new CommunityFeatureGate()).selectCurrentUserMenuTree())
                    .extracting(UserResourceTreeVO::getId).containsExactly(3L, 5L, 2L);
        }
        // 过滤不得修改 Mapper 返回集合或原实体，后台配置仍需要看到未授权资源。
        assertThat(source).hasSize(5);
        assertThat(source.get(1).getFeatureCode()).isEqualTo("ee.workflow");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldFilterFullResourceTreeIncludingButtonsAndApi(boolean admin) {
        allowResources(List.of(resource(1, 1, null), resource(2, 2, "ee.menu"),
                resource(3, 3, "ee.button"), resource(4, 4, "ee.api"), resource(5, 3, null)));
        try (MockedStatic<SessionHelper> session = session(user(admin))) {
            assertThat(service(new CommunityFeatureGate()).selectCurrentUserResourceTree())
                    .extracting(UserResourceTreeVO::getId).containsExactly(1L, 5L);
        }
    }

    @Test
    void shouldKeepHiddenAuthorizedPageAndFilterNormalButtonPermissionsAndIds() {
        SysResource hidden = resource(1, 2, null);
        hidden.setVisible(0);
        allowResources(List.of(hidden, resource(2, 3, "community.hello"), resource(3, 3, "ee.button"),
                resource(4, 4, "ee.api"), resource(5, 3, "")));
        SysResourceServiceImpl service = service(new CommunityFeatureGate());
        try (MockedStatic<SessionHelper> session = session(user(false))) {
            assertThat(service.selectCurrentUserPermissions())
                    .containsExactly("test:resource:2", "test:resource:5");
            assertThat(service.selectCurrentUserResourceIds()).containsExactly(1L, 2L, 5L);
            assertThat(service.selectCurrentUserMenuTree())
                    .extracting(UserResourceTreeVO::getId).containsExactly(1L);
        }
    }

    @Test
    void shouldBuildAllowedChildrenWithoutPromotingChildrenOfDeniedParent() {
        SysResource allowedChild = resource(3, 2, null);
        allowedChild.setParentId(1L);
        SysResource deniedChild = resource(4, 2, "ee.child");
        deniedChild.setParentId(1L);
        SysResource orphan = resource(5, 2, null);
        orphan.setParentId(2L);
        allowResources(List.of(resource(1, 1, null), resource(2, 1, "ee.parent"),
                allowedChild, deniedChild, orphan));
        try (MockedStatic<SessionHelper> session = session(user(false))) {
            List<UserResourceTreeVO> tree = service(new CommunityFeatureGate()).selectCurrentUserMenuTree();
            assertThat(tree).extracting(UserResourceTreeVO::getId).containsExactly(1L);
            assertThat(tree.get(0).getChildren()).extracting(UserResourceTreeVO::getId).containsExactly(3L);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"pc", "h5", "app"})
    void shouldKeepRoleTenantClientAndUserTypeQueryScopes(String client) {
        allowResources(List.of(resource(1, 2, "ee.menu")));
        LoginUser user = user(false);
        user.setUserClient(client);
        try (MockedStatic<SessionHelper> session = session(user)) {
            assertThat(service(new CommunityFeatureGate()).selectCurrentUserMenuTree()).isEmpty();
        }
        var roleCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(bindings).selectList(roleCaptor.capture());
        assertRoleScope(roleCaptor.getValue());
        var resourceCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(resources).selectList(resourceCaptor.capture());
        Wrapper<?> query = resourceCaptor.getValue();
        assertThat(query.getSqlSegment()).contains("id IN", "client_code =", "client_code IS NULL",
                "min_user_type IS NULL", "min_user_type >=", "sort ASC", "create_time DESC");
        assertThat(((AbstractWrapper<?, ?, ?>) query).getParamNameValuePairs().values())
                .contains(client, "", 2, 1L);
    }

    @Test
    void shouldDefaultAdminClientToPcAndKeepWildcardPermissions() {
        allowResources(List.of(resource(1, 2, "ee.menu")));
        try (MockedStatic<SessionHelper> session = session(user(true))) {
            SysResourceServiceImpl service = service(new CommunityFeatureGate());
            assertThat(service.selectCurrentUserMenuTree()).isEmpty();
            assertThat(service.selectCurrentUserPermissions()).containsExactly("*", "*:*:*");
        }
        var captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(resources).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("client_code =", "client_code IS NULL");
        assertThat(((AbstractWrapper<?, ?, ?>) captor.getValue()).getParamNameValuePairs().values())
                .contains("pc");
        verifyNoInteractions(bindings);
    }

    @Test
    void shouldUseCustomerGateInsteadOfHardcodedEditionRules() {
        FeatureGate gate = mock(FeatureGate.class);
        when(gate.isEnabled("ee.allowed")).thenReturn(true);
        allowResources(List.of(resource(1, 2, "ee.allowed"), resource(2, 2, "community.denied")));
        try (MockedStatic<SessionHelper> session = session(user(false))) {
            assertThat(service(gate).selectCurrentUserMenuTree())
                    .extracting(UserResourceTreeVO::getId).containsExactly(1L);
        }
        verify(gate).isEnabled("community.denied");
    }

    @Test
    void shouldNotReturnResourcesWhenGateFails() {
        FeatureGate gate = mock(FeatureGate.class);
        when(gate.isEnabled("ee.menu")).thenThrow(new IllegalStateException("gate unavailable"));
        allowResources(List.of(resource(1, 2, "ee.menu")));
        try (MockedStatic<SessionHelper> session = session(user(true))) {
            assertThatThrownBy(() -> service(gate).selectCurrentUserMenuTree())
                    .isInstanceOf(IllegalStateException.class).hasMessage("gate unavailable");
        }
    }

    @Test
    void shouldReturnEmptyWithoutQueryingResourcesWhenUserHasNoRolesOrBindings() {
        LoginUser user = user(false);
        user.setRoleIds(List.of());
        try (MockedStatic<SessionHelper> session = session(user)) {
            assertThat(service(new CommunityFeatureGate()).selectCurrentUserMenuTree()).isEmpty();
        }
        verifyNoInteractions(resources, bindings);
        when(bindings.selectList(any(Wrapper.class))).thenReturn(List.of());
        try (MockedStatic<SessionHelper> session = session(user(false))) {
            assertThat(service(new CommunityFeatureGate()).selectCurrentUserPermissions()).isEmpty();
        }
        verifyNoInteractions(resources);
    }

    @Test
    void shouldPreserveUnauthenticatedBehavior() {
        try (MockedStatic<SessionHelper> session = session(null)) {
            SysResourceServiceImpl service = service(new CommunityFeatureGate());
            assertThatThrownBy(service::selectCurrentUserMenuTree).hasMessage("用户未登录");
            assertThatThrownBy(service::selectCurrentUserResourceTree).hasMessage("用户未登录");
            assertThatThrownBy(service::selectCurrentUserPermissions).hasMessage("用户未登录");
            assertThat(service.selectCurrentUserResourceIds()).isEmpty();
        }
        verifyNoInteractions(resources, bindings);
    }

    private SysResourceServiceImpl service(FeatureGate gate) {
        return new SysResourceServiceImpl(resources, mock(SysUserRoleMapper.class), bindings,
                mock(PermissionServiceImpl.class), gate);
    }

    private void allowResources(List<SysResource> source) {
        when(resources.selectList(any(Wrapper.class))).thenReturn(source);
        when(bindings.selectList(any(Wrapper.class))).thenReturn(source.stream()
                .map(item -> binding(item.getId())).toList());
    }

    private MockedStatic<SessionHelper> session(LoginUser user) {
        MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getLoginUser).thenReturn(user);
        return session;
    }
}
