package com.mdframe.forge.plugin.mcp.security;

import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.Map;

public final class ForgeMcpTransportContextExtractor
        implements McpTransportContextExtractor<ServerRequest> {

    private final McpCallerContextResolver callerContextResolver;

    public ForgeMcpTransportContextExtractor(McpCallerContextResolver callerContextResolver) {
        this.callerContextResolver = callerContextResolver;
    }

    @Override
    public McpTransportContext extract(ServerRequest request) {
        Object existingCaller = request.servletRequest().getAttribute(McpTransportContextKeys.CALLER_CONTEXT);
        CapabilityCallerContext caller = existingCaller instanceof CapabilityCallerContext context
                ? context
                : callerContextResolver.resolve(request.servletRequest());
        if (caller == null) {
            throw new McpAuthenticationException("MCP 调用方身份未通过验证");
        }
        Object requestId = request.servletRequest().getAttribute(McpTransportContextKeys.REQUEST_ID);
        if (requestId instanceof String value && !value.isBlank()) {
            return McpTransportContext.create(Map.of(
                    McpTransportContextKeys.CALLER_CONTEXT, caller,
                    McpTransportContextKeys.REQUEST_ID, value));
        }
        return McpTransportContext.create(Map.of(McpTransportContextKeys.CALLER_CONTEXT, caller));
    }
}
