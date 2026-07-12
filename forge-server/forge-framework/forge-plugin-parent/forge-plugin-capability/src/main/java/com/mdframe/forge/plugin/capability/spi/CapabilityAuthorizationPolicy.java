package com.mdframe.forge.plugin.capability.spi;

import com.mdframe.forge.plugin.capability.model.CapabilityAuthorizationDecision;
import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.capability.model.CapabilityDefinition;

public interface CapabilityAuthorizationPolicy {

    CapabilityAuthorizationDecision evaluateDiscovery(
            CapabilityDefinition definition,
            CapabilityCallerContext caller);

    CapabilityAuthorizationDecision evaluateInvocation(
            CapabilityDefinition definition,
            CapabilityCallerContext caller);

    default boolean canDiscover(CapabilityDefinition definition, CapabilityCallerContext caller) {
        return evaluateDiscovery(definition, caller).allowed();
    }

    default boolean canInvoke(CapabilityDefinition definition, CapabilityCallerContext caller) {
        return evaluateInvocation(definition, caller).allowed();
    }
}
