package com.mdframe.forge.starter.plugin.autoconfigure;

import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import com.mdframe.forge.starter.plugin.license.RuntimeLicenseFeatureGate;
import com.mdframe.forge.starter.plugin.license.RuntimeLicenseLoader;
import com.mdframe.forge.starter.plugin.license.RuntimeLicenseProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** Pro 可主动启用；社区默认不读取文件，客户自定义 Gate 始终优先。 */
@AutoConfiguration(before = PluginAutoConfiguration.class)
@ConditionalOnProperty(prefix = "forge.license", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RuntimeLicenseProperties.class)
public class RuntimeLicenseAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(FeatureGate.class)
    public RuntimeLicenseFeatureGate runtimeLicenseFeatureGate(RuntimeLicenseProperties properties) {
        return RuntimeLicenseLoader.load(properties);
    }
}
