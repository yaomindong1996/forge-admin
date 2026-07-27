package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.SysClientDTO;
import com.mdframe.forge.plugin.system.entity.SysClient;
import com.mdframe.forge.plugin.system.mapper.SysClientMapper;
import com.mdframe.forge.plugin.system.security.ClientCredentialPolicy;
import com.mdframe.forge.plugin.system.security.ClientSecretCodec;
import com.mdframe.forge.plugin.system.service.IClientService;
import com.mdframe.forge.starter.cache.service.ICacheService;
import com.mdframe.forge.starter.core.context.AuthProperties;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl extends ServiceImpl<SysClientMapper, SysClient> implements IClientService {

    private static final String CLIENT_CONFIG_CACHE_KEY = "client:config:";

    private final ICacheService cacheService;
    private final SysClientMapper clientMapper;
    private final ClientSecretCodec clientSecretCodec;
    private final ClientCredentialPolicy credentialPolicy;
    private final AuthProperties authProperties;

    @Override
    public SysClient getByCode(String clientCode) {
        if (StrUtil.isBlank(clientCode)) {
            return null;
        }
        return clientMapper.selectByClientCode(clientCode);
    }

    @Override
    public SysClient getByAppId(String appId) {
        return StrUtil.isBlank(appId) ? null : clientMapper.selectByAppId(appId);
    }

    @Override
    public Page<SysClient> selectClientPage(long pageNum, long pageSize,
                                            String clientCode, String clientName, Integer status) {
        return clientMapper.selectClientPage(
                new Page<>(pageNum, pageSize), clientCode, clientName, status);
    }

    @Override
    public List<SysClient> listEnabledClients() {
        return clientMapper.selectEnabledClients(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createClient(SysClientDTO dto) {
        SysClient client = new SysClient();
        BeanUtils.copyProperties(dto, client, "appSecret", "clientAuthMethod");
        try {
            String authMethod = credentialPolicy.normalizeAuthMethod(dto.getClientAuthMethod());
            client.setClientAuthMethod(authMethod);
            client.setAppSecret(credentialPolicy.resolveCreateSecret(authMethod, dto.getAppSecret()));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(exception.getMessage());
        }
        return clientMapper.insert(client) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateClient(SysClientDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("客户端ID不能为空");
        }
        SysClient existing = clientMapper.selectById(dto.getId());
        if (existing == null) {
            return false;
        }

        SysClient client = new SysClient();
        BeanUtils.copyProperties(dto, client, "appSecret", "clientAuthMethod");
        String storedSecret;
        try {
            String requestedAuthMethod = StrUtil.isBlank(dto.getClientAuthMethod())
                    ? existing.getClientAuthMethod()
                    : dto.getClientAuthMethod();
            String authMethod = credentialPolicy.normalizeAuthMethod(requestedAuthMethod);
            client.setClientAuthMethod(authMethod);
            storedSecret = credentialPolicy.resolveUpdateSecret(
                    authMethod, dto.getAppSecret(), existing.getAppSecret());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(exception.getMessage());
        }

        boolean secretChanged = !Objects.equals(existing.getAppSecret(), storedSecret);
        if (clientMapper.updateClientIfUnchanged(
                client,
                existing.getClientAuthMethod(),
                existing.getAppSecret(),
                storedSecret,
                secretChanged) <= 0) {
            throw new BusinessException("客户端更新失败");
        }
        evictClientCacheAfterCommit(existing.getClientCode(), dto.getClientCode());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteClient(Long id) {
        SysClient existing = clientMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        boolean deleted = clientMapper.deleteById(id) > 0;
        if (deleted) {
            evictClientCacheAfterCommit(existing.getClientCode());
        }
        return deleted;
    }

    @Override
    public boolean requiresAppSecret(SysClient client) {
        return client != null && credentialPolicy.requiresSecret(client.getClientAuthMethod());
    }

    @Override
    public boolean validateAppSecret(SysClient client, String appSecret) {
        if (!requiresAppSecret(client)) {
            return false;
        }
        boolean allowLegacy = Boolean.TRUE.equals(authProperties.getEnableLegacyClientSecretRead());
        ClientSecretCodec.MatchResult result = clientSecretCodec.verify(
                appSecret, client.getAppSecret(), allowLegacy);
        if (result.matched() && result.legacy()) {
            upgradeLegacySecret(client, appSecret);
        }
        return result.matched();
    }

    @Override
    public List<SysClient> listByTenant(Long tenantId) {
        return clientMapper.selectEnabledClients(tenantId);
    }

    @Override
    public void reloadClientConfigCache(String clientCode) {
        evictClientCache(clientCode);
        SysClient client = clientMapper.selectByClientCode(clientCode);
        if (client != null) {
            log.info("客户端配置缓存已清理并校验数据库配置: clientCode={}", clientCode);
        }
    }

    @Override
    public String getMaskedAppSecret(Long clientId) {
        SysClient client = clientMapper.selectById(clientId);
        return client == null || StrUtil.isBlank(client.getAppSecret()) ? "" : "****";
    }

    @Override
    public int countLegacyPlaintextSecrets() {
        return clientMapper.countLegacyPlaintextSecrets();
    }

    private void upgradeLegacySecret(SysClient client, String rawSecret) {
        String encoded;
        try {
            encoded = clientSecretCodec.encodeLegacyUpgrade(rawSecret);
        } catch (IllegalArgumentException exception) {
            log.warn("历史客户端密钥无法自动升级: clientCode={}", client.getClientCode());
            return;
        }
        int upgraded = clientMapper.updateAppSecretIfUnchanged(
                client.getId(), client.getClientAuthMethod(), client.getAppSecret(), encoded);
        if (upgraded > 0) {
            evictClientCacheAfterCommit(client.getClientCode());
            log.info("历史客户端密钥已升级为摘要: clientCode={}", client.getClientCode());
        }
    }

    private void evictClientCacheAfterCommit(String... clientCodes) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictClientCaches(clientCodes);
                }
            });
            return;
        }
        evictClientCaches(clientCodes);
    }

    private void evictClientCaches(String... clientCodes) {
        for (String clientCode : clientCodes) {
            evictClientCache(clientCode);
        }
    }

    private void evictClientCache(String clientCode) {
        if (StrUtil.isNotBlank(clientCode)) {
            cacheService.delete(buildCacheKey(clientCode));
        }
    }

    private String buildCacheKey(String clientCode) {
        return CLIENT_CONFIG_CACHE_KEY + clientCode;
    }
}
