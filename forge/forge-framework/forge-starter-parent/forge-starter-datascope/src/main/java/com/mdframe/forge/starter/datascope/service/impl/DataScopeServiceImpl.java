package com.mdframe.forge.starter.datascope.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.starter.datascope.enums.DataScopeType;
import com.mdframe.forge.starter.datascope.mapper.*;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 数据权限服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataScopeServiceImpl implements IDataScopeService {
    
    private final SysDataScopeConfigMapper dataScopeConfigMapper;
    private final DataScopeRoleMapper roleMapper;
    private final DataScopeOrgMapper orgMapper;
    private final SysRoleDataScopeMapper roleDataScopeMapper;
    
    /**
     * 数据权限配置缓存（key: mapperMethod, value: config）
     */
    private final Cache<String, SysDataScopeConfig> configCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();
    
    /**
     * 组织子孙缓存（key: orgIds, value: allOrgIds）
     */
    private final Cache<String, Set<Long>> orgChildCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    
    @Override
    public DataScopeContext getCurrentUserDataScope() {
        // 1. 获取当前登录用户
        if (!StpUtil.isLogin()) {
            return null;
        }
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            return null;
        }
        
        // 2. 如果是超级管理员，拥有全部数据权限
        if (loginUser.isAdmin()) {
            DataScopeContext context = new DataScopeContext();
            context.setUserId(loginUser.getUserId());
            context.setTenantId(loginUser.getTenantId());
            context.setMinDataScope(DataScopeType.ALL.getCode());
            return context;
        }
        
        // 3. 如果是租户管理员，拥有租户全部数据权限
        if (loginUser.isTenantAdmin()) {
            DataScopeContext context = new DataScopeContext();
            context.setUserId(loginUser.getUserId());
            context.setTenantId(loginUser.getTenantId());
            context.setMinDataScope(DataScopeType.TENANT_ALL.getCode());
            return context;
        }
        
        // 4. 获取用户角色ID列表
        List<Long> roleIds = loginUser.getRoleIds();
        if (roleIds == null || roleIds.isEmpty()) {
            // 没有角色，默认只能查看本人数据
            DataScopeContext context = new DataScopeContext();
            context.setUserId(loginUser.getUserId());
            context.setTenantId(loginUser.getTenantId());
            context.setMinDataScope(DataScopeType.SELF.getCode());
            return context;
        }
        
        // 5. 查询最小数据权限范围（值最小=权限最大）
        Integer minDataScope = roleMapper.selectMinDataScope(roleIds);
        if (minDataScope == null) {
            minDataScope = DataScopeType.SELF.getCode();
        }
        
        // 6. 获取用户组织ID列表
        List<Long> orgIds = loginUser.getOrgIds();
        
        // 7. 如果是自定义数据权限，查询自定义组织ID
        Set<Long> customOrgIds = null;
        if (DataScopeType.CUSTOM.getCode().equals(minDataScope)) {
            customOrgIds = roleDataScopeMapper.selectOrgIdsByRoleIds(roleIds);
        }
        
        // 8. 构建上下文
        DataScopeContext context = new DataScopeContext();
        context.setUserId(loginUser.getUserId());
        context.setTenantId(loginUser.getTenantId());
        context.setRoleIds(roleIds);
        context.setOrgIds(orgIds);
        context.setMinDataScope(minDataScope);
        context.setCustomOrgIds(customOrgIds);
        
        return context;
    }
    
    @Override
    public SysDataScopeConfig getDataScopeConfig(String mapperId) {
        // 优先从缓存获取
        SysDataScopeConfig config = configCache.getIfPresent(mapperId);
        if (config != null) {
            return config;
        }
        
        // 从数据库查询
        LambdaQueryWrapper<SysDataScopeConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDataScopeConfig::getMapperMethod, mapperId)
               .eq(SysDataScopeConfig::getEnabled, 1);
        
        config = dataScopeConfigMapper.selectOne(wrapper);
        
        // 放入缓存
        if (config != null) {
            configCache.put(mapperId, config);
        }
        
        return config;
    }
    
    @Override
    public Set<Long> getOrgAndChildIds(List<Long> orgIds) {
        if (orgIds == null || orgIds.isEmpty()) {
            return Collections.emptySet();
        }
        
        // 生成缓存key
        String cacheKey = orgIds.stream()
                .sorted()
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        
        // 优先从缓存获取
        Set<Long> allOrgIds = orgChildCache.getIfPresent(cacheKey);
        if (allOrgIds != null) {
            return allOrgIds;
        }
        
        // 从数据库查询
        allOrgIds = orgMapper.selectChildOrgIds(orgIds);
        
        // 放入缓存
        if (allOrgIds != null && !allOrgIds.isEmpty()) {
            orgChildCache.put(cacheKey, allOrgIds);
        }
        
        return allOrgIds != null ? allOrgIds : Collections.emptySet();
    }
    
    @Override
    public void refreshDataScopeCache() {
        configCache.invalidateAll();
        orgChildCache.invalidateAll();
        log.info("数据权限配置缓存已刷新");
    }
}
