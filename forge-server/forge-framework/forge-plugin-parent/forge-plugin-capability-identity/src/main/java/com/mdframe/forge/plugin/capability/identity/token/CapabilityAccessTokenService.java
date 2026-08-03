package com.mdframe.forge.plugin.capability.identity.token;

import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityClientMapper;
import com.mdframe.forge.plugin.capability.controlplane.security.CapabilityClientActorMode;
import com.mdframe.forge.plugin.capability.identity.config.CapabilityIdentityProperties;
import com.mdframe.forge.plugin.capability.identity.domain.AiCapabilityAccessToken;
import com.mdframe.forge.plugin.capability.identity.mapper.AiCapabilityAccessTokenMapper;
import com.mdframe.forge.plugin.capability.identity.security.AuthenticatedCapabilityIdentity;
import com.mdframe.forge.plugin.capability.identity.security.CapabilityIdentityInfrastructureException;
import com.mdframe.forge.plugin.capability.identity.security.CapabilitySecurityPrincipal;
import com.mdframe.forge.plugin.capability.identity.security.CapabilityTenantContext;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CapabilityAccessTokenService {

    private static final String DUMMY_HASH = "0".repeat(64);

    private final AiCapabilityAccessTokenMapper tokenMapper;
    private final AiCapabilityClientMapper clientMapper;
    private final CapabilityAccessTokenCodec tokenCodec;
    private final CapabilityIdentityProperties properties;
    private final IUserLoadService userLoadService;
    private final Clock clock;

    @Transactional(rollbackFor = Exception.class)
    public CapabilityTokenResponse issue(CapabilityTokenIssueCommand command) {
        validateIssueCommand(command);
        return CapabilityTenantContext.execute(
                command.tenantId(), () -> issueWithinTenant(command));
    }

    private CapabilityTokenResponse issueWithinTenant(CapabilityTokenIssueCommand command) {
        LocalDateTime issuedAt = LocalDateTime.now(clock);
        AiCapabilityAccessToken identityProbe = identityProbe(command);
        AiCapabilityClient currentClient = clientMapper.selectTenantById(
                command.tenantId(), command.clientId());
        if (!isCurrentClient(currentClient, identityProbe, issuedAt)) {
            throw unauthorized();
        }
        loadCurrentUser(identityProbe, command.actorType());

        IssuedCapabilityAccessToken issued = tokenCodec.issue();
        long expiresIn = properties.validatedAccessTokenTtl().toSeconds();

        AiCapabilityAccessToken token = new AiCapabilityAccessToken();
        token.setTenantId(command.tenantId());
        token.setTokenKeyId(issued.keyId());
        token.setTokenPrefix(issued.prefix());
        token.setTokenHash(issued.tokenHash());
        token.setClientId(command.clientId());
        token.setCredentialVersion(command.credentialVersion());
        token.setActorType(command.actorType().name());
        token.setActorUserId(command.actorUserId());
        token.setServiceUserId(command.serviceUserId());
        token.setActiveOrgId(command.activeOrgId());
        token.setAudience(command.audience());
        token.setScopes(serializeScopes(command.scopes()));
        token.setStatus("ACTIVE");
        token.setIssuedAt(issuedAt);
        token.setExpiresAt(issuedAt.plusSeconds(expiresIn));
        token.setDelFlag(0L);
        tokenMapper.insert(token);

        return new CapabilityTokenResponse(
                issued.rawToken(), "Bearer", expiresIn, token.getScopes(), command.audience());
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthenticatedCapabilityIdentity authenticate(
            String rawToken,
            String expectedAudience,
            Set<String> requiredScopes) {
        String keyId = tokenCodec.extractKeyId(rawToken);
        AiCapabilityAccessToken token = keyId == null
                ? null
                : CapabilityTenantContext.executeCredentialLookup(
                        () -> tokenMapper.selectActiveByTokenKeyId(keyId));
        boolean tokenMatches = tokenCodec.matches(
                rawToken, token == null ? DUMMY_HASH : token.getTokenHash());
        LocalDateTime now = LocalDateTime.now(clock);
        if (token == null || !tokenMatches || !"ACTIVE".equals(token.getStatus())
                || token.getTenantId() == null || token.getTenantId() <= 0
                || token.getExpiresAt() == null || !token.getExpiresAt().isAfter(now)
                || !properties.supportsResource(expectedAudience)
                || !expectedAudience.equals(token.getAudience())) {
            throw unauthorized();
        }

        Set<String> scopes = parseScopes(token.getScopes());
        if (requiredScopes != null && !scopes.containsAll(requiredScopes)) {
            throw new BusinessException(403, "insufficient_scope");
        }

        return CapabilityTenantContext.execute(
                token.getTenantId(),
                () -> authenticateWithinTenant(token, now, scopes));
    }

    private AuthenticatedCapabilityIdentity authenticateWithinTenant(
            AiCapabilityAccessToken token,
            LocalDateTime now,
            Set<String> scopes) {
        AiCapabilityClient client = clientMapper.selectTenantById(token.getTenantId(), token.getClientId());
        if (!isCurrentClient(client, token, now)) {
            throw unauthorized();
        }

        CapabilityActorType actorType;
        try {
            actorType = CapabilityActorType.valueOf(token.getActorType());
        } catch (RuntimeException exception) {
            throw unauthorized();
        }
        LoginUser loginUser = loadCurrentUser(token, actorType);
        LocalDateTime touchCutoff = now.minus(properties.validatedLastUsedTouchInterval());
        if (token.getLastUsedAt() == null || token.getLastUsedAt().isBefore(touchCutoff)) {
            if (tokenMapper.touchLastUsed(token.getTenantId(), token.getId(), now) == 0) {
                throw unauthorized();
            }
        }

        CapabilitySecurityPrincipal principal = new CapabilitySecurityPrincipal(
                client.getId(), client.getClientCode(), actorType,
                token.getActorUserId(), token.getServiceUserId(), token.getTenantId(),
                token.getActiveOrgId(), token.getCredentialVersion(), token.getTokenKeyId(),
                token.getAudience(), scopes);
        return new AuthenticatedCapabilityIdentity(principal, loginUser);
    }

    @Transactional(rollbackFor = Exception.class)
    public void revoke(String rawToken, Long authenticatedClientId) {
        String keyId = tokenCodec.extractKeyId(rawToken);
        AiCapabilityAccessToken token = keyId == null
                ? null
                : CapabilityTenantContext.executeCredentialLookup(
                        () -> tokenMapper.selectActiveByTokenKeyId(keyId));
        boolean matches = tokenCodec.matches(rawToken, token == null ? DUMMY_HASH : token.getTokenHash());
        if (token == null || token.getTenantId() == null || token.getTenantId() <= 0
                || !matches || !Objects.equals(token.getClientId(), authenticatedClientId)) {
            return;
        }
        CapabilityTenantContext.execute(
                token.getTenantId(),
                () -> {
                    tokenMapper.revoke(
                            token.getTenantId(), token.getId(), LocalDateTime.now(clock));
                });
    }

    @Transactional(rollbackFor = Exception.class)
    public void revokeUserToken(
            String rawToken,
            Long currentUserId,
            Long currentTenantId,
            Long expectedClientId) {
        String keyId = tokenCodec.extractKeyId(rawToken);
        AiCapabilityAccessToken token = keyId == null
                ? null
                : CapabilityTenantContext.executeCredentialLookup(
                        () -> tokenMapper.selectActiveByTokenKeyId(keyId));
        boolean matches = tokenCodec.matches(
                rawToken, token == null ? DUMMY_HASH : token.getTokenHash());
        if (token == null || !matches) {
            return;
        }
        if (!CapabilityActorType.USER.name().equals(token.getActorType())
                || !Objects.equals(token.getActorUserId(), currentUserId)
                || !Objects.equals(token.getTenantId(), currentTenantId)
                || !Objects.equals(token.getClientId(), expectedClientId)) {
            throw new BusinessException(403, "无权撤销该 MCP 用户委托令牌");
        }
        CapabilityTenantContext.execute(
                currentTenantId,
                () -> {
                    tokenMapper.revoke(
                            token.getTenantId(), token.getId(), LocalDateTime.now(clock));
                });
    }

    private LoginUser loadCurrentUser(AiCapabilityAccessToken token, CapabilityActorType actorType) {
        LoginUser loginUser;
        try {
            loginUser = userLoadService.loadUserByUserId(
                    token.getActorUserId(), token.getTenantId(), token.getActiveOrgId());
        } catch (RuntimeException exception) {
            if (exception instanceof BusinessException) {
                throw unauthorized();
            }
            throw new CapabilityIdentityInfrastructureException("MCP 用户目录暂不可用", exception);
        }
        boolean exactIdentity = loginUser != null
                && token.getActorUserId().equals(loginUser.getUserId())
                && token.getTenantId().equals(loginUser.getTenantId())
                && token.getActiveOrgId().equals(loginUser.getActiveOrgId())
                && Integer.valueOf(1).equals(loginUser.getUserStatus())
                && !Boolean.TRUE.equals(loginUser.getForcePasswordChange());
        if (!exactIdentity) {
            throw unauthorized();
        }
        if (actorType == CapabilityActorType.SERVICE
                && (loginUser.isAdmin()
                || loginUser.getRoleIds() == null
                || loginUser.getRoleIds().isEmpty()
                || !token.getServiceUserId().equals(loginUser.getUserId()))) {
            throw unauthorized();
        }
        return loginUser;
    }

    private boolean isCurrentClient(
            AiCapabilityClient client,
            AiCapabilityAccessToken token,
            LocalDateTime now) {
        CapabilityClientActorMode actorMode = actorMode(client == null ? null : client.getActorMode());
        CapabilityActorType actorType = actorType(token.getActorType());
        if (client == null || actorMode == null || actorType == null
                || !"ENABLED".equals(client.getStatus())
                || !Integer.valueOf(1).equals(client.getOauthEnabled())
                || !Objects.equals(token.getCredentialVersion(), client.getCredentialVersion())
                || (client.getExpiresAt() != null && !client.getExpiresAt().isAfter(now))) {
            return false;
        }
        if (actorType == CapabilityActorType.SERVICE) {
            return actorMode.requiresServiceIdentity()
                    && Objects.equals(token.getActorUserId(), client.getServiceUserId())
                    && Objects.equals(token.getServiceUserId(), client.getServiceUserId())
                    && Objects.equals(token.getActiveOrgId(), client.getActiveOrgId());
        }
        if (!actorMode.allowsUserDelegation()) {
            return false;
        }
        if (actorMode == CapabilityClientActorMode.USER_DELEGATION) {
            return client.getServiceUserId() == null
                    && client.getActiveOrgId() == null
                    && token.getServiceUserId() == null;
        }
        return Objects.equals(token.getServiceUserId(), client.getServiceUserId())
                && Objects.equals(token.getActiveOrgId(), client.getActiveOrgId());
    }

    private void validateIssueCommand(CapabilityTokenIssueCommand command) {
        if (command == null
                || command.clientId() == null || command.clientId() <= 0
                || command.credentialVersion() == null || command.credentialVersion() <= 0
                || command.actorType() == null
                || command.actorUserId() == null || command.actorUserId() <= 0
                || command.tenantId() == null || command.tenantId() <= 0
                || command.activeOrgId() == null || command.activeOrgId() <= 0
                || !properties.supportsResource(command.audience())
                || command.scopes() == null || command.scopes().isEmpty()) {
            throw new BusinessException("短期令牌身份参数无效");
        }
        if (command.actorType() == CapabilityActorType.SERVICE) {
            if (command.serviceUserId() == null || command.serviceUserId() <= 0
                    || !command.actorUserId().equals(command.serviceUserId())) {
                throw new BusinessException("SERVICE Token 的 actor 必须是绑定服务账号");
            }
        }
        else if (command.serviceUserId() != null && command.serviceUserId() <= 0) {
            throw new BusinessException("USER Token 的服务账号参数无效");
        }
    }

    private CapabilityClientActorMode actorMode(String value) {
        try {
            return value == null || value.isBlank()
                    ? CapabilityClientActorMode.HYBRID
                    : CapabilityClientActorMode.valueOf(value);
        }
        catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private CapabilityActorType actorType(String value) {
        try {
            return CapabilityActorType.valueOf(value);
        }
        catch (RuntimeException exception) {
            return null;
        }
    }

    private AiCapabilityAccessToken identityProbe(CapabilityTokenIssueCommand command) {
        AiCapabilityAccessToken token = new AiCapabilityAccessToken();
        token.setTenantId(command.tenantId());
        token.setClientId(command.clientId());
        token.setCredentialVersion(command.credentialVersion());
        token.setActorType(command.actorType().name());
        token.setActorUserId(command.actorUserId());
        token.setServiceUserId(command.serviceUserId());
        token.setActiveOrgId(command.activeOrgId());
        return token;
    }

    private String serializeScopes(Set<String> scopes) {
        return String.join(" ", new TreeSet<>(scopes));
    }

    private Set<String> parseScopes(String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(scopes.trim().split(" +"))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private BusinessException unauthorized() {
        return new BusinessException(401, "invalid_token");
    }
}
