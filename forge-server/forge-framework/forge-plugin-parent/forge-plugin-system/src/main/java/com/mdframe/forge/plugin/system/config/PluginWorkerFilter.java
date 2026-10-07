package com.mdframe.forge.plugin.system.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Collections;

/** 始终注册、默认拒绝。SaIgnore 仅跳过用户会话，绝不能跳过本过滤器。 */
@RequiredArgsConstructor
@Slf4j
public class PluginWorkerFilter extends OncePerRequestFilter {
    private final PluginWorkerProperties properties;
    private final ObjectMapper json;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        if (!properties.usable(Instant.now())) {
            reject(response, 503, "构建执行器未启用或凭证已到期");
            return;
        }
        if (!validRequest(request) || !authenticated(request)) {
            reject(response, 401, "执行器认证或请求无效");
            return;
        }
        request.setAttribute(PluginWorkerIdentity.ATTRIBUTE,
                new PluginWorkerIdentity(properties.getTenantId(), properties.getId()));
        chain.doFilter(request, response);
    }

    private boolean validRequest(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return request.isSecure() && "POST".equals(request.getMethod())
                && path.matches("/internal/plugin-build/[a-f0-9-]{36}/(claim|archive|heartbeat|finish)")
                && request.getQueryString() == null && request.getHeader("X-Inner-Call") == null
                && request.getContentLengthLong() > 0 && request.getContentLengthLong() <= 65536
                && request.getContentType() != null && request.getContentType().startsWith("application/json");
    }

    private boolean authenticated(HttpServletRequest request) {
        var headers = Collections.list(request.getHeaders("Authorization"));
        if (headers.size() != 1 || !headers.get(0).matches("Bearer [a-f0-9]{64}")) {
            return false;
        }
        String hash = PackageDigests.sha256(headers.get(0).substring(7).getBytes(StandardCharsets.US_ASCII));
        return MessageDigest.isEqual(hash.getBytes(StandardCharsets.US_ASCII),
                properties.getTokenSha256().getBytes(StandardCharsets.US_ASCII));
    }

    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        log.warn("插件执行器请求被拒绝，状态码={}", status);
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        json.writeValue(response.getOutputStream(), RespInfo.error(status, message));
    }
}
