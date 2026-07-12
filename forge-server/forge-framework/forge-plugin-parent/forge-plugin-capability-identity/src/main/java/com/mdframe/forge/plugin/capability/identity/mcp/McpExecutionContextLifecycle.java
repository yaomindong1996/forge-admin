package com.mdframe.forge.plugin.capability.identity.mcp;

import com.mdframe.forge.plugin.capability.identity.security.AuthenticatedCapabilityIdentity;
import com.mdframe.forge.plugin.capability.identity.security.CapabilitySecurityPrincipal;
import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.mcp.security.McpRequestLifecycle;
import com.mdframe.forge.plugin.mcp.security.McpTransportContextKeys;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "forge.capability.identity", name = "enabled", havingValue = "true")
public class McpExecutionContextLifecycle implements McpRequestLifecycle {

    private static final String ACTOR_TYPE_MDC = "actorType";
    private static final String ACTOR_USER_ID_MDC = "actorUserId";
    private static final String CLIENT_ID_MDC = "capabilityClientId";
    private static final String REQUEST_ID_MDC = "requestId";

    @Override
    public AutoCloseable open(
            HttpServletRequest request,
            CapabilityCallerContext callerContext) {
        Object value = request.getAttribute(
                CapabilityMcpAccessTokenResolver.AUTHENTICATED_IDENTITY_ATTRIBUTE);
        if (!(value instanceof AuthenticatedCapabilityIdentity authenticated)) {
            throw new IllegalStateException("MCP 请求缺少已验证身份");
        }
        CapabilitySecurityPrincipal principal = authenticated.principal();
        Long previousTenantId = TenantContextHolder.getTenantId();
        Boolean previousIgnore = TenantContextHolder.getIgnoreValue();
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();

        try {
            TenantContextHolder.setTenantId(principal.tenantId());
            TenantContextHolder.setIgnore(false);
            MDC.put(ACTOR_TYPE_MDC, principal.actorType().name());
            MDC.put(ACTOR_USER_ID_MDC, principal.actorUserId().toString());
            MDC.put(CLIENT_ID_MDC, principal.clientId().toString());
            Object requestId = request.getAttribute(McpTransportContextKeys.REQUEST_ID);
            if (requestId instanceof String id && !id.isBlank()) {
                MDC.put(REQUEST_ID_MDC, id);
            }

            ExecutionIdentity executionIdentity = new ExecutionIdentity(
                    authenticated.loginUser(), principal.actorType().name(),
                    principal.actorUserId(), principal.serviceUserId(), principal.clientId(),
                    principal.clientCode(), principal.tokenId(), principal.scopes());
            ExecutionIdentityContextHolder.Scope identityScope =
                    ExecutionIdentityContextHolder.open(executionIdentity);
            return () -> {
                try {
                    identityScope.close();
                } finally {
                    restore(previousTenantId, previousIgnore, previousMdc);
                }
            };
        }
        catch (RuntimeException exception) {
            restore(previousTenantId, previousIgnore, previousMdc);
            throw exception;
        }
    }

    private void restore(
            Long previousTenantId,
            Boolean previousIgnore,
            Map<String, String> previousMdc) {
        TenantContextHolder.clear();
        if (previousTenantId != null) {
            TenantContextHolder.setTenantId(previousTenantId);
        }
        if (previousIgnore == null) {
            TenantContextHolder.clearIgnore();
        } else {
            TenantContextHolder.setIgnore(previousIgnore);
        }
        if (previousMdc == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(previousMdc);
        }
    }
}
