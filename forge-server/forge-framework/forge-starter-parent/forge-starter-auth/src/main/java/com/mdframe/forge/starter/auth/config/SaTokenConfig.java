package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.starter.auth.interceptor.ApiPermissionInterceptor;
import com.mdframe.forge.starter.core.context.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置类
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final ApiPermissionInterceptor apiPermissionInterceptor;
    
    private final AuthProperties authProperties;

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 注册 Sa-Token 登录校验拦截器
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 根据路由进行规则校验
            SaRouter.match("/**")
                    // 排除登录接口（统一入口）
                    .notMatch("/auth/login")
                    // 排除登出接口，支持前端重复/无效 token 登出时幂等返回
                    .notMatch("/auth/logout")
                    // 排除登录页配置接口
                    .notMatch("/auth/loginConfig")
                    // 排除登录页租户选项接口
                    .notMatch("/auth/tenant/options")
                    // 排除登录页租户品牌图接口
                    .notMatch("/auth/tenant/assets/**")
                    // 排除注册接口
                    .notMatch("/auth/register")
                    // 排除重置密码接口
                    .notMatch("/auth/resetPassword")
                    // 排除 SSO 票据交换接口
                    .notMatch("/auth/sso/exchange")
                    // 排除密钥交换相关接口（未登录时也需要先完成协商）
                    .notMatch("/crypto/public-key")
                    .notMatch("/crypto/exchange")
                    // MCP OAuth 公开元数据与 token 端点由专用认证链处理
                    .notMatch("/.well-known/oauth-protected-resource")
                    .notMatch("/.well-known/oauth-authorization-server")
                    .notMatch("/oauth2/token")
                    .notMatch("/oauth2/revoke")
                    // /mcp 必须由 MCP Bearer Filter 强制认证，禁止通用 Sa-Token 二次拦截
                    .notMatch("/mcp")
                    // 排除获取验证码接口
                    .notMatch("/auth/captcha")
                    .notMatch("/auth/captcha/slider")
                    .notMatch("/auth/captcha/sms")
                    // 排除静态资源
                    .notMatch("/static/**", "/css/**", "/js/**", "/images/**")
                    // 排除Swagger文档
                    .notMatch("/doc.html", "/webjars/**")
                    // 排除健康检查
                    .notMatch("/actuator/**", "/health")
                    .notMatch("/ws/**")
                    .notMatch(authProperties.getApiPermissionExcludePaths())
                    // 执行登录校验
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**").order(1);  // 优先级1，先执行登录校验

        // 2. 注册 API 接口权限拦截器（基于数据库资源表配置）
        registry.addInterceptor(apiPermissionInterceptor)
                .addPathPatterns("/**")
                // 排除登录相关接口
                .excludePathPatterns("/auth/login", "/auth/logout", "/auth/loginConfig", "/auth/tenant/options", "/auth/tenant/assets/**", "/auth/register", "/auth/resetPassword",
                        "/auth/captcha", "/auth/captcha/slider", "/auth/captcha/sms", "/auth/sso/exchange")
                .excludePathPatterns("/crypto/public-key", "/crypto/exchange")
                .excludePathPatterns("/.well-known/oauth-protected-resource",
                        "/.well-known/oauth-authorization-server", "/oauth2/token", "/oauth2/revoke")
                .excludePathPatterns("/ai/capability/oauth/**", "/mcp")
                // 排除静态资源
                .excludePathPatterns("/static/**", "/css/**", "/js/**", "/images/**")
                // 排除Swagger文档
                .excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**", "/doc.html", "/webjars/**")
                // 排除健康检查
                .excludePathPatterns("/actuator/**", "/health")
                .excludePathPatterns(authProperties.getApiPermissionExcludePaths())
                .order(2);  // 优先级2，在登录校验之后执行
    }
}
