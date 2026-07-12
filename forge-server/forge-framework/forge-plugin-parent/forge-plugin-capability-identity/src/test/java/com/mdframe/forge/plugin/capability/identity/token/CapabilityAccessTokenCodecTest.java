package com.mdframe.forge.plugin.capability.identity.token;

import com.mdframe.forge.plugin.capability.identity.config.CapabilityIdentityProperties;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapabilityAccessTokenCodecTest {

    @Test
    void shouldIssueOpaqueTokenAndStoreOnlyHmacMaterial() {
        CapabilityIdentityProperties properties = properties();
        CapabilityAccessTokenCodec codec = new CapabilityAccessTokenCodec(properties);

        IssuedCapabilityAccessToken first = codec.issue();
        IssuedCapabilityAccessToken second = codec.issue();

        assertThat(first.rawToken()).matches("^fdu_[A-Za-z0-9_-]{22}_[A-Za-z0-9_-]{43}$");
        assertThat(first.keyId()).hasSize(22);
        assertThat(first.prefix()).isEqualTo("fdu_" + first.keyId());
        assertThat(first.tokenHash()).matches("^[0-9a-f]{64}$");
        assertThat(first.tokenHash()).doesNotContain(first.rawToken());
        assertThat(first.rawToken()).isNotEqualTo(second.rawToken());
        assertThat(codec.extractKeyId(first.rawToken())).isEqualTo(first.keyId());
        assertThat(codec.matches(first.rawToken(), first.tokenHash())).isTrue();
        assertThat(codec.matches(second.rawToken(), first.tokenHash())).isFalse();
    }

    @Test
    void shouldRejectMalformedTokenAndMissingPepper() {
        CapabilityIdentityProperties properties = properties();
        CapabilityAccessTokenCodec codec = new CapabilityAccessTokenCodec(properties);
        assertThat(codec.extractKeyId("fcp_not-an-access-token")).isNull();

        properties.setTokenPepper("short");
        assertThatThrownBy(codec::issue)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Pepper");
    }

    private CapabilityIdentityProperties properties() {
        CapabilityIdentityProperties properties = new CapabilityIdentityProperties();
        properties.setTokenPepper("identity-test-pepper-at-least-32-characters");
        return properties;
    }
}
