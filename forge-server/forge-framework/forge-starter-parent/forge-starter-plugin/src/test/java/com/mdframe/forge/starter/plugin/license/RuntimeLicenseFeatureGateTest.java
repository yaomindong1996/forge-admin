package com.mdframe.forge.starter.plugin.license;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeLicenseFeatureGateTest {
    @TempDir
    Path temp;

    @Test
    void permanent_use_survives_maintenance_expiry_and_same_project_allows_multi_node() {
        var payload = LicenseFixture.payload();
        var firstNode = gate(payload.binding(), List.of(payload), 1000);
        var secondNode = gate(payload.binding(), List.of(payload), 1000);
        assertThat(firstNode.isEnabled("ee.test.view")).isTrue();
        assertThat(secondNode.isEnabled("ee.test.view")).isTrue();
        assertThat(firstNode.edition()).isEqualTo("ee");
        assertThat(firstNode.isEnabled("ee.other")).isFalse();
        assertThat(firstNode.isEnabled("community.view")).isTrue();
        assertThat(firstNode.isEnabled(null)).isTrue();
    }

    @Test
    void mismatched_customer_project_future_and_expired_fail_closed_only_for_ee() {
        var payload = LicenseFixture.payload();
        assertThat(gate(new LicensePayload.Binding("43", LicenseFixture.PROJECT), List.of(payload), 500)
                .isEnabled("ee.test.view")).isFalse();
        var otherProject = new LicensePayload.Binding("42", "5583fa26-4b8a-4f54-ab19-2cb8d08733e3");
        assertThat(gate(otherProject, List.of(payload), 500).isEnabled("ee.test.view")).isFalse();
        var limited = new LicensePayload(1, LicenseFixture.ID, payload.binding(), payload.scope(),
                new LicensePayload.Terms(100, 200, 300L, 1000L));
        for (long now : List.of(99L, 199L, 300L, 1001L)) {
            var gate = gate(payload.binding(), List.of(limited), now);
            assertThat(gate.isEnabled("ee.test.view")).isFalse();
            assertThat(gate.isEnabled("public.view")).isTrue();
            assertThat(gate.edition()).isEqualTo("community");
        }
        assertThat(gate(payload.binding(), List.of(limited), 200).isEnabled("ee.test.view")).isTrue();
    }

    @Test
    void combines_precise_features_without_wildcards() {
        var first = LicenseFixture.payload();
        var second = new LicensePayload(1, LicenseFixture.ID, first.binding(),
                new LicensePayload.Scope(List.of("second-plugin"), List.of("ee.second.view")), first.terms());
        var gate = gate(first.binding(), List.of(first, second), 500);
        assertThat(gate.isEnabled("ee.test.view")).isTrue();
        assertThat(gate.isEnabled("ee.second.view")).isTrue();
        assertThat(gate.isEnabled("ee.second.edit")).isFalse();
    }

    @Test
    void loader_ignores_bad_file_without_disabling_good_license_and_community() throws Exception {
        var pair = LicenseFixture.keyPair();
        Path key = temp.resolve("public.der");
        Path good = temp.resolve("license.json");
        Path bad = temp.resolve("bad.json");
        Files.writeString(key, java.util.Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
        Files.write(good, LicenseFixture.document(pair));
        Files.writeString(bad, "invalid");
        var properties = new RuntimeLicenseProperties();
        properties.setCustomerId("42");
        properties.setInstallationId(LicenseFixture.PROJECT);
        properties.setPublicKeys(Map.of("key-1", key));
        properties.setFiles(List.of(good, bad));
        var gate = RuntimeLicenseLoader.load(properties);
        assertThat(gate.isEnabled("ee.test.view")).isTrue();
        properties.setInstallationId("invalid");
        var invalid = RuntimeLicenseLoader.load(properties);
        assertThat(invalid.isEnabled("ee.test.view")).isFalse();
        assertThat(invalid.isEnabled("public.view")).isTrue();
    }

    private RuntimeLicenseFeatureGate gate(LicensePayload.Binding binding, List<LicensePayload> licenses, long time) {
        return new RuntimeLicenseFeatureGate(binding, licenses,
                Clock.fixed(Instant.ofEpochSecond(time), ZoneOffset.UTC));
    }
}
