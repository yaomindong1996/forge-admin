package com.mdframe.forge.plugin.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.config.PluginWorkerConfiguration;
import com.mdframe.forge.plugin.system.config.PluginWorkerFilter;
import com.mdframe.forge.plugin.system.config.PluginWorkerIdentity;
import com.mdframe.forge.plugin.system.config.PluginWorkerProperties;
import com.mdframe.forge.plugin.system.dto.PluginBuildLeaseDTO;
import com.mdframe.forge.plugin.system.service.plugin.PluginBuildService;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

class SysPluginWorkerSecurityTest {
    private static final String ID = "00000000-0000-4000-8000-000000000000";
    private final String token = randomToken();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void always_registers_filter_even_disabled_and_checks_identity_before_service() throws Exception {
        var properties = new PluginWorkerProperties();
        var registration = new PluginWorkerConfiguration().pluginWorkerFilter(properties, json);
        assertThat(registration.getUrlPatterns()).containsExactly("/internal/plugin-build/*");
        var service = mock(PluginBuildService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new PluginBuildWorkerController(service))
                .addFilters(registration.getFilter()).build();
        for (String action : new String[]{"claim", "archive", "heartbeat", "finish"}) {
            mvc.perform(post("/internal/plugin-build/" + ID + "/" + action).secure(true)
                    .contentType("application/json").content("{}")).andExpect(status().isServiceUnavailable());
        }
        verifyNoInteractions(service);
        var controller = new PluginBuildWorkerController(service);
        assertThatThrownBy(() -> controller.archive(ID, null, new MockHttpServletRequest()))
                .hasMessageContaining("未认证");
    }

    @Test
    void rejects_expired_or_invalid_credentials_http_inner_call_and_unbounded_requests() throws Exception {
        var properties = properties();
        var request = request(token);
        request.setSecure(false);
        assertDenied(properties, request, 401);
        request = request(token);
        request.addHeader("X-Inner-Call", "true");
        assertDenied(properties, request, 401);
        assertDenied(properties, request(randomToken()), 401);
        assertDenied(properties, request("user-session-token"), 401);
        request = request(token);
        request.setContent(new byte[65537]);
        assertDenied(properties, request, 401);
        request = request(token);
        request.setQueryString("tenantId=2");
        assertDenied(properties, request, 401);
        properties.setExpiresAt(Instant.now().minusSeconds(1));
        assertDenied(properties, request(token), 503);
        properties.setExpiresAt(Instant.now().plusSeconds(60));
        properties.setTokenSha256("invalid");
        assertDenied(properties, request(token), 503);
    }

    @Test
    void authenticated_identity_comes_from_operator_config_and_restores_tenant_scope() throws Exception {
        var properties = properties();
        var request = request(token);
        request.addHeader("X-Tenant-Id", "999");
        var response = new MockHttpServletResponse();
        new PluginWorkerFilter(properties, json).doFilter(request, response, (input, output) -> {
            var worker = PluginWorkerIdentity.required(request);
            assertThat(worker.tenantId()).isEqualTo(1L);
            assertThat(worker.workerId()).isEqualTo("test-worker");
            TenantContextHolder.setTenantId(9L);
            TenantContextHolder.setIgnore(true);
            assertThat(worker.inTenant(() -> {
                assertThat(TenantContextHolder.isIgnore()).isFalse();
                return TenantContextHolder.getTenantId();
            })).isEqualTo(1L);
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(9L);
            assertThat(TenantContextHolder.isIgnore()).isTrue();
            assertThatThrownBy(() -> worker.inTenant(() -> { throw new IllegalStateException("fixture"); }))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(9L);
            TenantContextHolder.clear();
        });
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    }

    @Test
    void mvc_machine_archive_is_validated_and_tenant_scoped_even_with_user_login_interceptor() throws Exception {
        var service = mock(PluginBuildService.class);
        var lease = randomToken();
        when(service.archive(eq(ID), any(PluginBuildLeaseDTO.class), any())).thenAnswer(invocation -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(1L);
            assertThat(TenantContextHolder.isIgnore()).isFalse();
            PluginBuildLeaseDTO command = invocation.getArgument(1);
            assertThat(command.getLeaseToken()).isEqualTo(lease);
            return new byte[]{1, 2, 3};
        });
        var login = new cn.dev33.satoken.interceptor.SaInterceptor(handler -> {
            throw new IllegalStateException("用户登录链不能作为机器认证");
        });
        var mvc = MockMvcBuilders.standaloneSetup(new PluginBuildWorkerController(service))
                .addInterceptors(login).addFilters(new PluginWorkerFilter(properties(), json)).build();
        String body = json.createObjectNode().put("leaseToken", lease).toString();
        mvc.perform(post("/internal/plugin-build/" + ID + "/archive").secure(true)
                .header("Authorization", "Bearer " + token).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(content().bytes(new byte[]{1, 2, 3}));
        assertThat(TenantContextHolder.getTenantId()).isNull();
        mvc.perform(post("/internal/plugin-build/" + ID + "/archive").secure(true)
                .header("Authorization", "Bearer " + token).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    private void assertDenied(PluginWorkerProperties properties, MockHttpServletRequest request, int code)
            throws Exception {
        var response = new MockHttpServletResponse();
        var called = new AtomicBoolean();
        new PluginWorkerFilter(properties, json).doFilter(request, response, (input, output) -> called.set(true));
        assertThat(called).isFalse();
        assertThat(response.getStatus()).isEqualTo(code);
        assertThat(response.getContentAsString()).doesNotContain(token, properties.getTokenSha256());
    }

    private MockHttpServletRequest request(String credential) {
        var request = new MockHttpServletRequest("POST", "/internal/plugin-build/" + ID + "/archive");
        request.setSecure(true);
        request.addHeader("Authorization", "Bearer " + credential);
        request.setContentType("application/json");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private PluginWorkerProperties properties() {
        var properties = new PluginWorkerProperties();
        properties.setEnabled(true);
        properties.setId("test-worker");
        properties.setTenantId(1L);
        properties.setTokenSha256(PackageDigests.sha256(token.getBytes(StandardCharsets.US_ASCII)));
        properties.setExpiresAt(Instant.now().plusSeconds(60));
        return properties;
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
