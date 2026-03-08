package com.mdframe.forge.starter.config.controller;

import com.mdframe.forge.starter.config.config.*;
import com.mdframe.forge.starter.config.service.ConfigManagerService;
import com.mdframe.forge.starter.core.context.AuthProperties;
import com.mdframe.forge.starter.core.context.CryptoProperties;
import com.mdframe.forge.starter.core.context.LogProperties;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.property.event.ConfigRefreshEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

/**
 * 通用配置管理控制器
 * 提供对各种配置的统一管理接口
 */
@RestController
@RequestMapping("/api/config/manage")
@RequiredArgsConstructor
@ApiEncrypt
@ApiDecrypt
@ApiPermissionIgnore
@IgnoreTenant
public class ConfigManageController {
    private final ConfigManagerService configManagerService;
    private final ApplicationContext applicationContext;

    /**
     * 获取登录配置
     */
    @GetMapping("/login")
    public RespInfo<LoginConfig> getLoginConfig() {
        return RespInfo.success(configManagerService.getLoginConfig());
    }

    /**
     * 更新登录配置
     */
    @PutMapping("/login")
    public RespInfo<Void> updateLoginConfig(@RequestBody LoginConfig config) {
        configManagerService.saveLoginConfig(config);
        return RespInfo.success();
    }

    /**
     * 获取水印配置
     */
    @GetMapping("/watermark")
    public RespInfo<WatermarkConfig> getWatermarkConfig() {
        return RespInfo.success(configManagerService.getWatermarkConfig());
    }

    /**
     * 更新水印配置
     */
    @PutMapping("/watermark")
    public RespInfo<Void> updateWatermarkConfig(@RequestBody WatermarkConfig config) {
        configManagerService.saveWatermarkConfig(config);
        return RespInfo.success();
    }

    /**
     * 获取安全配置
     */
    @GetMapping("/security")
    public RespInfo<SecurityConfig> getSecurityConfig() {
        return RespInfo.success(configManagerService.getSecurityConfig());
    }

    /**
     * 更新安全配置
     */
    @PutMapping("/security")
    public RespInfo<Void> updateSecurityConfig(@RequestBody SecurityConfig config) {
        configManagerService.saveSecurityConfig(config);
        return RespInfo.success();
    }

    /**
     * 获取系统配置
     */
    @GetMapping("/system")
    public RespInfo<SystemConfig> getSystemConfig() {
        return RespInfo.success(configManagerService.getSystemConfig());
    }

    /**
     * 更新系统配置
     */
    @PutMapping("/system")
    public RespInfo<Void> updateSystemConfig(@RequestBody SystemConfig config) {
        configManagerService.saveSystemConfig(config);
        return RespInfo.success();
    }

    /**
     * 获取加解密配置
     */
    @GetMapping("/crypto")
    public RespInfo<CryptoProperties> getCryptoConfig() {
        return RespInfo.success(configManagerService.getCryptoConfig());
    }

    /**
     * 更新加解密配置
     */
    @PutMapping("/crypto")
    public RespInfo<Void> updateCryptoConfig(@RequestBody CryptoProperties config) {
        configManagerService.saveCryptoConfig(config);
        return RespInfo.success();
    }

    /**
     * 获取认证配置
     */
    @GetMapping("/auth")
    public RespInfo<AuthProperties> getAuthConfig() {
        return RespInfo.success(configManagerService.getAuthConfig());
    }

    /**
     * 更新认证配置
     */
    @PutMapping("/auth")
    public RespInfo<Void> updateAuthConfig(@RequestBody AuthProperties config) {
        configManagerService.saveAuthConfig(config);
        return RespInfo.success();
    }

    /**
     * 获取日志配置
     */
    @GetMapping("/log")
    public RespInfo<LogProperties> getLogConfig() {
        return RespInfo.success(configManagerService.getLogConfig());
    }

    /**
     * 更新日志配置
     */
    @PutMapping("/log")
    public RespInfo<Void> updateLogConfig(@RequestBody LogProperties config) {
        configManagerService.saveLogConfig(config);
        return RespInfo.success();
    }

    /**
     * 刷新所有配置（触发配置刷新机制）
     */
    @PostMapping("/refresh")
    public RespInfo<Void> refreshConfig() {
        // 发布刷新事件 ConfigRefreshEvent
        ConfigRefreshEvent event = new ConfigRefreshEvent(this, null, null);
        applicationContext.publishEvent(event);
        return RespInfo.success();
    }
}
