package com.mdframe.forge.plugin.mcp.config;

import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;

public final class ForgeMcpProtocolGuard {

    public ForgeMcpProtocolGuard(McpServerProperties properties) {
        if (properties.getProtocol() != McpServerProperties.ServerProtocol.STREAMABLE) {
            throw new IllegalStateException(
                    "Forge MCP Server 只允许 STREAMABLE 协议，禁止 SSE 或 STATELESS transport");
        }
        if (properties.isStdio()) {
            throw new IllegalStateException("Forge MCP Server 禁止同时启用 stdio transport");
        }
        if (properties.getType() != McpServerProperties.ApiType.SYNC) {
            throw new IllegalStateException(
                    "Forge MCP Server 必须使用 SYNC 类型，以保证受信执行身份在工具线程中完整生效");
        }
    }
}
