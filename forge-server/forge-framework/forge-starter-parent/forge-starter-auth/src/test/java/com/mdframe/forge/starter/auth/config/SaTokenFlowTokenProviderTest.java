package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpLogic;
import com.mdframe.forge.flow.client.FlowTokenAcquisitionException;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        when(stpLogic.createLoginSession(eq(101L), any(SaLoginModel.class)))
                .thenReturn("delegated-flow-token");
        when(stpLogic.getTokenSessionByToken("delegated-flow-token", true))
                .thenReturn(tokenSession);
        SaTokenFlowTokenProvider provider = new SaTokenFlowTokenProvider(() -> stpLogic);
        ExecutionIdentity identity = identity("USER", 101L);

        try (var ignored = ExecutionIdentityContextHolder.open(identity)) {
            assertThat(provider.getToken()).isEqualTo("delegated-flow-token");
        }

        ArgumentCaptor<SaLoginModel> loginModel = ArgumentCaptor.forClass(SaLoginModel.class);
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

        verify(stpLogic, never()).createLoginSession(any(), any(SaLoginModel.class));
    }

    @Test
    void shouldFailClosedWhenSaTokenCreationReturnsBlankToken() {
        StpLogic stpLogic = mock(StpLogic.class);
        when(stpLogic.createLoginSession(eq(101L), any(SaLoginModel.class))).thenReturn(" ");
        SaTokenFlowTokenProvider provider = new SaTokenFlowTokenProvider(() -> stpLogic);

        try (var ignored = ExecutionIdentityContextHolder.open(identity("USER", 101L))) {
            assertThatThrownBy(provider::getToken)
                    .isInstanceOf(FlowTokenAcquisitionException.class)
                    .hasMessage("MCP_FLOW_DELEGATION_TOKEN_UNAVAILABLE");
        }

        verify(stpLogic, never()).getTokenSessionByToken(any(), eq(true));
    }

    @Test
    void shouldKeepExistingSaTokenBehaviorOutsideExecutionIdentity() {
        StpLogic stpLogic = mock(StpLogic.class);
        when(stpLogic.isLogin()).thenReturn(true);
        when(stpLogic.getTokenValue()).thenReturn("interactive-token");
        SaTokenFlowTokenProvider provider = new SaTokenFlowTokenProvider(() -> stpLogic);

        assertThat(provider.getToken()).isEqualTo("interactive-token");
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
