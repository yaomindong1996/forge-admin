package com.mdframe.forge.plugin.capability.identity.mcp;

import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;
import com.mdframe.forge.plugin.capability.identity.config.CapabilityIdentityProperties;
import com.mdframe.forge.plugin.capability.identity.security.AuthenticatedCapabilityIdentity;
import com.mdframe.forge.plugin.capability.identity.security.CapabilitySecurityPrincipal;
import com.mdframe.forge.plugin.capability.identity.token.CapabilityAccessTokenService;
import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.mcp.security.McpAccessDeniedException;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CapabilityMcpAccessTokenResolverTest {

    private CapabilityAccessTokenService tokenService;
    private CapabilityMcpAccessTokenResolver resolver;

    @BeforeEach
    void setUp() {
        tokenService = mock(CapabilityAccessTokenService.class);
        CapabilityIdentityProperties properties = new CapabilityIdentityProperties();
        properties.setIssuer("http://localhost:8580");
        properties.setResource("http://localhost:8580/mcp");
        properties.setAllowedOrigins(Set.of("https://desktop.example"));
        resolver = new CapabilityMcpAccessTokenResolver(tokenService, properties);
    }

    @Test
    void shouldResolveOnlyHeaderBearerAndExposeProtectedResourceChallenge() {
        String rawToken = "fdu_" + "a".repeat(22) + "_" + "b".repeat(43);
        LoginUser user = new LoginUser();
        user.setUserId(101L);
        CapabilitySecurityPrincipal principal = new CapabilitySecurityPrincipal(
                301L, "desktop_agent", CapabilityActorType.USER, 101L, 999L,
                1L, 201L, 1, "key-id", "http://localhost:8580/mcp",
                Set.of("capability:invoke:capability.ping"));
        when(tokenService.authenticate(eq(rawToken), eq("http://localhost:8580/mcp"), any()))
                .thenReturn(new AuthenticatedCapabilityIdentity(principal, user));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + rawToken);
        request.addHeader(HttpHeaders.ORIGIN, "https://desktop.example");

        CapabilityCallerContext caller = resolver.resolve(request);

        assertThat(caller.machineClientId()).isEqualTo("desktop_agent");
        assertThat(caller.userId()).isEqualTo(101L);
        assertThat(resolver.authenticationChallenge()).isEqualTo(
                "Bearer resource_metadata=\"http://localhost:8580/.well-known/oauth-protected-resource\"");
    }

    @Test
    void shouldRejectQueryTokenLongMachineSecretAndUnknownOrigin() {
        MockHttpServletRequest query = new MockHttpServletRequest();
        query.setParameter("access_token", "anything");
        assertThat(resolver.resolve(query)).isNull();

        MockHttpServletRequest machineSecret = new MockHttpServletRequest();
        machineSecret.addHeader(HttpHeaders.AUTHORIZATION, "Bearer fcp_old-long-secret");
        assertThat(resolver.resolve(machineSecret)).isNull();
        verify(tokenService, never()).authenticate(any(), any(), any());

        MockHttpServletRequest origin = new MockHttpServletRequest();
        origin.addHeader(HttpHeaders.ORIGIN, "https://evil.example");
        assertThatThrownBy(() -> resolver.resolve(origin))
                .isInstanceOf(McpAccessDeniedException.class)
                .hasMessageContaining("Origin");
    }
}
