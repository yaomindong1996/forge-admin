package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.plugin.system.vo.SysUserTenantVO;
import com.mdframe.forge.starter.auth.constant.AuthResultCodes;
import com.mdframe.forge.starter.auth.domain.LoginTenantOption;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class UserLoadServiceImplPasswordLoginTest {

    @Test
    void rejectsUnknownUsernameWithoutLeakingTenantList() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        UserLoadServiceImpl service = new UserLoadServiceImpl(
                userMapper, null, null, null, null, null, null, null, null,
                null, null, null, null);
        when(userMapper.selectUsersByUsernameForLogin("alice")).thenReturn(List.of());

        assertThatThrownBy(() -> service.authenticateByUsernamePassword("alice", "secret", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    void requiresWorkspaceChoiceAfterPasswordMatchesMultipleTenants() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserTenantMapper userTenantMapper = mock(SysUserTenantMapper.class);
        UserLoadServiceImpl service = spy(new UserLoadServiceImpl(
                userMapper, null, null, null, null, userTenantMapper, null, null, null,
                null, null, null, null));

        SysUser user = new SysUser();
        user.setId(10L);
        user.setUsername("alice");
        user.setPassword("encoded");
        user.setUserType(SystemConstants.UserType.NORMAL_USER);
        when(userMapper.selectUsersByUsernameForLogin("alice")).thenReturn(List.of(user));
        doReturn(true).when(service).matchPassword("secret", "encoded");
        when(userTenantMapper.selectUserTenants(10L, true)).thenReturn(List.of(
                membership(1L, "默认租户"),
                membership(2L, "分公司")));

        assertThatThrownBy(() -> service.authenticateByUsernamePassword("alice", "secret", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertThat(exception.getCode()).isEqualTo(AuthResultCodes.TENANT_SELECTION_REQUIRED);
                    assertThat(exception.getMessage()).isEqualTo("请选择要进入的工作区");
                    List<?> options = (List<?>) exception.getData();
                    assertThat(options).extracting("tenantId").containsExactly(1L, 2L);
                    assertThat(options).hasOnlyElementsOfType(LoginTenantOption.class);
                });
    }

    private SysUserTenantVO membership(Long tenantId, String tenantName) {
        SysUserTenantVO vo = new SysUserTenantVO();
        vo.setTenantId(tenantId);
        vo.setTenantName(tenantName);
        return vo;
    }
}
