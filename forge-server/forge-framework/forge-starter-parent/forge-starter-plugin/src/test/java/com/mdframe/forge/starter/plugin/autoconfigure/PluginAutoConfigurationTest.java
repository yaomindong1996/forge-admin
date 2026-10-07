package com.mdframe.forge.starter.plugin.autoconfigure;

import com.mdframe.forge.starter.plugin.descriptor.PluginRegistry;
import com.mdframe.forge.starter.plugin.catalog.RuntimePluginCatalog;
import com.mdframe.forge.starter.plugin.feature.CommunityFeatureGate;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import com.mdframe.forge.starter.plugin.feature.RequiresFeature;
import com.mdframe.forge.starter.plugin.web.FeatureGateInterceptor;
import com.mdframe.forge.starter.plugin.web.FeatureGateWebMvcConfigurer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PluginAutoConfigurationTest {

    @TempDir
    Path temp;

    private final ApplicationContextRunner core = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PluginAutoConfiguration.class, PluginWebAutoConfiguration.class));

    private final WebApplicationContextRunner web = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PluginAutoConfiguration.class, PluginWebAutoConfiguration.class,
                    JacksonAutoConfiguration.class, WebMvcAutoConfiguration.class))
            .withUserConfiguration(Controllers.class);

    @Test
    void should_register_one_default_gate_without_servlet_beans_in_non_web_host() {
        core.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(FeatureGate.class).hasSingleBean(PluginRegistry.class);
            assertThat(context).hasSingleBean(RuntimePluginCatalog.class);
            assertThat(context.getBean(FeatureGate.class)).isInstanceOf(CommunityFeatureGate.class);
            assertThat(context.getBean(PluginRegistry.class).getPlugins()).isEmpty();
            assertThat(context).doesNotHaveBean(FeatureGateInterceptor.class)
                    .doesNotHaveBean(FeatureGateWebMvcConfigurer.class);
        });
    }

    @Test
    void should_back_off_default_gate_for_user_implementation_without_primary() {
        FeatureGate gate = mock(FeatureGate.class);
        core.withBean("customerGate", FeatureGate.class, () -> gate).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(FeatureGate.class)
                    .doesNotHaveBean(CommunityFeatureGate.class);
            assertThat(context.getBean(FeatureGate.class)).isSameAs(gate);
        });
    }

    @Test
    void should_allow_customer_auto_configuration_ordered_before_default() {
        core.withConfiguration(AutoConfigurations.of(CustomerGateAutoConfiguration.class)).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(FeatureGate.class)
                    .doesNotHaveBean(CommunityFeatureGate.class);
            assertThat(context.getBean(FeatureGate.class)).isSameAs(context.getBean("customerGate"));
        });
    }

    @Test
    void should_eagerly_validate_registry_even_with_global_lazy_initialization() {
        core.withInitializer(context -> context.addBeanFactoryPostProcessor(
                new LazyInitializationBeanFactoryPostProcessor())).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeanFactory().containsSingleton("pluginRegistry")).isTrue();
            assertThat(context.getBeanFactory().containsSingleton("runtimePluginCatalog")).isTrue();
        });
    }

    @Test
    void should_register_real_mvc_interceptor_and_honor_method_override() {
        web.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(FeatureGateInterceptor.class)
                    .hasSingleBean(FeatureGateWebMvcConfigurer.class);
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context.getSourceApplicationContext()).build();
            mvc.perform(get("/fixture/restricted")).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.message").value("当前版本未开通该功能：ee.fixture"));
            mvc.perform(get("/fixture/community")).andExpect(status().isOk()).andExpect(content().string("community"));
            mvc.perform(get("/fixture/plain")).andExpect(status().isOk()).andExpect(content().string("plain"));
        });
    }

    @Test
    void should_use_custom_gate_in_real_mvc_request() {
        FeatureGate gate = mock(FeatureGate.class);
        when(gate.isEnabled("ee.fixture")).thenReturn(true);
        web.withBean("customerGate", FeatureGate.class, () -> gate).run(context -> {
            assertThat(context).hasSingleBean(FeatureGate.class).doesNotHaveBean(CommunityFeatureGate.class);
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context.getSourceApplicationContext()).build();
            mvc.perform(get("/fixture/restricted")).andExpect(status().isOk());
        });
    }

    @Test
    void should_run_login_interceptor_before_feature_check() {
        FeatureGate gate = mock(FeatureGate.class);
        web.withUserConfiguration(LoginFixture.class).withBean("customerGate", FeatureGate.class, () -> gate)
                .run(context -> {
                    MockMvc mvc = MockMvcBuilders.webAppContextSetup(context.getSourceApplicationContext()).build();
                    mvc.perform(get("/fixture/restricted")).andExpect(status().isUnauthorized());
                    verifyNoInteractions(gate);
                    mvc.perform(get("/fixture/restricted").header("X-Fixture-Login", "yes"))
                            .andExpect(status().isForbidden());
                });
    }

    @Test
    void should_fail_container_startup_when_plugin_core_version_is_incompatible() throws IOException {
        try (URLClassLoader loader = incompatiblePluginClassLoader()) {
            core.withClassLoader(loader).run(context -> assertThat(context).hasFailed()
                    .getFailure().hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasStackTraceContaining("插件 hello，要求 >=2.0.0，当前核心 1.2.0"));
        }
    }

    @Test
    void should_fail_lazy_container_startup_when_plugin_core_version_is_incompatible() throws IOException {
        try (URLClassLoader loader = incompatiblePluginClassLoader()) {
            core.withClassLoader(loader).withInitializer(context -> context.addBeanFactoryPostProcessor(
                    new LazyInitializationBeanFactoryPostProcessor())).run(context -> assertThat(context).hasFailed()
                    .getFailure().hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasStackTraceContaining("插件 hello，要求 >=2.0.0，当前核心 1.2.0"));
        }
    }

    private URLClassLoader incompatiblePluginClassLoader() throws IOException {
        Path classes = temp.resolve("bad-plugin");
        Files.createDirectories(classes.resolve("META-INF"));
        Files.writeString(classes.resolve("META-INF/forge-plugin.json"), """
                {"id":"hello","name":"示例","version":"1.0.0","edition":"community",
                 "requiresCore":">=2.0.0","features":[]}
                """);
        URL[] urls = {classes.toUri().toURL()};
        return new URLClassLoader(urls, getClass().getClassLoader());
    }

    @AutoConfiguration(before = PluginAutoConfiguration.class)
    static class CustomerGateAutoConfiguration {
        @Bean
        FeatureGate customerGate() {
            return mock(FeatureGate.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class Controllers {
        @Bean
        RestrictedController restrictedController() {
            return new RestrictedController();
        }

        @Bean
        PlainController plainController() {
            return new PlainController();
        }
    }

    @RestController
    @RequiresFeature("ee.fixture")
    static class RestrictedController {
        @GetMapping("/fixture/restricted")
        public String restricted() {
            return "restricted";
        }

        @GetMapping("/fixture/community")
        @RequiresFeature("community.fixture")
        public String community() {
            return "community";
        }
    }

    @RestController
    static class PlainController {
        @GetMapping("/fixture/plain")
        public String plain() {
            return "plain";
        }
    }

    /** 仅模拟已有 order=1 的登录拦截，不连接真实会话或 Redis。 */
    @Configuration(proxyBeanMethods = false)
    static class LoginFixture implements WebMvcConfigurer {
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new HandlerInterceptor() {
                @Override
                public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                    if (!"yes".equals(request.getHeader("X-Fixture-Login"))) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return false;
                    }
                    return true;
                }
            }).addPathPatterns("/**").order(1);
        }
    }
}
