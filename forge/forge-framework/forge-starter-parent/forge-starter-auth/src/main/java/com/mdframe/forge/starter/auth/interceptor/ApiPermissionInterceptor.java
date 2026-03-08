package com.mdframe.forge.starter.auth.interceptor;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.starter.apiconfig.domain.dto.ApiConfigInfo;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.core.context.AuthProperties;
import com.mdframe.forge.starter.auth.service.IPermissionService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API接口权限拦截器
 * 基于数据库资源表配置的接口权限控制，支持通配符匹配
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiPermissionInterceptor implements HandlerInterceptor {

    private final IPermissionService permissionService;
    private final AuthProperties authProperties;
    private final IApiConfigManager apiConfigManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 0. 检查是否启用API权限校验
        if (authProperties.getEnableApiPermission() == null || !authProperties.getEnableApiPermission()) {
            log.debug("API权限校验已禁用");
            return true;
        }

        // 1. 只拦截Controller方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        ApiConfigInfo apiConfig = apiConfigManager.getApiConfig(request.getRequestURI(), request.getMethod());
        if (apiConfig != null && !apiConfig.getNeedAuth()) {
            log.debug("匿名访问接口: {}", request.getRequestURI());
            return true;
        }
        
        // 2. 检查是否有@SaIgnore注解（匿名访问）
        SaIgnore anonymous = handlerMethod.getMethodAnnotation(SaIgnore.class);
        if (anonymous == null) {
            anonymous = handlerMethod.getBeanType().getAnnotation(SaIgnore.class);
        }
        ApiPermissionIgnore apiPermissionIgnore = handlerMethod.getMethodAnnotation(ApiPermissionIgnore.class);
        if (apiPermissionIgnore == null) {
            apiPermissionIgnore = handlerMethod.getBeanType().getAnnotation(ApiPermissionIgnore.class);
        }
        if (anonymous != null || apiPermissionIgnore != null || request.getRequestURI().startsWith("/ws")) {
            log.debug("匿名访问接口: {}", request.getRequestURI());
            return true;
        }

        // 3. 获取请求的URI
        String requestUri = request.getRequestURI();
        
        // 移除contextPath（如果存在）
        String contextPath = request.getContextPath();
        if (StrUtil.isNotBlank(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }

        // 4. 校验接口权限
        boolean hasPermission = permissionService.hasApiPermission(requestUri);
        
        if (!hasPermission) {
            log.warn("接口权限校验失败: uri={}, method={}, handler={}",
                    requestUri, request.getMethod(), handlerMethod.getMethod().getName());
            throw new NotPermissionException("无权限访问该接口: " + requestUri);
        }

        log.debug("接口权限校验通过: uri={}", requestUri);
        return true;
    }
}
