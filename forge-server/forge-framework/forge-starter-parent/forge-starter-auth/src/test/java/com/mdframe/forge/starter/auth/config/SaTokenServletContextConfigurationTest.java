package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStaff;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SaTokenServletContextConfigurationTest {

    @Test
    void shouldRegisterUpstreamFilterForAsyncAndErrorWithoutChangingItsOrder() {
        SaTokenContextFilterForJakartaServlet filter = new SaTokenContextFilterForJakartaServlet();
        var registration = new SaTokenServletContextConfiguration().saTokenServletContextRegistration(filter);

        assertThat(registration.getFilter()).isSameAs(filter);
        assertThat(registration.isAsyncSupported()).isTrue();
        assertThat(registration.determineDispatcherTypes())
                .containsExactlyInAnyOrder(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
        assertThat(registration.getOrder())
                .isEqualTo(OrderUtils.getOrder(filter.getClass(), Ordered.HIGHEST_PRECEDENCE));
    }

    @Test
    void shouldRebuildAndClearContextOnAsyncRedispatch() throws Exception {
        SaTokenContext previous = SaManager.getSaTokenContext();
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
            request.setDispatcherType(DispatcherType.ASYNC);
            new SaTokenContextFilterForJakartaServlet().doFilter(request, new MockHttpServletResponse(),
                    (req, res) -> assertThat(SaHolder.getRequest().getRequestPath()).isEqualTo("/mcp"));
            assertThat(SaTokenContextForThreadLocalStaff.getModelBoxOrNull()).isNull();
        } finally {
            SaTokenContextForThreadLocalStaff.clearModelBox();
            SaManager.setSaTokenContext(previous);
        }
    }
}
