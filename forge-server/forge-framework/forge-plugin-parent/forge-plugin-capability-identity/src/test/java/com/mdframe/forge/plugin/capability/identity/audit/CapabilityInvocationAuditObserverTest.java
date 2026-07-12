package com.mdframe.forge.plugin.capability.identity.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;
import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityInvocationAuditEvent;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityInvocationAuditService;
import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.capability.model.CapabilityInvocation;
import com.mdframe.forge.plugin.capability.model.CapabilityResult;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CapabilityInvocationAuditObserverTest {

    @AfterEach
    void cleanup() {
        ExecutionIdentityContextHolder.clear();
    }

    @Test
    void shouldPersistActualUserAndMachineServiceIdentity() {
        CapabilityCatalogService catalogService = mock(CapabilityCatalogService.class);
        CapabilityInvocationAuditService auditService = mock(CapabilityInvocationAuditService.class);
        CapabilityInvocationAuditObserver observer =
                new CapabilityInvocationAuditObserver(catalogService, auditService);
        LoginUser user = new LoginUser();
        user.setUserId(101L);
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        ExecutionIdentity identity = new ExecutionIdentity(
                user, "USER", 101L, 999L, 301L, "desktop_agent", "token-key",
                Set.of("capability:invoke:capability.ping"));
        CapabilityCallerContext caller = new CapabilityCallerContext(
                "desktop_agent", 1L, 101L, 201L, identity.scopes());
        CapabilityInvocation invocation = new CapabilityInvocation(
                "request-1", "capability.ping", "1.0.0", caller,
                new ObjectMapper().createObjectNode());
        CapabilityResult result = CapabilityResult.success(
                "request-1", "capability.ping", new ObjectMapper().createObjectNode(), 3L);

        try (ExecutionIdentityContextHolder.Scope ignored =
                     ExecutionIdentityContextHolder.open(identity)) {
            observer.onCompleted(invocation, result, null);
        }

        ArgumentCaptor<CapabilityInvocationAuditEvent> eventCaptor =
                ArgumentCaptor.forClass(CapabilityInvocationAuditEvent.class);
        verify(auditService).record(org.mockito.ArgumentMatchers.eq(1L), eventCaptor.capture());
        CapabilityInvocationAuditEvent event = eventCaptor.getValue();
        assertThat(event.actorType()).isEqualTo(CapabilityActorType.USER);
        assertThat(event.actorUserId()).isEqualTo(101L);
        assertThat(event.serviceUserId()).isEqualTo(999L);
        assertThat(event.clientId()).isEqualTo(301L);
        assertThat(event.activeOrgId()).isEqualTo(201L);
    }
}
