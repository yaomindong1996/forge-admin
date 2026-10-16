package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.stp.StpLogic;
import com.mdframe.forge.flow.client.FlowTokenAcquisitionException;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaTokenFlowTokenProviderTest {

    @AfterEach
    void clearIdentity() {
        ExecutionIdentityContextHolder.clear();
    }

    @Test
    void shouldIssueShortLivedSaTokenForTrustedMcpUser() {
        StpLogic stpLogic = mock(StpLogic.class);
        SaSession tokenSession = mock(SaSession.class);
        when(stpLogic.createLoginSession(eq(101L), any(SaLoginParameter.class)))
                .thenReturn("delegated-flow-token");
        when(stpLogic.getTokenSessionByToken("delegated-flow-token", true))
                .thenReturn(tokenSession);
        SaTokenFlowTokenProvider provider = new SaTokenFlowTokenProvider(() -> stpLogic);
        ExecutionIdentity identity = identity("USER", 101L);

        try (var ignored = ExecutionIdentityContextHolder.open(identity)) {
            assertThat(provider.getToken()).isEqualTo("delegated-flow-token");
        }

        ArgumentCaptor<SaLoginParameter> loginModel = ArgumentCaptor.forClass(SaLoginParameter.class);
        verify(stpLogic).createLoginSession(eq(101L), loginModel.capture());
        assertThat(loginModel.getValue().getTimeout()).isEqualTo(60L);
        assertThat(loginModel.getValue().getActiveTimeout()).isEqualTo(60L);
        assertThat(loginModel.getValue().getDevice()).startsWith("mcp-flow:301:201:");
        verify(tokenSession).set("loginUser", identity.loginUser());
        verify(tokenSession).set(FlowDelegationSessionVerifier.MARKER_KEY, Boolean.TRUE);
        verify(tokenSession).set(FlowDelegationSessionVerifier.ACTOR_USER_ID_KEY, 101L);
        verify(tokenSession).set(FlowDelegationSessionVerifier.TENANT_ID_KEY, 1L);
        verify(tokenSession).set(FlowDelegationSessionVerifier.ACTIVE_ORG_ID_KEY, 201L);
        verify(tokenSession).set(FlowDelegationSessionVerifier.CLIENT_ID_KEY, 301L);
        verify(stpLogic, never()).getTokenValue();
    }

    @Test
    void shouldFailClosedForServiceExecutionIdentity() {
        StpLogic stpLogic = mock(StpLogic.class);
        SaTokenFlowTokenProvider provider = new SaTokenFlowTokenProvider(() -> stpLogic);

        try (var ignored = ExecutionIdentityContextHolder.open(identity("SERVICE", 999L))) {
            assertThatThrownBy(provider::getToken)
                    .isInstanceOf(FlowTokenAcquisitionException.class)
                    .hasMessage("MCP_FLOW_DELEGATION_TOKEN_UNAVAILABLE");
        }

        verify(stpLogic, never()).createLoginSession(any(), any(SaLoginParameter.class));
    }

    @Test
    void shouldFailClosedWhenSaTokenCreationReturnsBlankToken() {
        StpLogic stpLogic = mock(StpLogic.class);
        when(stpLogic.createLoginSession(eq(101L), any(SaLoginParameter.class))).thenReturn(" ");
        SaTokenFlowTokenProvider provider = new SaTokenFlowTokenProvider(() -> stpLogic);

        try (var ignored = ExecutionIdentityContextHolder.open(identity("USER", 101L))) {
            assertThatThrownBy(provider::getToken)
                    .isInstanceOf(FlowTokenAcquisitionException.class)
                    .hasMessage("MCP_FLOW_DELEGATION_TOKEN_UNAVAILABLE");
        }

        verify(stpLogic, never()).getTokenSessionByToken(any(), eq(true));
    }

    @Test
    void shouldPassThroughBrowserTokenWithoutIssuingDelegation() {
        // 浏览器在线会话必须直接透传现有 token：常规页面操作高频发生，
        // 任何签发都会持续挤占账号会话池，堆积到 max-login-count 后
        // logoutByMaxLoginCount 会把最早的浏览器会话挤下线
        StpLogic stpLogic = mock(StpLogic.class);
        when(stpLogic.isLogin()).thenReturn(true);
        when(stpLogic.getTokenValue()).thenReturn("interactive-token");
        SaTokenFlowTokenProvider provider = new SaTokenFlowTokenProvider(() -> stpLogic);

        assertThat(provider.getToken()).isEqualTo("interactive-token");
        verify(stpLogic, never()).createLoginSession(any(), any(SaLoginParameter.class));
    }

    @Test
    void shouldReturnNullWhenNoIdentityAndNotLoggedIn() {
        StpLogic stpLogic = mock(StpLogic.class);
        when(stpLogic.isLogin()).thenReturn(false);
        SaTokenFlowTokenProvider provider = new SaTokenFlowTokenProvider(() -> stpLogic);

        assertThat(provider.getToken()).isNull();
        verify(stpLogic, never()).createLoginSession(any(), any(SaLoginParameter.class));
    }

    @Test
    void shouldReuseDelegatedTokenWithinRefreshWindowAndRotateNearExpiry() {
        // 同一身份在复用窗口内不得重复 createLoginSession（否则会挤占账号会话池上限，
        // 触发 logoutByMaxLoginCount 把浏览器会话挤下线）；临近过期才换发新 Token
        StpLogic stpLogic = mock(StpLogic.class);
        SaSession tokenSession = mock(SaSession.class);
        when(stpLogic.createLoginSession(eq(101L), any(SaLoginParameter.class)))
                .thenReturn("delegated-token-1", "delegated-token-2");
        when(stpLogic.getTokenSessionByToken(any(), eq(true))).thenReturn(tokenSession);
        AtomicLong clock = new AtomicLong();
        SaTokenFlowTokenProvider provider = new SaTokenFlowTokenProvider(() -> stpLogic, clock::get);
        ExecutionIdentity identity = identity("USER", 101L);

        try (var ignored = ExecutionIdentityContextHolder.open(identity)) {
            assertThat(provider.getToken()).isEqualTo("delegated-token-1");
            clock.set(30_000L);
            assertThat(provider.getToken()).isEqualTo("delegated-token-1");
            clock.set(55_000L);
            assertThat(provider.getToken()).isEqualTo("delegated-token-2");
        }

        verify(stpLogic, times(2)).createLoginSession(eq(101L), any(SaLoginParameter.class));
    }

    @Test
    void shouldIssueSeparateDelegatedTokensForDifferentIdentityScopes() {
        StpLogic stpLogic = mock(StpLogic.class);
        SaSession tokenSession = mock(SaSession.class);
        when(stpLogic.createLoginSession(eq(101L), any(SaLoginParameter.class))).thenReturn("delegated-token-101");
        when(stpLogic.createLoginSession(eq(202L), any(SaLoginParameter.class))).thenReturn("delegated-token-202");
        when(stpLogic.getTokenSessionByToken(any(), eq(true))).thenReturn(tokenSession);
        SaTokenFlowTokenProvider provider = new SaTokenFlowTokenProvider(() -> stpLogic);

        try (var ignored = ExecutionIdentityContextHolder.open(identity("USER", 101L))) {
            assertThat(provider.getToken()).isEqualTo("delegated-token-101");
        }
        try (var ignored = ExecutionIdentityContextHolder.open(identity("USER", 202L))) {
            assertThat(provider.getToken()).isEqualTo("delegated-token-202");
        }

        verify(stpLogic, times(1)).createLoginSession(eq(101L), any(SaLoginParameter.class));
        verify(stpLogic, times(1)).createLoginSession(eq(202L), any(SaLoginParameter.class));
    }

    private ExecutionIdentity identity(String actorType, Long userId) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        user.setPermissions(Set.of("flow:task:approve"));
        return new ExecutionIdentity(user, actorType, userId, 999L,
                301L, "agent-client", "token-1", Set.of("capability:invoke"));
    }
}
