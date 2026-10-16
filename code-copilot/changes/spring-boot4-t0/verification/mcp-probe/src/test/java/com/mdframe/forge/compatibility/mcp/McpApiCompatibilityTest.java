package com.mdframe.forge.compatibility.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.mcp.config.ForgeMcpProtocolGuard;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/** 对照 Forge 原始 transport 构造签名；未开放真实 MCP endpoint。 */
class McpApiCompatibilityTest {

    @Test
    void acceptsOnlyTrustedSynchronousStreamableProtocol() {
        McpServerProperties properties = new McpServerProperties();
        properties.setProtocol(McpServerProperties.ServerProtocol.STREAMABLE);
        properties.setType(McpServerProperties.ApiType.SYNC);
        properties.setStdio(false);
        assertDoesNotThrow(() -> new ForgeMcpProtocolGuard(properties));
        properties.setStdio(true);
        assertThrows(IllegalStateException.class, () -> new ForgeMcpProtocolGuard(properties));
    }

    @Test
    void constructsTransportWithTheCurrentJackson2Api() {
        WebMvcStreamableServerTransportProvider provider = WebMvcStreamableServerTransportProvider.builder()
                .jsonMapper(new JacksonMcpJsonMapper(new ObjectMapper()))
                .mcpEndpoint("/t0-mcp")
                .keepAliveInterval(Duration.ofSeconds(30))
                .disallowDelete(false)
                .build();
        assertNotNull(provider.getRouterFunction());
        provider.closeGracefully().block(Duration.ofSeconds(3));
    }
}
