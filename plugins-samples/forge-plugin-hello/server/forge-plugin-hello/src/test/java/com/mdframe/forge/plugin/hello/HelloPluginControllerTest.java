package com.mdframe.forge.plugin.hello;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.hello.controller.HelloPluginController;
import com.mdframe.forge.starter.plugin.ForgeVersion;
import com.mdframe.forge.starter.plugin.descriptor.PluginRegistry;
import com.mdframe.forge.starter.plugin.feature.CommunityFeatureGate;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import com.mdframe.forge.starter.plugin.feature.RequiresFeature;
import com.mdframe.forge.starter.plugin.web.FeatureGateInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 不读取 application 配置、不启用 Redis；真实会话授权留给宿主运行验收。 */
class HelloPluginControllerTest {

    private final PluginRegistry registry = new PluginRegistry(new PathMatchingResourcePatternResolver());

    @Test
    void should_load_real_runtime_descriptor() {
        var descriptor = registry.findById("hello").orElseThrow();
        assertThat(descriptor.version()).isEqualTo("1.0.0");
        assertThat(descriptor.features()).containsExactly("community.hello");
        assertThat(ForgeVersion.satisfies(descriptor.requiresCore())).isTrue();
    }

    @Test
    void should_return_typed_registry_and_core_versions() throws Exception {
        mvc(new CommunityFeatureGate()).perform(get("/plugin/hello/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("hello"))
                .andExpect(jsonPath("$.data.version").value(registry.findById("hello").orElseThrow().version()))
                .andExpect(jsonPath("$.data.coreVersion").value(ForgeVersion.CURRENT));
    }

    @Test
    void should_require_rbac_and_feature_annotations() throws Exception {
        var method = HelloPluginController.class.getMethod("info");
        assertThat(method.getAnnotation(SaCheckPermission.class).value()).containsExactly("plugin:hello:info");
        assertThat(HelloPluginController.class.getAnnotation(RequiresFeature.class).value())
                .isEqualTo("community.hello");
    }

    @Test
    void should_block_custom_gate_denial() throws Exception {
        FeatureGate denied = new FeatureGate() {
            @Override
            public boolean isEnabled(String featureCode) {
                return false;
            }

            @Override
            public String edition() {
                return "community";
            }
        };
        mvc(denied).perform(get("/plugin/hello/info"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void should_scan_controller_in_host_package() {
        String base = HelloPluginController.class.getPackageName().split("\\.plugin\\.hello")[0];
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(PluginRegistry.class, () -> registry);
            context.scan(base + ".plugin.hello");
            context.refresh();
            assertThat(context.getBean(HelloPluginController.class).info().getData().id()).isEqualTo("hello");
        }
    }

    @Test
    void should_fail_if_runtime_descriptor_is_missing() {
        var empty = new PathMatchingResourcePatternResolver() {
            @Override
            public Resource[] getResources(String location) {
                return new Resource[0];
            }
        };
        assertThatThrownBy(() -> new HelloPluginController(new PluginRegistry(empty)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("缺少运行描述");
    }

    private MockMvc mvc(FeatureGate gate) {
        return MockMvcBuilders.standaloneSetup(new HelloPluginController(registry))
                .addInterceptors(new FeatureGateInterceptor(gate, new ObjectMapper())).build();
    }
}
