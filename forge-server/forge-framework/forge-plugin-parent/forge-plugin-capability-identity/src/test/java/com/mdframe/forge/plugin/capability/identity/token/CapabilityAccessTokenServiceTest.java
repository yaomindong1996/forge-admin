package com.mdframe.forge.plugin.capability.identity.token;

import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityClientMapper;
import com.mdframe.forge.plugin.capability.identity.config.CapabilityIdentityProperties;
import com.mdframe.forge.plugin.capability.identity.domain.AiCapabilityAccessToken;
import com.mdframe.forge.plugin.capability.identity.mapper.AiCapabilityAccessTokenMapper;
import com.mdframe.forge.plugin.capability.identity.security.AuthenticatedCapabilityIdentity;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class CapabilityAccessTokenServiceTest {

    private AiCapabilityAccessTokenMapper tokenMapper;
    private AiCapabilityClientMapper clientMapper;
    private IUserLoadService userLoadService;
    private CapabilityAccessTokenService tokenService;
    private AiCapabilityClient client;
    private LoginUser user;

    @BeforeEach
    void setUp() {
        tokenMapper = mock(AiCapabilityAccessTokenMapper.class);
        clientMapper = mock(AiCapabilityClientMapper.class);
        userLoadService = mock(IUserLoadService.class);
        CapabilityIdentityProperties properties = new CapabilityIdentityProperties();
        properties.setResource("http://localhost:8580/mcp");
        properties.setTokenPepper("identity-test-pepper-at-least-32-characters");
        tokenService = new CapabilityAccessTokenService(
                tokenMapper, clientMapper, new CapabilityAccessTokenCodec(properties),
                properties, userLoadService,
                Clock.fixed(Instant.parse("2026-07-12T01:00:00Z"), ZoneOffset.UTC));

        client = client();
        user = user(101L, 201L, 2);
        when(clientMapper.selectTenantById(1L, 301L)).thenReturn(client);
        when(userLoadService.loadUserByUserId(101L, 1L, 201L)).thenReturn(user);
    }

    @Test
    void shouldIssueUserTokenWithoutPersistingRawValueAndAuthenticateIt() {
        CapabilityTokenResponse response = tokenService.issue(command(CapabilityActorType.USER, 101L));

        ArgumentCaptor<AiCapabilityAccessToken> captor =
                ArgumentCaptor.forClass(AiCapabilityAccessToken.class);
        verify(tokenMapper).insert(captor.capture());
        AiCapabilityAccessToken persisted = captor.getValue();
        persisted.setId(401L);
        assertThat(response.accessToken()).startsWith("fdu_");
        assertThat(persisted.getTokenHash()).doesNotContain(response.accessToken());
        assertThat(persisted.getScopes()).isEqualTo(
                "capability:discover:capability.ping capability:invoke:capability.ping");

        when(tokenMapper.selectActiveByTokenKeyId(persisted.getTokenKeyId())).thenReturn(persisted);
        when(tokenMapper.touchLastUsed(any(), any(), any())).thenReturn(1);
        AuthenticatedCapabilityIdentity authenticated = tokenService.authenticate(
                response.accessToken(), "http://localhost:8580/mcp",
                Set.of("capability:invoke:capability.ping"));

        assertThat(authenticated.principal().actorType()).isEqualTo(CapabilityActorType.USER);
        assertThat(authenticated.principal().actorUserId()).isEqualTo(101L);
        assertThat(authenticated.principal().serviceUserId()).isEqualTo(999L);
        assertThat(authenticated.loginUser()).isSameAs(user);
    }

    @Test
    void shouldRejectCredentialVersionChangeOnNextRequest() {
        CapabilityTokenResponse response = tokenService.issue(command(CapabilityActorType.USER, 101L));
        ArgumentCaptor<AiCapabilityAccessToken> captor =
                ArgumentCaptor.forClass(AiCapabilityAccessToken.class);
        verify(tokenMapper).insert(captor.capture());
        AiCapabilityAccessToken persisted = captor.getValue();
        persisted.setId(401L);
        when(tokenMapper.selectActiveByTokenKeyId(persisted.getTokenKeyId())).thenReturn(persisted);
        client.setCredentialVersion(2);

        assertThatThrownBy(() -> tokenService.authenticate(
                response.accessToken(), "http://localhost:8580/mcp", Set.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("invalid_token");
    }

    @Test
    void shouldRejectAdminAsServiceIdentity() {
        LoginUser admin = user(999L, 201L, 0);
        when(userLoadService.loadUserByUserId(999L, 1L, 201L)).thenReturn(admin);

        assertThatThrownBy(() -> tokenService.issue(command(CapabilityActorType.SERVICE, 999L)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("invalid_token");
    }

    @Test
    void shouldThrottleLastUsedWritesWithinConfiguredInterval() {
        CapabilityTokenResponse response = tokenService.issue(command(CapabilityActorType.USER, 101L));
        ArgumentCaptor<AiCapabilityAccessToken> captor =
                ArgumentCaptor.forClass(AiCapabilityAccessToken.class);
        verify(tokenMapper).insert(captor.capture());
        AiCapabilityAccessToken persisted = captor.getValue();
        persisted.setId(401L);
        persisted.setLastUsedAt(java.time.LocalDateTime.of(2026, 7, 12, 0, 59, 30));
        when(tokenMapper.selectActiveByTokenKeyId(persisted.getTokenKeyId())).thenReturn(persisted);

        tokenService.authenticate(response.accessToken(), "http://localhost:8580/mcp", Set.of());

        verify(tokenMapper, never()).touchLastUsed(any(), any(), any());
    }

    @Test
    void shouldRevokeOnlyCurrentUsersOwnDelegatedTokenAndRemainIdempotent() {
        CapabilityTokenResponse response = tokenService.issue(command(CapabilityActorType.USER, 101L));
        ArgumentCaptor<AiCapabilityAccessToken> captor =
                ArgumentCaptor.forClass(AiCapabilityAccessToken.class);
        verify(tokenMapper).insert(captor.capture());
        AiCapabilityAccessToken persisted = captor.getValue();
        persisted.setId(401L);
        when(tokenMapper.selectActiveByTokenKeyId(persisted.getTokenKeyId())).thenReturn(persisted);

        tokenService.revokeUserToken(response.accessToken(), 101L, 1L, 301L);
        verify(tokenMapper).revoke(1L, 401L, java.time.LocalDateTime.of(2026, 7, 12, 1, 0));

        assertThatThrownBy(() -> tokenService.revokeUserToken(
                response.accessToken(), 102L, 1L, 301L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权撤销");
        assertThatThrownBy(() -> tokenService.revokeUserToken(
                response.accessToken(), 101L, 1L, 302L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权撤销");

        when(tokenMapper.selectActiveByTokenKeyId(persisted.getTokenKeyId())).thenReturn(null);
        tokenService.revokeUserToken(response.accessToken(), 101L, 1L, 301L);
        verify(tokenMapper, never()).deleteById(401L);
    }

    private CapabilityTokenIssueCommand command(CapabilityActorType actorType, Long actorUserId) {
        return new CapabilityTokenIssueCommand(
                301L, 1, actorType, actorUserId, 999L, 1L, 201L,
                "http://localhost:8580/mcp",
                Set.of("capability:invoke:capability.ping", "capability:discover:capability.ping"));
    }

    private AiCapabilityClient client() {
        AiCapabilityClient value = new AiCapabilityClient();
        value.setId(301L);
        value.setTenantId(1L);
        value.setClientCode("desktop_agent");
        value.setCredentialVersion(1);
        value.setServiceUserId(999L);
        value.setActiveOrgId(201L);
        value.setOauthEnabled(1);
        value.setOauthClientType("CONFIDENTIAL");
        value.setStatus("ENABLED");
        return value;
    }

    private LoginUser user(Long userId, Long activeOrgId, Integer userType) {
        LoginUser value = new LoginUser();
        value.setUserId(userId);
        value.setTenantId(1L);
        value.setActiveOrgId(activeOrgId);
        value.setUserStatus(1);
        value.setForcePasswordChange(false);
        value.setUserType(userType);
        value.setRoleIds(List.of(501L));
        return value;
    }
}
