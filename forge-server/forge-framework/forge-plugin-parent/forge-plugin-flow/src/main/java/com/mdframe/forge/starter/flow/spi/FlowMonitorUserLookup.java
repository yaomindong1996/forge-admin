package com.mdframe.forge.starter.flow.spi;

/**
 * Resolves optional user display information for flow monitoring views.
 */
public interface FlowMonitorUserLookup {

    String findDisplayName(String userId);
}
