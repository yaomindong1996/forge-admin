package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.servlet.model.SaRequestForServlet;
import cn.dev33.satoken.servlet.model.SaResponseForServlet;
import cn.dev33.satoken.servlet.model.SaStorageForServlet;
import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.plugin.system.service.SystemVersionService;
import com.mdframe.forge.plugin.system.vo.SystemVersionVO;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 实际 Sa 会话、注解拦截器与 MVC 协议；不加载数据源和 Flyway，不连接 Redis。 */
class SystemVersionControllerTest {
    private SaTokenConfig previousConfig;
    private SaTokenContext previousContext;
    private SaTokenDao previousDao;
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final SaInterceptor interceptor = new SaInterceptor(handler -> StpUtil.checkLogin());
    private final RecordingVersionService service = new RecordingVersionService();
    private final SystemVersionController controller = new SystemVersionController(service);
    private HandlerMethod handler;

    @BeforeEach
    void setup() throws Exception {
        previousConfig = SaManager.getConfig();
        previousContext = SaManager.getSaTokenContext();
        previousDao = SaManager.getSaTokenDao();
        SaManager.setConfig(new SaTokenConfig().setIsPrint(false).setDataRefreshPeriod(-1).setIsReadCookie(false));
        SaManager.setSaTokenDao(new SaTokenDaoDefaultImpl());
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new SaRequestForServlet(request),
                new SaResponseForServlet(response), new SaStorageForServlet(request));
        handler = new HandlerMethod(controller, SystemVersionController.class.getMethod(
                "current", jakarta.servlet.http.HttpServletResponse.class));
    }

    @AfterEach
    void cleanup() {
        SaTokenContextForThreadLocalStorage.clearBox();
        SaManager.setConfig(previousConfig);
        SaManager.setSaTokenContext(previousContext);
        SaManager.setSaTokenDao(previousDao);
    }

    @Test
    void anonymous_is_rejected_even_when_called_without_interceptor() {
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
                .isInstanceOf(NotLoginException.class);
        assertThatThrownBy(() -> controller.current(response)).isInstanceOf(NotLoginException.class);
        assertThat(service.calls).isZero();
    }

    @Test
    void ordinary_logged_in_user_needs_no_plugin_or_admin_permission() throws Exception {
        StpUtil.login(7L);
        assertThat(handler.hasMethodAnnotation(ApiPermissionIgnore.class)).isTrue();
        var mvc = MockMvcBuilders.standaloneSetup(controller).addInterceptors(interceptor).build();
        mvc.perform(get("/system/version"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.version").value("1.2.0"))
                .andExpect(jsonPath("$.data.coreVersion").value("1.2.0"));
    }

    @Test
    void response_schema_is_limited_to_non_sensitive_release_fields() {
        var data = new SystemVersionVO("1.2.0", "1.2.0", "community",
                new SystemVersionVO.Build("forge-admin-server", "2026-10-10T10:00:00Z", null));
        var tree = new ObjectMapper().valueToTree(data);
        var keys = new java.util.ArrayList<String>();
        tree.fieldNames().forEachRemaining(keys::add);
        assertThat(keys).containsExactlyInAnyOrder("version", "coreVersion", "edition", "build");
        keys.clear();
        tree.get("build").fieldNames().forEachRemaining(keys::add);
        assertThat(keys).containsExactlyInAnyOrder("service", "time", "commit");
    }

    private static class RecordingVersionService extends SystemVersionService {
        private int calls;

        RecordingVersionService() { super(null, null); }

        @Override
        public SystemVersionVO current() {
            calls++;
            return new SystemVersionVO("1.2.0", "1.2.0", "community", null);
        }
    }
}
