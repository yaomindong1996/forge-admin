package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.config.*;
import com.mdframe.forge.plugin.system.dto.PluginDeliveryCreateDTO;
import com.mdframe.forge.plugin.system.service.plugin.PluginDeliveryService;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.util.CryptoDeploymentSecretPolicy;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SysPluginDeliverySecurityTest {
    private final String token = "7".repeat(64);
    private PluginDeliveryProperties config() {
        var config = new PluginDeliveryProperties(); config.setEnabled(true);
        config.getWorker().setId("delivery"); config.getWorker().setTenantId(1L);
        config.getWorker().setTokenSha256(PackageDigests.sha256(token.getBytes(StandardCharsets.US_ASCII)));
        config.getWorker().setExpiresAt(Instant.now().plusSeconds(60));
        var target = new PluginDeliveryProperties.Target();
        target.setId("test"); target.setName("测试目标"); target.setRepositoryId("local-test");
        config.setTargets(List.of(target)); return config;
    }
    private MockHttpServletRequest request() {
        var req = new MockHttpServletRequest("POST", "/internal/plugin-delivery/"
                + "00000000-0000-4000-8000-000000000001/claim");
        req.setSecure(true); req.setContentType("application/json"); req.setContent("{}".getBytes());
        req.addHeader("Authorization", "Bearer " + token); return req;
    }
    @Test void dedicated_identity_no_query_no_inner_no_build_token_and_expiry() throws Exception {
        var config = config(); var filter = new PluginDeliveryFilter(config, new ObjectMapper());
        var req = request(); var passed = new AtomicBoolean();
        filter.doFilter(req, new MockHttpServletResponse(), (a, b) -> passed.set(true));
        assertThat(passed).isTrue();
        assertThat(PluginDeliveryFilter.identity(req).tenantId()).isEqualTo(1L);
        for (String kind : new String[]{"query", "inner", "http", "token", "duplicate", "large", "path"}) {
            var invalid = request();
            switch (kind) {
                case "query" -> invalid.setQueryString("tenantId=2");
                case "inner" -> invalid.addHeader("X-Inner-Call", "true");
                case "http" -> invalid.setSecure(false);
                case "token" -> { invalid.removeHeader("Authorization"); invalid.addHeader("Authorization", "Bearer "
                        + "8".repeat(64)); }
                case "duplicate" -> invalid.addHeader("Authorization", "Bearer " + token);
                case "large" -> invalid.setContent(new byte[65537]);
                case "path" -> invalid.setRequestURI("/internal/plugin-delivery/arbitrary");
                default -> throw new AssertionError();
            }
            var res = new MockHttpServletResponse(); passed.set(false);
            filter.doFilter(invalid, res, (a, b) -> passed.set(true));
            assertThat(passed).isFalse(); assertThat(res.getStatus()).isEqualTo(401);
            assertThat(res.getContentAsString()).doesNotContain(token);
        }
        config.getWorker().setExpiresAt(Instant.now().minusSeconds(1));
        var res = new MockHttpServletResponse();
        filter.doFilter(request(), res, (a, b) -> fail("expired credentials"));
        assertThat(res.getStatus()).isEqualTo(401);
    }
    @Test void probe_local_http_requires_explicit_option_and_nonce() throws Exception {
        var config = new PluginRuntimeProbeProperties(); config.setEnabled(true);
        config.setTokenSha256(PackageDigests.sha256(token.getBytes(StandardCharsets.US_ASCII)));
        config.setExpiresAt(Instant.now().plusSeconds(60));
        var filter = new PluginRuntimeProbeFilter(config, new ObjectMapper());
        var req = new MockHttpServletRequest("GET", "/internal/plugin-runtime/probe");
        req.setRemoteAddr("127.0.0.1"); req.addHeader("Authorization", "Bearer " + token);
        req.addHeader("X-Forge-Probe-Nonce", "00000000-0000-4000-8000-000000000001");
        var denied = new MockHttpServletResponse();
        filter.doFilter(req, denied, (a, b) -> fail("implicit local permission"));
        assertThat(denied.getStatus()).isEqualTo(401);
        config.setAllowLoopback(true);
        var passed = new AtomicBoolean();
        filter.doFilter(req, new MockHttpServletResponse(), (a, b) -> passed.set(true));
        assertThat(passed).isTrue();
        req.removeHeader("X-Forge-Probe-Nonce");
        filter.doFilter(req, new MockHttpServletResponse(), (a, b) -> fail("missing nonce"));
    }
    @Test void platform_actions_have_separate_permissions_and_deployment_keys_cannot_be_database_overridden()
            throws Exception {
        var add = PluginDeliveryController.class.getMethod("add", PluginDeliveryCreateDTO.class);
        assertThat(add.getAnnotation(SaCheckPermission.class).value()).containsExactly("system:plugin:delivery:execute");
        assertThat(add.getAnnotation(OperationLog.class).saveRequestParams()).isFalse();
        var service = mock(PluginDeliveryService.class);
        assertThatThrownBy(() -> new PluginDeliveryController(service).add(new PluginDeliveryCreateDTO()))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(service);
        for (String property : new String[]{"forge.plugin-delivery.worker.token-sha256",
                "forge.plugin-delivery.targets[0].repository-id", "forge.plugin-runtime-probe.allow-loopback"}) {
            assertThat(CryptoDeploymentSecretPolicy.isDeploymentSecretPropertyKey(property)).as(property).isTrue();
        }
    }
}
