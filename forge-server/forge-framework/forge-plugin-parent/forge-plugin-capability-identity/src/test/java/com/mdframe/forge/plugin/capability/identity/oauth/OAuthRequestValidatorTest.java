package com.mdframe.forge.plugin.capability.identity.oauth;

import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.identity.config.CapabilityIdentityProperties;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthRequestValidatorTest {

    private static final String REDIRECT = "http://127.0.0.1:43110/callback";
    private static final String VERIFIER = "v".repeat(43);

    private OAuthRequestValidator validator;
    private AiCapabilityClient client;

    @BeforeEach
    void setUp() {
        CapabilityIdentityProperties properties = new CapabilityIdentityProperties();
        properties.setResource("http://localhost:8580/mcp");
        ExactRedirectUriRegistry redirects = (tenantId, clientId, uri) -> REDIRECT.equals(uri);
        validator = new OAuthRequestValidator(
                properties, redirects,
                Clock.fixed(Instant.parse("2026-07-12T01:00:00Z"), ZoneOffset.UTC));

        client = new AiCapabilityClient();
        client.setId(10L);
        client.setTenantId(1L);
        client.setClientCode("desktop_agent");
        client.setCredentialVersion(1);
        client.setServiceUserId(20L);
        client.setActiveOrgId(30L);
        client.setOauthEnabled(1);
        client.setOauthClientType("PUBLIC");
        client.setStatus("ENABLED");
    }

    @Test
    void shouldAcceptPublicClientWithExactRedirectResourceAndPkceS256() {
        String challenge = OAuthRequestValidator.s256(VERIFIER);
        ValidatedAuthorizationRequest request = validator.validateAuthorizationRequest(
                client, "code", REDIRECT, "http://localhost:8580/mcp",
                "capability:invoke:capability.ping", challenge, "S256", "opaque-state");

        assertThat(request.clientId()).isEqualTo(10L);
        assertThat(request.state()).isEqualTo("opaque-state");
        validator.verifyTokenExchange(
                REDIRECT, REDIRECT,
                "http://localhost:8580/mcp", "http://localhost:8580/mcp",
                challenge, VERIFIER);
    }

    @Test
    void shouldAcceptGenericDiscoveryAndInvocationScopesForGovernedCatalog() {
        String challenge = OAuthRequestValidator.s256(VERIFIER);

        ValidatedAuthorizationRequest request = validator.validateAuthorizationRequest(
                client, "code", REDIRECT, "http://localhost:8580/mcp",
                "capability:discover capability:invoke", challenge, "S256", null);

        assertThat(request.scopes()).containsExactlyInAnyOrder(
                "capability:discover", "capability:invoke");
    }

    @Test
    void shouldRejectRedirectPrefixPlainPkceAndChangedResource() {
        String challenge = OAuthRequestValidator.s256(VERIFIER);
        assertThatThrownBy(() -> validator.validateAuthorizationRequest(
                client, "code", REDIRECT + "/extra", "http://localhost:8580/mcp",
                "capability:invoke:capability.ping", challenge, "S256", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("redirect_uri");

        assertThatThrownBy(() -> validator.validateAuthorizationRequest(
                client, "code", REDIRECT, "http://localhost:8580/mcp",
                "capability:invoke:capability.ping", VERIFIER, "plain", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("S256");

        assertThatThrownBy(() -> validator.verifyTokenExchange(
                REDIRECT, REDIRECT,
                "http://localhost:8580/mcp", "http://localhost:8580/other",
                challenge, VERIFIER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("resource");
    }

    @Test
    void shouldRejectOversizedParametersAndNonCapabilityScopes() {
        String challenge = OAuthRequestValidator.s256(VERIFIER);
        assertThatThrownBy(() -> validator.validateAuthorizationRequest(
                client, "code", REDIRECT, "http://localhost:8580/mcp",
                "capability:invoke:" + "x".repeat(200), challenge, "S256", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("scope");
        assertThatThrownBy(() -> validator.validateAuthorizationRequest(
                client, "code", REDIRECT, "http://localhost:8580/mcp",
                "openid", challenge, "S256", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("scope");
        assertThatThrownBy(() -> validator.validateAuthorizationRequest(
                client, "code", REDIRECT, "http://localhost:8580/mcp",
                "capability:invoke:order.submit", challenge, "S256", "s".repeat(513)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("state");
    }

    @Test
    void shouldRejectIssuerWithQuery() {
        CapabilityIdentityProperties properties = new CapabilityIdentityProperties();
        properties.setIssuer("https://forge.example?tenant=1");

        assertThatThrownBy(properties::validatedIssuer)
                .isInstanceOf(IllegalStateException.class);
    }
}
