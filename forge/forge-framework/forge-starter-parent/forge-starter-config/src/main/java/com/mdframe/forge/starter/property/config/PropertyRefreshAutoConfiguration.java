package com.mdframe.forge.starter.property.config;

import com.mdframe.forge.starter.property.scope.RefreshScopeImpl;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 配置刷新自动配置类
 */
@Configuration
@EnableScheduling
public class PropertyRefreshAutoConfiguration {
    
    /**
     * 注册RefreshScope作用域
     */
    @Bean
    public static BeanFactoryPostProcessor refreshScopeRegistrar() {
        return (ConfigurableListableBeanFactory beanFactory) -> {
            RefreshScopeImpl refreshScope = new RefreshScopeImpl();
            beanFactory.registerScope("refresh", refreshScope);
            // 将RefreshScope注册为单例Bean，供ConfigRefresher使用
            beanFactory.registerSingleton("refreshScope", refreshScope);
        };
    }
    
    @Bean
    public RefreshScopeImpl refreshScope() {
        return new RefreshScopeImpl();
    }
}
