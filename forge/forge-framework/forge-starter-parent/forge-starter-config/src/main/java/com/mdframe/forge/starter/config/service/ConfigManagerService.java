package com.mdframe.forge.starter.config.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.config.config.*;
import com.mdframe.forge.starter.config.entity.SysConfigGroup;
import com.mdframe.forge.starter.core.context.AuthProperties;
import com.mdframe.forge.starter.core.context.CryptoProperties;
import com.mdframe.forge.starter.core.context.LogProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用配置管理服务
 * 提供对各种配置的统一管理功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigManagerService {

    private final ISysConfigGroupService sysConfigGroupService;
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取登录配置
     */
    public LoginConfig getLoginConfig() {
        return getConfigByGroup(LoginConfig.class, "login");
    }

    /**
     * 获取安全配置
     */
    public SecurityConfig getSecurityConfig() {
        return getConfigByGroup(SecurityConfig.class, "security");
    }

    /**
     * 获取系统配置
     */
    public SystemConfig getSystemConfig() {
        return getConfigByGroup(SystemConfig.class, "system");
    }

    /**
     * 获取水印配置
     */
    public WatermarkConfig getWatermarkConfig() {
        return getConfigByGroup(WatermarkConfig.class, "watermark");
    }

    /**
     * 获取加解密配置
     */
    public CryptoProperties getCryptoConfig() {
        return getConfigByGroup(CryptoProperties.class, "crypto");
    }

    /**
     * 获取认证配置
     */
    public AuthProperties getAuthConfig() {
        return getConfigByGroup(AuthProperties.class, "auth");
    }

    /**
     * 获取日志配置
     */
    public LogProperties getLogConfig() {
        return getConfigByGroup(LogProperties.class, "log");
    }

    /**
     * 保存登录配置
     */
    public boolean saveLoginConfig(LoginConfig config) {
        return saveConfigByGroup(config, "login");
    }

    /**
     * 保存安全配置
     */
    public boolean saveSecurityConfig(SecurityConfig config) {
        return saveConfigByGroup(config, "security");
    }

    /**
     * 保存系统配置
     */
    public boolean saveSystemConfig(SystemConfig config) {
        return saveConfigByGroup(config, "system");
    }

    /**
     * 保存水印配置
     */
    public boolean saveWatermarkConfig(WatermarkConfig config) {
        return saveConfigByGroup(config, "watermark");
    }

    /**
     * 保存加解密配置
     */
    public boolean saveCryptoConfig(CryptoProperties config) {
        return saveConfigByGroup(config, "crypto");
    }

    /**
     * 保存认证配置
     */
    public boolean saveAuthConfig(AuthProperties config) {
        return saveConfigByGroup(config, "auth");
    }

    /**
     * 保存日志配置
     */
    public boolean saveLogConfig(LogProperties config) {
        return saveConfigByGroup(config, "log");
    }

    /**
     * 根据分组获取配置
     */
    private <T> T getConfigByGroup(Class<T> clazz, String groupCode) {
        try {
            SysConfigGroup configGroup = sysConfigGroupService.selectByGroupCode(groupCode);
            if (configGroup != null && configGroup.getConfigValue() != null) {
                return objectMapper.readValue(configGroup.getConfigValue(), clazz);
            }
            // 如果没有配置，则返回默认配置
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("获取配置失败，分组: {}, 类型: {}", groupCode, clazz.getSimpleName(), e);
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                log.error("创建默认配置实例失败，类型: {}", clazz.getSimpleName(), ex);
                return null;
            }
        }
    }

    /**
     * 保存配置到指定分组
     */
    private <T> boolean saveConfigByGroup(T config, String groupCode) {
        try {
            String jsonValue = objectMapper.writeValueAsString(config);
            return sysConfigGroupService.updateConfigValueByGroupCode(groupCode, jsonValue);
        } catch (JsonProcessingException e) {
            log.error("保存配置失败，分组: {}, 类型: {}", groupCode, config.getClass().getSimpleName(), e);
            return false;
        }
    }

    /**
     * 获取指定分组的配置值
     */
    public Map<String, Object> getConfigMap(String groupCode) {
        try {
            SysConfigGroup configGroup = sysConfigGroupService.selectByGroupCode(groupCode);
            if (configGroup != null && configGroup.getConfigValue() != null) {
                return objectMapper.readValue(configGroup.getConfigValue(), Map.class);
            }
            return new HashMap<>();
        } catch (Exception e) {
            log.error("获取配置映射失败，分组: {}", groupCode, e);
            return new HashMap<>();
        }
    }

    /**
     * 保存配置映射到指定分组
     */
    public boolean saveConfigMap(String groupCode, Map<String, Object> configMap) {
        try {
            String jsonValue = objectMapper.writeValueAsString(configMap);
            return sysConfigGroupService.updateConfigValueByGroupCode(groupCode, jsonValue);
        } catch (JsonProcessingException e) {
            log.error("保存配置映射失败，分组: {}", groupCode, e);
            return false;
        }
    }
}
