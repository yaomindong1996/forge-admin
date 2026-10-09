package com.mdframe.forge.plugin.system.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
@Configuration
@EnableConfigurationProperties({PluginDeliveryProperties.class, PluginRuntimeProbeProperties.class})
public class PluginDeliveryConfiguration {
    @Bean public FilterRegistrationBean<PluginDeliveryFilter> pluginDeliveryFilter(
            PluginDeliveryProperties properties, ObjectMapper json) {
        var filter = new FilterRegistrationBean<>(new PluginDeliveryFilter(properties, json));
        filter.addUrlPatterns("/internal/plugin-delivery/*");
        filter.setOrder(Ordered.HIGHEST_PRECEDENCE + 21);
        return filter;
    }
    @Bean public FilterRegistrationBean<PluginRuntimeProbeFilter> pluginRuntimeProbeFilter(
            PluginRuntimeProbeProperties properties, ObjectMapper json) {
        var filter = new FilterRegistrationBean<>(new PluginRuntimeProbeFilter(properties, json));
        filter.addUrlPatterns("/internal/plugin-runtime/probe");
        filter.setOrder(Ordered.HIGHEST_PRECEDENCE + 22);
        return filter;
    }
}
