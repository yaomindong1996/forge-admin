package com.mdframe.forge.plugin.mcp.security;

import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import jakarta.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface McpCallerContextResolver {

    CapabilityCallerContext resolve(HttpServletRequest request);

    default String authenticationChallenge() {
        return "Bearer";
    }
}
