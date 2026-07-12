package com.mdframe.forge.plugin.capability.spi;

import com.mdframe.forge.plugin.capability.model.CapabilityDefinition;
import com.mdframe.forge.plugin.capability.model.CapabilityQuery;

import java.util.Collection;

@FunctionalInterface
public interface CapabilitySource {

    Collection<CapabilityDefinition> load(CapabilityQuery query);
}
