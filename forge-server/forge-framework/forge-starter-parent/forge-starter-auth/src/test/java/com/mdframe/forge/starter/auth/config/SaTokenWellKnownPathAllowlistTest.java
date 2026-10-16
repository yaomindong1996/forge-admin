package com.mdframe.forge.starter.auth.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SaTokenWellKnownPathAllowlistTest {

    @Test
    void shouldAllowOnlyOauthWellKnownMetadataPaths() {
        assertThat(SaTokenWellKnownPathAllowlist.isOauthWellKnownMetadata(
                "/.well-known/oauth-protected-resource")).isTrue();
        assertThat(SaTokenWellKnownPathAllowlist.isOauthWellKnownMetadata(
                "/.well-known/oauth-authorization-server")).isTrue();
        assertThat(SaTokenWellKnownPathAllowlist.isOauthWellKnownMetadata(
                "/.well-known/openid-configuration")).isFalse();
        assertThat(SaTokenWellKnownPathAllowlist.isOauthWellKnownMetadata(
                "/oauth2/token")).isFalse();
        assertThat(SaTokenWellKnownPathAllowlist.isOauthWellKnownMetadata(
                "/../etc/passwd")).isFalse();
        assertThat(SaTokenWellKnownPathAllowlist.isOauthWellKnownMetadata(null)).isFalse();
    }
}
