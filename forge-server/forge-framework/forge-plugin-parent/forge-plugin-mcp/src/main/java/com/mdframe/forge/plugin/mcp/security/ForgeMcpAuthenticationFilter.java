package com.mdframe.forge.plugin.mcp.security;

import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.starter.core.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class ForgeMcpAuthenticationFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Logger log = LoggerFactory.getLogger(ForgeMcpAuthenticationFilter.class);

    private final String mcpEndpoint;
    private final McpCallerContextResolver callerContextResolver;
    private final McpRequestLifecycle requestLifecycle;

    public ForgeMcpAuthenticationFilter(
            String mcpEndpoint,
            McpCallerContextResolver callerContextResolver) {
        this(mcpEndpoint, callerContextResolver, McpRequestLifecycle.noop());
    }

    public ForgeMcpAuthenticationFilter(
            String mcpEndpoint,
            McpCallerContextResolver callerContextResolver,
            McpRequestLifecycle requestLifecycle) {
        this.mcpEndpoint = mcpEndpoint;
        this.callerContextResolver = callerContextResolver;
        this.requestLifecycle = requestLifecycle;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI().substring(request.getContextPath().length());
        return !mcpEndpoint.equals(requestPath);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        CapabilityCallerContext caller;
        try {
            caller = callerContextResolver.resolve(request);
        }
        catch (McpAccessDeniedException exception) {
            writeForbidden(response, requestId);
            return;
        }
        catch (BusinessException exception) {
            if (Integer.valueOf(403).equals(exception.getCode())) {
                writeForbidden(response, requestId);
            } else if (Integer.valueOf(401).equals(exception.getCode())) {
                writeUnauthorized(response, requestId);
            } else {
                writeUnavailable(response, requestId, exception);
            }
            return;
        }
        catch (RuntimeException exception) {
            writeUnavailable(response, requestId, exception);
            return;
        }
        if (caller == null) {
            writeUnauthorized(response, requestId);
            return;
        }
        request.setAttribute(McpTransportContextKeys.CALLER_CONTEXT, caller);
        request.setAttribute(McpTransportContextKeys.REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try (AutoCloseable ignored = requestLifecycle.open(request, caller)) {
            filterChain.doFilter(request, response);
        }
        catch (IOException | ServletException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new ServletException("MCP 执行身份上下文处理失败", exception);
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String requestId) throws IOException {
        log.warn("[MCP身份验证] requestId={}, resultCode=UNAUTHENTICATED", requestId);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader("WWW-Authenticate", callerContextResolver.authenticationChallenge());
        response.getWriter().write("{\"requestId\":\"" + requestId
                + "\",\"errorCode\":\"UNAUTHENTICATED\","
                + "\"message\":\"MCP 调用方身份未通过验证\"}");
    }

    private void writeForbidden(HttpServletResponse response, String requestId) throws IOException {
        log.warn("[MCP身份验证] requestId={}, resultCode=ORIGIN_FORBIDDEN", requestId);
        writeError(response, requestId, HttpServletResponse.SC_FORBIDDEN,
                "FORBIDDEN", "MCP 请求来源未获允许");
    }

    private void writeUnavailable(
            HttpServletResponse response,
            String requestId,
            RuntimeException exception) throws IOException {
        log.error("[MCP身份验证] requestId={}, resultCode=AUTHENTICATION_UNAVAILABLE, exceptionType={}",
                requestId, exception.getClass().getSimpleName(), exception);
        writeError(response, requestId, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "AUTHENTICATION_UNAVAILABLE", "MCP 身份服务暂不可用");
    }

    private void writeError(
            HttpServletResponse response,
            String requestId,
            int status,
            String errorCode,
            String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.getWriter().write("{\"requestId\":\"" + requestId
                + "\",\"errorCode\":\"" + errorCode + "\","
                + "\"message\":\"" + message + "\"}");
    }
}
