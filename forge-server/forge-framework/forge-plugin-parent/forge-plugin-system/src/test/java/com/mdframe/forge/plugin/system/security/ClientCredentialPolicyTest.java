package com.mdframe.forge.plugin.system.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientCredentialPolicyTest {

    private final ClientSecretCodec codec = new ClientSecretCodec();
    private final ClientCredentialPolicy policy = new ClientCredentialPolicy(codec);

    @Test
    void shouldClearSecretForPublicClient() {
        assertThat(policy.resolveCreateSecret("none", "ignored-client-secret"))
                .isNull();
        assertThat(policy.resolveUpdateSecret("none", null, "{bcrypt}existing"))
                .isNull();
    }

    @Test
    void shouldRequireAndEncodeSecretForNewConfidentialClient() {
        assertThatThrownBy(() -> policy.resolveCreateSecret("client_secret", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能为空");

        String encoded = policy.resolveCreateSecret("client_secret", "new-client-secret-123456");
        assertThat(encoded).startsWith("{bcrypt}");
    }

    @Test
    void shouldPreserveOrRotateSecretWhenUpdatingConfidentialClient() {
        String existing = codec.encode("existing-client-secret-123456");

        assertThat(policy.resolveUpdateSecret("client_secret", "", existing)).isEqualTo(existing);
        assertThat(policy.resolveUpdateSecret("client_secret", "rotated-client-secret-123456", existing))
                .startsWith("{bcrypt}")
                .isNotEqualTo(existing);
    }

    @Test
    void shouldRejectUnknownAuthenticationMethod() {
        assertThatThrownBy(() -> policy.normalizeAuthMethod("password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("认证方式");
    }
}
