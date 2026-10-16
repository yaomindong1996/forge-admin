package com.mdframe.forge.starter.social.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.starter.collaboration.CollaborationCapability;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.social.domain.dto.SocialAppSaveCommand;
import com.mdframe.forge.starter.social.domain.entity.SysSocialAppConfig;
import com.mdframe.forge.starter.social.domain.entity.SysSocialCapabilityBinding;
import com.mdframe.forge.starter.social.mapper.SysSocialAppConfigMapper;
import com.mdframe.forge.starter.social.mapper.SysSocialCapabilityBindingMapper;
import com.mdframe.forge.starter.social.security.SecretContext;
import com.mdframe.forge.starter.social.security.SecretSummary;
import com.mdframe.forge.starter.social.security.SocialAppCredentialService;
import com.mdframe.forge.starter.social.service.ISocialAppConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import com.mdframe.forge.starter.core.enums.EnableStatus;

/**
 * 企业协同物理应用配置服务实现
 * <p>
 * Secret 只保存一份密文或外部引用；轮换经 CAS 防并发覆盖，掩码回传零写保留。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialAppConfigServiceImpl extends ServiceImpl<SysSocialAppConfigMapper, SysSocialAppConfig>
        implements ISocialAppConfigService {

    private static final String SECRET_MODE_CIPHER = "CIPHER";
    private static final String SECRET_MODE_EXTERNAL_REF = "EXTERNAL_REF";
    private static final String TYPE_APP_SECRET = "APP_SECRET";
    private static final String TYPE_CALLBACK_TOKEN = "CALLBACK_TOKEN";
    private static final String TYPE_ENCODING_AES_KEY = "ENCODING_AES_KEY";

    private final SysSocialCapabilityBindingMapper capabilityBindingMapper;
    private final SocialAppCredentialService credentialService;

    @Override
    public SysSocialAppConfig requireEnabledApp(Long tenantId, Long connectionId, CollaborationCapability capability) {
        if (connectionId == null || capability == null) {
            throw new BusinessException("连接ID与能力不能为空");
        }
        Long resolvedTenant = resolveTenantId(tenantId);
        SysSocialAppConfig app = baseMapper.selectEnabledAppByCapability(resolvedTenant, connectionId, capability.name());
        if (app == null) {
            // 失败关闭：无活动绑定、绑定停用或应用停用均拒绝，不回退旧配置
            throw new BusinessException(StrUtil.format("连接[{}]未配置能力[{}]的启用应用", connectionId, capability.name()));
        }
        return app;
    }

    @Override
    public List<SysSocialAppConfig> listApps(Long tenantId, Long connectionId) {
        return baseMapper.selectByConnection(resolveTenantId(tenantId), connectionId);
    }

    @Override
    public List<SysSocialCapabilityBinding> listBindings(Long tenantId, Long connectionId) {
        return capabilityBindingMapper.selectByConnection(resolveTenantId(tenantId), connectionId);
    }

    @Override
    public boolean createApp(SocialAppSaveCommand command) {
        if (command == null || command.getConnectionId() == null || StrUtil.isBlank(command.getAppCode())) {
            throw new BusinessException("连接ID与应用编码不能为空");
        }
        Long tenantId = resolveTenantId(command.getTenantId());
        SysSocialAppConfig exists = baseMapper.selectByAppCode(tenantId, command.getConnectionId(), command.getAppCode());
        if (exists != null) {
            throw new BusinessException(StrUtil.format("应用编码[{}]在连接内已存在", command.getAppCode()));
        }

        SysSocialAppConfig app = new SysSocialAppConfig();
        app.setTenantId(tenantId);
        app.setConnectionId(command.getConnectionId());
        app.setAppCode(command.getAppCode());
        applyPlainFields(app, command);

        // Secret 明文只在此处短暂持有，加密后立即落库
        String secret = command.getSecret();
        if (isMeaningfulSecret(secret)) {
            SecretContext context = SecretContext.of(tenantId, command.getConnectionId(), null, TYPE_APP_SECRET);
            if (SocialAppCredentialService.isExternalRef(secret)) {
                app.setSecretMode(SECRET_MODE_EXTERNAL_REF);
                app.setSecretRef(secret);
            } else {
                app.setSecretMode(SECRET_MODE_CIPHER);
                app.setSecretCipher(credentialService.encrypt(secret.toCharArray(), context));
            }
            app.setSecretUpdateTime(LocalDateTime.now());
        } else {
            app.setSecretMode(SECRET_MODE_CIPHER);
        }
        app.setCallbackTokenCipher(encryptIfPresent(command.getCallbackToken(), tenantId,
                command.getConnectionId(), null, TYPE_CALLBACK_TOKEN));
        app.setEncodingAesKeyCipher(encryptIfPresent(command.getEncodingAesKey(), tenantId,
                command.getConnectionId(), null, TYPE_ENCODING_AES_KEY));

        try {
            return this.save(app);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(StrUtil.format("应用编码[{}]在连接内已存在", command.getAppCode()));
        }
    }

    @Override
    public boolean updateApp(SocialAppSaveCommand command, String expectedCredentialCipher) {
        if (command == null || command.getId() == null) {
            throw new BusinessException("应用配置ID不能为空");
        }
        Long tenantId = resolveTenantId(command.getTenantId());
        SysSocialAppConfig current = this.getById(command.getId());
        if (current == null || (tenantId != null && !tenantId.equals(current.getTenantId()))) {
            throw new BusinessException("应用配置不存在");
        }
        if (command.getConnectionId() != null && !command.getConnectionId().equals(current.getConnectionId())) {
            throw new BusinessException("应用配置不允许跨连接迁移");
        }
        if (StrUtil.isNotBlank(command.getAppCode()) && !command.getAppCode().equals(current.getAppCode())) {
            SysSocialAppConfig conflict = baseMapper.selectByAppCode(current.getTenantId(),
                    current.getConnectionId(), command.getAppCode());
            if (conflict != null && !conflict.getId().equals(current.getId())) {
                throw new BusinessException(StrUtil.format("应用编码[{}]在连接内已存在", command.getAppCode()));
            }
        }

        // Secret 轮换：空值/掩码回传零写保留；轮换必须通过 CAS，期望值不一致失败关闭
        String secret = command.getSecret();
        if (isMeaningfulSecret(secret)) {
            String expected = expectedCredentialCipher != null
                    ? expectedCredentialCipher
                    : currentStoredCredential(current);
            String newCipher;
            String newRef;
            String newMode;
            if (SocialAppCredentialService.isExternalRef(secret)) {
                newCipher = null;
                newRef = secret;
                newMode = SECRET_MODE_EXTERNAL_REF;
            } else {
                SecretContext context = SecretContext.of(current.getTenantId(),
                        current.getConnectionId(), current.getId(), TYPE_APP_SECRET);
                newCipher = credentialService.encrypt(secret.toCharArray(), context);
                newRef = null;
                newMode = SECRET_MODE_CIPHER;
            }
            int rows = baseMapper.rotateSecretCipherCas(current.getId(), current.getTenantId(),
                    expected, newCipher, newRef, newMode, safeUserId());
            if (rows == 0) {
                throw new BusinessException("应用Secret已被并发修改，请刷新后重试");
            }
        }

        // 回调凭据：空值/掩码回传零写保留
        SysSocialAppConfig update = new SysSocialAppConfig();
        update.setId(current.getId());
        if (StrUtil.isNotBlank(command.getAppCode())) {
            update.setAppCode(command.getAppCode());
        }
        applyPlainFields(update, command);
        if (isMeaningfulSecret(command.getCallbackToken())) {
            update.setCallbackTokenCipher(encryptIfPresent(command.getCallbackToken(), current.getTenantId(),
                    current.getConnectionId(), current.getId(), TYPE_CALLBACK_TOKEN));
        }
        if (isMeaningfulSecret(command.getEncodingAesKey())) {
            update.setEncodingAesKeyCipher(encryptIfPresent(command.getEncodingAesKey(), current.getTenantId(),
                    current.getConnectionId(), current.getId(), TYPE_ENCODING_AES_KEY));
        }
        return this.updateById(update);
    }

    @Override
    public boolean deleteApp(Long tenantId, Long appConfigId) {
        Long resolvedTenant = resolveTenantId(tenantId);
        SysSocialAppConfig app = this.getById(appConfigId);
        if (app == null || (resolvedTenant != null && !resolvedTenant.equals(app.getTenantId()))) {
            throw new BusinessException("应用配置不存在");
        }
        int refs = capabilityBindingMapper.countActiveByApp(app.getTenantId(), appConfigId);
        if (refs > 0) {
            throw new BusinessException(StrUtil.format("应用仍被{}个能力绑定引用，请先解绑", refs));
        }
        return this.removeById(appConfigId);
    }

    @Override
    public boolean bindCapability(Long tenantId, Long connectionId, CollaborationCapability capability, Long appConfigId) {
        if (connectionId == null || capability == null || appConfigId == null) {
            throw new BusinessException("连接ID、能力与应用配置ID不能为空");
        }
        Long resolvedTenant = resolveTenantId(tenantId);
        SysSocialAppConfig app = this.getById(appConfigId);
        if (app == null || (resolvedTenant != null && !resolvedTenant.equals(app.getTenantId()))) {
            throw new BusinessException("应用配置不存在");
        }
        if (!connectionId.equals(app.getConnectionId())) {
            throw new BusinessException("应用配置不属于目标连接");
        }

        SysSocialCapabilityBinding existing = capabilityBindingMapper
                .selectActiveBinding(app.getTenantId(), connectionId, capability.name());
        if (existing != null) {
            existing.setAppConfigId(appConfigId);
            existing.setStatus(EnableStatus.ENABLED.getCode());
            return capabilityBindingMapper.updateById(existing) > 0;
        }
        SysSocialCapabilityBinding binding = new SysSocialCapabilityBinding();
        binding.setTenantId(app.getTenantId());
        binding.setConnectionId(connectionId);
        binding.setCapability(capability.name());
        binding.setAppConfigId(appConfigId);
        binding.setStatus(EnableStatus.ENABLED.getCode());
        try {
            return capabilityBindingMapper.insert(binding) > 0;
        } catch (DuplicateKeyException e) {
            // 唯一键 uk_social_capability_active 拦截并发重复绑定
            throw new BusinessException(StrUtil.format("能力[{}]已存在活动绑定，请刷新后重试", capability.name()));
        }
    }

    @Override
    public boolean unbindCapability(Long tenantId, Long connectionId, CollaborationCapability capability) {
        if (connectionId == null || capability == null) {
            throw new BusinessException("连接ID与能力不能为空");
        }
        SysSocialCapabilityBinding existing = capabilityBindingMapper
                .selectActiveBinding(resolveTenantId(tenantId), connectionId, capability.name());
        if (existing == null) {
            return true;
        }
        return capabilityBindingMapper.deleteById(existing.getId()) > 0;
    }

    @Override
    public char[] decryptAppSecret(SysSocialAppConfig app) {
        requireApp(app);
        String stored = currentStoredCredential(app);
        SecretContext context = SecretContext.of(app.getTenantId(), app.getConnectionId(), app.getId(), TYPE_APP_SECRET);
        return credentialService.decrypt(stored, context);
    }

    @Override
    public char[] decryptCallbackToken(SysSocialAppConfig app) {
        requireApp(app);
        SecretContext context = SecretContext.of(app.getTenantId(), app.getConnectionId(), app.getId(), TYPE_CALLBACK_TOKEN);
        return credentialService.decrypt(app.getCallbackTokenCipher(), context);
    }

    @Override
    public char[] decryptEncodingAesKey(SysSocialAppConfig app) {
        requireApp(app);
        SecretContext context = SecretContext.of(app.getTenantId(), app.getConnectionId(), app.getId(), TYPE_ENCODING_AES_KEY);
        return credentialService.decrypt(app.getEncodingAesKeyCipher(), context);
    }

    @Override
    public SecretSummary secretSummary(SysSocialAppConfig app) {
        if (app == null) {
            return SecretSummary.empty();
        }
        return credentialService.summary(currentStoredCredential(app));
    }

    /**
     * 当前存储的凭据值：外部引用模式取 secret_ref，否则取 secret_cipher
     */
    private String currentStoredCredential(SysSocialAppConfig app) {
        if (SECRET_MODE_EXTERNAL_REF.equals(app.getSecretMode())) {
            return app.getSecretRef();
        }
        return StrUtil.isNotBlank(app.getSecretCipher()) ? app.getSecretCipher() : app.getSecretRef();
    }

    private void applyPlainFields(SysSocialAppConfig app, SocialAppSaveCommand command) {
        app.setAppName(command.getAppName());
        app.setClientId(command.getClientId());
        app.setAgentId(command.getAgentId());
        app.setRedirectUri(command.getRedirectUri());
        app.setScope(command.getScope());
        app.setConfigJson(command.getConfigJson());
        app.setStatus(command.getStatus());
        app.setRemark(command.getRemark());
    }

    private String encryptIfPresent(String plaintext, Long tenantId, Long connectionId, Long appId, String type) {
        if (!isMeaningfulSecret(plaintext)) {
            return null;
        }
        return credentialService.encrypt(plaintext.toCharArray(),
                SecretContext.of(tenantId, connectionId, appId, type));
    }

    /**
     * 有效凭据输入：非空且不是掩码回传
     */
    private boolean isMeaningfulSecret(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        return !value.chars().allMatch(c -> c == '*');
    }

    private void requireApp(SysSocialAppConfig app) {
        if (app == null) {
            throw new BusinessException("应用配置不能为空");
        }
    }

    private Long resolveTenantId(Long tenantId) {
        if (tenantId != null) {
            return tenantId;
        }
        try {
            return SessionHelper.getTenantId();
        } catch (Exception e) {
            // 后台任务等无会话场景由调用方显式传租户
            return null;
        }
    }

    private Long safeUserId() {
        try {
            return SessionHelper.getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
