package com.mdframe.forge.plugin.mcp.spi;

import com.mdframe.forge.plugin.mcp.adapter.McpToolSchemaProjector;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.Collection;

/**
 * Forge MCP 固定工具贡献者。业务模块只能贡献稳定元工具，不能创建额外 transport。
 */
@FunctionalInterface
public interface McpToolContributor {

    Collection<McpServerFeatures.SyncToolSpecification> contribute(
            McpToolSchemaProjector schemaProjector);
}
