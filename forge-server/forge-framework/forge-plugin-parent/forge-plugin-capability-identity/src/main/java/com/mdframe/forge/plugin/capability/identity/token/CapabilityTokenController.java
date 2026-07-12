package com.mdframe.forge.plugin.capability.identity.token;

import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityClientMapper;
import com.mdframe.forge.plugin.capability.controlplane.security.CapabilityClientPrincipal;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityClientService;
import com.mdframe.forge.plugin.capability.identity.oauth.DelegationAuthorizationCode;
import com.mdframe.forge.plugin.capability.identity.oauth.DelegationAuthorizationCodeStore;
import com.mdframe.forge.plugin.capability.identity.oauth.OAuthRequestValidator;
import com.mdframe.forge.plugin.mcp.security.ForgeMcpAuthenticationFilter;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.capability.identity", name = "enabled", havingValue = "true")
public class CapabilityTokenController {

    private static final Logger log = LoggerFactory.getLogger(CapabilityTokenController.class);

    private final AiCapabilityClientMapper clientMapper;
    private final CapabilityClientService clientService;
    private final DelegationAuthorizationCodeStore authorizationCodeStore;
    private final OAuthRequestValidator requestValidator;
    private final CapabilityAccessTokenService accessTokenService;

    @PostMapping(value = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @OperationLog(
            module = "MCP OAuth",
            type = OperationType.OTHER,
            desc = "换取MCP短期令牌",
            saveRequestParams = false,
            saveResponseResult = false)
    public ResponseEntity<Map<String, Object>> token(
            HttpServletRequest request,
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "code_verifier", required = false) String codeVerifier,
            @RequestParam(value = "resource", required = false) String resource,
            @RequestParam(value = "scope", required = false) String scope) {
        String requestId = requestId(request);
        try {
            validateTokenParameters(
                    grantType, clientId, clientSecret, code, redirectUri, codeVerifier, resource, scope);
            ClientCredentials credentials = resolveCredentials(request, clientId, clientSecret);
            AiCapabilityClient client = requireClient(credentials.clientId());
            CapabilityTokenResponse response;
            if ("authorization_code".equals(grantType)) {
                authenticateForAuthorizationCode(client, credentials.clientSecret());
                response = exchangeAuthorizationCode(
                        client, code, redirectUri, codeVerifier, resource);
            } else if ("client_credentials".equals(grantType)) {
                authenticateConfidentialClient(client, credentials.clientSecret());
                Set<String> scopes = requestValidator.validateClientCredentials(client, resource, scope);
                response = accessTokenService.issue(new CapabilityTokenIssueCommand(
                        client.getId(), client.getCredentialVersion(), CapabilityActorType.SERVICE,
                        client.getServiceUserId(), client.getServiceUserId(), client.getTenantId(),
                        client.getActiveOrgId(), resource, scopes));
            } else {
                throw oauthError("unsupported_grant_type");
            }
            return tokenSuccess(response);
        } catch (BusinessException exception) {
            return tokenError(exception, requestId);
        } catch (RuntimeException exception) {
            return temporarilyUnavailable("/oauth2/token", grantType, requestId, exception);
        }
    }

    @PostMapping(value = "/oauth2/revoke", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @OperationLog(
            module = "MCP OAuth",
            type = OperationType.OTHER,
            desc = "撤销MCP短期令牌",
            saveRequestParams = false,
            saveResponseResult = false)
    public ResponseEntity<Map<String, Object>> revoke(
            HttpServletRequest request,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestParam String token) {
        String requestId = requestId(request);
        try {
            requestValidator.requireOptionalLength(
                    clientId, OAuthRequestValidator.MAX_CLIENT_ID_LENGTH, "client_id");
            requestValidator.requireOptionalLength(
                    clientSecret, OAuthRequestValidator.MAX_TOKEN_VALUE_LENGTH, "client_secret");
            requestValidator.requireLength(
                    token, OAuthRequestValidator.MAX_TOKEN_VALUE_LENGTH, "token");
            ClientCredentials credentials = resolveCredentials(request, clientId, clientSecret);
            AiCapabilityClient client = requireClient(credentials.clientId());
            authenticateConfidentialClient(client, credentials.clientSecret());
            accessTokenService.revoke(token, client.getId());
            return noStore(ResponseEntity.ok()).body(Map.of());
        } catch (BusinessException exception) {
            return tokenError(exception, requestId);
        } catch (RuntimeException exception) {
            return temporarilyUnavailable("/oauth2/revoke", null, requestId, exception);
        }
    }

    private CapabilityTokenResponse exchangeAuthorizationCode(
            AiCapabilityClient client,
            String rawCode,
            String redirectUri,
            String codeVerifier,
            String resource) {
        DelegationAuthorizationCode code = authorizationCodeStore.consume(rawCode);
        if (code == null
                || !client.getId().equals(code.clientId())
                || !client.getCredentialVersion().equals(code.credentialVersion())
                || !client.getTenantId().equals(code.tenantId())) {
            throw oauthError("invalid_grant");
        }
        requestValidator.verifyTokenExchange(
                code.redirectUri(), redirectUri, code.resource(), resource,
                code.codeChallenge(), codeVerifier);
        return accessTokenService.issue(new CapabilityTokenIssueCommand(
                code.clientId(), code.credentialVersion(), CapabilityActorType.USER,
                code.actorUserId(), code.serviceUserId(), code.tenantId(),
                code.activeOrgId(), code.resource(), code.scopes()));
    }

    private void authenticateForAuthorizationCode(
            AiCapabilityClient client,
            String clientSecret) {
        if ("CONFIDENTIAL".equals(client.getOauthClientType())) {
            authenticateConfidentialClient(client, clientSecret);
        } else if (!"PUBLIC".equals(client.getOauthClientType())
                || (clientSecret != null && !clientSecret.isBlank())) {
            throw oauthError("invalid_client");
        }
    }

    private void authenticateConfidentialClient(
            AiCapabilityClient client,
            String clientSecret) {
        if (!"CONFIDENTIAL".equals(client.getOauthClientType())
                || clientSecret == null || clientSecret.isBlank()) {
            throw oauthError("invalid_client");
        }
        CapabilityClientPrincipal principal;
        try {
            principal = clientService.authenticate(clientSecret);
        } catch (BusinessException exception) {
            throw oauthError("invalid_client");
        }
        if (!client.getId().equals(principal.clientId())
                || !Integer.valueOf(1).equals(client.getOauthEnabled())) {
            throw oauthError("invalid_client");
        }
    }

    private AiCapabilityClient requireClient(String clientId) {
        Long id;
        try {
            id = Long.valueOf(clientId);
        } catch (NumberFormatException exception) {
            throw oauthError("invalid_client");
        }
        AiCapabilityClient client = clientMapper.selectCredentialById(id);
        if (client == null
                || !"ENABLED".equals(client.getStatus())
                || !Integer.valueOf(1).equals(client.getOauthEnabled())) {
            throw oauthError("invalid_client");
        }
        return client;
    }

    private ClientCredentials resolveCredentials(
            HttpServletRequest request,
            String parameterClientId,
            String parameterClientSecret) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.length() > 1024) {
            throw oauthError("invalid_client");
        }
        if (header == null || !header.regionMatches(true, 0, "Basic ", 0, 6)) {
            if (parameterClientId == null || parameterClientId.isBlank()) {
                throw oauthError("invalid_client");
            }
            return new ClientCredentials(parameterClientId, parameterClientSecret);
        }
        String decoded;
        try {
            decoded = new String(
                    Base64.getDecoder().decode(header.substring(6)),
                    StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw oauthError("invalid_client");
        }
        int separator = decoded.indexOf(':');
        if (separator <= 0) {
            throw oauthError("invalid_client");
        }
        String basicClientId = decoded.substring(0, separator);
        String basicSecret = decoded.substring(separator + 1);
        if (parameterClientId != null && !parameterClientId.equals(basicClientId)) {
            throw oauthError("invalid_client");
        }
        return new ClientCredentials(basicClientId, basicSecret);
    }

    private ResponseEntity<Map<String, Object>> tokenSuccess(CapabilityTokenResponse response) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", response.accessToken());
        body.put("token_type", response.tokenType());
        body.put("expires_in", response.expiresIn());
        body.put("scope", response.scope());
        body.put("resource", response.resource());
        return noStore(ResponseEntity.ok()).body(body);
    }

    private ResponseEntity<Map<String, Object>> tokenError(
            BusinessException exception,
            String requestId) {
        String message = exception.getMessage();
        String error = message == null ? "invalid_request" : message.split(":", 2)[0];
        Set<String> allowedErrors = Set.of(
                "invalid_request", "invalid_client", "invalid_grant", "invalid_scope",
                "invalid_target", "unauthorized_client", "unsupported_grant_type",
                "temporarily_unavailable");
        if (!allowedErrors.contains(error)) {
            error = "invalid_request";
        }
        HttpStatus status = switch (error) {
            case "invalid_client" -> HttpStatus.UNAUTHORIZED;
            case "temporarily_unavailable" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
        ResponseEntity.BodyBuilder builder = noStore(ResponseEntity.status(status))
                .header(ForgeMcpAuthenticationFilter.REQUEST_ID_HEADER, requestId);
        if (status == HttpStatus.UNAUTHORIZED) {
            builder.header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"forge-mcp-oauth\"");
        }
        return builder.body(Map.of("error", error));
    }

    private ResponseEntity.BodyBuilder noStore(ResponseEntity.BodyBuilder builder) {
        return builder.cacheControl(CacheControl.noStore()).header(HttpHeaders.PRAGMA, "no-cache");
    }

    private BusinessException oauthError(String error) {
        return new BusinessException(400, error);
    }

    private void validateTokenParameters(
            String grantType,
            String clientId,
            String clientSecret,
            String code,
            String redirectUri,
            String codeVerifier,
            String resource,
            String scope) {
        requestValidator.requireLength(grantType, 64, "grant_type");
        requestValidator.requireOptionalLength(
                clientId, OAuthRequestValidator.MAX_CLIENT_ID_LENGTH, "client_id");
        requestValidator.requireOptionalLength(
                clientSecret, OAuthRequestValidator.MAX_TOKEN_VALUE_LENGTH, "client_secret");
        requestValidator.requireOptionalLength(
                code, OAuthRequestValidator.MAX_TOKEN_VALUE_LENGTH, "code");
        requestValidator.requireOptionalLength(
                redirectUri, OAuthRequestValidator.MAX_REDIRECT_URI_LENGTH, "redirect_uri");
        requestValidator.requireOptionalLength(codeVerifier, 128, "code_verifier");
        requestValidator.requireOptionalLength(
                resource, OAuthRequestValidator.MAX_RESOURCE_LENGTH, "resource");
        requestValidator.requireOptionalLength(
                scope, OAuthRequestValidator.MAX_SCOPE_LENGTH, "scope");
        if ("authorization_code".equals(grantType)) {
            requestValidator.requireLength(code, OAuthRequestValidator.MAX_TOKEN_VALUE_LENGTH, "code");
            requestValidator.requireLength(
                    redirectUri, OAuthRequestValidator.MAX_REDIRECT_URI_LENGTH, "redirect_uri");
            requestValidator.requireLength(codeVerifier, 128, "code_verifier");
            requestValidator.requireLength(
                    resource, OAuthRequestValidator.MAX_RESOURCE_LENGTH, "resource");
        }
        if ("client_credentials".equals(grantType)) {
            requestValidator.requireLength(
                    resource, OAuthRequestValidator.MAX_RESOURCE_LENGTH, "resource");
            requestValidator.requireLength(scope, OAuthRequestValidator.MAX_SCOPE_LENGTH, "scope");
        }
    }

    private String requestId(HttpServletRequest request) {
        String candidate = request.getHeader(ForgeMcpAuthenticationFilter.REQUEST_ID_HEADER);
        return candidate != null && candidate.matches("^[A-Za-z0-9_-]{1,64}$")
                ? candidate
                : UUID.randomUUID().toString();
    }

    private ResponseEntity<Map<String, Object>> temporarilyUnavailable(
            String endpoint,
            String grantType,
            String requestId,
            RuntimeException exception) {
        log.error("[MCP OAuth] requestId={}, endpoint={}, grantType={}, resultCode=temporarily_unavailable, exceptionType={}",
                requestId, endpoint, safeGrantType(grantType),
                exception.getClass().getSimpleName(), exception);
        return tokenError(new BusinessException(503, "temporarily_unavailable"), requestId);
    }

    private String safeGrantType(String grantType) {
        return grantType != null && grantType.matches("^[a-z_]{1,64}$") ? grantType : "unknown";
    }

    private record ClientCredentials(String clientId, String clientSecret) {
    }
}
