package com.mdframe.forge.plugin.capability.registry;

import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.capability.model.CapabilityDefinition;
import com.mdframe.forge.plugin.capability.model.CapabilityInvocation;
import com.mdframe.forge.plugin.capability.model.CapabilityPage;
import com.mdframe.forge.plugin.capability.model.CapabilityQuery;
import com.mdframe.forge.plugin.capability.model.CapabilityResult;

public interface CapabilityRegistry {

    CapabilityPage list(CapabilityQuery query, CapabilityCallerContext caller);

    CapabilityDefinition requireActive(String capabilityCode, String version);

    CapabilityResult invoke(CapabilityInvocation invocation);
}
