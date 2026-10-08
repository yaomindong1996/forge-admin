package com.mdframe.forge.plugin.system.service;

import com.mdframe.forge.plugin.system.service.plugin.RuntimeLicenseStatusService;
import com.mdframe.forge.starter.plugin.feature.CommunityFeatureGate;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import com.mdframe.forge.starter.plugin.license.RuntimeLicenseFeatureGate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeLicenseStatusServiceTest {
    @Test
    void community_and_custom_gate_are_not_presented_as_file_verification() {
        var factory = new StaticListableBeanFactory();
        var service = new RuntimeLicenseStatusService(factory.getBeanProvider(FeatureGate.class));
        assertThat(service.status().mode()).isEqualTo("unavailable");
        factory.addBean("gate", new CommunityFeatureGate());
        assertThat(service.status().mode()).isEqualTo("community");
        assertThat(service.status().report()).isNull();
        factory.addBean("gate", new FeatureGate() {
            @Override
            public boolean isEnabled(String feature) { return false; }
            @Override
            public String edition() { return "community"; }
        });
        assertThat(service.status().mode()).isEqualTo("custom");
        assertThat(service.status().report()).isNull();
    }

    @Test
    void missing_invalid_license_can_still_be_diagnosed_without_commercial_grant() {
        var factory = new StaticListableBeanFactory();
        factory.addBean("gate", new RuntimeLicenseFeatureGate(null, List.of(), Clock.systemUTC()));
        var service = new RuntimeLicenseStatusService(factory.getBeanProvider(FeatureGate.class));
        var status = service.status();
        assertThat(status.mode()).isEqualTo("license");
        assertThat(status.edition()).isEqualTo("community");
        assertThat(status.report().configuration().binding()).isNull();
        assertThat(status.report().entries()).isEmpty();
    }
}
