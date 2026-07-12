package com.mdframe.forge.plugin.capability.identity.oauth;

import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.identity.config.CapabilityIdentityProperties;
import com.mdframe.forge.starter.core.exception.BusinessException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class OAuthRequestValidator {

    private static final Pattern PKCE_VALUE = Pattern.compile("^[A-Za-z0-9._~-]{43,128}$");
    public static final int MAX_CLIENT_ID_LENGTH = 64;
    public static final int MAX_REDIRECT_URI_LENGTH = 2048;
    public static final int MAX_RESOURCE_LENGTH = 2048;
    public static final int MAX_SCOPE_LENGTH = 2048;
    public static final int MAX_STATE_LENGTH = 512;
    public static final int MAX_TOKEN_VALUE_LENGTH = 512;

    private static final Pattern CAPABILITY_SCOPE = Pattern.compile(
            "^capability:(discover|invoke)(?::[a-z0-9][a-z0-9._-]{0,127})?$");

    private final CapabilityIdentityProperties properties;
    private final ExactRedirectUriRegistry redirectUriRegistry;
    private final Clock clock;

    public OAuthRequestValidator(
            CapabilityIdentityProperties properties,
            ExactRedirectUriRegistry redirectUriRegistry,
            Clock clock) {
        this.properties = properties;
        this.redirectUriRegistry = redirectUriRegistry;
        this.clock = clock;
    }

    public ValidatedAuthorizationRequest validateAuthorizationRequest(
            AiCapabilityClient client,
            String responseType,
            String redirectUri,
            String resource,
            String scope,
            String codeChallenge,
            String codeChallengeMethod,
            String state) {
        requireUsableOAuthClient(client);
        requireLength(redirectUri, MAX_REDIRECT_URI_LENGTH, "redirect_uri");
        requireLength(resource, MAX_RESOURCE_LENGTH, "resource");
        requireLength(scope, MAX_SCOPE_LENGTH, "scope");
        requireOptionalLength(state, MAX_STATE_LENGTH, "state");
        if (!"code".equals(responseType)) {
            throw oauthError("unsupported_response_type", "只支持 authorization code");
        }
        if (!properties.getResource().equals(resource)) {
            throw oauthError("invalid_target", "resource 与 MCP 资源不匹配");
        }
        if (!redirectUriRegistry.contains(client.getTenantId(), client.getId(), redirectUri)) {
            throw oauthError("invalid_request", "redirect_uri 未精确登记");
        }
        if (!"S256".equals(codeChallengeMethod)
                || codeChallenge == null
                || !PKCE_VALUE.matcher(codeChallenge).matches()) {
            throw oauthError("invalid_request", "必须使用 PKCE S256");
        }
        Set<String> scopes = parseScopes(scope);
        if (scopes.isEmpty() || scopes.stream().anyMatch(value -> !CAPABILITY_SCOPE.matcher(value).matches())) {
            throw oauthError("invalid_scope", "请求 scope 未获支持");
        }
        return new ValidatedAuthorizationRequest(
                client.getId(), client.getClientCode(), client.getTenantId(), redirectUri,
                resource, scopes, codeChallenge, state);
    }

    public void verifyTokenExchange(
            String expectedRedirectUri,
            String actualRedirectUri,
            String expectedResource,
            String actualResource,
            String codeChallenge,
            String codeVerifier) {
        if (!constantEquals(expectedRedirectUri, actualRedirectUri)) {
            throw oauthError("invalid_grant", "redirect_uri 与授权请求不一致");
        }
        if (!constantEquals(expectedResource, actualResource)) {
            throw oauthError("invalid_target", "resource 与授权请求不一致");
        }
        if (codeVerifier == null || !PKCE_VALUE.matcher(codeVerifier).matches()) {
            throw oauthError("invalid_grant", "code_verifier 无效");
        }
        String actualChallenge = s256(codeVerifier);
        if (!constantEquals(codeChallenge, actualChallenge)) {
            throw oauthError("invalid_grant", "PKCE 校验失败");
        }
    }

    public Set<String> validateClientCredentials(
            AiCapabilityClient client,
            String resource,
            String scope) {
        requireUsableOAuthClient(client);
        requireLength(resource, MAX_RESOURCE_LENGTH, "resource");
        requireLength(scope, MAX_SCOPE_LENGTH, "scope");
        if (!"CONFIDENTIAL".equals(client.getOauthClientType())) {
            throw oauthError("unauthorized_client", "PUBLIC 客户端不能使用 client_credentials");
        }
        if (!properties.getResource().equals(resource)) {
            throw oauthError("invalid_target", "resource 与 MCP 资源不匹配");
        }
        Set<String> scopes = parseScopes(scope);
        if (scopes.isEmpty() || scopes.stream().anyMatch(value -> !CAPABILITY_SCOPE.matcher(value).matches())) {
            throw oauthError("invalid_scope", "请求 scope 未获支持");
        }
        return scopes;
    }

    public static String s256(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(digest.digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }

    private void requireUsableOAuthClient(AiCapabilityClient client) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (client == null
                || !Integer.valueOf(1).equals(client.getOauthEnabled())
                || !"ENABLED".equals(client.getStatus())
                || client.getCredentialVersion() == null
                || client.getCredentialVersion() <= 0
                || client.getServiceUserId() == null
                || client.getServiceUserId() <= 0
                || client.getActiveOrgId() == null
                || client.getActiveOrgId() <= 0
                || (client.getExpiresAt() != null && !client.getExpiresAt().isAfter(now))) {
            throw oauthError("unauthorized_client", "客户端未启用 OAuth 或已失效");
        }
        if (!Set.of("PUBLIC", "CONFIDENTIAL").contains(client.getOauthClientType())) {
            throw oauthError("unauthorized_client", "OAuth 客户端类型无效");
        }
    }

    private Set<String> parseScopes(String scope) {
        if (scope == null || scope.isBlank()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String value : scope.trim().split(" +")) {
            if (!value.isBlank()) {
                result.add(value);
            }
        }
        return Set.copyOf(result);
    }

    public void requireLength(String value, int maxLength, String parameterName) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw oauthError("invalid_request", parameterName + " 长度无效");
        }
    }

    public void requireOptionalLength(String value, int maxLength, String parameterName) {
        if (value != null && value.length() > maxLength) {
            throw oauthError("invalid_request", parameterName + " 长度无效");
        }
    }

    private boolean constantEquals(String expected, String actual) {
        byte[] left = expected == null ? new byte[0] : expected.getBytes(StandardCharsets.UTF_8);
        byte[] right = actual == null ? new byte[0] : actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(left, right);
    }

    private BusinessException oauthError(String error, String description) {
        return new BusinessException(400, error + ": " + description);
    }
}
