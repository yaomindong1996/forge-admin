package com.mdframe.forge.plugin.capability.spi;

import com.mdframe.forge.plugin.capability.model.CapabilityInvocation;
import com.mdframe.forge.plugin.capability.model.CapabilityResult;

@FunctionalInterface
public interface CapabilityInvocationObserver {

    void onCompleted(
            CapabilityInvocation invocation,
            CapabilityResult result,
            String schemaPath);
}
