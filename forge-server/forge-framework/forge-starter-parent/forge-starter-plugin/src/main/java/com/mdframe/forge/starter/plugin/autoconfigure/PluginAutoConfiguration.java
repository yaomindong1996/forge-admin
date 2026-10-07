package com.mdframe.forge.starter.plugin.autoconfigure;

import com.mdframe.forge.starter.plugin.descriptor.PluginRegistry;
import com.mdframe.forge.starter.plugin.feature.CommunityFeatureGate;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** 扩展点仅由自动配置加载，普通用户配置先注册，默认 Gate 再决定是否退出。 */
@AutoConfiguration
public class PluginAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FeatureGate.class)
    public CommunityFeatureGate communityFeatureGate() {
        return new CommunityFeatureGate();
    }

    @Bean
    @Lazy(false)
    public PluginRegistry pluginRegistry(ResourceLoader resourceLoader) {
        return new PluginRegistry(new PathMatchingResourcePatternResolver(resourceLoader));
    }
}
