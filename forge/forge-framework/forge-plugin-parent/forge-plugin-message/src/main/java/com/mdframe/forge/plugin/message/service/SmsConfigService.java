package com.mdframe.forge.plugin.message.service;

import com.mdframe.forge.plugin.message.domain.entity.SysSmsConfig;

public interface SmsConfigService {

    SysSmsConfig getEnabledConfig(Long tenantId);

    SysSmsConfig getById(Long id);

    boolean save(SysSmsConfig config);

    boolean updateById(SysSmsConfig config);

    boolean updateStatus(Long id, Integer status);
}
