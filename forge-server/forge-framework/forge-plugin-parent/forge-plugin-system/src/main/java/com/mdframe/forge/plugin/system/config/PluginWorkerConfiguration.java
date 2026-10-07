package com.mdframe.forge.plugin.system.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableConfigurationProperties(PluginWorkerProperties.class)
public class PluginWorkerConfiguration {
    @Bean
    public FilterRegistrationBean<PluginWorkerFilter> pluginWorkerFilter(
            PluginWorkerProperties properties, ObjectMapper json) {
        var registration = new FilterRegistrationBean<>(new PluginWorkerFilter(properties, json));
        registration.addUrlPatterns("/internal/plugin-build/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
