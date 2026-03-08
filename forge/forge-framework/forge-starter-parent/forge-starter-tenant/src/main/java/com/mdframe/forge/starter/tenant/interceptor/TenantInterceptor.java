package com.mdframe.forge.starter.tenant.interceptor;

import cn.hutool.extra.spring.SpringUtil;
import com.mdframe.forge.starter.apiconfig.domain.dto.ApiConfigInfo;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

/**
 * 租户拦截器
 * 从Session中获取租户ID并设置到上下文中
 * 依赖于认证模块，需要在认证拦截器之后执行
 */
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 检查是否标记了 @IgnoreTenant 注解
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            
            IApiConfigManager apiConfigManager = SpringUtil.getBean(IApiConfigManager.class);
            
            ApiConfigInfo apiConfig = apiConfigManager.getApiConfig(request.getRequestURI(), request.getMethod());
            if (apiConfig != null && !apiConfig.getNeedTenant()) {
                TenantContextHolder.setIgnore(true);
                return true;
            }
            
            // 检查方法上的注解
            IgnoreTenant methodAnnotation = AnnotationUtils.findAnnotation(method, IgnoreTenant.class);
            if (methodAnnotation != null && methodAnnotation.value()) {
                log.debug("方法 {}.{} 标记了 @IgnoreTenant，跳过租户设置",
                        method.getDeclaringClass().getSimpleName(), method.getName());
                // 设置忽略标记，让租户拦截器也跳过
                TenantContextHolder.setIgnore(true);
                return true;
            }
            
            // 检查类上的注解
            Class<?> beanType = handlerMethod.getBeanType();
            IgnoreTenant classAnnotation = AnnotationUtils.findAnnotation(beanType, IgnoreTenant.class);
            if (classAnnotation != null && classAnnotation.value()) {
                log.debug("类 {} 标记了 @IgnoreTenant，跳过租户设置", beanType.getSimpleName());
                TenantContextHolder.setIgnore(true);
                return true;
            }
        }
        
        try {
            // 从SessionHelper获取租户ID（需要引入forge-starter-auth）
            // 这里使用反射调用，避免强依赖
            Class<?> sessionHelperClass = Class.forName("com.mdframe.forge.starter.core.session.SessionHelper");
            Method getTenantIdMethod = sessionHelperClass.getMethod("getTenantId");
            Long tenantId = (Long) getTenantIdMethod.invoke(null);
            
            if (tenantId != null) {
                TenantContextHolder.setTenantId(tenantId);
                log.debug("设置租户上下文，租户ID: {}", tenantId);
            }
        } catch (ClassNotFoundException e) {
            // 如果没有引入auth模块，尝试从请求头获取
            String tenantIdHeader = request.getHeader("X-Tenant-Id");
            if (tenantIdHeader != null) {
                try {
                    Long tenantId = Long.parseLong(tenantIdHeader);
                    TenantContextHolder.setTenantId(tenantId);
                    log.debug("从请求头设置租户上下文，租户ID: {}", tenantId);
                } catch (NumberFormatException ex) {
                    log.warn("请求头中的租户ID格式错误: {}", tenantIdHeader);
                }
            }
        } catch (Exception e) {
            log.error("设置租户上下文失败", e);
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 清除租户上下文，避免内存泄漏
        TenantContextHolder.clear();
    }
}
