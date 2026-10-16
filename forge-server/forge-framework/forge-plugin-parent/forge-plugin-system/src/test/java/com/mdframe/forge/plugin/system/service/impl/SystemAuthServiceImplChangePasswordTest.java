package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class SystemAuthServiceImplChangePasswordTest {

    @Test
    void shouldRejectWrongOldPasswordWithBusinessCode() {
        IUserLoadService userLoadService = mock(IUserLoadService.class);
        when(userLoadService.getUserPassword(1L)).thenReturn("encoded");
        when(userLoadService.matchPassword("wrong", "encoded")).thenReturn(false);

        SystemAuthServiceImpl service = new SystemAuthServiceImpl(
                null, null, null, userLoadService, null, null, null,
                null, null, null, null, null, null, null);

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(loginUser);
            assertThatThrownBy(() -> service.changePassword("wrong", "NewPass12!"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException business = (BusinessException) ex;
                        assertThat(business.getCode()).isEqualTo(400);
                        assertThat(business.getMessage()).isEqualTo("旧密码错误");
                    });
        }
    }

    @Test
    void shouldRejectUnauthenticatedChangeWithBusinessCode() {
        SystemAuthServiceImpl service = new SystemAuthServiceImpl(
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);

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
}
