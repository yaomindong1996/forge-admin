package com.mdframe.forge.starter.auth.interceptor;

import com.mdframe.forge.starter.apiconfig.domain.dto.ApiConfigInfo;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.auth.service.ApiRateLimitManager;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 独立的普通 API 限流拦截器，不受 API 权限开关或注解豁免影响。
 */
@Component
@RequiredArgsConstructor
public class ApiRateLimitInterceptor implements HandlerInterceptor {

    private final IApiConfigManager apiConfigManager;
    private final ObjectProvider<ApiRateLimitManager> rateLimitManagerProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String requestPath = request.getRequestURI();
        if (StringUtils.hasText(request.getContextPath()) && requestPath.startsWith(request.getContextPath())) {
            requestPath = requestPath.substring(request.getContextPath().length());
        }
        ApiConfigInfo config = apiConfigManager.getApiConfig(requestPath, request.getMethod());
        if (config == null || !Boolean.TRUE.equals(config.getNeedLimit())) {
            return true;
        }
        ApiRateLimitManager manager = rateLimitManagerProvider.getIfAvailable();
        if (manager != null) {
            manager.acquire(rateLimitScope(request), request.getMethod().toUpperCase() + ":" + config.getUrlPath());
        }
        return true;
    }

    private String rateLimitScope(HttpServletRequest request) {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser != null && loginUser.getUserId() != null) {
            return "tenant-" + String.valueOf(loginUser.getTenantId()) + ":user-" + loginUser.getUserId();
        }
        String remoteAddress = request.getRemoteAddr();
        return "ip-" + (remoteAddress == null ? "unknown" : remoteAddress);
    }
}
