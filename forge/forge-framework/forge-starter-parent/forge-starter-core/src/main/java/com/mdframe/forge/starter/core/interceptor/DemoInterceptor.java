package com.mdframe.forge.starter.core.interceptor;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.starter.core.context.DemoProperties;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoInterceptor implements HandlerInterceptor {
    
    private final DemoProperties demoProperties;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        
        if (!demoProperties.getEnabled()) {
            return true;
        }
        
        String method = request.getMethod();
        String requestURI = request.getRequestURI();
        
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        
        if (isWhitelisted(requestURI)) {
            log.debug("演示环境白名单放行: {} {}", method, requestURI);
            return true;
        }
        
        if (demoProperties.getBlockedMethods().contains(method.toUpperCase())) {
            log.warn("演示环境拦截写操作: {} {}", method, requestURI);
            
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            
            RespInfo<Object> result = RespInfo.error(
                HttpServletResponse.SC_FORBIDDEN,
                demoProperties.getMessage()
            );
            
            response.getWriter().write(JSON.toJSONString(result));
            return false;
        }
        
        return true;
    }
    
    private boolean isWhitelisted(String requestURI) {
        if (demoProperties.getWhitelistUrls() == null || demoProperties.getWhitelistUrls().isEmpty()) {
            return false;
        }
        
        return demoProperties.getWhitelistUrls().stream()
            .anyMatch(pattern -> match(pattern, requestURI));
    }
    
    private boolean match(String pattern, String path) {
        if (StrUtil.isBlank(pattern) || StrUtil.isBlank(path)) {
            return false;
        }
        
        pattern = pattern.trim();
        path = path.trim();
        
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            if (!path.startsWith(prefix)) {
                return false;
            }
            String suffix = path.substring(prefix.length());
            return !suffix.contains("/");
        }
        
        return pattern.equals(path);
    }
}