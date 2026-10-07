package com.mdframe.forge.starter.plugin.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 不自行启用 MVC，不改变原有 Sa-Token 配置和匿名路径。 */
@RequiredArgsConstructor
public final class FeatureGateWebMvcConfigurer implements WebMvcConfigurer {

    public static final int INTERCEPTOR_ORDER = 4;

    private final FeatureGateInterceptor interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/**").order(INTERCEPTOR_ORDER);
    }
}
