package com.mdframe.forge.starter.plugin.license;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeLicenseReportTest {
    @TempDir
    Path temp;
    private final MutableClock clock = new MutableClock();

    @Test
    void real_verified_files_report_exact_order_and_hide_untrusted_or_foreign_declarations() throws Exception {
        var pair = LicenseFixture.keyPair();
        var original = LicenseFixture.payload();
        var properties = properties(pair);
        var foreign = new LicensePayload(1, "00000000-0000-4000-8000-000000000009",
                new LicensePayload.Binding("99", LicenseFixture.PROJECT),
                new LicensePayload.Scope(List.of("foreign-plugin"), List.of("ee.foreign")), original.terms());
        Path broken = temp.resolve("secret-filename.json");
        Files.writeString(broken, "{\"customerId\":\"untrusted-account\",\"payload\":\"secret-value\"}");
        properties.setFiles(List.of(file(pair, original, "first"), file(pair, foreign, "foreign"), broken));
        var gate = RuntimeLicenseLoader.load(properties, clock);
        var report = gate.report();
        assertThat(report.entries()).extracting(RuntimeLicenseReport.Entry::position).containsExactly(1, 2, 3);
        assertThat(report.entries()).extracting(RuntimeLicenseReport.Entry::state)
                .containsExactly("valid", "binding_mismatch", "invalid");
        assertThat(report.entries().get(0).scope().featureCodes()).containsExactly("ee.test.view");
        assertThat(report.entries().get(1).scope()).isNull();
        assertThat(report.entries().get(1).licenseId()).isNull();
        assertThat(report.entries().get(2).terms()).isNull();
        assertThat(gate.isEnabled("ee.foreign")).isFalse();
        String json = new ObjectMapper().writeValueAsString(report);
        assertThat(json).doesNotContain(temp.toString(), "secret-filename", "secret-value", "untrusted-account",
                "foreign-plugin", "ee.foreign", foreign.licenseId(), "signature", "payload");
    }

    @Test
    void status_updates_on_clock_without_reloading_or_mutating_startup_snapshot() throws Exception {
        var pair = LicenseFixture.keyPair();
        var base = LicenseFixture.payload();
        var finite = new LicensePayload(1, base.licenseId(), base.binding(), base.scope(),
                new LicensePayload.Terms(100, 100, 600L, 200L));
        Path file = file(pair, finite, "changing");
        var properties = properties(pair);
        properties.setFiles(List.of(file));
        var gate = RuntimeLicenseLoader.load(properties, clock);
        var initial = gate.report();
        Files.writeString(file, "invalid-after-startup");
        assertThat(gate.report().entries().get(0).state()).isEqualTo("valid");
        assertThat(gate.isEnabled("ee.test.view")).isTrue();
        clock.now = Instant.ofEpochSecond(600);
        var expired = gate.report();
        assertThat(expired.entries().get(0).state()).isEqualTo("expired");
        assertThat(expired.edition()).isEqualTo(gate.edition()).isEqualTo("community");
        assertThat(gate.isEnabled("ee.test.view")).isFalse();
        assertThat(gate.isEnabled("community.view")).isTrue();
        assertThat(expired.configuration().loadedAt()).isEqualTo(initial.configuration().loadedAt());
        assertThat(initial.entries().get(0).state()).isEqualTo("valid");
        assertThat(RuntimeLicenseLoader.load(properties, clock).report().entries().get(0).state()).isEqualTo("invalid");
    }

    @Test
    void future_start_future_issue_and_maintenance_expiry_agree_with_gate() throws Exception {
        var pair = LicenseFixture.keyPair();
        var base = LicenseFixture.payload();
        var futureStart = new LicensePayload(1, base.licenseId(), base.binding(), base.scope(),
                new LicensePayload.Terms(100, 550, 700L, null));
        var futureIssue = new LicensePayload(1, base.licenseId(), base.binding(), base.scope(),
                new LicensePayload.Terms(550, 100, null, null));
        var properties = properties(pair);
        properties.setFiles(List.of(file(pair, futureStart, "future"), file(pair, futureIssue, "issue"),
                file(pair, base, "permanent")));
        var gate = RuntimeLicenseLoader.load(properties, clock);
        assertThat(gate.report().entries()).extracting(RuntimeLicenseReport.Entry::state)
                .containsExactly("not_yet_valid", "not_yet_valid", "valid");
        assertThat(gate.isEnabled("ee.test.view")).isTrue();
        clock.now = Instant.ofEpochSecond(550);
        assertThat(gate.report().entries()).extracting(RuntimeLicenseReport.Entry::state)
                .containsOnly("valid");
        assertThat(gate.report().edition()).isEqualTo(gate.edition()).isEqualTo("ee");
    }

    @Test
    void broken_keys_and_unknown_key_do_not_disclose_key_material_or_file_details() throws Exception {
        var pair = LicenseFixture.keyPair();
        var properties = properties(pair);
        Path badKey = temp.resolve("secret-key.pem");
        Files.writeString(badKey, "invalid-key-secret");
        properties.setPublicKeys(Map.of("unknown", temp.resolve("public.pem"), "broken", badKey));
        properties.setFiles(List.of(file(pair, LicenseFixture.payload(), "unknown-key")));
        var report = RuntimeLicenseLoader.load(properties, clock).report();
        assertThat(report.configuration().trustedPublicKeys()).isEqualTo(1);
        assertThat(report.configuration().rejectedPublicKeys()).isEqualTo(1);
        assertThat(report.entries().get(0).state()).isEqualTo("invalid");
        assertThat(new ObjectMapper().writeValueAsString(report))
                .doesNotContain("invalid-key-secret", "secret-key", "unknown-key", "unknown", temp.toString());
    }

    @Test
    void invalid_binding_and_oversized_configuration_fail_closed_without_echoing_input() throws Exception {
        var properties = new RuntimeLicenseProperties();
        properties.setCustomerId("bad-sensitive-config");
        properties.setInstallationId("bad-project");
        var report = RuntimeLicenseLoader.load(properties, clock).report();
        assertThat(report.configuration().binding()).isNull();
        assertThat(report.entries()).isEmpty();
        assertThat(new ObjectMapper().writeValueAsString(report)).doesNotContain("bad-sensitive-config", "bad-project");
        properties.setCustomerId("42");
        properties.setInstallationId(LicenseFixture.PROJECT);
        properties.setFiles(Collections.nCopies(101, temp.resolve("not-read")));
        report = RuntimeLicenseLoader.load(properties, clock).report();
        assertThat(report.configuration().filesConfigurationValid()).isFalse();
        assertThat(report.entries()).isEmpty();
    }

    private RuntimeLicenseProperties properties(KeyPair pair) throws Exception {
        Path key = temp.resolve("public.pem");
        Files.writeString(key, Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
        var properties = new RuntimeLicenseProperties();
        properties.setCustomerId("42");
        properties.setInstallationId(LicenseFixture.PROJECT);
        properties.setPublicKeys(Map.of("key-1", key));
        return properties;
    }

    private Path file(KeyPair pair, LicensePayload payload, String name) throws Exception {
        Path file = temp.resolve(name + ".json");
        Files.write(file, LicenseFixture.sign(LicenseCodec.payload(payload), pair));
        return file;
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.ofEpochSecond(500);
        @Override
        public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override
        public Clock withZone(ZoneId zone) { return this; }
        @Override
        public Instant instant() { return now; }
    }
}
