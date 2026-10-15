package com.mdframe.forge.starter.auth.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorAnonymousSurfaceContractTest {

    @Test
    void saTokenShouldOnlyAnonymizeHealthEndpoint() throws IOException {
        String config = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/auth/config/SaTokenConfig.java"));
        assertThat(config)
                .contains("\"/actuator/health\"")
                .contains("\"/health\"")
                .contains(".excludePathPatterns(anonymousAuthPaths)")
                .contains(".notMatch(anonymousAuthPaths)")
                .contains("StpUtil.checkLogin()")
                .contains("LoginSessionRenewal.renewIfHalfwayExpired()")
                .doesNotContain("\"/actuator/**\"")
                .doesNotContain("/doc.html")
                .doesNotContain("/swagger-ui/**")
                .doesNotContain("/v3/api-docs/**")
                .doesNotContain("/webjars/**");
    }
}
