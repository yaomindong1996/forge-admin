package com.mdframe.forge.starter.core.config;

import com.mdframe.forge.starter.core.web.SecurityHeadersFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/** Enables the framework browser security header baseline. */
@AutoConfiguration
public class SecurityHeadersAutoConfiguration {

    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityHeadersFilter());
        registration.addUrlPatterns("/*");
        registration.setName("forgeSecurityHeadersFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
