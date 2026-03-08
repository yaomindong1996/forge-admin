package com.mdframe.forge.starter.config.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.config.config.*;
import com.mdframe.forge.starter.config.converter.ConfigConverter;
import com.mdframe.forge.starter.config.entity.SysConfigGroup;
import com.mdframe.forge.starter.property.refresh.ConfigRefresher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置同步服务
 * 负责将SysConfigGroup中的配置同步到sys_config表，以兼容现有的配置刷新机制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigSyncService implements ApplicationRunner {

    private final ISysConfigGroupService sysConfigGroupService;
    private final JdbcTemplate jdbcTemplate;
    private final ConfigRefresher configRefresher; // 复用现有的配置刷新器
    private final ConfigConverter configConverter; // 使用配置转换器

    /**
     * 同步所有配置到sys_config表
     */
    public boolean syncAllConfigs() {
        try {
            // 获取所有启用的配置分组
            List<SysConfigGroup> configGroups = sysConfigGroupService.selectEnabledGroups();
            
            Map<String, String> groupToSysConfigAll = new HashMap<>();
            for (SysConfigGroup configGroup : configGroups) {
                Map<String, String> groupToSysConfig = syncConfigGroupToSysConfig(configGroup);
                if (groupToSysConfig != null) {
                    groupToSysConfigAll.putAll(groupToSysConfig);
                }
            }
            // 触发配置刷新
            boolean refreshResult = configRefresher.refresh(groupToSysConfigAll);
            log.info("配置同步完成，刷新结果: {}", refreshResult);
            return true;
        } catch (Exception e) {
            log.error("同步配置失败", e);
            return false;
        }
    }

    /**
     * 同步特定配置分组到sys_config表
     */
    public boolean syncConfigGroup(String groupCode) {
        try {
            SysConfigGroup configGroup = sysConfigGroupService.selectByGroupCode(groupCode);
            if (configGroup == null) {
                log.warn("配置分组不存在: {}", groupCode);
                return false;
            }
            
            Map<String, String> groupToSysConfig = syncConfigGroupToSysConfig(configGroup);
            
            if (groupToSysConfig != null) {
                // 触发配置刷新
                boolean refreshResult = configRefresher.refresh(groupToSysConfig);
                log.info("配置分组[{}]同步完成，刷新结果: {}", groupCode, refreshResult);
            }
            return true;
        } catch (Exception e) {
            log.error("同步配置分组[{}]失败", groupCode, e);
            return false;
        }
    }

    /**
     * 将配置分组同步到sys_config表
     */
    private Map<String, String> syncConfigGroupToSysConfig(SysConfigGroup configGroup) {
        try {
            String groupCode = configGroup.getGroupCode();
            String configValue = configGroup.getConfigValue();
            
            if (configValue == null || configValue.trim().isEmpty()) {
                log.debug("配置分组[{}]的配置值为空，跳过同步", groupCode);
                return null;
            }

            // 根据分组类型解析配置
            Map<String, String> configMap = switch (groupCode) {
                case "login" -> configConverter.convertLoginConfig(configValue);
                case "watermark" -> configConverter.convertWatermarkConfig(configValue);
                case "crypto" -> configConverter.convertCryptoConfig(configValue);
                case "auth" -> configConverter.convertAuthConfig(configValue);
                case "log" -> configConverter.convertLogConfig(configValue);
                default -> {
                    log.warn("未知的配置分组: {}", groupCode);
                    yield null;
                }
            };
            return configMap;
        } catch (Exception e) {
            log.error("同步配置分组[{}]到sys_config表失败", configGroup.getGroupCode(), e);
        }
        return null;
    }
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        syncAllConfigs();
    }
}
