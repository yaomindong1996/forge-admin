package com.mdframe.forge.plugin.mcp.security;

import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import jakarta.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface McpRequestLifecycle {

    AutoCloseable open(HttpServletRequest request, CapabilityCallerContext callerContext);

    static McpRequestLifecycle noop() {
        return (request, caller) -> () -> {
        };
    }
}
