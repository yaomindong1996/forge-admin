package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.entity.SysClient;
import com.mdframe.forge.plugin.system.mapper.SysClientMapper;
import com.mdframe.forge.plugin.system.service.IClientService;
import com.mdframe.forge.starter.cache.service.ICacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl extends ServiceImpl<SysClientMapper, SysClient> implements IClientService {
    
    private final ICacheService cacheService;
    private static final String CLIENT_CONFIG_CACHE_KEY = "client:config:";
    private static final long CLIENT_CONFIG_CACHE_EXPIRE = 3600;
    
    @Override
    public SysClient getByCode(String clientCode) {
        if (StrUtil.isBlank(clientCode)) {
            return null;
        }
        
        String cacheKey = CLIENT_CONFIG_CACHE_KEY + clientCode;
        SysClient cached = cacheService.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        LambdaQueryWrapper<SysClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysClient::getClientCode, clientCode);
        SysClient client = this.getOne(wrapper);
        
        if (client != null) {
            cacheService.set(cacheKey, client, CLIENT_CONFIG_CACHE_EXPIRE, TimeUnit.SECONDS);
        }
        
        return client;
    }
    
    @Override
    public SysClient getByAppId(String appId) {
        if (StrUtil.isBlank(appId)) {
            return null;
        }
        
        LambdaQueryWrapper<SysClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysClient::getAppId, appId);
        return this.getOne(wrapper);
    }
    
    @Override
    public boolean validateAppSecret(String appId, String appSecret) {
        SysClient client = getByAppId(appId);
        if (client == null) {
            return false;
        }
        
        return client.getAppSecret().equals(appSecret);
    }
    
    @Override
    public List<SysClient> listByTenant(Long tenantId) {
        LambdaQueryWrapper<SysClient> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            wrapper.eq(SysClient::getTenantId, tenantId);
        }
        wrapper.eq(SysClient::getStatus, 1);
        return this.list(wrapper);
    }
    
    @Override
    public void reloadClientConfigCache(String clientCode) {
        String cacheKey = CLIENT_CONFIG_CACHE_KEY + clientCode;
        cacheService.delete(cacheKey);
        
        SysClient client = getByCode(clientCode);
        if (client != null) {
            cacheService.set(cacheKey, client, CLIENT_CONFIG_CACHE_EXPIRE, TimeUnit.SECONDS);
            log.info("客户端配置缓存已刷新: {}", clientCode);
        }
    }
    
    @Override
    public String getMaskedAppSecret(Long clientId) {
        SysClient client = this.getById(clientId);
        if (client == null || StrUtil.isBlank(client.getAppSecret())) {
            return "";
        }
        
        String secret = client.getAppSecret();
        if (secret.length() > 8) {
            return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
        }
        return "****";
    }
}