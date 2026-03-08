package com.mdframe.forge.starter.apiconfig.controller;

import com.mdframe.forge.starter.apiconfig.domain.dto.ApiConfigInfo;
import com.mdframe.forge.starter.apiconfig.domain.event.ApiConfigRefreshEvent;
import com.mdframe.forge.starter.apiconfig.registry.ApiConfigAutoRegistrar;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API配置管理Controller（缓存管理）
 */
@RestController
@RequestMapping("/apiConfig")
@RequiredArgsConstructor
@ApiPermissionIgnore
@ApiDecrypt
@ApiEncrypt
public class ApiConfigManageController {

    private final IApiConfigManager apiConfigManager;
    private final ApplicationEventPublisher eventPublisher;
    private final ApiConfigAutoRegistrar apiConfigAutoRegistrar;
    
    
    
    @PostMapping("/registerApiConfigs")
    @OperationLog(module = "API配置管理", type = OperationType.OTHER, desc = "自动注册API配置")
    public RespInfo<?> registerApiConfigs() {
        apiConfigAutoRegistrar.registerApiConfigsAsync();
        return RespInfo.success();
    }

    /**
     * 刷新所有API配置缓存
     */
    @PostMapping("/refresh")
    @OperationLog(module = "API配置管理", type = OperationType.OTHER, desc = "刷新所有API配置缓存")
    public RespInfo<Void> refreshAll() {
        eventPublisher.publishEvent(new ApiConfigRefreshEvent(this, ApiConfigRefreshEvent.RefreshType.ALL));
        return RespInfo.success();
    }

    /**
     * 刷新单个API配置缓存
     */
    @PostMapping("/refreshSingle")
    @OperationLog(module = "API配置管理", type = OperationType.OTHER, desc = "刷新单个API配置缓存")
    public RespInfo<Void> refreshSingle(@RequestParam String urlPath, @RequestParam String method) {
        eventPublisher.publishEvent(new ApiConfigRefreshEvent(this, ApiConfigRefreshEvent.RefreshType.SINGLE, urlPath, method));
        return RespInfo.success();
    }

    /**
     * 清空所有缓存
     */
    @PostMapping("/clearCache")
    @OperationLog(module = "API配置管理", type = OperationType.OTHER, desc = "清空所有API配置缓存")
    public RespInfo<Void> clearCache() {
        apiConfigManager.clearAllCache();
        return RespInfo.success();
    }

    /**
     * 获取缓存统计信息
     */
    @GetMapping("/cacheStats")
    public RespInfo<String> getCacheStats() {
        String stats = apiConfigManager.getCacheStats();
        return RespInfo.success(stats);
    }

    /**
     * 获取所有启用的配置
     */
    @GetMapping("/getAllEnabled")
    public RespInfo<List<ApiConfigInfo>> getAllEnabled() {
        List<ApiConfigInfo> configs = apiConfigManager.getAllEnabledConfigs();
        return RespInfo.success(configs);
    }
}
