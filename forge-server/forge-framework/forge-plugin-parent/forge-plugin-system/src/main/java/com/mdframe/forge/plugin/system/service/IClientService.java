package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysClientDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.system.entity.SysClient;
import java.util.List;

public interface IClientService extends IService<SysClient> {
    
    SysClient getByCode(String clientCode);
    
    SysClient getByAppId(String appId);

    Page<SysClient> selectClientPage(long pageNum, long pageSize,
                                     String clientCode, String clientName, Integer status);

    List<SysClient> listEnabledClients();

    boolean createClient(SysClientDTO dto);

    boolean updateClient(SysClientDTO dto);

    boolean deleteClient(Long id);

    boolean requiresAppSecret(SysClient client);

    boolean validateAppSecret(SysClient client, String appSecret);
    
    List<SysClient> listByTenant(Long tenantId);
    
    void reloadClientConfigCache(String clientCode);
    
    String getMaskedAppSecret(Long clientId);

    int countLegacyPlaintextSecrets();
}
