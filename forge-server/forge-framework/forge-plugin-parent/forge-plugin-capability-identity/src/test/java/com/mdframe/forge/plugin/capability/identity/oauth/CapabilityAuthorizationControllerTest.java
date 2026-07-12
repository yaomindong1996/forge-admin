package com.mdframe.forge.plugin.capability.identity.oauth;

import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityClientMapper;
import com.mdframe.forge.plugin.capability.identity.config.CapabilityIdentityProperties;
import com.mdframe.forge.plugin.capability.identity.token.CapabilityAccessTokenService;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CapabilityAuthorizationControllerTest {

    private AiCapabilityClientMapper clientMapper;
    private OAuthRequestValidator validator;
    private DelegationAuthorizationCodeStore codeStore;
    private CapabilityAuthorizationController controller;
    private CapabilityAccessTokenService accessTokenService;
    private ExecutionIdentityContextHolder.Scope identityScope;

    @BeforeEach
    void setUp() {
        clientMapper = mock(AiCapabilityClientMapper.class);
        validator = mock(OAuthRequestValidator.class);
        codeStore = mock(DelegationAuthorizationCodeStore.class);
        accessTokenService = mock(CapabilityAccessTokenService.class);
        controller = new CapabilityAuthorizationController(
                clientMapper, validator, codeStore, new CapabilityIdentityProperties(),
                accessTokenService);
        LoginUser user = new LoginUser();
        user.setUserId(101L);
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        user.setUserStatus(1);
        user.setForcePasswordChange(false);
        identityScope = ExecutionIdentityContextHolder.open(new ExecutionIdentity(
                user, "USER", 101L, 999L, 301L,
                "desktop_agent", "token-id", Set.of()));

        AiCapabilityClient client = new AiCapabilityClient();
        client.setId(301L);
        client.setTenantId(1L);
        client.setActiveOrgId(202L);
        when(clientMapper.selectTenantById(1L, 301L)).thenReturn(client);
    }

    @AfterEach
    void tearDown() {
        if (identityScope != null) {
            identityScope.close();
        }
        ExecutionIdentityContextHolder.clear();
    }

    @Test
    void shouldRejectAuthorizationRequestBeforeValidationWhenActiveOrgDiffers() {
        assertThatThrownBy(() -> controller.authorizationRequest(
                "301", "code", "http://127.0.0.1/callback",
                "http://localhost:8580/mcp", "capability:invoke:capability.ping",
                "c".repeat(43), "S256", "state"))
                .hasMessageContaining("组织");

        verify(validator, never()).validateAuthorizationRequest(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectAuthorizationDecisionWithoutIssuingCodeWhenActiveOrgDiffers() {
        AuthorizationDecisionRequest decision = new AuthorizationDecisionRequest(
                "301", "code", "http://127.0.0.1/callback",
                "http://localhost:8580/mcp", "capability:invoke:capability.ping",
                "c".repeat(43), "S256", "state", true);

        assertThatThrownBy(() -> controller.authorize(decision)).hasMessageContaining("组织");
        verify(codeStore, never()).issue(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldDelegateUserTokenRevocationWithCurrentActorAndTenant() {
        controller.revokeUserToken(new UserTokenRevokeRequest(
                "301", "fdu_" + "a".repeat(22) + "_" + "b".repeat(43)));

        verify(accessTokenService).revokeUserToken(
                "fdu_" + "a".repeat(22) + "_" + "b".repeat(43),
                101L, 1L, 301L);
    }
}
