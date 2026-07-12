package com.mdframe.forge.plugin.capability.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.builtin.PingCapabilityExecutor;
import com.mdframe.forge.plugin.capability.builtin.PingCapabilitySource;
import com.mdframe.forge.plugin.capability.naming.CapabilityToolNameMapper;
import com.mdframe.forge.plugin.capability.registry.CapabilityRegistry;
import com.mdframe.forge.plugin.capability.registry.InMemoryCapabilityRegistry;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.spi.CapabilityAuthorizationPolicy;
import com.mdframe.forge.plugin.capability.spi.CapabilityExecutor;
import com.mdframe.forge.plugin.capability.spi.CapabilityInvocationObserver;
import com.mdframe.forge.plugin.capability.spi.CapabilitySource;
import com.mdframe.forge.plugin.capability.spi.ScopeBasedCapabilityAuthorizationPolicy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.util.List;

@AutoConfiguration
public class CapabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock capabilityClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    public CapabilityToolNameMapper capabilityToolNameMapper() {
        return new CapabilityToolNameMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public CapabilitySchemaValidator capabilitySchemaValidator() {
        return new CapabilitySchemaValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public CapabilityAuthorizationPolicy capabilityAuthorizationPolicy() {
        return new ScopeBasedCapabilityAuthorizationPolicy();
    }

    @Bean
    public PingCapabilitySource pingCapabilitySource(ObjectMapper objectMapper) {
        return new PingCapabilitySource(objectMapper);
    }

    @Bean
    public PingCapabilityExecutor pingCapabilityExecutor(ObjectMapper objectMapper, Clock capabilityClock) {
        return new PingCapabilityExecutor(objectMapper, capabilityClock);
    }

    @Bean
    @ConditionalOnMissingBean(CapabilityRegistry.class)
    public CapabilityRegistry capabilityRegistry(
            List<CapabilitySource> sources,
            List<CapabilityExecutor> executors,
            List<CapabilityInvocationObserver> invocationObservers,
            CapabilityAuthorizationPolicy authorizationPolicy,
            CapabilityToolNameMapper nameMapper,
            CapabilitySchemaValidator schemaValidator) {
        return new InMemoryCapabilityRegistry(
                sources, executors, authorizationPolicy, nameMapper, schemaValidator,
                invocationObservers);
    }
}
