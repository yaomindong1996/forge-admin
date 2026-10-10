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
import java.time.Instant;
import java.util.Collections;

/** 独立部署凭证，构建身份和用户会话不能代替此边界。 */
@RequiredArgsConstructor
public class PluginDeliveryFilter extends OncePerRequestFilter {
    public static final String IDENTITY = PluginDeliveryFilter.class.getName();
    private final PluginDeliveryProperties properties;
    private final ObjectMapper json;
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        res.setHeader("Cache-Control", "no-store");
        var headers = Collections.list(req.getHeaders("Authorization"));
        String path = req.getRequestURI().substring(req.getContextPath().length());
        boolean valid = req.isSecure() && "POST".equals(req.getMethod()) && req.getQueryString() == null
                && req.getHeader("X-Inner-Call") == null && req.getContentLengthLong() > 0
                && req.getContentLengthLong() <= 65536 && req.getContentType() != null
                && req.getContentType().startsWith("application/json")
                && path.matches("/internal/plugin-delivery/[a-f0-9-]{36}/(claim|heartbeat|authorize|finish|recovery)");
        if (!properties.usable(Instant.now()) || !valid || headers.size() != 1 || !matches(headers.get(0))) {
            res.setStatus(401);
            res.setContentType("application/json;charset=UTF-8");
            json.writeValue(res.getOutputStream(), RespInfo.error(401, "交付执行器认证或请求无效"));
            return;
        }
        var worker = properties.getWorker();
        req.setAttribute(IDENTITY, new PluginWorkerIdentity(worker.getTenantId(), worker.getId()));
        req.setAttribute(com.mdframe.forge.starter.core.util.MachineProtocolAttributes.VERIFIED_JSON, Boolean.TRUE);
        chain.doFilter(req, res);
    }
    private boolean matches(String value) {
        if (!value.matches("Bearer [a-f0-9]{64}")) { return false; }
        String actual = PackageDigests.sha256(value.substring(7).getBytes(StandardCharsets.US_ASCII));
        return MessageDigest.isEqual(actual.getBytes(StandardCharsets.US_ASCII),
                properties.getWorker().getTokenSha256().getBytes(StandardCharsets.US_ASCII));
    }
    public static PluginWorkerIdentity identity(HttpServletRequest request) {
        if (request.getAttribute(IDENTITY) instanceof PluginWorkerIdentity value) { return value; }
        throw new com.mdframe.forge.starter.core.exception.BusinessException(403, "交付身份未认证");
    }
}
