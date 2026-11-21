package com.mdframe.forge.starter.auth.controller;

import com.mdframe.forge.starter.auth.service.IAuthService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void getUserInfoShouldRefreshByAuthenticatedUserIdInActiveTenant() {
        IAuthService authService = mock(IAuthService.class);
        AuthController controller = new AuthController(authService);
        LoginUser sessionUser = loginUser(7L, 23L, 91L);
        sessionUser.setUsername("admin");
        sessionUser.setLoginTime(1000L);
        sessionUser.setLoginIp("127.0.0.1");
        sessionUser.setUserClient("pc");

        LoginUser freshUser = loginUser(7L, 23L, 91L);
        when(authService.loadUserByUserId(7L, 23L, 91L)).thenReturn(freshUser);

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(sessionUser);

            RespInfo<LoginUser> response = controller.getUserInfo();

            assertThat(response.getData()).isSameAs(freshUser);
            assertThat(freshUser.getLoginTime()).isEqualTo(1000L);
            assertThat(freshUser.getLoginIp()).isEqualTo("127.0.0.1");
            assertThat(freshUser.getUserClient()).isEqualTo("pc");
            assertThat(freshUser.getPasswordExpired()).isNull();
            assertThat(freshUser.getForcePasswordChange()).isNull();
            verify(authService).loadUserByUserId(7L, 23L, 91L);
            verify(authService, never()).loadUserByUsername(anyString(), anyLong(), anyLong());
            session.verify(() -> SessionHelper.setLoginUser(freshUser));
        }
    }

    @Test
    void getUserInfoShouldKeepPasswordExpiredMarkerAcrossRefresh() {
        IAuthService authService = mock(IAuthService.class);
        AuthController controller = new AuthController(authService);
        LoginUser sessionUser = loginUser(7L, 23L, 91L);
        sessionUser.setPasswordExpired(true);
        sessionUser.setForcePasswordChange(true);

        // 数据库重建出来的用户不带过期标记（force_password_change 列为 0）
        LoginUser freshUser = loginUser(7L, 23L, 91L);
        freshUser.setForcePasswordChange(false);
        when(authService.loadUserByUserId(7L, 23L, 91L)).thenReturn(freshUser);

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(sessionUser);

            RespInfo<LoginUser> response = controller.getUserInfo();

            assertThat(response.getData()).isSameAs(freshUser);
            assertThat(freshUser.getPasswordExpired()).isTrue();
            assertThat(freshUser.getForcePasswordChange()).isTrue();
            session.verify(() -> SessionHelper.setLoginUser(freshUser));
        }
    }

    @Test
    void getUserInfoShouldNotLoadUserWhenSessionIsEmpty() {
        IAuthService authService = mock(IAuthService.class);
        AuthController controller = new AuthController(authService);

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(null);

            RespInfo<LoginUser> response = controller.getUserInfo();

            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getData()).isNull();
            verify(authService, never()).loadUserByUserId(anyLong(), anyLong(), anyLong());
        }
    }

    private LoginUser loginUser(Long userId, Long tenantId, Long activeOrgId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setTenantId(tenantId);
        loginUser.setActiveOrgId(activeOrgId);
        return loginUser;
    }
}
