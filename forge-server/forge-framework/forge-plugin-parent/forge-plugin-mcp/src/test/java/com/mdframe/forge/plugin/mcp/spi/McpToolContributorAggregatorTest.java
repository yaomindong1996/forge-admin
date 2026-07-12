package com.mdframe.forge.plugin.mcp.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.mcp.adapter.McpToolSchemaProjector;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpToolContributorAggregatorTest {

    private final McpToolSchemaProjector projector = new McpToolSchemaProjector(new ObjectMapper());

    @Test
    void shouldMergeContributorsInStableToolNameOrder() {
        McpToolContributor second = ignored -> List.of(tool("capability.invoke"));
        McpToolContributor first = ignored -> List.of(tool("capability.describe"));

        var result = McpToolContributorAggregator.aggregate(List.of(second, first), projector);

        assertThat(result).extracting(item -> item.tool().name())
                .containsExactly("capability.describe", "capability.invoke");
    }

    @Test
    void shouldRejectDuplicateToolNames() {
        McpToolContributor first = ignored -> List.of(tool("capability.invoke"));
        McpToolContributor duplicate = ignored -> List.of(tool("capability.invoke"));

        assertThatThrownBy(() -> McpToolContributorAggregator.aggregate(
                List.of(first, duplicate), projector))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("capability.invoke");
    }

    private McpServerFeatures.SyncToolSpecification tool(String name) {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name(name)
                .description(name)
                .inputSchema(new McpSchema.JsonSchema(
                        "object", java.util.Map.of(), List.of(), false, java.util.Map.of(), java.util.Map.of()))
                .build();
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> McpSchema.CallToolResult.builder()
                        .addTextContent("{}")
                        .isError(false)
                        .build())
                .build();
    }
}
