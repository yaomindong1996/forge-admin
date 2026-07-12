package com.mdframe.forge.plugin.capability.identity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.config.CapabilityAutoConfiguration;
import com.mdframe.forge.plugin.capability.controlplane.config.CapabilityControlPlaneAutoConfiguration;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityClientMapper;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityGrantService;
import com.mdframe.forge.plugin.capability.identity.authorization.ForgeCapabilityAuthorizationPolicy;
import com.mdframe.forge.plugin.capability.identity.mapper.AiCapabilityAccessTokenMapper;
import com.mdframe.forge.plugin.capability.identity.mapper.AiCapabilityOAuthRedirectUriMapper;
import com.mdframe.forge.plugin.capability.spi.CapabilityAuthorizationPolicy;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CapabilityIdentityAutoConfigurationTest {

    @Test
    void shouldReplaceScopeOnlyPolicyWithForgeGovernancePolicy() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        CapabilityControlPlaneAutoConfiguration.class,
                        CapabilityIdentityAutoConfiguration.class,
                        CapabilityAutoConfiguration.class))
                .withPropertyValues(
                        "forge.capability.identity.enabled=true",
                        "forge.capability.client-pepper=client-pepper-1234567890",
                        "forge.capability.identity.token-pepper=token-pepper-123456789012345678901234567890",
                        "forge.capability.identity.authorization-code-pepper=code-pepper-123456789012345678901234567890")
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(AiCapabilityClientMapper.class, () -> mock(AiCapabilityClientMapper.class))
                .withBean(AiCapabilityAccessTokenMapper.class,
                        () -> mock(AiCapabilityAccessTokenMapper.class))
                .withBean(AiCapabilityOAuthRedirectUriMapper.class,
                        () -> mock(AiCapabilityOAuthRedirectUriMapper.class))
                .withBean(IUserLoadService.class, () -> mock(IUserLoadService.class))
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .withBean(CapabilityGrantService.class, () -> mock(CapabilityGrantService.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(CapabilityAuthorizationPolicy.class);
                    assertThat(context.getBean(CapabilityAuthorizationPolicy.class))
                            .isInstanceOf(ForgeCapabilityAuthorizationPolicy.class);
                });
    }
}
