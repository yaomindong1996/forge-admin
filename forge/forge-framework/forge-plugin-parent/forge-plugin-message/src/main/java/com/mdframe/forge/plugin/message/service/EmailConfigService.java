package com.mdframe.forge.plugin.message.service;

import com.mdframe.forge.plugin.message.domain.entity.SysEmailConfig;

public interface EmailConfigService {

    SysEmailConfig getEnabledConfig(Long tenantId);

    SysEmailConfig getById(Long id);

    boolean save(SysEmailConfig config);

    boolean updateById(SysEmailConfig config);

    boolean updateStatus(Long id, Integer status);
}
