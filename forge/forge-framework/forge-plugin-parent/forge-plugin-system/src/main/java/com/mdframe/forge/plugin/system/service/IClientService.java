package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.system.entity.SysClient;
import java.util.List;

public interface IClientService extends IService<SysClient> {
    
    SysClient getByCode(String clientCode);
    
    SysClient getByAppId(String appId);
    
    boolean validateAppSecret(String appId, String appSecret);
    
    List<SysClient> listByTenant(Long tenantId);
    
    void reloadClientConfigCache(String clientCode);
    
    String getMaskedAppSecret(Long clientId);
}