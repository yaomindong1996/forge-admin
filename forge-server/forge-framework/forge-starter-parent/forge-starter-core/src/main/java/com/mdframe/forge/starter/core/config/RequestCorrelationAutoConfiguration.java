package com.mdframe.forge.starter.core.config;

import com.mdframe.forge.starter.core.web.RequestCorrelationFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/** Enables request correlation for HTTP logs and downstream diagnostics. */
@AutoConfiguration
public class RequestCorrelationAutoConfiguration {

    @Bean
    public FilterRegistrationBean<RequestCorrelationFilter> requestCorrelationFilter() {
        FilterRegistrationBean<RequestCorrelationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestCorrelationFilter());
        registration.addUrlPatterns("/*");
        registration.setName("forgeRequestCorrelationFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
