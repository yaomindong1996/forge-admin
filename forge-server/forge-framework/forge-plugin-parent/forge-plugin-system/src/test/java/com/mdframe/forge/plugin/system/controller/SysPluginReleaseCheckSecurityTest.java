package com.mdframe.forge.plugin.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.config.PluginWorkerFilter;
import com.mdframe.forge.plugin.system.config.PluginWorkerProperties;
import com.mdframe.forge.plugin.system.dto.PluginReleaseCheckDTO;
import com.mdframe.forge.plugin.system.service.plugin.PluginReleaseCheckService;
import com.mdframe.forge.plugin.system.vo.PluginReleaseCheckVO;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class SysPluginReleaseCheckSecurityTest {
    private final String id = "00000000-0000-4000-8000-000000000000";
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void default_off_and_direct_controller_cannot_bypass_machine_identity() throws Exception {
        var service = mock(PluginReleaseCheckService.class);
        var controller = new PluginReleaseCheckController(service);
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new PluginWorkerFilter(new PluginWorkerProperties(), json)).build();
        mvc.perform(post("/internal/plugin-build/" + id + "/approval-check").secure(true)
                .contentType("application/json").content("{}")).andExpect(status().isServiceUnavailable());
        assertThatThrownBy(() -> controller.check(id, null, new MockHttpServletRequest()))
                .hasMessageContaining("未认证");
        verifyNoInteractions(service);
    }

    @Test
    void strict_dto_secure_original_machine_tenant_no_user_or_inner_call() throws Exception {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        var properties = new PluginWorkerProperties();
        properties.setEnabled(true);
        properties.setId("worker");
        properties.setTenantId(1L);
        properties.setExpiresAt(Instant.now().plusSeconds(60));
        properties.setTokenSha256(PackageDigests.sha256(token.getBytes(StandardCharsets.US_ASCII)));
        var service = mock(PluginReleaseCheckService.class);
        when(service.check(eq(id), any(PluginReleaseCheckDTO.class), any())).thenAnswer(invocation -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(1L);
            assertThat(TenantContextHolder.isIgnore()).isFalse();
            PluginReleaseCheckDTO command = invocation.getArgument(1);
            return new PluginReleaseCheckVO(1, command.getCheckId(), id, 3, command.getReviewId(), "worker",
                    command.getServerResultSha256(), command.getManifestSha256(),
                    Instant.now().toString(), true, false);
        });
        var mvc = MockMvcBuilders.standaloneSetup(new PluginReleaseCheckController(service))
                .addFilters(new PluginWorkerFilter(properties, json)).build();
        String path = "/internal/plugin-build/" + id + "/approval-check";
        mvc.perform(post(path).secure(true).header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(path).secure(true).header("Authorization", "Bearer user-session-token")
                .contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(path).secure(true).header("Authorization", "Bearer " + token)
                .header("X-Inner-Call", "true").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(path).secure(false).header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
        String body = body();
        mvc.perform(post(path).secure(true).header("Authorization", "Bearer " + token)
                .header("X-Tenant-Id", "999").contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.liveTaskApprovalVerified").value(true))
                .andExpect(jsonPath("$.data.deployed").value(false));
        assertThat(TenantContextHolder.getTenantId()).isNull();
        mvc.perform(post(path).secure(true).header("Authorization", "Bearer " + token)
                .contentType("application/json").content(body.replace("\"artifactCount\":1", "\"artifactCount\":0")))
                .andExpect(status().isBadRequest());
    }

    private String body() {
        return "{\"checkId\":\"" + id + "\",\"reviewId\":\"" + id + "\",\"revision\":3,"
                + "\"serverResultSha256\":\"" + "a".repeat(64) + "\",\"manifestSha256\":\"" + "b".repeat(64)
                + "\",\"pluginId\":\"demo\",\"pluginVersion\":\"1.0.0\",\"coreVersion\":\"1.2.0\","
                + "\"operation\":\"install\",\"result\":{\"success\":true,\"jobId\":\"job-test\","
                + "\"packageSha256\":\"" + "a".repeat(64) + "\",\"sourceCommit\":\"" + "c".repeat(40)
                + "\",\"image\":\"builder@sha256:" + "d".repeat(64) + "\",\"phase\":\"artifact_verification\","
                + "\"sourceSha256\":\"" + "e".repeat(64) + "\",\"artifactCount\":1,\"artifactBytes\":100,"
                + "\"artifactManifestSha256\":\"" + "f".repeat(64) + "\"}}";
    }
}
