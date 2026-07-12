package com.mdframe.forge.plugin.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.mcp.security.ForgeMcpAuthenticationFilter;
import com.mdframe.forge.plugin.mcp.security.McpCallerContextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = McpStreamableProtocolIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.ai.mcp.server.enabled=true",
                "spring.ai.mcp.server.name=forge-ai-hub",
                "spring.ai.mcp.server.version=1.0.0",
                "spring.ai.mcp.server.type=SYNC",
                "spring.ai.mcp.server.stdio=false",
                "spring.ai.mcp.server.protocol=STREAMABLE",
                "spring.ai.mcp.server.capabilities.tool=true",
                "spring.ai.mcp.server.capabilities.resource=false",
                "spring.ai.mcp.server.capabilities.prompt=false",
                "spring.ai.mcp.server.capabilities.completion=false",
                "spring.ai.mcp.server.streamable-http.mcp-endpoint=/mcp"
        })
@AutoConfigureMockMvc
class McpStreamableProtocolIntegrationTest {

    private static final MediaType EVENT_STREAM = MediaType.TEXT_EVENT_STREAM;
    private static final String SESSION_HEADER = "Mcp-Session-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCompleteInitializeListAndCallOnSingleMcpEndpoint() throws Exception {
        MvcResult initialize = mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, EVENT_STREAM)
                        .content("""
                                {
                                  "jsonrpc":"2.0",
                                  "id":1,
                                  "method":"initialize",
                                  "params":{
                                    "protocolVersion":"2025-06-18",
                                    "capabilities":{},
                                    "clientInfo":{"name":"forge-test-client","version":"1.0.0"}
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists(SESSION_HEADER))
                .andExpect(jsonPath("$.result.protocolVersion").value("2025-06-18"))
                .andExpect(jsonPath("$.result.serverInfo.name").value("forge-ai-hub"))
                .andReturn();
        String sessionId = initialize.getResponse().getHeader(SESSION_HEADER);
        assertThat(sessionId).isNotBlank();

        mockMvc.perform(post("/mcp")
                        .header(SESSION_HEADER, sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, EVENT_STREAM)
                        .content("""
                                {"jsonrpc":"2.0","method":"notifications/initialized"}
                                """))
                .andExpect(status().isAccepted());

        MvcResult listPending = mockMvc.perform(post("/mcp")
                        .header(SESSION_HEADER, sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, EVENT_STREAM)
                        .content("""
                                {"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();
        MvcResult listResult = mockMvc.perform(asyncDispatch(listPending))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listPayload = readSsePayload(listResult);
        assertThat(listPayload.path("result").path("tools")).hasSize(1);
        assertThat(listPayload.path("result").path("tools").get(0).path("name").asText())
                .isEqualTo("capability.ping");
        assertThat(listPayload.path("result").has("nextCursor")).isFalse();

        MvcResult callPending = mockMvc.perform(post("/mcp")
                        .header(SESSION_HEADER, sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, EVENT_STREAM)
                        .content("""
                                {
                                  "jsonrpc":"2.0",
                                  "id":3,
                                  "method":"tools/call",
                                  "params":{"name":"capability.ping","arguments":{}}
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();
        MvcResult callResult = mockMvc.perform(asyncDispatch(callPending))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode callPayload = readSsePayload(callResult);
        assertThat(callPayload.path("result").path("isError").asBoolean()).isFalse();
        assertThat(callPayload.path("result").path("structuredContent").path("status").asText())
                .isEqualTo("ok");
        assertThat(callPayload.path("result").path("structuredContent").path("requestId").asText())
                .isEqualTo(callResult.getResponse().getHeader(ForgeMcpAuthenticationFilter.REQUEST_ID_HEADER));
    }

    @Test
    void shouldReturnHttpUnauthorizedBeforeMcpHandlerWhenIdentityIsMissing() throws Exception {
        mockMvc.perform(post("/mcp")
                        .header("X-Test-Mcp-Anonymous", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, EVENT_STREAM)
                        .content("""
                                {
                                  "jsonrpc":"2.0",
                                  "id":1,
                                  "method":"initialize",
                                  "params":{
                                    "protocolVersion":"2025-06-18",
                                    "capabilities":{},
                                    "clientInfo":{"name":"anonymous-client","version":"1.0.0"}
                                  }
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(ForgeMcpAuthenticationFilter.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    private JsonNode readSsePayload(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String data = body.lines()
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring("data:".length()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Streamable HTTP 响应缺少 data 事件"));
        return objectMapper.readTree(data);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        McpCallerContextResolver mcpCallerContextResolver() {
            return request -> "true".equals(request.getHeader("X-Test-Mcp-Anonymous"))
                    ? null
                    : new CapabilityCallerContext(
                            "integration-test-client",
                            1L,
                            null,
                            null,
                            Set.of("capability:discover", "capability:invoke"));
        }
    }
}
