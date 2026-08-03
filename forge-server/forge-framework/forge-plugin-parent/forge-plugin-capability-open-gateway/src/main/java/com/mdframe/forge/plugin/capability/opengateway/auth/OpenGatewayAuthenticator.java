package com.mdframe.forge.plugin.capability.opengateway.auth;

import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityClientMapper;
import com.mdframe.forge.plugin.capability.controlplane.security.CapabilityClientActorMode;
import com.mdframe.forge.plugin.capability.identity.config.CapabilityIdentityProperties;
import com.mdframe.forge.plugin.capability.identity.security.AuthenticatedCapabilityIdentity;
import com.mdframe.forge.plugin.capability.identity.security.CapabilityIdentityInfrastructureException;
import com.mdframe.forge.plugin.capability.identity.security.CapabilitySecurityPrincipal;
import com.mdframe.forge.plugin.capability.identity.security.CapabilityTenantContext;
import com.mdframe.forge.plugin.capability.identity.token.CapabilityAccessTokenService;
import com.mdframe.forge.plugin.capability.opengateway.exception.OpenGatewayException;
import com.mdframe.forge.plugin.capability.spi.ScopeBasedCapabilityAuthorizationPolicy;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import com.mdframe.forge.starter.openapi.security.replay.OpenApiReplayGuard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

/**
 * 开放网关认证器：支持 OAuth 短期令牌（USER 委托 / SERVICE）与
 * HMAC-SHA256 客户端签名（仅 SERVICE）两种模式，产出统一的已验证能力身份。
 */
@RequiredArgsConstructor
@Slf4j
public class OpenGatewayAuthenticator {

    public static final String HEADER_APP_ID = "X-Forge-App-Id";
    public static final String HEADER_TIMESTAMP = "X-Forge-Timestamp";
    public static final String HEADER_NONCE = "X-Forge-Nonce";
    public static final String HEADER_SIGNATURE = "X-Forge-Signature";

    private static final String AUTH_MODE_OAUTH = "OAUTH";
    private static final String AUTH_MODE_SIGNATURE = "SIGNATURE";
    private static final String SIGNATURE_AUDIENCE = "openapi";

    private final CapabilityAccessTokenService tokenService;
    private final AiCapabilityClientMapper clientMapper;
    private final OpenApiReplayGuard replayGuard;
    private final PersistentCryptoService persistentCryptoService;
    private final IUserLoadService userLoadService;
    private final CapabilityIdentityProperties identityProperties;

    /**
     * 认证入口。请求体字节由入口层缓存后传入（签名校验需要 body 摘要，流只能读取一次）。
     */
    public AuthenticatedCapabilityIdentity authenticate(HttpServletRequest request, byte[] body) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authenticateBearer(authorization.substring("Bearer ".length()).trim());
        }
        if (StringUtils.isNotBlank(request.getHeader(HEADER_APP_ID))) {
            return authenticateSignature(request, body);
        }
        throw unauthorized("缺少有效的开放网关调用凭据");
    }

    private AuthenticatedCapabilityIdentity authenticateBearer(String rawToken) {
        if (StringUtils.isBlank(rawToken)) {
            throw unauthorized("访问令牌缺失");
        }
        AuthenticatedCapabilityIdentity identity;
        try {
            identity = tokenService.authenticate(
                    rawToken, identityProperties.validatedOpenapiResource(), null);
        }
        catch (BusinessException exception) {
            if (Integer.valueOf(403).equals(exception.getCode())) {
                throw new OpenGatewayException("FORBIDDEN", 403, "访问令牌权限不足", exception);
            }
            throw new OpenGatewayException("UNAUTHORIZED", 401, "访问令牌无效或已过期", exception);
        }
        catch (CapabilityIdentityInfrastructureException exception) {
            throw new OpenGatewayException("INTERNAL_ERROR", 503, "开放网关身份服务暂不可用", exception);
        }
        AiCapabilityClient client = CapabilityTenantContext.executeCredentialLookup(
                () -> clientMapper.selectCredentialById(identity.principal().clientId()));
        if (client == null || !hasAuthMode(client.getAuthModes(), AUTH_MODE_OAUTH)) {
            throw unauthorized("该客户端未启用 OAuth 调用模式");
        }
        return identity;
    }

    private AuthenticatedCapabilityIdentity authenticateSignature(HttpServletRequest request, byte[] body) {
        String appId = StringUtils.trimToNull(request.getHeader(HEADER_APP_ID));
        String timestamp = StringUtils.trimToNull(request.getHeader(HEADER_TIMESTAMP));
        String nonce = StringUtils.trimToNull(request.getHeader(HEADER_NONCE));
        String signature = StringUtils.trimToNull(request.getHeader(HEADER_SIGNATURE));
        if (appId == null || timestamp == null || nonce == null || signature == null) {
            throw unauthorized("签名请求头不完整");
        }
        long timestampMillis;
        try {
            timestampMillis = Long.parseLong(timestamp);
        }
        catch (NumberFormatException exception) {
            throw unauthorized("请求时间戳格式非法");
        }
        assertNotReplayed(appId, timestampMillis, nonce);

        Long clientId;
        try {
            clientId = Long.valueOf(appId);
        }
        catch (NumberFormatException exception) {
            throw unauthorized("签名 AppId 格式非法");
        }
        AiCapabilityClient client = CapabilityTenantContext.executeCredentialLookup(
                () -> clientMapper.selectCredentialById(clientId));
        LocalDateTime now = LocalDateTime.now();
        if (client == null
                || !"ENABLED".equals(client.getStatus())
                || (client.getExpiresAt() != null && !client.getExpiresAt().isAfter(now))
                || !allowsServiceIdentity(client.getActorMode())
                || !hasAuthMode(client.getAuthModes(), AUTH_MODE_SIGNATURE)
                || StringUtils.isBlank(client.getSigningKeyCipher())
                || client.getServiceUserId() == null || client.getServiceUserId() <= 0
                || client.getTenantId() == null || client.getTenantId() <= 0
                || client.getActiveOrgId() == null || client.getActiveOrgId() <= 0) {
            throw unauthorized("签名凭据无效");
        }
        verifySignature(request, body, appId, timestamp, nonce, signature, client);

        LoginUser loginUser = CapabilityTenantContext.execute(
                client.getTenantId(), () -> loadServiceUser(client));
        CapabilitySecurityPrincipal principal = new CapabilitySecurityPrincipal(
                client.getId(), client.getClientCode(), CapabilityActorType.SERVICE,
                client.getServiceUserId(), client.getServiceUserId(), client.getTenantId(),
                client.getActiveOrgId(), client.getCredentialVersion(),
                "sig:" + appId + ":" + client.getSigningKeyVersion(), SIGNATURE_AUDIENCE,
                Set.of(ScopeBasedCapabilityAuthorizationPolicy.INVOKE_SCOPE));
        return new AuthenticatedCapabilityIdentity(principal, loginUser);
    }

    private void assertNotReplayed(String appId, long timestampMillis, String nonce) {
        try {
            replayGuard.assertNotReplayed(appId, timestampMillis, nonce);
        }
        catch (BusinessException exception) {
            if (Integer.valueOf(503).equals(exception.getCode())) {
                throw new OpenGatewayException("INTERNAL_ERROR", 503, exception.getMessage(), exception);
            }
            throw new OpenGatewayException("REPLAY_REJECTED", 401, exception.getMessage(), exception);
        }
    }

    private void verifySignature(
            HttpServletRequest request, byte[] body,
            String appId, String timestamp, String nonce, String signature,
            AiCapabilityClient client) {
        String signingKey;
        try {
            signingKey = persistentCryptoService.decrypt(client.getSigningKeyCipher(), null);
        }
        catch (RuntimeException exception) {
            throw new OpenGatewayException("INTERNAL_ERROR", 503, "开放网关签名密钥暂不可用", exception);
        }
        String canonical = appId + "\n" + timestamp + "\n" + nonce + "\n"
                + request.getMethod().toUpperCase(Locale.ROOT) + "\n"
                + request.getRequestURI() + "\n" + sha256Hex(body);
        String expected = hmacSha256Hex(signingKey, canonical);
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = signature.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedBytes, providedBytes)) {
            throw unauthorized("请求签名校验失败");
        }
    }

    /**
     * 加载并严格校验客户端绑定的服务账号（镜像短期令牌 SERVICE 身份校验语义）。
     */
    private LoginUser loadServiceUser(AiCapabilityClient client) {
        LoginUser loginUser;
        try {
            loginUser = userLoadService.loadUserByUserId(
                    client.getServiceUserId(), client.getTenantId(), client.getActiveOrgId());
        }
        catch (RuntimeException exception) {
            if (exception instanceof BusinessException) {
                throw unauthorized("签名凭据无效");
            }
            throw new OpenGatewayException("INTERNAL_ERROR", 503, "开放网关用户目录暂不可用", exception);
        }
        boolean exactIdentity = loginUser != null
                && client.getServiceUserId().equals(loginUser.getUserId())
                && client.getTenantId().equals(loginUser.getTenantId())
                && client.getActiveOrgId().equals(loginUser.getActiveOrgId())
                && Integer.valueOf(1).equals(loginUser.getUserStatus())
                && !Boolean.TRUE.equals(loginUser.getForcePasswordChange());
        if (!exactIdentity
                || loginUser.isAdmin()
                || loginUser.getRoleIds() == null
                || loginUser.getRoleIds().isEmpty()) {
            throw unauthorized("签名凭据无效");
        }
        return loginUser;
    }

    private boolean hasAuthMode(String authModes, String mode) {
        return authModes != null && Arrays.stream(authModes.split(","))
                .map(String::trim)
                .anyMatch(mode::equals);
    }

    private boolean allowsServiceIdentity(String actorMode) {
        try {
            CapabilityClientActorMode mode = actorMode == null || actorMode.isBlank()
                    ? CapabilityClientActorMode.HYBRID
                    : CapabilityClientActorMode.valueOf(actorMode);
            return mode.requiresServiceIdentity();
        }
        catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String sha256Hex(byte[] body) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(body == null ? new byte[0] : body);
            return HexFormat.of().formatHex(digest);
        }
        catch (Exception exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }

    private String hmacSha256Hex(String key, String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception exception) {
            throw new IllegalStateException("当前 JDK 不支持 HmacSHA256", exception);
        }
    }

    private OpenGatewayException unauthorized(String message) {
        return new OpenGatewayException("UNAUTHORIZED", 401, message);
    }
}
