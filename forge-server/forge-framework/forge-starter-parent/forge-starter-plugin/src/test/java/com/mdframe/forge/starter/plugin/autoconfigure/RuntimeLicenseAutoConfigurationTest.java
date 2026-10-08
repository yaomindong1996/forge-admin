package com.mdframe.forge.starter.plugin.autoconfigure;

import com.mdframe.forge.starter.plugin.feature.CommunityFeatureGate;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import com.mdframe.forge.starter.plugin.license.RuntimeLicenseFeatureGate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RuntimeLicenseAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PluginAutoConfiguration.class,
                    RuntimeLicenseAutoConfiguration.class));

    @Test
    void default_disabled_preserves_community() {
        runner.run(context -> assertThat(context).hasNotFailed().hasSingleBean(FeatureGate.class)
                .hasSingleBean(CommunityFeatureGate.class).doesNotHaveBean(RuntimeLicenseFeatureGate.class));
    }

    @Test
    void enabled_missing_license_fails_closed_without_disabling_community() {
        runner.withPropertyValues("forge.license.enabled=true").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(FeatureGate.class)
                    .doesNotHaveBean(CommunityFeatureGate.class).hasSingleBean(RuntimeLicenseFeatureGate.class);
            var gate = context.getBean(FeatureGate.class);
            assertThat(gate.isEnabled("ee.test")).isFalse();
            assertThat(gate.isEnabled("public.test")).isTrue();
        });
    }

    @Test
    void custom_gate_has_priority_when_licensing_enabled() {
        FeatureGate custom = mock(FeatureGate.class);
        runner.withPropertyValues("forge.license.enabled=true")
                .withBean("customGate", FeatureGate.class, () -> custom).run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(FeatureGate.class)
                            .doesNotHaveBean(RuntimeLicenseFeatureGate.class);
                    assertThat(context.getBean(FeatureGate.class)).isSameAs(custom);
                });
    }
}
