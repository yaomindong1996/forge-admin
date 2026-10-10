package com.mdframe.forge.plugin.system.service;

import com.mdframe.forge.plugin.system.dto.SysPluginQuery;
import com.mdframe.forge.starter.plugin.autoconfigure.PluginAutoConfiguration;
import com.mdframe.forge.starter.plugin.catalog.RuntimePluginCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/** 读取 Maven 实际产出的内置资源，不用 mock 替代 Spring 装配或资源过滤。 */
class SysPluginContextTest {
    @Test
    void should_assemble_service_and_report_actual_packaged_builtin_module() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(PluginAutoConfiguration.class))
                .withUserConfiguration(ServiceConfiguration.class).run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(SysPluginService.class)
                            .hasSingleBean(RuntimePluginCatalog.class);
                    var catalog = context.getBean(RuntimePluginCatalog.class);
                    var module = catalog.findById("plugin-system").orElseThrow();
                    assertThat(module.version()).isEqualTo("1.2.0");
                    assertThat(module.serverModule()).endsWith("plugin-system");
                    var page = context.getBean(SysPluginService.class).page(new SysPluginQuery());
                    assertThat(page.records()).extracting("id").contains("plugin-system");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(SysPluginService.class)
    static class ServiceConfiguration {
    }
}
