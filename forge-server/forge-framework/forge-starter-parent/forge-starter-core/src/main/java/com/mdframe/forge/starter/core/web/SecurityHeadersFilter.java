package com.mdframe.forge.starter.core.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Applies a conservative set of browser security headers to every HTTP response.
 * HSTS is emitted only when the request was served over HTTPS, so local HTTP
 * development is not pinned accidentally.
 */
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        setIfMissing(response, "X-Content-Type-Options", "nosniff");
        setIfMissing(response, "X-Frame-Options", "SAMEORIGIN");
        setIfMissing(response, "Referrer-Policy", "strict-origin-when-cross-origin");
        setIfMissing(response, "Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        setIfMissing(response, "Content-Security-Policy", "default-src 'self'; frame-ancestors 'self'");
        String requestPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        String relativePath = requestPath;
        if (contextPath != null && !contextPath.isEmpty() && !"/".equals(contextPath)
                && requestPath.startsWith(contextPath)) {
            relativePath = requestPath.substring(contextPath.length());
        }
        if (relativePath.startsWith("/auth/") || relativePath.startsWith("/oauth2/")) {
            setIfMissing(response, "Cache-Control", "no-store, max-age=0");
            setIfMissing(response, "Pragma", "no-cache");
        }
        if (request.isSecure()) {
            setIfMissing(response, "Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        filterChain.doFilter(request, response);
    }

    private void setIfMissing(HttpServletResponse response, String name, String value) {
        if (!response.containsHeader(name)) {
            response.setHeader(name, value);
        }
    }
}
