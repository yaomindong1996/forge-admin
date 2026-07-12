package com.mdframe.forge.plugin.capability.identity.oauth;

import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityClientMapper;
import com.mdframe.forge.plugin.capability.identity.domain.AiCapabilityOAuthRedirectUri;
import com.mdframe.forge.plugin.capability.identity.mapper.AiCapabilityOAuthRedirectUriMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CapabilityOAuthClientConfigService {

    private final AiCapabilityClientMapper clientMapper;
    private final AiCapabilityOAuthRedirectUriMapper redirectUriMapper;

    @Transactional(rollbackFor = Exception.class)
    public void configure(
            Long tenantId,
            Long clientId,
            CapabilityOAuthClientConfigRequest request) {
        AiCapabilityClient client = clientMapper.selectTenantById(requireTenant(tenantId), clientId);
        if (client == null) {
            throw new BusinessException("客户端不存在或无权访问");
        }
        String clientType = request.clientType().trim().toUpperCase();
        if (!Set.of("PUBLIC", "CONFIDENTIAL").contains(clientType)) {
            throw new BusinessException("OAuth 客户端类型只允许 PUBLIC 或 CONFIDENTIAL");
        }
        Set<String> redirects = validateRedirectUris(request.redirectUris());
        if (Boolean.TRUE.equals(request.enabled())
                && "PUBLIC".equals(clientType)
                && redirects.isEmpty()) {
            throw new BusinessException("PUBLIC 客户端至少需要一个回调地址");
        }
        if (clientMapper.configureOAuth(
                tenantId, clientId, client.getCredentialVersion(),
                Boolean.TRUE.equals(request.enabled()) ? 1 : 0, clientType) == 0) {
            throw new BusinessException("客户端配置已发生变化，请刷新后重试");
        }
        redirectUriMapper.disableByClient(tenantId, clientId);
        if (Boolean.TRUE.equals(request.enabled())) {
            for (String redirect : redirects) {
                AiCapabilityOAuthRedirectUri entity = new AiCapabilityOAuthRedirectUri();
                entity.setTenantId(tenantId);
                entity.setClientId(clientId);
                entity.setRedirectUri(redirect);
                entity.setRedirectUriHash(DatabaseExactRedirectUriRegistry.sha256(redirect));
                entity.setStatus("ENABLED");
                entity.setDelFlag(0);
                redirectUriMapper.insert(entity);
            }
        }
    }

    public List<String> listRedirectUris(Long tenantId, Long clientId) {
        return redirectUriMapper.selectEnabledByClient(requireTenant(tenantId), clientId)
                .stream()
                .map(AiCapabilityOAuthRedirectUri::getRedirectUri)
                .toList();
    }

    private Set<String> validateRedirectUris(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        if (values.size() > 20) {
            throw new BusinessException("单个客户端最多配置 20 个回调地址");
        }
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank() || value.length() > 2048) {
                throw new BusinessException("回调地址不能为空且长度不能超过 2048");
            }
            URI uri;
            try {
                uri = URI.create(value);
            } catch (IllegalArgumentException exception) {
                throw new BusinessException("回调地址格式无效");
            }
            boolean localHttp = "http".equalsIgnoreCase(uri.getScheme())
                    && ("localhost".equalsIgnoreCase(uri.getHost())
                    || "127.0.0.1".equals(uri.getHost())
                    || "::1".equals(uri.getHost()));
            if (!("https".equalsIgnoreCase(uri.getScheme()) || localHttp)
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getFragment() != null) {
                throw new BusinessException("回调地址必须使用 HTTPS；仅本地回调允许 localhost HTTP");
            }
            result.add(value);
        }
        return result;
    }

    private Long requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("未获取到有效租户上下文");
        }
        return tenantId;
    }
}
