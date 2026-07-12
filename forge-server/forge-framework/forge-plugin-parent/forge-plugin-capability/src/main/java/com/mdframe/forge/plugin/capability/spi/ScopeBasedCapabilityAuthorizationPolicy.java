package com.mdframe.forge.plugin.capability.spi;

import com.mdframe.forge.plugin.capability.model.CapabilityAuthorizationDecision;
import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.capability.model.CapabilityDefinition;
import com.mdframe.forge.plugin.capability.model.CapabilityErrorCode;

public final class ScopeBasedCapabilityAuthorizationPolicy implements CapabilityAuthorizationPolicy {

    public static final String ALL_SCOPE = "capability:*";
    public static final String DISCOVER_SCOPE = "capability:discover";
    public static final String INVOKE_SCOPE = "capability:invoke";

    @Override
    public CapabilityAuthorizationDecision evaluateDiscovery(
            CapabilityDefinition definition,
            CapabilityCallerContext caller) {
        CapabilityAuthorizationDecision identityDecision = validateIdentity(caller);
        if (!identityDecision.allowed()) {
            return identityDecision;
        }
        boolean allowed = caller.scopes().contains(ALL_SCOPE)
                || caller.scopes().contains(DISCOVER_SCOPE)
                || caller.scopes().contains("capability:discover:" + definition.capabilityCode());
        return allowed ? CapabilityAuthorizationDecision.allow()
                : CapabilityAuthorizationDecision.deny(CapabilityErrorCode.FORBIDDEN);
    }

    @Override
    public CapabilityAuthorizationDecision evaluateInvocation(
            CapabilityDefinition definition,
            CapabilityCallerContext caller) {
        CapabilityAuthorizationDecision identityDecision = validateIdentity(caller);
        if (!identityDecision.allowed()) {
            return identityDecision;
        }
        boolean allowed = caller.scopes().contains(ALL_SCOPE)
                || caller.scopes().contains(INVOKE_SCOPE)
                || caller.scopes().contains("capability:invoke:" + definition.capabilityCode());
        return allowed ? CapabilityAuthorizationDecision.allow()
                : CapabilityAuthorizationDecision.deny(CapabilityErrorCode.FORBIDDEN);
    }

    private CapabilityAuthorizationDecision validateIdentity(CapabilityCallerContext caller) {
        if (caller == null
                || caller.machineClientId() == null
                || caller.machineClientId().isBlank()
                || caller.tenantId() == null
                || caller.tenantId() <= 0) {
            return CapabilityAuthorizationDecision.deny(CapabilityErrorCode.UNAUTHENTICATED);
        }
        if (caller.activeOrgId() != null && caller.activeOrgId() <= 0) {
            return CapabilityAuthorizationDecision.deny(CapabilityErrorCode.ORG_SCOPE_VIOLATION);
        }
        return CapabilityAuthorizationDecision.allow();
    }
}
