package com.mdframe.forge.plugin.capability.identity.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;
import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityInvocationAuditEvent;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapability;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityInvocationAuditService;
import com.mdframe.forge.plugin.capability.config.CapabilityAutoConfiguration;
import com.mdframe.forge.plugin.capability.identity.audit.CapabilityInvocationAuditObserver;
import com.mdframe.forge.plugin.capability.identity.config.CapabilityIdentityProperties;
import com.mdframe.forge.plugin.capability.identity.security.AuthenticatedCapabilityIdentity;
import com.mdframe.forge.plugin.capability.identity.security.CapabilitySecurityPrincipal;
import com.mdframe.forge.plugin.capability.identity.token.CapabilityAccessTokenService;
import com.mdframe.forge.plugin.capability.model.CapabilityInvocation;
import com.mdframe.forge.plugin.capability.model.CapabilityResult;
import com.mdframe.forge.plugin.capability.spi.CapabilityInvocationObserver;
import com.mdframe.forge.plugin.mcp.config.ForgeMcpServerAutoConfiguration;
import com.mdframe.forge.starter.auth.config.SaTokenConfig;
import com.mdframe.forge.starter.auth.interceptor.ApiPermissionInterceptor;
import com.mdframe.forge.starter.core.context.AuthProperties;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import cn.dev33.satoken.spring.SaTokenContextRegister;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.ai.mcp.server.autoconfigure.McpServerStreamableHttpWebMvcAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerObjectMapperAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = McpDelegatedIdentityIntegrationTest.TestApplication.class,
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
                "spring.ai.mcp.server.streamable-http.mcp-endpoint=/mcp",
                "forge.capability.identity.enabled=true",
                "forge.capability.identity.issuer=http://localhost:8580",
                "forge.capability.identity.resource=http://localhost:8580/mcp"
        })
@AutoConfigureMockMvc
class McpDelegatedIdentityIntegrationTest {

    private static final MediaType EVENT_STREAM = MediaType.TEXT_EVENT_STREAM;
    private static final String SESSION_HEADER = "Mcp-Session-Id";
    private static final String RAW_TOKEN = "fdu_" + "a".repeat(22) + "_" + "b".repeat(43);
    private static SaTokenContext previousSaTokenContext;

    @BeforeAll
    static void installSaTokenServletContext() {
        previousSaTokenContext = SaManager.getSaTokenContext();
        SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
    }

    @AfterAll
    static void restoreSaTokenContext() {
        SaManager.setSaTokenContext(previousSaTokenContext);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CapabilityAccessTokenService tokenService;
    @Autowired
    private CapabilityInvocationAuditService auditService;
    @Autowired
    private IdentityProbeObserver identityProbeObserver;

    @BeforeEach
    void setUp() {
        reset(tokenService, auditService);
        identityProbeObserver.clear();
        when(tokenService.authenticate(eq(RAW_TOKEN), eq("http://localhost:8580/mcp"), any()))
                .thenReturn(authenticatedIdentity());
    }

    @AfterEach
    void cleanup() {
        assertContextCleared();
        ExecutionIdentityContextHolder.clear();
        TenantContextHolder.clear();
        MDC.clear();
    }

    @Test
    void shouldRunFduBearerThroughRealFilterLifecycleSaTokenExclusionAndAuditObserver() throws Exception {
        MvcResult initialize = perform(post("/mcp")
                .content("""
                        {"jsonrpc":"2.0","id":1,"method":"initialize","params":{
                          "protocolVersion":"2025-06-18","capabilities":{},
                          "clientInfo":{"name":"delegated-client","version":"1.0.0"}}}
                        """))
                .andExpect(status().isOk())
                .andExpect(header().exists(SESSION_HEADER))
                .andExpect(jsonPath("$.result.protocolVersion").value("2025-06-18"))
                .andReturn();
        String sessionId = initialize.getResponse().getHeader(SESSION_HEADER);
        assertThat(sessionId).isNotBlank();
        assertContextCleared();

        perform(post("/mcp").header(SESSION_HEADER, sessionId)
                .content("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"))
                .andExpect(status().isAccepted());
        assertContextCleared();

        MvcResult listPending = perform(post("/mcp").header(SESSION_HEADER, sessionId)
                .content("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        MvcResult listResult = mockMvc.perform(asyncDispatch(listPending))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readSsePayload(listResult).path("result").path("tools")).hasSize(1);
        assertContextCleared();

        MvcResult callPending = perform(post("/mcp").header(SESSION_HEADER, sessionId)
                .content("""
                        {"jsonrpc":"2.0","id":3,"method":"tools/call",
                         "params":{"name":"capability.ping","arguments":{}}}
                        """))
                .andExpect(request().asyncStarted())
                .andReturn();
        MvcResult callResult = mockMvc.perform(asyncDispatch(callPending))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readSsePayload(callResult).path("result").path("isError").asBoolean()).isFalse();

        ArgumentCaptor<CapabilityInvocationAuditEvent> event =
                ArgumentCaptor.forClass(CapabilityInvocationAuditEvent.class);
        verify(auditService).record(eq(1L), event.capture());
        assertThat(event.getValue().actorType()).isEqualTo(CapabilityActorType.USER);
        assertThat(event.getValue().actorUserId()).isEqualTo(101L);
        assertThat(event.getValue().serviceUserId()).isEqualTo(999L);
        assertThat(event.getValue().clientId()).isEqualTo(301L);
        assertThat(event.getValue().activeOrgId()).isEqualTo(201L);
        assertThat(identityProbeObserver.userId.get()).isEqualTo(101L);
        assertThat(identityProbeObserver.tenantId.get()).isEqualTo(1L);
        verify(tokenService, times(4)).authenticate(
                RAW_TOKEN, "http://localhost:8580/mcp", Set.of());
        assertContextCleared();
    }

    @Test
    void shouldReturn401AndClearContextWhenBearerIsMissing() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, EVENT_STREAM)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE,
                        "Bearer resource_metadata=\"http://localhost:8580/.well-known/oauth-protected-resource\""));
        assertContextCleared();
    }

    @Test
    void shouldDistinguishForbiddenOriginFromAuthenticationInfrastructureFailure() throws Exception {
        perform(post("/mcp")
                .header(HttpHeaders.ORIGIN, "https://evil.example")
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        when(tokenService.authenticate(eq(RAW_TOKEN), eq("http://localhost:8580/mcp"), any()))
                .thenThrow(new IllegalStateException("database unavailable"));
        perform(post("/mcp")
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_UNAVAILABLE"));
        assertContextCleared();
    }

    private org.springframework.test.web.servlet.ResultActions perform(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        return mockMvc.perform(request
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + RAW_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, EVENT_STREAM));
    }

    private JsonNode readSsePayload(MvcResult result) throws Exception {
        String data = result.getResponse().getContentAsString(StandardCharsets.UTF_8).lines()
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring("data:".length()))
                .findFirst()
                .orElseThrow();
        return objectMapper.readTree(data);
    }

    private void assertContextCleared() {
        assertThat(ExecutionIdentityContextHolder.current()).isEmpty();
        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(MDC.get("actorUserId")).isNull();
        assertThat(MDC.get("capabilityClientId")).isNull();
    }

    private AuthenticatedCapabilityIdentity authenticatedIdentity() {
        LoginUser user = new LoginUser();
        user.setUserId(101L);
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        user.setUserStatus(1);
        user.setPermissions(Set.of("ai:capability:invoke:capability.ping"));
        user.setRoleIds(List.of(501L));
        CapabilitySecurityPrincipal principal = new CapabilitySecurityPrincipal(
                301L, "desktop_agent", CapabilityActorType.USER, 101L, 999L,
                1L, 201L, 1, "token-key", "http://localhost:8580/mcp",
                Set.of("capability:discover:capability.ping", "capability:invoke:capability.ping"));
        return new AuthenticatedCapabilityIdentity(principal, user);
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            JacksonAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            WebMvcAutoConfiguration.class,
            TaskExecutionAutoConfiguration.class,
            CapabilityAutoConfiguration.class,
            McpServerObjectMapperAutoConfiguration.class,
            ForgeMcpServerAutoConfiguration.class,
            McpServerStreamableHttpWebMvcAutoConfiguration.class,
            McpServerAutoConfiguration.class
    })
    @EnableConfigurationProperties(CapabilityIdentityProperties.class)
    @Import({
            CapabilityMcpAccessTokenResolver.class,
            McpExecutionContextLifecycle.class,
            CapabilityInvocationAuditObserver.class,
            SaTokenConfig.class,
            SaTokenContextRegister.class
    })
    static class TestApplication {

        @Bean
        CapabilityAccessTokenService capabilityAccessTokenService() {
            return mock(CapabilityAccessTokenService.class);
        }

        @Bean
        CapabilityCatalogService capabilityCatalogService() {
            CapabilityCatalogService service = mock(CapabilityCatalogService.class);
            AiCapability capability = new AiCapability();
            capability.setId(1L);
            when(service.getByCode(1L, "capability.ping")).thenReturn(capability);
            return service;
        }

        @Bean
        CapabilityInvocationAuditService capabilityInvocationAuditService() {
            return mock(CapabilityInvocationAuditService.class);
        }

        @Bean
        ApiPermissionInterceptor apiPermissionInterceptor() {
            return mock(ApiPermissionInterceptor.class);
        }

        @Bean
        AuthProperties authProperties() {
            AuthProperties properties = new AuthProperties();
            properties.setEnableApiPermission(true);
            return properties;
        }

        @Bean
        IdentityProbeObserver identityProbeObserver() {
            return new IdentityProbeObserver();
        }
    }

    static final class IdentityProbeObserver implements CapabilityInvocationObserver {

        private final AtomicReference<Long> userId = new AtomicReference<>();
        private final AtomicReference<Long> tenantId = new AtomicReference<>();

        @Override
        public void onCompleted(
                CapabilityInvocation invocation,
                CapabilityResult result,
                String schemaPath) {
            userId.set(SessionHelper.getUserId());
            tenantId.set(TenantContextHolder.getTenantId());
        }

        void clear() {
            userId.set(null);
            tenantId.set(null);
        }
    }
}
