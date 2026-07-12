package com.mdframe.forge.plugin.capability.identity.mcp;

import com.mdframe.forge.plugin.capability.identity.config.CapabilityIdentityProperties;
import com.mdframe.forge.plugin.capability.identity.security.AuthenticatedCapabilityIdentity;
import com.mdframe.forge.plugin.capability.identity.security.CapabilitySecurityPrincipal;
import com.mdframe.forge.plugin.capability.identity.token.CapabilityAccessTokenService;
import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.mcp.security.McpCallerContextResolver;
import com.mdframe.forge.plugin.mcp.security.McpAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.capability.identity", name = "enabled", havingValue = "true")
public class CapabilityMcpAccessTokenResolver implements McpCallerContextResolver {

    public static final String AUTHENTICATED_IDENTITY_ATTRIBUTE =
            "forge.capability.authenticated-identity";

    private static final String BEARER_PREFIX = "Bearer ";

    private final CapabilityAccessTokenService accessTokenService;
    private final CapabilityIdentityProperties properties;

    @Override
    public CapabilityCallerContext resolve(HttpServletRequest request) {
        validateOrigin(request);
        if (request.getParameter("access_token") != null || request.getParameter("token") != null) {
            return null;
        }
        String rawToken = extractBearerToken(request);
        if (rawToken == null
                || !rawToken.matches("^fdu_[A-Za-z0-9_-]{22}_[A-Za-z0-9_-]{43}$")) {
            return null;
        }
        AuthenticatedCapabilityIdentity authenticated = accessTokenService.authenticate(
                rawToken, properties.validatedResource(), Set.of());
        request.setAttribute(AUTHENTICATED_IDENTITY_ATTRIBUTE, authenticated);
        CapabilitySecurityPrincipal principal = authenticated.principal();
        return new CapabilityCallerContext(
                principal.clientCode(), principal.tenantId(), principal.actorUserId(),
                principal.activeOrgId(), principal.scopes());
    }

    @Override
    public String authenticationChallenge() {
        return "Bearer resource_metadata=\"" + properties.validatedIssuer()
                + "/.well-known/oauth-protected-resource\"";
    }

    private String extractBearerToken(HttpServletRequest request) {
        List<String> values = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));
        if (values.size() != 1) {
            return null;
        }
        String authorization = values.get(0);
        if (authorization == null
                || authorization.length() <= BEARER_PREFIX.length()
                || !authorization.regionMatches(
                        true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length());
        return token.isBlank() || !token.equals(token.trim()) ? null : token;
    }

    private void validateOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || origin.isBlank()) {
            return;
        }
        if (!properties.getAllowedOrigins().contains(origin)) {
            throw new McpAccessDeniedException("MCP Origin 未获允许");
        }
    }
}
