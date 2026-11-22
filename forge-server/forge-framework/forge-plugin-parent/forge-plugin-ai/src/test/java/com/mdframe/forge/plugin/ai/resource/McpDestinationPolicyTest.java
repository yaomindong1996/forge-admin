package com.mdframe.forge.plugin.ai.resource;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.domain.entity.SysOutboundWhitelist;
import com.mdframe.forge.starter.outbound.mapper.SysOutboundWhitelistMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    void defaultModeAllowsUnlistedPublicHttpsButNotHttp() {
        McpDestinationPolicy policy = new McpDestinationPolicy("", true, null);
        assertTrue(policy.allowed("https://other.example.com/tools"));
        assertFalse(policy.allowed("http://other.example.com/tools"));
        assertEquals("example.com", policy.require("https://example.com/mcp").getHost());
        BusinessException httpBlocked = assertThrows(BusinessException.class,
                () -> policy.require("http://example.com/mcp"));
        assertTrue(httpBlocked.getMessage().contains("白名单") || httpBlocked.getMessage().contains("HTTPS"));
    }

    @Test
    void strictAllowlistRejectsUnlistedHttps() {
        McpDestinationPolicy policy = new McpDestinationPolicy("https://mcp.example.com", false, null);
        assertTrue(policy.allowed("https://mcp.example.com/tools"));
        assertFalse(policy.allowed("https://other.example.com/tools"));
        assertThrows(BusinessException.class, () -> policy.require("https://other.example.com/tools"));
    }

    @Test
    void requireShouldBlockLoopbackEvenIfWhitelisted() {
        McpDestinationPolicy policy = new McpDestinationPolicy("http://127.0.0.1:8080", true, null);
        assertTrue(policy.allowed("http://127.0.0.1:8080/mcp"));
        assertThrows(BusinessException.class, () -> policy.require("http://127.0.0.1:8080/mcp"));
    }

    @Test
    void requireShouldAllowWhitelistedPublicHttp() {
        McpDestinationPolicy policy = new McpDestinationPolicy("http://example.com", true, null);
        assertTrue(policy.allowed("http://example.com/mcp"));
        assertEquals("example.com", policy.require("http://example.com/mcp").getHost());
    }

    @Test
    void dbWhitelistShouldAllowPublicHttpWithoutEnvOrigins() {
        SysOutboundWhitelistMapper mapper = mock(SysOutboundWhitelistMapper.class);
        when(mapper.selectActiveRules(anyLong(), eq(OutboundScenes.AI_MCP), eq("http"), eq("example.com"), anyInt()))
                .thenReturn(List.of(new SysOutboundWhitelist()));
        when(mapper.selectActiveRules(anyLong(), eq(OutboundScenes.AI_MCP), eq("http"), eq("other.example.com"), anyInt()))
                .thenReturn(List.of());

        McpDestinationPolicy policy = new McpDestinationPolicy("", true, mapper);
        assertTrue(policy.allowed("http://example.com/mcp"));
        assertFalse(policy.allowed("http://other.example.com/mcp"));
        assertEquals("example.com", policy.require("http://example.com/mcp").getHost());
    }
}
