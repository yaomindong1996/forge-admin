package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.system.dto.PluginArtifactRegisterDTO;
import com.mdframe.forge.plugin.system.service.plugin.PluginArtifactRegistrationService;
import com.mdframe.forge.plugin.system.service.plugin.PluginTaskQueryService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SysPluginArtifactSecurityTest {
    @Test
    void independent_platform_permission_crypto_no_saved_payload_and_typed_validation() throws Exception {
        var method = PluginArtifactController.class.getDeclaredMethod("register", String.class,
                PluginArtifactRegisterDTO.class);
        assertThat(method.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("system:plugin:artifact:register", "system:plugin:task:detail");
        assertThat(PluginArtifactController.class.getAnnotation(ApiDecrypt.class)).isNotNull();
        assertThat(PluginArtifactController.class.getAnnotation(ApiEncrypt.class)).isNotNull();
        assertThat(method.getAnnotation(OperationLog.class).saveRequestParams()).isFalse();
        assertThat(method.getAnnotation(OperationLog.class).saveResponseResult()).isFalse();
        assertThat(PluginArtifactRegisterDTO.class.getDeclaredFields()).extracting("name")
                .containsExactly("requestId", "metadataJson", "localVerified", "notDeployed", "note");
        var service = mock(PluginArtifactRegistrationService.class);
        var queries = mock(PluginTaskQueryService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new PluginArtifactController(service, queries)).build();
        mvc.perform(post("/system/plugin-task/task/artifact").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service, queries);
    }

    @Test
    void no_identity_or_non_platform_user_cannot_call_controller_even_with_wildcard() {
        var service = mock(PluginArtifactRegistrationService.class);
        var queries = mock(PluginTaskQueryService.class);
        var controller = new PluginArtifactController(service, queries);
        assertThatThrownBy(() -> controller.register("task", new PluginArtifactRegisterDTO()))
                .isInstanceOf(RuntimeException.class);
        for (int type : new int[]{1, 2}) {
            var user = new LoginUser();
            user.setUserId(9L);
            user.setTenantId(1L);
            user.setUserType(type);
            user.setPermissions(Set.of("**"));
            var identity = new ExecutionIdentity(user, "USER", 9L, null, 1L, "pc", "fixture", Set.of());
            try (var scope = ExecutionIdentityContextHolder.open(identity)) {
                assertThatThrownBy(() -> controller.register("task", new PluginArtifactRegisterDTO()))
                        .isInstanceOf(RuntimeException.class);
            }
        }
        verifyNoInteractions(service, queries);
    }
}
