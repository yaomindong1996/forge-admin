package com.mdframe.forge.plugin.capability.controlplane.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityClientCreateDTO;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityClientMapper;
import com.mdframe.forge.plugin.capability.controlplane.security.CapabilityClientPrincipal;
import com.mdframe.forge.plugin.capability.controlplane.security.CapabilityClientSecretCodec;
import com.mdframe.forge.plugin.capability.controlplane.security.IssuedClientSecret;
import com.mdframe.forge.plugin.capability.controlplane.vo.CapabilityClientSecretVO;
import com.mdframe.forge.plugin.capability.controlplane.vo.CapabilityClientVO;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CapabilityClientService {

    private static final String DUMMY_HASH = "0".repeat(64);

    private final AiCapabilityClientMapper clientMapper;
    private final CapabilityClientSecretCodec secretCodec;
    private final Clock capabilityClock;

    public Page<CapabilityClientVO> page(
            Long tenantId,
            PageQuery pageQuery,
            String keyword,
            String status) {
        Page<AiCapabilityClient> source = clientMapper.selectPage(
                pageQuery.toPage(), requireTenant(tenantId), keyword, status);
        Page<CapabilityClientVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(CapabilityClientVO::from).toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public CapabilityClientSecretVO create(Long tenantId, CapabilityClientCreateDTO dto) {
        Long safeTenantId = requireTenant(tenantId);
        requireServiceIdentity(dto.serviceUserId(), dto.activeOrgId());
        if (clientMapper.selectByCode(safeTenantId, dto.clientCode()) != null) {
            throw new BusinessException("客户端编码已存在");
        }
        IssuedClientSecret issued = secretCodec.issue(dto.clientCode());
        AiCapabilityClient client = new AiCapabilityClient();
        client.setTenantId(safeTenantId);
        client.setClientCode(dto.clientCode());
        client.setClientName(dto.clientName());
        client.setKeyId(issued.keyId());
        client.setKeyPrefix(issued.keyPrefix());
        client.setKeyHash(issued.keyHash());
        client.setCredentialVersion(1);
        client.setServiceUserId(dto.serviceUserId());
        client.setActiveOrgId(dto.activeOrgId());
        client.setOauthEnabled(0);
        client.setOauthClientType("CONFIDENTIAL");
        client.setStatus("ENABLED");
        client.setExpiresAt(dto.expiresAt());
        client.setRemark(dto.remark());
        client.setDelFlag(0);
        clientMapper.insert(client);
        return secretResponse(client, issued.rawSecret());
    }

    @Transactional(rollbackFor = Exception.class)
    public CapabilityClientSecretVO rotate(Long tenantId, Long clientId) {
        AiCapabilityClient client = requireClient(tenantId, clientId);
        if ("REVOKED".equals(client.getStatus())) {
            throw new BusinessException("已吊销客户端不能轮换密钥");
        }
        IssuedClientSecret issued = secretCodec.issue(client.getClientCode());
        Integer currentVersion = requireCredentialVersion(client);
        if (clientMapper.rotateCredential(
                client.getTenantId(), client.getId(), currentVersion,
                issued.keyId(), issued.keyPrefix(), issued.keyHash()) == 0) {
            throw new BusinessException("客户端凭据已发生变化，请刷新后重试");
        }
        client.setKeyId(issued.keyId());
        client.setKeyPrefix(issued.keyPrefix());
        client.setCredentialVersion(currentVersion + 1);
        return secretResponse(client, issued.rawSecret());
    }

    public CapabilityClientPrincipal authenticate(String rawSecret) {
        String keyId = secretCodec.extractKeyId(rawSecret);
        AiCapabilityClient client = keyId == null ? null : clientMapper.selectCredentialByKeyId(keyId);
        boolean matches = secretCodec.matches(
                rawSecret,
                client == null ? DUMMY_HASH : client.getKeyHash());
        LocalDateTime now = LocalDateTime.now(capabilityClock);
        if (client == null || !matches || !"ENABLED".equals(client.getStatus())
                || !hasValidServiceIdentity(client.getServiceUserId(), client.getActiveOrgId())
                || (client.getExpiresAt() != null && !client.getExpiresAt().isAfter(now))) {
            throw new BusinessException("客户端凭据无效或已失效");
        }
        Integer credentialVersion = requireCredentialVersion(client);
        if (clientMapper.touchLastUsed(
                client.getTenantId(), client.getId(), credentialVersion, client.getKeyHash(), now) == 0) {
            throw new BusinessException("客户端凭据无效或已失效");
        }
        return new CapabilityClientPrincipal(
                client.getId(), client.getClientCode(), client.getTenantId(),
                client.getServiceUserId(), client.getActiveOrgId(), credentialVersion);
    }

    public void revoke(Long tenantId, Long clientId) {
        AiCapabilityClient client = requireClient(tenantId, clientId);
        if ("REVOKED".equals(client.getStatus())) {
            return;
        }
        Integer credentialVersion = requireCredentialVersion(client);
        if (clientMapper.revokeCredential(
                client.getTenantId(), client.getId(), credentialVersion) == 0) {
            throw new BusinessException("客户端凭据已发生变化，请刷新后重试");
        }
    }

    public AiCapabilityClient requireClient(Long tenantId, Long clientId) {
        AiCapabilityClient client = clientMapper.selectTenantById(requireTenant(tenantId), clientId);
        if (client == null) {
            throw new BusinessException("客户端不存在或无权访问");
        }
        return client;
    }

    private CapabilityClientSecretVO secretResponse(AiCapabilityClient client, String rawSecret) {
        return new CapabilityClientSecretVO(
                client.getId(), client.getClientCode(), client.getKeyPrefix(),
                rawSecret, client.getCredentialVersion());
    }

    private Long requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("未获取到有效租户上下文");
        }
        return tenantId;
    }

    private void requireServiceIdentity(Long serviceUserId, Long activeOrgId) {
        if (!hasValidServiceIdentity(serviceUserId, activeOrgId)) {
            throw new BusinessException("机器客户端必须绑定有效服务账号和活动组织");
        }
    }

    private boolean hasValidServiceIdentity(Long serviceUserId, Long activeOrgId) {
        return serviceUserId != null && serviceUserId > 0
                && activeOrgId != null && activeOrgId > 0;
    }

    private Integer requireCredentialVersion(AiCapabilityClient client) {
        if (client.getCredentialVersion() == null || client.getCredentialVersion() <= 0) {
            throw new BusinessException("客户端凭据版本无效");
        }
        return client.getCredentialVersion();
    }
}
