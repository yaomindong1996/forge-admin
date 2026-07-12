package com.mdframe.forge.plugin.capability.controlplane.config;

import com.mdframe.forge.plugin.capability.controlplane.security.CapabilityClientSecretCodec;
import com.mdframe.forge.plugin.capability.controlplane.security.CapabilityGrantPolicy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(CapabilityControlPlaneProperties.class)
public class CapabilityControlPlaneAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CapabilityClientSecretCodec capabilityClientSecretCodec(
            CapabilityControlPlaneProperties properties) {
        return new CapabilityClientSecretCodec(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CapabilityGrantPolicy capabilityGrantPolicy() {
        return new CapabilityGrantPolicy();
    }
}
