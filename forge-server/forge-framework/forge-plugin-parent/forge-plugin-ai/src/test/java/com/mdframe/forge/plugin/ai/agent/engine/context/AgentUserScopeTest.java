package com.mdframe.forge.plugin.ai.agent.engine.context;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentUserScopeTest {

    @AfterEach
    void clearFixtureContext() {
        ExecutionIdentityContextHolder.clear();
        TenantContextHolder.clear();
        DynamicDataSourceContextHolder.clear();
        MDC.clear();
        assertThat(LoginUserContextHolder.current()).isNull();
    }

    @Test
    void should_copyPermissionsAndRestoreOuterScope_when_nestedUsersFinish() {
        LoginUser user = user(31L, 7L);
        user.setPermissions(new HashSet<>(Set.of("fixture:read")));
        try (var outer = LoginUserContextHolder.open(user)) {
            user.getPermissions().clear();
            assertThat(SessionHelper.getPermissions()).containsExactly("fixture:read");
            SessionHelper.getLoginUser().setUserId(999L);
            assertThat(SessionHelper.getUserId()).isEqualTo(31L);
            try (var inner = LoginUserContextHolder.open(user(41L, 9L))) {
                assertThat(SessionHelper.getUserId()).isEqualTo(41L);
            }
            assertThat(SessionHelper.getUserId()).isEqualTo(31L);
        }
    }

    @Test
    void should_keepMachineIdentityFirst_when_taskUserScopeAlsoExists() {
        LoginUser machineUser = user(51L, 7L);
        var identity = new ExecutionIdentity(machineUser, "SERVICE", 51L, 51L, 61L,
                "fixture-client", "fixture-token", Set.of("fixture:invoke"));
        try (var task = LoginUserContextHolder.open(user(31L, 7L));
             var machine = ExecutionIdentityContextHolder.open(identity)) {
            assertThat(SessionHelper.getUserId()).isEqualTo(51L);
            SessionHelper.getLoginUser().setUserId(999L);
            assertThat(SessionHelper.getUserId()).isEqualTo(51L);
        }
    }

    @Test
    void should_restoreUserTenantDatasourceAndMdc_when_decoratedTaskThrows() {
        Runnable task;
        LoginUser caller = user(31L, 7L);
        try (var ignored = LoginUserContextHolder.open(caller)) {
            TenantContextHolder.setTenantId(7L);
            DynamicDataSourceContextHolder.push("fixture-ds");
            MDC.put("fixture", "request");
            task = new AgentExecutionTaskDecorator().decorate(() -> {
                assertThat(SessionHelper.getUserId()).isEqualTo(31L);
                assertThat(TenantContextHolder.getTenantId()).isEqualTo(7L);
                assertThat(TenantContextHolder.isIgnore()).isFalse();
                assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("fixture-ds");
                assertThat(MDC.get("fixture")).isEqualTo("request");
                throw new IllegalStateException("fixture failure");
            });
        }
        TenantContextHolder.setTenantId(9L);
        TenantContextHolder.setIgnore(true);
        DynamicDataSourceContextHolder.clear();
        DynamicDataSourceContextHolder.push("outer-ds");
        MDC.put("fixture", "outer");
        try (var ignored = LoginUserContextHolder.open(user(41L, 9L))) {
            assertThatThrownBy(task::run).hasMessage("fixture failure");
            assertThat(SessionHelper.getUserId()).isEqualTo(41L);
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(9L);
            assertThat(TenantContextHolder.isIgnore()).isTrue();
            assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("outer-ds");
            assertThat(MDC.get("fixture")).isEqualTo("outer");
        }
    }

    @Test
    void should_rejectAnonymousDecoration_when_noVerifiedCallerExists() {
        var called = new AtomicBoolean();
        assertThatThrownBy(() -> new AgentExecutionTaskDecorator().decorate(() -> called.set(true)))
                .hasMessageContaining("登录");
        assertThat(called).isFalse();
        assertThatThrownBy(() -> LoginUserContextHolder.open(new LoginUser())).hasMessageContaining("验证");
    }

    @Test
    void should_restoreMachineIdentityAcrossDecoration_when_aTrustedMachineCallsAgent() {
        var identity = new ExecutionIdentity(user(51L, 7L), "SERVICE", 51L, 51L, 61L,
                "fixture-client", "fixture-token", Set.of("fixture:invoke"));
        Runnable decorated;
        try (var ignored = ExecutionIdentityContextHolder.open(identity)) {
            TenantContextHolder.setTenantId(7L);
            decorated = new AgentExecutionTaskDecorator().decorate(() -> {
                assertThat(ExecutionIdentityContextHolder.current()).containsSame(identity);
                assertThat(SessionHelper.getUserId()).isEqualTo(51L);
                assertThat(TenantContextHolder.getTenantId()).isEqualTo(7L);
            });
        }
        decorated.run();
        assertThat(ExecutionIdentityContextHolder.current()).isEmpty();
        assertThat(LoginUserContextHolder.current()).isNull();
    }

    private LoginUser user(Long userId, Long tenantId) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setTenantId(tenantId);
        return user;
    }
}
