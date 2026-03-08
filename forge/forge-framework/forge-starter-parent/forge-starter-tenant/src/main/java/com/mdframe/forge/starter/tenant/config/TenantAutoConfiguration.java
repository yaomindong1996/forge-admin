package com.mdframe.forge.starter.tenant.config;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.tenant.aspect.IgnoreTenantAspect;
import com.mdframe.forge.starter.tenant.handler.DefaultTenantLineHandler;
import com.mdframe.forge.starter.tenant.interceptor.TenantInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 多租户自动配置类
 * 注意：只提供 TenantLineInnerInterceptor Bean，由 MybatisPlusConfig 统一注册
 */
@Slf4j
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)  // 优先级高，确保在 ORM 配置之前加载
@EnableConfigurationProperties(TenantProperties.class)
@ConditionalOnProperty(prefix = "forge.tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class TenantAutoConfiguration implements WebMvcConfigurer {

    private final TenantProperties tenantProperties;

    /**
     * 租户处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultTenantLineHandler tenantLineHandler() {
        log.info("初始化多租户处理器，租户字段: {}", tenantProperties.getColumn());
        return new DefaultTenantLineHandler(tenantProperties);
    }

    /**
     * 租户拦截器配置
     * 注意：只提供 Bean，不直接注册到 MybatisPlusInterceptor
     * 由 MybatisPlusConfig 通过自动注入 List<InnerInterceptor> 来获取并注册
     */
    @Bean
    @ConditionalOnMissingBean(name = "tenantLineInnerInterceptor")
    public TenantLineInnerInterceptor tenantLineInnerInterceptor(DefaultTenantLineHandler tenantLineHandler) {
        TenantLineInnerInterceptor interceptor = new TenantLineInnerInterceptor(tenantLineHandler);
        log.info("多租户SQL拦截器已创建，等待注册到 MybatisPlusInterceptor");
        return interceptor;
    }

    /**
     * 租户Web拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantInterceptor tenantInterceptor() {
        log.info("租户Web拦截器已启用");
        return new TenantInterceptor();
    }

    /**
     * 租户忽略注解切面
     */
    @Bean
    @ConditionalOnMissingBean
    public IgnoreTenantAspect ignoreTenantAspect() {
        log.info("租户忽略注解切面已启用");
        return new IgnoreTenantAspect();
    }

    /**
     * 注册Web拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor())
                .addPathPatterns("/**")
                .order(10); // 设置优先级，在认证拦截器之后
        log.info("租户拦截器已注册到Web容器");
    }
}
