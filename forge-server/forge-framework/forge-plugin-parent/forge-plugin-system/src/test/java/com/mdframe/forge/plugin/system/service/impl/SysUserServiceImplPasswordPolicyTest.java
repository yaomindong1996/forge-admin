package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.auth.PasswordPolicySupport;
import com.mdframe.forge.plugin.system.auth.UserPasswordCredential;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.dto.SysUserDTO;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserCredentialMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import com.mdframe.forge.starter.config.service.ConfigManagerService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysUserServiceImplPasswordPolicyTest {

    private static final Long TENANT_ID = 1L;
    private static final Long TARGET_USER_ID = 5L;

    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysTenantMapper tenantMapper = mock(SysTenantMapper.class);
    private final SysUserCredentialMapper credentialMapper = mock(SysUserCredentialMapper.class);
    // getSecurityConfig 返回 null 时使用默认策略：8 位 + 大小写 + 数字，历史 5 个
    private final SysUserServiceImpl service = new SysUserServiceImpl(
            userMapper, null, null, null, null, null, tenantMapper, null, null, null, null, null, null,
            new PasswordPolicySupport(mock(ConfigManagerService.class), credentialMapper));

    @Test
    void insertUserRejectsPasswordThatViolatesPolicy() {
        when(tenantMapper.selectCount(any())).thenReturn(1L);
        SysUserDTO dto = new SysUserDTO();
        dto.setUsername("alice");
        dto.setPassword("123456");

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(systemAdmin());

            assertThatThrownBy(() -> service.insertUser(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("密码长度必须在8到72位之间");
        }
        verify(userMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void insertUserRejectsMissingPassword() {
        when(tenantMapper.selectCount(any())).thenReturn(1L);
        SysUserDTO dto = new SysUserDTO();
        dto.setUsername("alice");

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(systemAdmin());

            assertThatThrownBy(() -> service.insertUser(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("密码不能为空");
        }
        verify(userMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void adminResetRejectsPasswordThatViolatesPolicy() {
        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(systemAdmin());

            assertThatThrownBy(() -> service.resetPassword(TARGET_USER_ID, "abcdefgh"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("密码必须包含大写字母");
        }
        verify(credentialMapper, never()).updateCredential(anyLong(), anyString(), anyBoolean(), any());
    }

    @Test
    void adminResetWritesThroughCredentialPolicyWithoutRevealingReuse() {
        // 管理员设的新密码恰好等于用户当前密码：不能报「与当前密码相同」，否则可用来验证他人密码
        String currentHash = PasswordUtil.encrypt("Current123");
        UserPasswordCredential credential = new UserPasswordCredential();
        credential.setUserId(TARGET_USER_ID);
        credential.setTenantId(TENANT_ID);
        credential.setPassword(currentHash);
        when(credentialMapper.selectCredential(TARGET_USER_ID)).thenReturn(credential);
        when(credentialMapper.updateCredential(eq(TARGET_USER_ID), anyString(), eq(false), any())).thenReturn(1);

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(systemAdmin());

            assertThat(service.resetPassword(TARGET_USER_ID, "Current123")).isTrue();
        }

        ArgumentCaptor<String> storedHash = ArgumentCaptor.forClass(String.class);
        verify(credentialMapper).updateCredential(eq(TARGET_USER_ID), storedHash.capture(), eq(false), any());
        assertThat(PasswordUtil.matches("Current123", storedHash.getValue())).isTrue();
        verify(credentialMapper).insertPasswordHistory(eq(TENANT_ID), eq(TARGET_USER_ID), eq(currentHash), any());
        verify(credentialMapper, never()).selectRecentPasswordHashes(anyLong(), anyLong(), anyInt());
    }

    @Test
    void adminResetReportsFailureWhenUserNoLongerExists() {
        when(credentialMapper.selectCredential(TARGET_USER_ID)).thenReturn(null);

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(systemAdmin());

            assertThat(service.resetPassword(TARGET_USER_ID, "FreshPass123")).isFalse();
        }
        verify(credentialMapper, never()).updateCredential(anyLong(), anyString(), anyBoolean(), any());
    }

    private static LoginUser systemAdmin() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setTenantId(TENANT_ID);
        loginUser.setUserType(SystemConstants.UserType.SYSTEM_ADMIN);
        return loginUser;
    }
}
