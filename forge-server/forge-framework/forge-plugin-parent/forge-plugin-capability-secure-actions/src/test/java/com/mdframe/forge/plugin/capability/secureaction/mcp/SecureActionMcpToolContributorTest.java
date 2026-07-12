package com.mdframe.forge.plugin.capability.secureaction.mcp;

import com.mdframe.forge.plugin.mcp.adapter.McpToolSchemaProjector;
import io.modelcontextprotocol.server.McpServerFeatures;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecureActionMcpToolContributorTest {

    @Test
    void shouldExposeStructuredOutputSchemasAndSafeAnnotations() {
        SecureActionMcpToolContributor contributor = new SecureActionMcpToolContributor(
                mock(SecureActionMcpHandler.class));

        Map<String, McpServerFeatures.SyncToolSpecification> tools = contributor
                .contribute(mock(McpToolSchemaProjector.class)).stream()
                .collect(Collectors.toMap(item -> item.tool().name(), Function.identity()));

        assertThat(tools).containsOnlyKeys(
                "capability.search", "capability.describe", "capability.invoke");
        tools.values().forEach(item -> {
            assertThat(item.tool().outputSchema()).isNotEmpty();
            assertThat(item.tool().outputSchema()).containsEntry(
                    "$schema", "https://json-schema.org/draft/2020-12/schema");
        });
        assertThat(tools.get("capability.search").tool().annotations().readOnlyHint()).isTrue();
        assertThat(tools.get("capability.describe").tool().annotations().readOnlyHint()).isTrue();
        assertThat(tools.get("capability.invoke").tool().annotations().readOnlyHint()).isFalse();
        assertThat(tools.get("capability.invoke").tool().annotations().destructiveHint()).isTrue();
        assertThat(tools.get("capability.invoke").tool().annotations().idempotentHint()).isTrue();
        assertThat(tools.get("capability.invoke").tool().annotations().openWorldHint()).isFalse();
        @SuppressWarnings("unchecked")
        Map<String, Object> invokeProperties = (Map<String, Object>)
                tools.get("capability.invoke").tool().outputSchema().get("properties");
        assertThat(invokeProperties).containsKey("approvalRequestId");
        @SuppressWarnings("unchecked")
        Map<String, Object> searchProperties = (Map<String, Object>) ((Map<String, Object>)
                tools.get("capability.search").tool().outputSchema().get("properties"))
                .get("items");
        @SuppressWarnings("unchecked")
        Map<String, Object> itemProperties = (Map<String, Object>) ((Map<String, Object>)
                searchProperties.get("items")).get("properties");
        assertThat(itemProperties).containsKeys("sourceType", "behavior", "operation");
    }
}
