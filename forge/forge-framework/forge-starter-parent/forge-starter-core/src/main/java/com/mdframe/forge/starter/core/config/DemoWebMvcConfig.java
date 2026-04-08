package com.mdframe.forge.starter.core.config;

import com.mdframe.forge.starter.core.interceptor.DemoInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class DemoWebMvcConfig implements WebMvcConfigurer {
    
    private final DemoInterceptor demoInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(demoInterceptor)
            .addPathPatterns("/**")
            .order(0);
    }
}