package com.mdframe.forge.starter.apiconfig.config;

import com.mdframe.forge.starter.apiconfig.listener.ApiConfigRefreshListener;
import com.mdframe.forge.starter.apiconfig.registry.ApiConfigAutoRegistrar;
import com.mdframe.forge.starter.apiconfig.registry.ApiConfigScanner;
import com.mdframe.forge.starter.apiconfig.service.ISysApiConfigService;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.apiconfig.service.impl.SysApiConfigServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * API配置管理自动配置类
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "forge.api-config", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableAsync
@RequiredArgsConstructor
public class ApiConfigAutoConfiguration {

    /**
     * 配置API配置扫描器
     */
    @Bean
    public ApiConfigScanner apiConfigScanner(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        return new ApiConfigScanner(requestMappingHandlerMapping);
    }

    /**
     * 配置API配置自动注册器
     */
    @Bean
    @ConditionalOnProperty(prefix = "forge.api-config", name = "auto-register", havingValue = "true", matchIfMissing = true)
    public ApiConfigAutoRegistrar apiConfigAutoRegistrar(ApiConfigScanner apiConfigScanner,
                                                        com.mdframe.forge.starter.apiconfig.mapper.SysApiConfigMapper apiConfigMapper,
                                                        ApiConfigProperties configProperties,
                                                        IApiConfigManager apiConfigManager) {
        return new ApiConfigAutoRegistrar(apiConfigScanner, apiConfigMapper, configProperties, apiConfigManager);
    }

    /**
     * 配置API配置刷新事件监听器
     */
    @Bean
    public ApiConfigRefreshListener apiConfigRefreshListener(IApiConfigManager apiConfigManager) {
        return new ApiConfigRefreshListener(apiConfigManager);
    }
}
