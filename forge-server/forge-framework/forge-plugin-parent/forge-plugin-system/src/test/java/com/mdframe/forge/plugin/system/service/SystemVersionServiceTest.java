package com.mdframe.forge.plugin.system.service;

import com.mdframe.forge.starter.plugin.ForgeVersion;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.info.BuildProperties;

import java.time.Instant;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SystemVersionServiceTest {
    @Test
    void missing_build_info_is_unknown_not_replaced_by_core_version() {
        var result = service(null).current();
        assertThat(result.version()).isNull();
        assertThat(result.build()).isNull();
        assertThat(result.coreVersion()).isEqualTo(ForgeVersion.CURRENT);
        assertThat(result.edition()).isEqualTo("community");
    }

    @Test
    void exposes_host_build_info_and_core_version_independently() {
        var properties = new Properties();
        properties.setProperty("version", "2.0.1");
        properties.setProperty("artifact", "customer-admin-server");
        properties.setProperty("time", "1791612000000");
        properties.setProperty("commit", "abc123456789");
        var result = service(properties).current();
        assertThat(result.version()).isEqualTo("2.0.1");
        assertThat(result.build().service()).isEqualTo("customer-admin-server");
        assertThat(result.build().time()).isEqualTo(Instant.ofEpochMilli(1791612000000L).toString());
        assertThat(result.build().commit()).isEqualTo("abc123456789");
    }

    @Test
    void invalid_or_unsupplied_commit_is_not_exposed_as_a_real_revision() {
        for (String value : new String[] { "unknown", "${forge.build.commit}", "/private/project", "" }) {
            var properties = new Properties();
            properties.setProperty("commit", value);
            var result = service(properties).current();
            assertThat(result.build().commit()).isNull();
            assertThat(result.build().time()).isNull();
        }
    }

    private SystemVersionService service(Properties properties) {
        var factory = new StaticListableBeanFactory();
        if (properties != null) {
            factory.addBean("buildProperties", new BuildProperties(properties));
        }
        var gate = new FeatureGate() {
            public boolean isEnabled(String code) { return true; }
            public String edition() { return "community"; }
        };
        return new SystemVersionService(factory.getBeanProvider(BuildProperties.class), gate);
    }
}
