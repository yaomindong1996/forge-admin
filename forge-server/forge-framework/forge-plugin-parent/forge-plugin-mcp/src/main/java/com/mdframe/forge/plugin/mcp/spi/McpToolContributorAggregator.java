package com.mdframe.forge.plugin.mcp.spi;

import com.mdframe.forge.plugin.mcp.adapter.McpToolSchemaProjector;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 合并固定 MCP 工具，并在启动阶段拒绝重复名称。
 */
public final class McpToolContributorAggregator {

    private McpToolContributorAggregator() {
    }

    public static List<McpServerFeatures.SyncToolSpecification> aggregate(
            List<McpToolContributor> contributors,
            McpToolSchemaProjector schemaProjector) {
        Map<String, McpServerFeatures.SyncToolSpecification> byName = new LinkedHashMap<>();
        List<McpToolContributor> safeContributors = contributors == null ? List.of() : contributors;
        for (McpToolContributor contributor : safeContributors) {
            if (contributor == null) {
                continue;
            }
            var specifications = contributor.contribute(schemaProjector);
            if (specifications == null) {
                continue;
            }
            for (McpServerFeatures.SyncToolSpecification specification : specifications) {
                if (specification == null || specification.tool() == null
                        || specification.tool().name() == null
                        || specification.tool().name().isBlank()) {
                    throw new IllegalStateException("MCP Tool contributor 返回了无效工具定义");
                }
                String toolName = specification.tool().name();
                if (byName.putIfAbsent(toolName, specification) != null) {
                    throw new IllegalStateException("MCP Tool 名称重复: " + toolName);
                }
            }
        }
        List<McpServerFeatures.SyncToolSpecification> result = new ArrayList<>(byName.values());
        result.sort(Comparator.comparing(item -> item.tool().name()));
        return List.copyOf(result);
    }
}
