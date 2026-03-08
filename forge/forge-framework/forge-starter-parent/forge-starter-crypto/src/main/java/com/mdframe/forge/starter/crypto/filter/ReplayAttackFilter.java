package com.mdframe.forge.starter.crypto.filter;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.crypto.cache.ReplayTokenCache;
import com.mdframe.forge.starter.core.context.CryptoProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 防重放攻击过滤器
 */
@Slf4j
@RequiredArgsConstructor
public class ReplayAttackFilter implements Filter {

    private final CryptoProperties properties;
    private final ReplayTokenCache tokenCache;
    private final ObjectMapper objectMapper;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // 检查是否需要防重放验证
        if (!needReplayProtection(path) || path.startsWith("/ws")) {
            chain.doFilter(request, response);
            return;
        }

        // 获取请求头中的防重放参数
        String timestamp = httpRequest.getHeader("X-Timestamp");
        String nonce = httpRequest.getHeader("X-Nonce");

        // 验证参数
        if (StrUtil.isBlank(timestamp) || StrUtil.isBlank(nonce)) {
            sendError(httpResponse, "缺少防重放参数");
            return;
        }

        try {
            // 1. 验证时间戳（在时间窗口内）
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            long timeWindow = properties.getReplayTimeWindow() * 1000;

            if (Math.abs(currentTime - requestTime) > timeWindow) {
                log.warn("请求已过期, 请求时间: {}, 当前时间: {}, 时间窗口: {}ms",
                        requestTime, currentTime, timeWindow);
                sendError(httpResponse, "请求已过期");
                return;
            }

            // 2. 验证nonce（是否已使用）
            if (tokenCache.exists(nonce)) {
                log.warn("检测到重复请求, nonce: {}", nonce);
                sendError(httpResponse, "重复的请求");
                return;
            }

            // 3. 缓存nonce，防止重放
            tokenCache.cache(nonce, properties.getReplayTimeWindow());

            // 验证通过，继续处理
            chain.doFilter(request, response);

        } catch (NumberFormatException e) {
            log.error("时间戳格式错误: {}", timestamp);
            sendError(httpResponse, "时间戳格式错误");
        } catch (Exception e) {
            log.error("防重放验证失败", e);
            sendError(httpResponse, "防重放验证失败");
        }
    }

    /**
     * 判断是否需要防重放保护
     */
    private boolean needReplayProtection(String path) {
        // 先检查排除路径
        for (String pattern : properties.getReplayExcludePaths()) {
            if (pathMatcher.match(pattern, path)) {
                return false;
            }
        }

        // 再检查包含路径
        if (properties.getReplayIncludePaths().isEmpty()) {
            return true;
        }

        for (String pattern : properties.getReplayIncludePaths()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }

        return false;
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);
        result.put("msg", message);

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
