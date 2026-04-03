package com.mdframe.forge.plugin.message.service.impl;

import com.mdframe.forge.plugin.message.domain.entity.SysEmailConfig;
import com.mdframe.forge.plugin.message.mapper.SysEmailConfigMapper;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.message.config.EmailConfigProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailConfigProviderImpl implements EmailConfigProvider {

    private final SysEmailConfigMapper sysEmailConfigMapper;

    @Override
    @IgnoreTenant
    public EmailConfig getEmailConfig() {
        SysEmailConfig entity = sysEmailConfigMapper.selectEnabledConfig(null);
        if (entity == null) {
            return null;
        }
        
        EmailConfig config = new EmailConfig();
        config.setConfigId(entity.getConfigId());
        config.setSmtpServer(entity.getSmtpServer());
        config.setPort(entity.getPort());
        config.setUsername(entity.getUsername());
        config.setPassword(entity.getPassword());
        config.setFromAddress(entity.getFromAddress());
        config.setFromName(entity.getFromName());
        config.setIsSsl(entity.getIsSsl());
        config.setIsAuth(entity.getIsAuth());
        config.setRetryInterval(entity.getRetryInterval());
        config.setMaxRetries(entity.getMaxRetries());
        config.setStatus(entity.getStatus());
        
        return config;
    }
}
