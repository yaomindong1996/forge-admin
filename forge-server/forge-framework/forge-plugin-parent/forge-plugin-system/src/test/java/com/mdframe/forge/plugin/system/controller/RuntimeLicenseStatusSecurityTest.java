package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.servlet.model.SaRequestForServlet;
import cn.dev33.satoken.servlet.model.SaResponseForServlet;
import cn.dev33.satoken.servlet.model.SaStorageForServlet;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.plugin.system.service.plugin.RuntimeLicenseStatusService;
import com.mdframe.forge.plugin.system.vo.RuntimeLicenseStatusVO;
import com.mdframe.forge.starter.auth.config.StpInterfaceImpl;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 实际 Sa 注解、会话权限提供器和平台管理员断言；测试 DAO 只在进程内。 */
class RuntimeLicenseStatusSecurityTest {
    private SaTokenConfig previousConfig;
    private SaTokenContext previousContext;
    private SaTokenDao previousDao;
    private StpInterface previousPermissions;
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final SaInterceptor interceptor = new SaInterceptor(handler -> StpUtil.checkLogin());
    private final RuntimeLicenseStatusService service = mock(RuntimeLicenseStatusService.class);
    private final RuntimeLicenseStatusController controller = new RuntimeLicenseStatusController(service);
    private HandlerMethod handler;

    @BeforeEach
    void setup() throws Exception {
        previousConfig = SaManager.getConfig();
        previousContext = SaManager.getSaTokenContext();
        previousDao = SaManager.getSaTokenDao();
        previousPermissions = SaManager.getStpInterface();
        SaManager.setConfig(new SaTokenConfig().setIsPrint(false).setDataRefreshPeriod(-1).setIsReadCookie(false));
        SaManager.setSaTokenDao(new SaTokenDaoDefaultImpl());
        SaManager.setStpInterface(new StpInterfaceImpl());
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new SaRequestForServlet(request),
                new SaResponseForServlet(response), new SaStorageForServlet(request));
        var method = RuntimeLicenseStatusController.class.getMethod(
                "status", jakarta.servlet.http.HttpServletResponse.class);
        handler = new HandlerMethod(controller, method);
    }

    @AfterEach
    void cleanup() {
        SaTokenContextForThreadLocalStorage.clearBox();
        SaManager.setConfig(previousConfig);
        SaManager.setSaTokenContext(previousContext);
        SaManager.setSaTokenDao(previousDao);
        SaManager.setStpInterface(previousPermissions);
    }

    @Test
    void anonymous_and_unrelated_grants_are_rejected_by_actual_interceptor() {
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
                .isInstanceOf(NotLoginException.class);
        login(0, Set.of("system:plugin:detail"));
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
                .isInstanceOf(NotPermissionException.class);
        login(0, Set.of());
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
                .isInstanceOf(NotPermissionException.class);
        verifyNoInteractions(service);
    }

    @Test
    void tenant_admin_with_wildcard_cannot_read_instance_license_bindings() throws Exception {
        login(1, Set.of("**", "system:plugin:list"));
        assertThat(interceptor.preHandle(request, response, handler)).isTrue();
        assertThatThrownBy(() -> controller.status(response)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("平台超级管理员");
        verifyNoInteractions(service);
    }

    @Test
    void platform_admin_can_read_without_enterprise_grant_and_response_is_not_cached() throws Exception {
        login(0, Set.of("system:plugin:list"));
        when(service.status()).thenReturn(new RuntimeLicenseStatusVO("community", "community", null));
        assertThat(interceptor.preHandle(request, response, handler)).isTrue();
        var result = controller.status(response);
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().mode()).isEqualTo("community");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    }

    private void login(int type, Set<String> permissions) {
        StpUtil.login(7L);
        var user = new LoginUser();
        user.setUserId(7L);
        user.setTenantId(1L);
        user.setUserType(type);
        user.setPermissions(permissions);
        SessionHelper.setLoginUser(user);
    }
}
