package com.mdframe.forge.starter.datascope.config;

import com.mdframe.forge.starter.datascope.handler.DataScopeInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;

/**
 * 数据权限自动配置类
 * 注意：只提供 DataScopeInterceptor Bean，由 MybatisPlusConfig 统一注册
 */
@Slf4j
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)  // 最高优先级，数据权限需要在最前面
@EnableConfigurationProperties(DataScopeProperties.class)
@ComponentScan("com.mdframe.forge.starter.datascope")
@MapperScan("com.mdframe.forge.starter.datascope.mapper")
@ConditionalOnProperty(prefix = "forge.datascope", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DataScopeAutoConfiguration {
    
    /**
     * 注册数据权限拦截器
     * 注意：只提供 Bean，由 MybatisPlusConfig 通过 List<InnerInterceptor> 自动注入并注册
     */
    @Bean
    public DataScopeInterceptor dataScopeInterceptor() {
        log.info("数据权限拦截器已创建，等待注册到 MybatisPlusInterceptor");
        return new DataScopeInterceptor();
    }
}
