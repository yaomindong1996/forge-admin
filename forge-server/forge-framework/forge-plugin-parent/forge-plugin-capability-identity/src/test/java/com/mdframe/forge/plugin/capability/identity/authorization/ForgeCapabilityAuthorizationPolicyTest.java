package com.mdframe.forge.plugin.capability.identity.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.security.CapabilityGrantDecision;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityGrantService;
import com.mdframe.forge.plugin.capability.model.CapabilityBehavior;
import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.capability.model.CapabilityDefinition;
import com.mdframe.forge.plugin.capability.model.CapabilityErrorCode;
import com.mdframe.forge.plugin.capability.model.CapabilityRiskLevel;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForgeCapabilityAuthorizationPolicyTest {

    private CapabilityGrantService grantService;
    private ForgeCapabilityAuthorizationPolicy policy;
    private LoginUser user;
    private ExecutionIdentityContextHolder.Scope scope;

    @BeforeEach
    void setUp() {
        grantService = mock(CapabilityGrantService.class);
        policy = new ForgeCapabilityAuthorizationPolicy(
                grantService, new ForgeCapabilityPermissionMapper());
        user = user(Set.of("ai:capability:invoke:order.submit"));
        scope = ExecutionIdentityContextHolder.open(identity(user));
    }

    @AfterEach
    void tearDown() {
        if (scope != null) {
            scope.close();
        }
        ExecutionIdentityContextHolder.clear();
    }

    @Test
    void shouldRequireScopeGrantPermissionAndExactOrganizationForBusinessCapability() {
        CapabilityDefinition definition = definition("order.submit");
        CapabilityCallerContext caller = caller(Set.of("capability:invoke:order.submit"));
        when(grantService.evaluate(1L, 201L, 301L, "order.submit", "1.0.0"))
                .thenReturn(CapabilityGrantDecision.allow("1.0.0"));

        assertThat(policy.evaluateInvocation(definition, caller).allowed()).isTrue();

        reopenIdentity(user(Set.of()));
        assertThat(policy.evaluateInvocation(definition, caller).errorCode())
                .isEqualTo(CapabilityErrorCode.FORBIDDEN);

        reopenIdentity(user(Set.of("ai:capability:invoke:order.submit")));
        when(grantService.evaluate(1L, 201L, 301L, "order.submit", "1.0.0"))
                .thenReturn(CapabilityGrantDecision.deny("GRANT_DISABLED"));
        assertThat(policy.evaluateInvocation(definition, caller).errorCode())
                .isEqualTo(CapabilityErrorCode.FORBIDDEN);

        CapabilityCallerContext wrongOrg = new CapabilityCallerContext(
                "desktop_agent", 1L, 101L, 202L,
                Set.of("capability:invoke:order.submit"));
        assertThat(policy.evaluateInvocation(definition, wrongOrg).errorCode())
                .isEqualTo(CapabilityErrorCode.UNAUTHENTICATED);
    }

    @Test
    void shouldKeepPingAsExplicitAuthenticatedExceptionOnly() {
        CapabilityDefinition ping = definition("capability.ping");

        assertThat(policy.evaluateInvocation(
                ping, caller(Set.of("capability:invoke:capability.ping"))).allowed()).isTrue();
        verify(grantService, never()).evaluate(1L, 201L, 301L, "capability.ping", "1.0.0");

        assertThat(policy.evaluateInvocation(ping, caller(Set.of())).errorCode())
                .isEqualTo(CapabilityErrorCode.FORBIDDEN);
    }

    private CapabilityDefinition definition(String code) {
        ObjectMapper objectMapper = new ObjectMapper();
        return new CapabilityDefinition(
                code, code, "1.0.0", CapabilityBehavior.READ_ONLY, CapabilityRiskLevel.LOW,
                "test", objectMapper.createObjectNode().put("type", "object"),
                objectMapper.createObjectNode().put("type", "object"));
    }

    private CapabilityCallerContext caller(Set<String> scopes) {
        return new CapabilityCallerContext("desktop_agent", 1L, 101L, 201L, scopes);
    }

    private LoginUser user(Set<String> permissions) {
        LoginUser value = new LoginUser();
        value.setUserId(101L);
        value.setTenantId(1L);
        value.setActiveOrgId(201L);
        value.setPermissions(permissions);
        return value;
    }

    private ExecutionIdentity identity(LoginUser loginUser) {
        return new ExecutionIdentity(
                loginUser, "USER", 101L, 999L, 301L,
                "desktop_agent", "token-id", Set.of("capability:invoke:order.submit"));
    }

    private void reopenIdentity(LoginUser loginUser) {
        scope.close();
        scope = ExecutionIdentityContextHolder.open(identity(loginUser));
    }
}
