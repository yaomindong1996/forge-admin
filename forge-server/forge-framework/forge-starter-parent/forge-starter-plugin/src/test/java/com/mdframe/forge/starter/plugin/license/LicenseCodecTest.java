package com.mdframe.forge.starter.plugin.license;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LicenseCodecTest {
    @TempDir
    Path temp;

    @Test
    void real_ed25519_round_trip_and_fixed_public_key_pem() throws Exception {
        var pair = LicenseFixture.keyPair();
        var document = LicenseFixture.document(pair);
        assertThat(LicenseCodec.verify(document, Map.of("key-1", pair.getPublic())))
                .isEqualTo(LicenseFixture.payload());
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + java.util.Base64.getEncoder().encodeToString(pair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";
        assertThat(LicenseCodec.publicKey(pem.getBytes(StandardCharsets.US_ASCII))).isEqualTo(pair.getPublic());
    }

    @Test
    void rejects_wrong_key_unknown_key_algorithm_and_tampered_payload() throws Exception {
        var pair = LicenseFixture.keyPair();
        var other = LicenseFixture.keyPair();
        byte[] document = LicenseFixture.document(pair);
        assertThatThrownBy(() -> LicenseCodec.verify(document, Map.of())).hasMessageContaining("不受信任");
        assertThatThrownBy(() -> LicenseCodec.verify(document, Map.of("key-1", other.getPublic())))
                .hasMessageContaining("无效");
        String text = new String(document, StandardCharsets.UTF_8);
        assertThatThrownBy(() -> LicenseCodec.verify(text.replace("Ed25519", "none").getBytes(),
                Map.of("key-1", pair.getPublic()))).hasMessageContaining("非法许可证封装");
        String original = LicenseCodec.encode(LicenseCodec.payload(LicenseFixture.payload()));
        String tampered = LicenseCodec.encode(LicenseCodec.payload(LicenseFixture.payload()))
                .replaceFirst("^.", original.startsWith("A") ? "B" : "A");
        assertThatThrownBy(() -> LicenseCodec.verify(text.replace(original, tampered).getBytes(),
                Map.of("key-1", pair.getPublic()))).hasMessageContaining("无效");
    }

    @Test
    void strict_original_bytes_reject_duplicate_unknown_missing_fields_and_trailing_data() throws Exception {
        var pair = LicenseFixture.keyPair();
        String payload = new String(LicenseCodec.payload(LicenseFixture.payload()), StandardCharsets.UTF_8);
        for (String invalid : java.util.List.of(
                payload.replace("\"version\":1", "\"version\":\"1\""),
                payload.replace("\"version\":1", "\"version\":1.1"),
                payload.replace("\"issuedAt\":100", "\"issuedAt\":\"100\""),
                payload.replace("\"version\":1", "\"version\":1,\"version\":1"),
                payload.replace("\"version\":1", "\"extra\":1,\"version\":1"),
                payload.replace("\"maintenanceUntil\":200", "\"maintenanceUntil\":200,\"notBefore\":100"),
                payload.replace(",\"maintenanceUntil\":200", ""),
                payload + " {}")) {
            byte[] signed = LicenseFixture.sign(invalid.getBytes(StandardCharsets.UTF_8), pair);
            assertThatThrownBy(() -> LicenseCodec.verify(signed, Map.of("key-1", pair.getPublic())));
        }
        // 原始合法 JSON 即使有额外空格，原字节签名仍有效，不能重序列化后校验。
        byte[] spaced = LicenseFixture.sign(payload.replace("{", "{ ").getBytes(StandardCharsets.UTF_8), pair);
        assertThat(LicenseCodec.verify(spaced, Map.of("key-1", pair.getPublic())))
                .isEqualTo(LicenseFixture.payload());
    }

    @Test
    void rejects_size_limits_bad_scopes_and_symlink() throws Exception {
        assertThatThrownBy(() -> LicenseCodec.verify(new byte[LicenseCodec.MAX_DOCUMENT_BYTES + 1], Map.of()));
        assertThatThrownBy(() -> new LicensePayload.Scope(java.util.List.of("test-plugin"),
                java.util.List.of("ee.*")));
        assertThatThrownBy(() -> new LicensePayload.Binding("42", "1-1-1-1-1"));
        Path regular = temp.resolve("license.json");
        Files.write(regular, LicenseFixture.document(LicenseFixture.keyPair()));
        assertThat(LicenseCodec.readFile(regular, LicenseCodec.MAX_DOCUMENT_BYTES)).isNotEmpty();
        Path symlink = temp.resolve("link");
        Files.createSymbolicLink(symlink, regular);
        assertThatThrownBy(() -> LicenseCodec.readFile(symlink, LicenseCodec.MAX_DOCUMENT_BYTES));
        assertThatThrownBy(() -> LicenseCodec.readFile(regular, 10));
    }
}
