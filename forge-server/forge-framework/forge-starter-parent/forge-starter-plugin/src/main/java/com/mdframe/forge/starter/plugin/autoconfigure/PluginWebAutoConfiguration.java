package com.mdframe.forge.starter.plugin.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import com.mdframe.forge.starter.plugin.web.FeatureGateInterceptor;
import com.mdframe.forge.starter.plugin.web.FeatureGateWebMvcConfigurer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/** 非 Servlet 宿主不注册 HTTP 拦截器；不让 MVC 配置进入业务包扫描。 */
@AutoConfiguration(after = {PluginAutoConfiguration.class, JacksonAutoConfiguration.class})
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class PluginWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FeatureGateInterceptor featureGateInterceptor(FeatureGate featureGate, ObjectMapper objectMapper) {
        return new FeatureGateInterceptor(featureGate, objectMapper);
    }

    @Bean
    public FeatureGateWebMvcConfigurer featureGateWebMvcConfigurer(FeatureGateInterceptor interceptor) {
        return new FeatureGateWebMvcConfigurer(interceptor);
    }
}
