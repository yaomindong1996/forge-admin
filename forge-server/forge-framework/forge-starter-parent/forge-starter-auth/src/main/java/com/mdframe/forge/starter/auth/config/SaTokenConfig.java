package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.starter.auth.interceptor.ApiPermissionInterceptor;
import com.mdframe.forge.starter.auth.interceptor.ApiRateLimitInterceptor;
import com.mdframe.forge.starter.auth.session.LoginSessionRenewal;
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

    private final ApiRateLimitInterceptor apiRateLimitInterceptor;
    
    private final AuthProperties authProperties;

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 普通 API 限流独立于权限开关和注解豁免，使用数据库配置的模板路径匹配。
        registry.addInterceptor(apiRateLimitInterceptor)
                .addPathPatterns("/**")
                .order(2);

        // 登录页/认证公开面：Spring 级排除 + SaRouter.notMatch 双保险。
        // 仅靠 SaRouter.notMatch 时，个别路径（如 /auth/loginConfig）仍可能落入 checkLogin，
        // 导致登录页读不到群二维码配置；公开 Controller 方法另补 @SaIgnore。
        String[] anonymousAuthPaths = {
                "/auth/login",
                "/auth/logout",
                "/auth/loginConfig",
                "/auth/loginQrcode",
                "/auth/tenant/options",
                "/auth/tenant/assets/**",
                "/auth/register",
                "/auth/resetPassword",
                "/auth/resetPassword/code",
                "/auth/sso/exchange",
                "/auth/captcha",
                "/auth/captcha/slider",
                "/auth/captcha/sms",
                "/crypto/config",
                "/crypto/public-key",
                "/crypto/exchange",
                "/.well-known/oauth-protected-resource",
                "/.well-known/oauth-authorization-server",
                "/oauth2/token",
                "/oauth2/revoke",
                "/oauth2/userinfo",
                "/mcp",
                "/openapi/v1/jobs",
                "/openapi/v1/jobs/**",
                "/openapi/v1/executions/**",
                "/openapi/v1/capabilities/**",
                "/static/**",
                "/css/**",
                "/js/**",
                "/images/**",
                "/actuator/health",
                "/health",
                "/ws/**"
        };

        // 1. 注册 Sa-Token 登录校验拦截器
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 根据路由进行规则校验
            SaRouter.match("/**")
                    .notMatch(anonymousAuthPaths)
                    .check(r -> {
                        StpUtil.checkLogin();
                        LoginSessionRenewal.renewIfHalfwayExpired();
                    });
        })).addPathPatterns("/**")
                .excludePathPatterns(anonymousAuthPaths)
                .order(1);  // 优先级1，先执行登录校验

        // 2. 注册 API 接口权限拦截器（基于数据库资源表配置）
        registry.addInterceptor(apiPermissionInterceptor)
                .addPathPatterns("/**")
                // 排除登录相关接口
                .excludePathPatterns("/auth/login", "/auth/logout", "/auth/loginConfig", "/auth/loginQrcode", "/auth/tenant/options", "/auth/tenant/assets/**", "/auth/register", "/auth/resetPassword",
                        "/auth/resetPassword/code",
                        "/auth/captcha", "/auth/captcha/slider", "/auth/captcha/sms", "/auth/sso/exchange")
                .excludePathPatterns("/crypto/config", "/crypto/public-key", "/crypto/exchange")
                .excludePathPatterns("/.well-known/oauth-protected-resource",
                        "/.well-known/oauth-authorization-server", "/oauth2/token", "/oauth2/revoke",
                        "/oauth2/userinfo")
                .excludePathPatterns("/ai/capability/oauth/**", "/mcp")
                .excludePathPatterns("/openapi/v1/jobs", "/openapi/v1/jobs/**", "/openapi/v1/executions/**")
                .excludePathPatterns("/openapi/v1/capabilities/**")
                // 排除静态资源
                .excludePathPatterns("/static/**", "/css/**", "/js/**", "/images/**")
                // 排除健康检查
                .excludePathPatterns("/actuator/health", "/health")
                .excludePathPatterns(authProperties.getApiPermissionExcludePaths())
                .order(3);  // 优先级3，在登录校验和限流之后执行
    }
}
