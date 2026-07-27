package com.mdframe.forge.plugin.system.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientSecretCodecTest {

    private final ClientSecretCodec codec = new ClientSecretCodec();

    @Test
    void shouldEncodeAndMatchBcryptSecret() {
        String rawSecret = "test-client-secret-123456";

        String encoded = codec.encode(rawSecret);

        assertThat(encoded).startsWith("{bcrypt}");
        assertThat(encoded).doesNotContain(rawSecret);
        assertThat(codec.verify(rawSecret, encoded, true))
                .isEqualTo(new ClientSecretCodec.MatchResult(true, false));
        assertThat(codec.verify(rawSecret + "-wrong", encoded, true).matched()).isFalse();
    }

    @Test
    void shouldMatchLegacyPlaintextWithoutTreatingItAsEncoded() {
        String legacySecret = "legacy-client-secret-123456";

        assertThat(codec.verify(legacySecret, legacySecret, true))
                .isEqualTo(new ClientSecretCodec.MatchResult(true, true));
        assertThat(codec.verify("wrong-client-secret", legacySecret, true).matched()).isFalse();
        assertThat(codec.verify(legacySecret, legacySecret, false).matched()).isFalse();
    }

    @Test
    void shouldFailClosedForMalformedOrUnknownEncodedValues() {
        String encoded = codec.encode("test-client-secret-123456");
        String bcryptHash = encoded.substring(ClientSecretCodec.BCRYPT_PREFIX.length());
        String excessiveCost = ClientSecretCodec.BCRYPT_PREFIX
                + bcryptHash.substring(0, 4) + "31" + bcryptHash.substring(6);
        String unsupportedVersion = ClientSecretCodec.BCRYPT_PREFIX
                + bcryptHash.substring(0, 2) + "x" + bcryptHash.substring(3);

        assertThat(codec.verify("test-client-secret-123456", "{bcrypt}broken", true).matched()).isFalse();
        assertThat(codec.verify("test-client-secret-123456", excessiveCost, true).matched()).isFalse();
        assertThat(codec.verify("test-client-secret-123456", unsupportedVersion, true).matched()).isFalse();
        assertThat(codec.verify("test-client-secret-123456", "{unknown}stored-value", true).matched()).isFalse();
        assertThat(codec.verify(null, "legacy-client-secret-123456", true).matched()).isFalse();
    }

    @Test
    void shouldRejectSecretsOutsideLengthContract() {
        assertThatThrownBy(() -> codec.encode("short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("16");
        assertThatThrownBy(() -> codec.encode("x".repeat(73)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("72");
        assertThatThrownBy(() -> codec.encode("密".repeat(25)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UTF-8");

        String boundarySecret = "x".repeat(72);
        String encoded = codec.encode(boundarySecret);
        assertThat(codec.verify(boundarySecret + "y", encoded, true).matched()).isFalse();
    }
}
