package com.mdframe.forge.plugin.capability.identity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationFrameworkIsolationTest {

    @Test
    void shouldNotDependOnAuthorizationServerFrameworks() {
        ClassLoader classLoader = getClass().getClassLoader();
        assertThat(classLoader.getResource(
                "org/springframework/security/oauth2/server/authorization/config/annotation/"
                        + "web/configurers/OAuth2AuthorizationServerConfigurer.class"))
                .isNull();
        assertThat(classLoader.getResource("cn/dev33/satoken/oauth2/SaOAuth2Manager.class"))
                .isNull();
    }
}
