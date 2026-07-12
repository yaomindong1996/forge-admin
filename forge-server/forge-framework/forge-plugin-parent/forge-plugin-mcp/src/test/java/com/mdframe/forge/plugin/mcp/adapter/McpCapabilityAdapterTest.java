package com.mdframe.forge.plugin.mcp.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.builtin.PingCapabilityExecutor;
import com.mdframe.forge.plugin.capability.builtin.PingCapabilitySource;
import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.capability.naming.CapabilityToolNameMapper;
import com.mdframe.forge.plugin.capability.registry.InMemoryCapabilityRegistry;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.spi.ScopeBasedCapabilityAuthorizationPolicy;
import com.mdframe.forge.plugin.mcp.security.McpTransportContextKeys;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpCapabilityAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldInvokePingWithCallerFromTransportContext() {
        McpCapabilityAdapter adapter = adapter();
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        CapabilityCallerContext caller = new CapabilityCallerContext(
                "test-client", 1L, null, null,
                Set.of("capability:discover", "capability:invoke"));
        when(exchange.transportContext()).thenReturn(McpTransportContext.create(
                Map.of(McpTransportContextKeys.CALLER_CONTEXT, caller)));

        McpSchema.CallToolResult result = adapter.call(exchange,
                new McpSchema.CallToolRequest("capability.ping", Map.of()));

        assertThat(result.isError()).isFalse();
        assertThat(result.structuredContent()).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) result.structuredContent()).get("status")).isEqualTo("ok");
        assertThat(result.meta()).containsKeys("requestId", "durationMs");
    }

    @Test
    void shouldFailClosedWithoutTrustedCallerContext() {
        McpCapabilityAdapter adapter = adapter();
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        when(exchange.transportContext()).thenReturn(McpTransportContext.EMPTY);

        McpSchema.CallToolResult result = adapter.call(exchange,
                new McpSchema.CallToolRequest("capability.ping", Map.of()));

        assertThat(result.isError()).isTrue();
        assertThat(result.meta()).containsEntry("errorCode", "UNAUTHENTICATED");
    }

    @Test
    void shouldRejectUnknownToolWithoutInvokingPing() {
        McpCapabilityAdapter adapter = adapter();
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);

        McpSchema.CallToolResult result = adapter.call(exchange,
                new McpSchema.CallToolRequest("capability.unknown", Map.of()));

        assertThat(result.isError()).isTrue();
        assertThat(result.meta()).containsEntry("errorCode", "CAPABILITY_NOT_FOUND");
    }

    private McpCapabilityAdapter adapter() {
        PingCapabilitySource source = new PingCapabilitySource(objectMapper);
        PingCapabilityExecutor executor = new PingCapabilityExecutor(objectMapper,
                Clock.fixed(Instant.parse("2026-07-11T12:00:00Z"), ZoneOffset.UTC));
        InMemoryCapabilityRegistry registry = new InMemoryCapabilityRegistry(
                Set.of(source), Set.of(executor), new ScopeBasedCapabilityAuthorizationPolicy(),
                new CapabilityToolNameMapper(), new CapabilitySchemaValidator());
        return new McpCapabilityAdapter(
                registry,
                objectMapper,
                new McpCapabilityResultMapper(objectMapper),
                new CapabilitySchemaValidator());
    }
}
