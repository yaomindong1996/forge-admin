package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;

/** Servlet 异步重新分派也必须建立 Sa-Token 上下文，不能依赖已结束的请求线程。 */
@Configuration(proxyBeanMethods = false)
public class SaTokenServletContextConfiguration {

    @Bean
    FilterRegistrationBean<SaTokenContextFilterForJakartaServlet> saTokenServletContextRegistration(
            SaTokenContextFilterForJakartaServlet filter) {
        // 注册上游同一个 Filter 实例，避免 Boot 再自动注册一次或重复维护认证实现。
        FilterRegistrationBean<SaTokenContextFilterForJakartaServlet> registration =
                new FilterRegistrationBean<>(filter);
        registration.setName("saTokenServletContext");
        registration.addUrlPatterns("/*");
        registration.setAsyncSupported(true);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
        registration.setOrder(OrderUtils.getOrder(filter.getClass(), Ordered.HIGHEST_PRECEDENCE));
        return registration;
    }
}
