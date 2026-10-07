package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.system.dto.SysPluginTaskCommandDTO;
import com.mdframe.forge.plugin.system.dto.SysPluginTaskQuery;
import com.mdframe.forge.plugin.system.dto.SysPluginUploadDTO;
import com.mdframe.forge.plugin.system.service.plugin.PluginTaskCommandService;
import com.mdframe.forge.plugin.system.service.plugin.PluginTaskQueryService;
import com.mdframe.forge.plugin.system.service.plugin.PluginTaskUploadService;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class SysPluginTaskControllerTest {
    private final PluginTaskUploadService uploads = mock(PluginTaskUploadService.class);
    private final PluginTaskQueryService queries = mock(PluginTaskQueryService.class);
    private final PluginTaskCommandService commands = mock(PluginTaskCommandService.class);
    private final SysPluginTaskController controller = new SysPluginTaskController(uploads, queries, commands);

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void denies_every_action_to_non_platform_even_with_wildcard(int type) {
        LoginUser user = new LoginUser();
        user.setUserId(9L);
        user.setTenantId(1L);
        user.setUserType(type);
        user.setPermissions(Set.of("**"));
        var identity = new ExecutionIdentity(user, "USER", 9L, null, 1L, "pc", "fixture", Set.of());
        try (var scope = ExecutionIdentityContextHolder.open(identity)) {
            assertThatThrownBy(() -> controller.upload(new SysPluginUploadDTO())).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> controller.page(new SysPluginTaskQuery())).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> controller.detail("id")).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> controller.confirm("id", new SysPluginTaskCommandDTO()))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> controller.cancel("id", new SysPluginTaskCommandDTO()))
                    .isInstanceOf(BusinessException.class);
            verifyNoInteractions(uploads, queries, commands);
        }
    }

    @Test
    void keeps_explicit_permissions_and_typed_protocol_without_identity_or_commands() throws Exception {
        for (var method : SysPluginTaskController.class.getDeclaredMethods()) {
            assertThat(method.getAnnotation(SaCheckPermission.class)).isNotNull();
            var audit = method.getAnnotation(OperationLog.class);
            if (Set.of("upload", "confirm", "cancel").contains(method.getName())) {
                assertThat(audit).isNotNull();
                assertThat(audit.saveRequestParams()).isFalse();
                assertThat(audit.saveResponseResult()).isFalse();
            }
        }
        assertThat(SysPluginTaskCommandDTO.class.getDeclaredFields()).extracting("name")
                .containsExactly("revision", "sha256");
        assertThat(SysPluginUploadDTO.class.getDeclaredFields()).extracting("name")
                .containsExactly("file", "requestId");
    }
}
