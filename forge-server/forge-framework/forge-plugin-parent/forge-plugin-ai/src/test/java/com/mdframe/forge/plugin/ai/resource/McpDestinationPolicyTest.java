package com.mdframe.forge.plugin.ai.resource;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpDestinationPolicyTest {

    @Test
    void parseShouldRejectCredentialsQueryAndFragment() {
        assertThrows(BusinessException.class, () -> McpDestinationPolicy.parse("https://user:pass@mcp.example.com/sse"));
        assertThrows(BusinessException.class, () -> McpDestinationPolicy.parse("https://mcp.example.com/mcp?token=1"));
        assertThrows(BusinessException.class, () -> McpDestinationPolicy.parse("https://mcp.example.com/mcp#frag"));
        assertThrows(BusinessException.class, () -> McpDestinationPolicy.parse("ftp://mcp.example.com/mcp"));
    }

    @Test
    void originShouldNormalizeDefaultPortsAndCase() {
        assertEquals("https://mcp.example.com:443",
                McpDestinationPolicy.origin(URI.create("https://MCP.Example.com/v1/mcp")));
        assertEquals("http://10.0.0.5:8080",
                McpDestinationPolicy.origin(URI.create("http://10.0.0.5:8080/mcp")));
    }

    @Test
    void allowedShouldMatchConfiguredExactOriginsOnly() {
        McpDestinationPolicy policy = new McpDestinationPolicy("https://mcp.example.com,http://10.0.0.5:8080");
        assertTrue(policy.allowed("https://mcp.example.com/tools"));
        assertTrue(policy.allowed("http://10.0.0.5:8080/mcp"));
        assertFalse(policy.allowed("https://mcp.example.com:8443/tools"));
        assertFalse(policy.allowed("https://other.example.com/tools"));
        assertThrows(BusinessException.class, () -> policy.require("https://other.example.com/tools"));
    }

    @Test
    void requireShouldBlockLoopbackEvenIfWhitelisted() {
        McpDestinationPolicy policy = new McpDestinationPolicy("http://127.0.0.1:8080");
        assertTrue(policy.allowed("http://127.0.0.1:8080/mcp"));
        assertThrows(BusinessException.class, () -> policy.require("http://127.0.0.1:8080/mcp"));
    }
}
