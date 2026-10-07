package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.system.dto.SysPluginQuery;
import com.mdframe.forge.plugin.system.service.SysPluginService;
import com.mdframe.forge.plugin.system.vo.SysPluginPageVO;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SysPluginControllerTest {
    private final SysPluginService service = mock(SysPluginService.class);
    private final SysPluginController controller = new SysPluginController(service);

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void should_deny_non_platform_admin_even_with_wildcard_grant(int userType) {
        try (var scope = ExecutionIdentityContextHolder.open(identity(userType))) {
            assertThatThrownBy(() -> controller.page(new SysPluginQuery())).isInstanceOf(BusinessException.class)
                    .hasMessageContaining("平台超级管理员");
            assertThatThrownBy(() -> controller.detail("plugin-system")).isInstanceOf(BusinessException.class);
            verifyNoInteractions(service);
        }
    }

    @Test
    void should_bind_page_protocol_and_validate_boundary_for_platform_admin() throws Exception {
        when(service.page(any())).thenReturn(new SysPluginPageVO(List.of(), 0, 2, 15, "1.2.0", "community"));
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        try (var scope = ExecutionIdentityContextHolder.open(identity(0))) {
            mvc.perform(get("/system/plugin/page").param("pageNum", "2").param("pageSize", "15"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.current").value(2))
                    .andExpect(jsonPath("$.data.coreVersion").value("1.2.0"));
            mvc.perform(get("/system/plugin/page").param("pageSize", "101")).andExpect(status().isBadRequest());
            mvc.perform(get("/system/plugin/page").param("origin", "other")).andExpect(status().isBadRequest());
        }
    }

    @Test
    void should_keep_explicit_rbac_on_both_non_public_endpoints() throws Exception {
        assertThat(SysPluginController.class.getMethod("page", SysPluginQuery.class)
                .getAnnotation(SaCheckPermission.class).value()).containsExactly("system:plugin:list");
        assertThat(SysPluginController.class.getMethod("detail", String.class)
                .getAnnotation(SaCheckPermission.class).value()).containsExactly("system:plugin:detail");
    }

    private ExecutionIdentity identity(int type) {
        LoginUser user = new LoginUser();
        user.setUserId(900001L);
        user.setTenantId(1L);
        user.setUserType(type);
        user.setPermissions(Set.of("**", "system:plugin:list", "system:plugin:detail"));
        return new ExecutionIdentity(user, "USER", user.getUserId(), null, 1L, "pc", "fixture", Set.of());
    }
}
