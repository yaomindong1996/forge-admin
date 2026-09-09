package com.mdframe.forge.starter.core.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Propagates a bounded request correlation identifier into the response and MDC.
 * Untrusted values containing control characters or excessive data are discarded.
 */
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_KEY = "requestId";
    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = sanitize(request.getHeader(REQUEST_ID_HEADER));
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        String previousRequestId = MDC.get(MDC_KEY);
        String previousTraceId = MDC.get(TRACE_ID_MDC_KEY);
        MDC.put(MDC_KEY, requestId);
        MDC.put(TRACE_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (previousRequestId == null) {
                MDC.remove(MDC_KEY);
            } else {
                MDC.put(MDC_KEY, previousRequestId);
            }
            if (previousTraceId == null) {
                MDC.remove(TRACE_ID_MDC_KEY);
            } else {
                MDC.put(TRACE_ID_MDC_KEY, previousTraceId);
            }
        }
    }

    private String sanitize(String candidate) {
        if (candidate == null) {
            return null;
        }
        String value = candidate.trim();
        return SAFE_REQUEST_ID.matcher(value).matches() ? value : null;
    }
}
