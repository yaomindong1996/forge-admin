package com.mdframe.forge.plugin.system.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
@RequiredArgsConstructor
public class PluginRuntimeProbeFilter extends OncePerRequestFilter {
    public static final String VERIFIED = PluginRuntimeProbeFilter.class.getName();
    private final PluginRuntimeProbeProperties properties;
    private final ObjectMapper json;
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        res.setHeader("Cache-Control", "no-store");
        var headers = Collections.list(req.getHeaders("Authorization"));
        boolean local = properties.isAllowLoopback()
                && ("127.0.0.1".equals(req.getRemoteAddr()) || "::1".equals(req.getRemoteAddr())
                || "0:0:0:0:0:0:0:1".equals(req.getRemoteAddr()));
        boolean valid = properties.usable() && (req.isSecure() || local) && "GET".equals(req.getMethod())
                && req.getQueryString() == null && req.getHeader("X-Inner-Call") == null
                && req.getHeader("X-Forge-Probe-Nonce") != null
                && req.getHeader("X-Forge-Probe-Nonce").matches("[a-f0-9-]{36}")
                && headers.size() == 1 && matches(headers.get(0));
        if (!valid) {
            res.setStatus(401);
            res.setContentType("application/json;charset=UTF-8");
            json.writeValue(res.getOutputStream(), RespInfo.error(401, "运行探针认证失败"));
            return;
        }
        req.setAttribute(VERIFIED, Boolean.TRUE);
        req.setAttribute(com.mdframe.forge.starter.core.util.MachineProtocolAttributes.VERIFIED_JSON, Boolean.TRUE);
        chain.doFilter(req, res);
    }
    private boolean matches(String value) {
        if (!value.matches("Bearer [a-f0-9]{64}")) { return false; }
        var hash = PackageDigests.sha256(value.substring(7).getBytes(StandardCharsets.US_ASCII));
        return MessageDigest.isEqual(hash.getBytes(StandardCharsets.US_ASCII),
                properties.getTokenSha256().getBytes(StandardCharsets.US_ASCII));
    }
}
