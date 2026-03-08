package com.mdframe.forge.starter.config.config;

import com.mdframe.forge.starter.config.converter.ConfigConverter;
import com.mdframe.forge.starter.config.service.ConfigCenterService;
import com.mdframe.forge.starter.config.service.ConfigManagerService;
import com.mdframe.forge.starter.config.service.ConfigSyncService;
import com.mdframe.forge.starter.config.service.ISysConfigGroupService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 配置自动配置类
 */
@Configuration
public class ConfigAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ConfigManagerService.class)
    public ConfigManagerService configManagerService(ISysConfigGroupService sysConfigGroupService) {
        return new ConfigManagerService(sysConfigGroupService);
    }
    
    @Bean
    @ConditionalOnMissingBean(ConfigConverter.class)
    public ConfigConverter configConverter() {
        return new ConfigConverter();
    }
    
    @Bean
    @ConditionalOnMissingBean(ConfigSyncService.class)
    public ConfigSyncService configSyncService(ISysConfigGroupService sysConfigGroupService, 
                                              JdbcTemplate jdbcTemplate,
                                              com.mdframe.forge.starter.property.refresh.ConfigRefresher configRefresher,
                                              ConfigConverter configConverter) {
        return new ConfigSyncService(sysConfigGroupService, jdbcTemplate, configRefresher, configConverter);
    }
    
    @Bean
    @ConditionalOnMissingBean(ConfigCenterService.class)
    public ConfigCenterService configCenterService(ConfigSyncService configSyncService,
                                                  ApplicationEventPublisher eventPublisher) {
        return new ConfigCenterService(configSyncService, eventPublisher);
    }
}