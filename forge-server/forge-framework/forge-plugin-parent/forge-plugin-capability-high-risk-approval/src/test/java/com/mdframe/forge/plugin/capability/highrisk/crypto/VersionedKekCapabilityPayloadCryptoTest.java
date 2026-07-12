package com.mdframe.forge.plugin.capability.highrisk.crypto;

import com.mdframe.forge.plugin.capability.highrisk.config.HighRiskApprovalProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VersionedKekCapabilityPayloadCryptoTest {

    @Test
    void shouldRoundTripPayload() {
        var crypto = crypto("v1", Map.of("v1", key(1)));
        byte[] aad = "tenant|approval".getBytes(StandardCharsets.UTF_8);

        EncryptedCapabilityPayload encrypted = crypto.encrypt("secret-value".getBytes(StandardCharsets.UTF_8), aad);

        assertThat(encrypted.keyId()).isEqualTo("v1");
        assertThat(new String(crypto.decrypt(encrypted, aad), StandardCharsets.UTF_8))
                .isEqualTo("secret-value");
        assertThat(encrypted.ciphertext()).doesNotContain("secret-value");
    }

    @Test
    void shouldRejectTamperedTag() {
        var crypto = crypto("v1", Map.of("v1", key(1)));
        byte[] aad = "aad".getBytes(StandardCharsets.UTF_8);
        EncryptedCapabilityPayload encrypted = crypto.encrypt("value".getBytes(StandardCharsets.UTF_8), aad);
        byte[] tag = Base64.getDecoder().decode(encrypted.authTag());
        tag[0] ^= 1;
        EncryptedCapabilityPayload tampered = new EncryptedCapabilityPayload(
                encrypted.keyId(), encrypted.wrappedDek(), encrypted.iv(),
                encrypted.ciphertext(), Base64.getEncoder().encodeToString(tag));

        assertThatThrownBy(() -> crypto.decrypt(tampered, aad))
                .hasMessage("APPROVAL_PAYLOAD_INVALID");
    }

    @Test
    void shouldRejectWrongAad() {
        var crypto = crypto("v1", Map.of("v1", key(1)));
        EncryptedCapabilityPayload encrypted = crypto.encrypt(
                "value".getBytes(StandardCharsets.UTF_8), "aad-1".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> crypto.decrypt(
                encrypted, "aad-2".getBytes(StandardCharsets.UTF_8)))
                .hasMessage("APPROVAL_PAYLOAD_INVALID");
    }

    @Test
    void shouldDecryptOldEnvelopeAfterActiveKeyRotation() {
        EncryptedCapabilityPayload old = crypto("v1", Map.of("v1", key(1)))
                .encrypt("old-value".getBytes(StandardCharsets.UTF_8), new byte[]{1});
        var rotated = crypto("v2", Map.of("v1", key(1), "v2", key(2)));

        assertThat(new String(rotated.decrypt(old, new byte[]{1}), StandardCharsets.UTF_8))
                .isEqualTo("old-value");
        assertThat(rotated.encrypt(new byte[]{2}, new byte[]{1}).keyId()).isEqualTo("v2");
    }

    @Test
    void shouldFailClosedForMissingOrInvalidKey() {
        assertThatThrownBy(() -> crypto("v1", Map.of()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> crypto("v1", Map.of("v1", Base64.getEncoder().encodeToString(new byte[16]))))
                .isInstanceOf(IllegalStateException.class);
    }

    private VersionedKekCapabilityPayloadCrypto crypto(String active, Map<String, String> keys) {
        HighRiskApprovalProperties properties = new HighRiskApprovalProperties();
        properties.getCrypto().setActiveKeyId(active);
        properties.getCrypto().setKeys(keys);
        return new VersionedKekCapabilityPayloadCrypto(properties);
    }

    private String key(int seed) {
        byte[] bytes = new byte[32];
        java.util.Arrays.fill(bytes, (byte) seed);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
