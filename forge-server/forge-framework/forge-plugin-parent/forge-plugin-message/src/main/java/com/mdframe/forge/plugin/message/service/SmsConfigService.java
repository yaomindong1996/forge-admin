package com.mdframe.forge.plugin.message.service;

import com.mdframe.forge.plugin.message.domain.entity.SysSmsConfig;
import com.mdframe.forge.plugin.message.domain.entity.SysSmsPingLog;
import com.mdframe.forge.plugin.message.domain.entity.SysSmsRoutePolicy;

import java.util.List;

public interface SmsConfigService {

    SysSmsConfig getEnabledConfig(Long tenantId);

    List<SysSmsConfig> listByTenant(Long tenantId);

    SysSmsConfig getById(Long id);

    boolean save(SysSmsConfig config);

    boolean updateById(SysSmsConfig config);

    boolean updateStatus(Long id, Integer status);

    boolean deleteById(Long id);

    boolean setPrimary(Long id);

    SysSmsRoutePolicy getRoutePolicy(Long tenantId);

    SysSmsRoutePolicy saveRoutePolicy(SysSmsRoutePolicy policy);

    List<SysSmsPingLog> listPingLogs(Long tenantId, Integer limit);

    int runPingNow(Long tenantId);
}
