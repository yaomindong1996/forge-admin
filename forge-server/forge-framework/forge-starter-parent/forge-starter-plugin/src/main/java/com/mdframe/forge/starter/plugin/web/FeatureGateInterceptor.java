package com.mdframe.forge.starter.plugin.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import com.mdframe.forge.starter.plugin.feature.RequiresFeature;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 在登录/RBAC 之后按请求判断功能使用权，不缓存租户相关授权结论。 */
@RequiredArgsConstructor
@Slf4j
public final class FeatureGateInterceptor implements HandlerInterceptor {

    private final FeatureGate featureGate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequiresFeature requirement = findRequirement(method);
        if (requirement == null || featureGate.isEnabled(requirement.value())) {
            return true;
        }
        log.warn("功能使用权拒绝，功能编码={}", requirement.value());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), RespInfo.error(HttpServletResponse.SC_FORBIDDEN,
                "当前版本未开通该功能：" + requirement.value()));
        return false;
    }

    private RequiresFeature findRequirement(HandlerMethod handler) {
        RequiresFeature method = AnnotatedElementUtils.findMergedAnnotation(handler.getMethod(), RequiresFeature.class);
        return method != null ? method
                : AnnotatedElementUtils.findMergedAnnotation(handler.getBeanType(), RequiresFeature.class);
    }
}
