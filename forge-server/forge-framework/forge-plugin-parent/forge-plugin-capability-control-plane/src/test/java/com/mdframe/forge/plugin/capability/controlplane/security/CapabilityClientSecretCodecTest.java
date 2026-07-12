package com.mdframe.forge.plugin.capability.controlplane.security;

import com.mdframe.forge.plugin.capability.controlplane.config.CapabilityControlPlaneProperties;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapabilityClientSecretCodecTest {

    @Test
    void shouldFailClosedWithoutPepper() {
        CapabilityClientSecretCodec codec = new CapabilityClientSecretCodec(
                new CapabilityControlPlaneProperties());

        assertThatThrownBy(() -> codec.issue("client_a"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Pepper");
    }

    @Test
    void shouldIssueOneTimeSecretAndVerifyWithConstantHashContract() {
        CapabilityControlPlaneProperties properties = new CapabilityControlPlaneProperties();
        properties.setClientPepper("test-only-pepper-with-sufficient-length");
        CapabilityClientSecretCodec codec = new CapabilityClientSecretCodec(properties);

        IssuedClientSecret issued = codec.issue("client_a");

        assertThat(issued.rawSecret()).matches("fcp_[A-Za-z0-9_-]{22}_[A-Za-z0-9_-]{43}");
        assertThat(issued.keyId()).matches("[A-Za-z0-9_-]{22}");
        assertThat(issued.keyPrefix()).isEqualTo("fcp_" + issued.keyId());
        assertThat(issued.keyHash()).matches("[0-9a-f]{64}");
        assertThat(issued.keyHash()).doesNotContain(issued.rawSecret());
        assertThat(codec.extractKeyId(issued.rawSecret())).isEqualTo(issued.keyId());
        assertThat(codec.matches(issued.rawSecret(), issued.keyHash())).isTrue();
        assertThat(codec.matches(issued.rawSecret() + "x", issued.keyHash())).isFalse();
        assertThat(codec.extractKeyId("invalid-secret")).isNull();
    }
}
