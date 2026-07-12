package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.session.SaSession;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlowDelegationSessionVerifierTest {

    @AfterEach
    void clearIdentity() {
        ExecutionIdentityContextHolder.clear();
    }

    @Test
    void shouldAcceptSessionBoundToCurrentDelegatedIdentity() {
        SaSession session = session(true);
        FlowDelegationSessionVerifier verifier = new FlowDelegationSessionVerifier(() -> session);

        try (var ignored = ExecutionIdentityContextHolder.open(identity())) {
            assertThatCode(verifier::requireTrustedDelegation).doesNotThrowAnyException();
        }
    }

    @Test
    void shouldRejectOrdinarySessionWithoutDelegationMarker() {
        SaSession session = session(false);
        FlowDelegationSessionVerifier verifier = new FlowDelegationSessionVerifier(() -> session);

        try (var ignored = ExecutionIdentityContextHolder.open(identity())) {
            assertThatThrownBy(verifier::requireTrustedDelegation)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("FLOW_START_DELEGATION_REQUIRED");
        }
    }

    @Test
    void shouldRejectDelegationWithoutPositiveClientId() {
        SaSession session = session(true);
        when(session.get(FlowDelegationSessionVerifier.CLIENT_ID_KEY)).thenReturn(0L);
        FlowDelegationSessionVerifier verifier = new FlowDelegationSessionVerifier(() -> session);

        try (var ignored = ExecutionIdentityContextHolder.open(identity())) {
            assertThatThrownBy(verifier::requireTrustedDelegation)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("FLOW_START_DELEGATION_REQUIRED");
        }
    }

    private SaSession session(boolean trusted) {
        SaSession session = mock(SaSession.class);
        when(session.get(FlowDelegationSessionVerifier.MARKER_KEY))
                .thenReturn(trusted ? Boolean.TRUE : null);
        when(session.get(FlowDelegationSessionVerifier.ACTOR_USER_ID_KEY)).thenReturn(101L);
        when(session.get(FlowDelegationSessionVerifier.TENANT_ID_KEY)).thenReturn(1L);
        when(session.get(FlowDelegationSessionVerifier.ACTIVE_ORG_ID_KEY)).thenReturn(201L);
        when(session.get(FlowDelegationSessionVerifier.CLIENT_ID_KEY)).thenReturn(301L);
        return session;
    }

    private ExecutionIdentity identity() {
        LoginUser user = new LoginUser();
        user.setUserId(101L);
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        return new ExecutionIdentity(user, "USER", 101L, 999L,
                301L, "agent-client", "token-1", Set.of("capability:invoke"));
    }
}
