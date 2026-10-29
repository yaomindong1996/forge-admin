package com.mdframe.forge.plugin.message.service.impl;

import com.mdframe.forge.plugin.message.domain.entity.SysSmsConfig;
import com.mdframe.forge.plugin.message.domain.entity.SysSmsRoutePolicy;
import com.mdframe.forge.plugin.message.mapper.SysSmsConfigMapper;
import com.mdframe.forge.plugin.message.mapper.SysSmsRoutePolicyMapper;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.message.config.SmsConfigProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SmsConfigProviderImpl implements SmsConfigProvider {

    private final SysSmsConfigMapper sysSmsConfigMapper;
    private final SysSmsRoutePolicyMapper sysSmsRoutePolicyMapper;

    @Override
    @IgnoreTenant
    public SmsConfig getSmsConfig() {
        Long tenantId = MessageChannelConfigSupport.resolveTenantId(null);
        SysSmsConfig entity = sysSmsConfigMapper.selectEnabledConfig(tenantId);
        return entity == null ? null : toProviderConfig(entity);
    }

    @Override
    @IgnoreTenant
    public List<SmsConfig> listEnabledSmsConfigs() {
        Long tenantId = MessageChannelConfigSupport.resolveTenantId(null);
        List<SysSmsConfig> entities = sysSmsConfigMapper.selectEnabledConfigs(tenantId);
        List<SmsConfig> result = new ArrayList<>();
        if (entities == null) {
            return result;
        }
        for (SysSmsConfig entity : entities) {
            result.add(toProviderConfig(entity));
        }
        return result;
    }

    @Override
    @IgnoreTenant
    public SmsRoutePolicy getRoutePolicy() {
        Long tenantId = MessageChannelConfigSupport.resolveTenantId(null);
        SysSmsRoutePolicy entity = sysSmsRoutePolicyMapper.selectByTenantId(tenantId);
        SmsRoutePolicy policy = SmsRoutePolicy.defaults();
        if (entity == null) {
            return policy;
        }
        policy.setRouteMode(entity.getRouteMode());
        policy.setCaptchaLockPrimary(entity.getCaptchaLockPrimary() == null || entity.getCaptchaLockPrimary() == 1);
        policy.setFailoverEnabled(entity.getFailoverEnabled() == null || entity.getFailoverEnabled() == 1);
        if (entity.getFailoverMaxAttempts() != null && entity.getFailoverMaxAttempts() > 0) {
            policy.setFailoverMaxAttempts(entity.getFailoverMaxAttempts());
        }
        return policy;
    }

    private SmsConfig toProviderConfig(SysSmsConfig entity) {
        SmsConfig config = new SmsConfig();
        config.setConfigId(entity.getConfigId());
        config.setSupplier(entity.getSupplier());
        config.setAccessKeyId(entity.getAccessKeyId());
        config.setAccessKeySecret(entity.getAccessKeySecret());
        config.setSignature(entity.getSignature());
        config.setTemplateId(entity.getTemplateId());
        config.setSceneTemplates(entity.getSceneTemplates());
        config.setWeight(entity.getWeight());
        config.setRetryInterval(entity.getRetryInterval());
        config.setMaxRetries(entity.getMaxRetries());
        config.setMaximum(entity.getMaximum());
        config.setExtraConfig(entity.getExtraConfig());
        config.setDailyLimit(entity.getDailyLimit());
        config.setMinuteLimit(entity.getMinuteLimit());
        config.setStatus(entity.getStatus());
        config.setChannelRole(entity.getChannelRole());
        return config;
    }
}
