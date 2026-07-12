package com.mdframe.forge.plugin.capability.identity.mcp;

import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;
import com.mdframe.forge.plugin.capability.identity.security.AuthenticatedCapabilityIdentity;
import com.mdframe.forge.plugin.capability.identity.security.CapabilitySecurityPrincipal;
import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.mcp.security.McpTransportContextKeys;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class McpExecutionContextLifecycleTest {

    @AfterEach
    void cleanup() {
        ExecutionIdentityContextHolder.clear();
        TenantContextHolder.clear();
        MDC.clear();
    }

    @Test
    void shouldEstablishAndRestoreForgeExecutionContext() throws Exception {
        TenantContextHolder.setTenantId(9L);
        TenantContextHolder.setIgnore(true);
        MDC.put("outer", "kept");

        LoginUser user = new LoginUser();
        user.setUserId(101L);
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        CapabilitySecurityPrincipal principal = new CapabilitySecurityPrincipal(
                301L, "desktop_agent", CapabilityActorType.USER, 101L, 999L,
                1L, 201L, 1, "token-key", "http://localhost:8580/mcp",
                Set.of("capability:invoke:capability.ping"));
        AuthenticatedCapabilityIdentity authenticated =
                new AuthenticatedCapabilityIdentity(principal, user);
        CapabilityCallerContext caller = new CapabilityCallerContext(
                "desktop_agent", 1L, 101L, 201L, principal.scopes());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(
                CapabilityMcpAccessTokenResolver.AUTHENTICATED_IDENTITY_ATTRIBUTE,
                authenticated);
        request.setAttribute(McpTransportContextKeys.REQUEST_ID, "request-1");

        AutoCloseable scope = new McpExecutionContextLifecycle().open(request, caller);
        assertThat(SessionHelper.getUserId()).isEqualTo(101L);
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(1L);
        assertThat(TenantContextHolder.isIgnore()).isFalse();
        assertThat(MDC.get("actorUserId")).isEqualTo("101");

        scope.close();
        assertThat(ExecutionIdentityContextHolder.current()).isEmpty();
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(9L);
        assertThat(TenantContextHolder.isIgnore()).isTrue();
        assertThat(MDC.get("outer")).isEqualTo("kept");
        assertThat(MDC.get("actorUserId")).isNull();
    }
}
