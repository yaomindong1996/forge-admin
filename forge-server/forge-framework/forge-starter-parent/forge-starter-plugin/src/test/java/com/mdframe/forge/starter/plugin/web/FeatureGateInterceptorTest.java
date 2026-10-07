package com.mdframe.forge.starter.plugin.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.plugin.feature.CommunityFeatureGate;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import com.mdframe.forge.starter.plugin.feature.RequiresFeature;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FeatureGateInterceptorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final FeatureGateInterceptor interceptor = new FeatureGateInterceptor(new CommunityFeatureGate(), mapper);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void should_deny_class_requirement_with_uniform_json_response() throws Exception {
        assertThat(interceptor.preHandle(request, response, handler(new Restricted(), "restricted"))).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(mapper.readTree(response.getContentAsString()).get("code").asInt()).isEqualTo(403);
        assertThat(mapper.readTree(response.getContentAsString()).get("message").asText())
                .isEqualTo("当前版本未开通该功能：ee.class");
        assertThat(response.getContentAsString()).doesNotContain("stackTrace", "Exception");
    }

    @Test
    void should_prioritize_method_requirement_over_class_requirement() throws Exception {
        assertThat(interceptor.preHandle(request, response, handler(new Restricted(), "community"))).isTrue();
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(interceptor.preHandle(request, response, handler(new Restricted(), "method"))).isFalse();
        assertThat(response.getContentAsString()).contains("ee.method");
    }

    @Test
    void should_support_inherited_and_composed_annotations() throws Exception {
        assertThat(interceptor.preHandle(request, response, handler(new Child(), "restricted"))).isFalse();
        MockHttpServletResponse composed = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(request, composed, handler(new Plain(), "composed"))).isFalse();
        assertThat(composed.getContentAsString()).contains("ee.composed");
    }

    @Test
    void should_support_interface_method_annotation() throws Exception {
        assertThat(interceptor.preHandle(request, response, handler(new InterfaceController(), "contract"))).isFalse();
        assertThat(response.getContentAsString()).contains("ee.contract");
    }

    @Test
    void should_allow_unannotated_and_non_controller_handlers_without_gate_lookup() throws Exception {
        FeatureGate gate = mock(FeatureGate.class);
        FeatureGateInterceptor unannotated = new FeatureGateInterceptor(gate, mapper);
        assertThat(unannotated.preHandle(request, response, new Object())).isTrue();
        assertThat(unannotated.preHandle(request, response, handler(new Plain(), "plain"))).isTrue();
        verifyNoInteractions(gate);
    }

    @Test
    void should_use_custom_gate_and_not_treat_failure_as_permission() throws Exception {
        FeatureGate gate = mock(FeatureGate.class);
        when(gate.isEnabled("ee.class")).thenReturn(true);
        FeatureGateInterceptor custom = new FeatureGateInterceptor(gate, mapper);
        HandlerMethod handler = handler(new Restricted(), "restricted");
        assertThat(custom.preHandle(request, response, handler)).isTrue();
        when(gate.isEnabled("ee.class")).thenThrow(new IllegalStateException("fixture license unavailable"));
        assertThatThrownBy(() -> custom.preHandle(request, response, handler))
                .isInstanceOf(IllegalStateException.class);
    }

    private HandlerMethod handler(Object bean, String method) throws NoSuchMethodException {
        return new HandlerMethod(bean, bean.getClass().getMethod(method));
    }

    @RequiresFeature("ee.class")
    static class Restricted {
        public void restricted() {
        }

        @RequiresFeature("community.hello")
        public void community() {
        }

        @RequiresFeature("ee.method")
        public void method() {
        }
    }

    static class Child extends Restricted {
    }

    static class Plain {
        public void plain() {
        }

        @ComposedFeature
        public void composed() {
        }
    }

    interface Contract {
        @RequiresFeature("ee.contract")
        void contract();
    }

    static class InterfaceController implements Contract {
        public void contract() {
        }
    }

    @RequiresFeature("ee.composed")
    @Retention(RetentionPolicy.RUNTIME)
    @interface ComposedFeature {
    }
}
