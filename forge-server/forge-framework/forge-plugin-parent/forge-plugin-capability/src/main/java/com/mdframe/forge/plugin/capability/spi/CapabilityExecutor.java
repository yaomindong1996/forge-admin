package com.mdframe.forge.plugin.capability.spi;

import com.mdframe.forge.plugin.capability.model.CapabilityDefinition;
import com.mdframe.forge.plugin.capability.model.CapabilityInvocation;
import com.mdframe.forge.plugin.capability.model.CapabilityResult;

public interface CapabilityExecutor {

    boolean supports(CapabilityDefinition definition);

    CapabilityResult invoke(CapabilityDefinition definition, CapabilityInvocation invocation);
}
