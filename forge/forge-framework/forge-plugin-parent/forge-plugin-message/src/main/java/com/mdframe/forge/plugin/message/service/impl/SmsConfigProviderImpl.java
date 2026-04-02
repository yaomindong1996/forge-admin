package com.mdframe.forge.plugin.message.service.impl;

import com.mdframe.forge.plugin.message.domain.entity.SysSmsConfig;
import com.mdframe.forge.plugin.message.mapper.SysSmsConfigMapper;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.message.config.SmsConfigProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmsConfigProviderImpl implements SmsConfigProvider {

    private final SysSmsConfigMapper sysSmsConfigMapper;

    @Override
    @IgnoreTenant
    public SmsConfig getSmsConfig() {
        SysSmsConfig entity = sysSmsConfigMapper.selectEnabledConfig(null);
        if (entity == null) {
            return null;
        }
        
        SmsConfig config = new SmsConfig();
        config.setConfigId(entity.getConfigId());
        config.setSupplier(entity.getSupplier());
        config.setAccessKeyId(entity.getAccessKeyId());
        config.setAccessKeySecret(entity.getAccessKeySecret());
        config.setSignature(entity.getSignature());
        config.setTemplateId(entity.getTemplateId());
        config.setWeight(entity.getWeight());
        config.setRetryInterval(entity.getRetryInterval());
        config.setMaxRetries(entity.getMaxRetries());
        config.setMaximum(entity.getMaximum());
        config.setExtraConfig(entity.getExtraConfig());
        config.setDailyLimit(entity.getDailyLimit());
        config.setMinuteLimit(entity.getMinuteLimit());
        config.setStatus(entity.getStatus());
        
        return config;
    }
}
