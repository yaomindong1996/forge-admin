package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.auth.PasswordPolicySupport;
import com.mdframe.forge.plugin.system.auth.UserPasswordCredential;
import com.mdframe.forge.plugin.system.mapper.SysUserCredentialMapper;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import com.mdframe.forge.starter.config.service.ConfigManagerService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SystemAuthServiceImplChangePasswordTest {

    private static final Long USER_ID = 1L;
    private static final Long TENANT_ID = 1L;

    private final IUserLoadService userLoadService = mock(IUserLoadService.class);
    private final SysUserCredentialMapper credentialMapper = mock(SysUserCredentialMapper.class);
    // getSecurityConfig 返回 null 时使用默认策略：8 位 + 大小写 + 数字，历史 5 个
    private final PasswordPolicySupport passwordPolicySupport =
            new PasswordPolicySupport(mock(ConfigManagerService.class), credentialMapper);

    @Test
    void shouldRejectWrongOldPasswordWithBusinessCode() {
        when(userLoadService.getUserPassword(USER_ID)).thenReturn("encoded");
        when(userLoadService.matchPassword("wrong", "encoded")).thenReturn(false);
        SystemAuthServiceImpl service = service();

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(loginUser());
            assertThatThrownBy(() -> service.changePassword("wrong", "NewPass12!"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException business = (BusinessException) ex;
                        assertThat(business.getCode()).isEqualTo(400);
                        assertThat(business.getMessage()).isEqualTo("旧密码错误");
                    });
        }
        verifyNoInteractions(credentialMapper);
    }

    @Test
    void shouldRejectUnauthenticatedChangeWithBusinessCode() {
        SystemAuthServiceImpl service = service();

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(null);
            assertThatThrownBy(() -> service.changePassword("old", "NewPass12!"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException business = (BusinessException) ex;
                        assertThat(business.getCode()).isEqualTo(401);
                        assertThat(business.getMessage()).isEqualTo("未登录");
                    });
        }
    }

    @Test
    void shouldRejectNewPasswordThatViolatesComplexityPolicy() {
        oldPasswordMatches();
        SystemAuthServiceImpl service = service();

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(loginUser());
            assertThatThrownBy(() -> service.changePassword("old", "123456"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("密码长度必须在8到72位之间");
            assertThatThrownBy(() -> service.changePassword("old", "alllowercase1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("密码必须包含大写字母");
        }
        verifyNoInteractions(credentialMapper);
    }

    @Test
    void shouldRejectNewPasswordEqualToCurrentOrRecentPasswords() {
        oldPasswordMatches();
        when(credentialMapper.selectCredential(USER_ID)).thenReturn(credential(PasswordUtil.encrypt("Current123")));
        when(credentialMapper.selectRecentPasswordHashes(TENANT_ID, USER_ID, 5))
                .thenReturn(List.of(PasswordUtil.encrypt("Previous123")));
        SystemAuthServiceImpl service = service();

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(loginUser());
            assertThatThrownBy(() -> service.changePassword("old", "Current123"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("新密码不能与当前密码相同");
            assertThatThrownBy(() -> service.changePassword("old", "Previous123"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("新密码不能与最近5次使用过的密码相同");
        }
        verify(credentialMapper, never()).updateCredential(anyLong(), anyString(), anyBoolean(), any());
    }

    @Test
    void shouldWriteThroughCredentialPolicyAndClearForcedChangeFlags() {
        oldPasswordMatches();
        String currentHash = PasswordUtil.encrypt("Current123");
        when(credentialMapper.selectCredential(USER_ID)).thenReturn(credential(currentHash));
        when(credentialMapper.updateCredential(eq(USER_ID), anyString(), eq(false), any())).thenReturn(1);
        SystemAuthServiceImpl service = service();
        LoginUser loginUser = loginUser();
        loginUser.setForcePasswordChange(true);
        loginUser.setPasswordExpired(true);

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(loginUser);

            assertThat(service.changePassword("old", "FreshPass123")).isTrue();

            session.verify(() -> SessionHelper.setLoginUser(loginUser));
        }
        assertThat(loginUser.getForcePasswordChange()).isFalse();
        assertThat(loginUser.getPasswordExpired()).isFalse();

        ArgumentCaptor<String> storedHash = ArgumentCaptor.forClass(String.class);
        verify(credentialMapper).updateCredential(eq(USER_ID), storedHash.capture(), eq(false), any());
        assertThat(PasswordUtil.matches("FreshPass123", storedHash.getValue())).isTrue();
        verify(credentialMapper).insertPasswordHistory(eq(TENANT_ID), eq(USER_ID), eq(currentHash), any());
    }

    @Test
    void shouldKeepForcedChangeFlagsWhenCredentialWriteFails() {
        oldPasswordMatches();
        when(credentialMapper.selectCredential(USER_ID)).thenReturn(credential(PasswordUtil.encrypt("Current123")));
        when(credentialMapper.updateCredential(eq(USER_ID), anyString(), eq(false), any())).thenReturn(0);
        SystemAuthServiceImpl service = service();
        LoginUser loginUser = loginUser();
        loginUser.setForcePasswordChange(true);
        loginUser.setPasswordExpired(true);

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(loginUser);

            assertThat(service.changePassword("old", "FreshPass123")).isFalse();
        }
        assertThat(loginUser.getForcePasswordChange()).isTrue();
        assertThat(loginUser.getPasswordExpired()).isTrue();
    }

    private void oldPasswordMatches() {
        when(userLoadService.getUserPassword(USER_ID)).thenReturn("encoded");
        when(userLoadService.matchPassword("old", "encoded")).thenReturn(true);
    }

    private SystemAuthServiceImpl service() {
        return new SystemAuthServiceImpl(
                null, null, null, userLoadService, null, null, null,
                null, null, null, null, null, null, null, passwordPolicySupport);
    }

    private static LoginUser loginUser() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(USER_ID);
        return loginUser;
    }

    private static UserPasswordCredential credential(String passwordHash) {
        UserPasswordCredential credential = new UserPasswordCredential();
        credential.setUserId(USER_ID);
        credential.setTenantId(TENANT_ID);
        credential.setPassword(passwordHash);
        return credential;
    }
}
