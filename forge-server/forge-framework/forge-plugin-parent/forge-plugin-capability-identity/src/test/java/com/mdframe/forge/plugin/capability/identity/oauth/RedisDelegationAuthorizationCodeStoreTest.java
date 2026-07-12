package com.mdframe.forge.plugin.capability.identity.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.identity.config.CapabilityIdentityProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisDelegationAuthorizationCodeStoreTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldUseHmacRedisKeyAndAtomicGetDeleteScript() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        CapabilityIdentityProperties properties = new CapabilityIdentityProperties();
        properties.setAuthorizationCodePepper("authorization-code-test-pepper-32-characters");
        properties.setAuthorizationCodeTtl(Duration.ofMinutes(2));
        ObjectMapper objectMapper = new ObjectMapper();
        RedisDelegationAuthorizationCodeStore store =
                new RedisDelegationAuthorizationCodeStore(redisTemplate, objectMapper, properties);
        DelegationAuthorizationCode payload = new DelegationAuthorizationCode(
                1L, "desktop_agent", 1, 2L, 3L, 4L, 5L,
                "http://127.0.0.1/callback", "http://localhost:8580/mcp",
                Set.of("capability:invoke:capability.ping"), "challenge");

        String rawCode = store.issue(payload);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                keyCaptor.capture(), valueCaptor.capture(), eqDuration(Duration.ofMinutes(2)));
        assertThat(rawCode).matches("^fdc_[A-Za-z0-9_-]{43}$");
        assertThat(keyCaptor.getValue()).doesNotContain(rawCode);
        assertThat(valueCaptor.getValue()).doesNotContain(rawCode);

        when(redisTemplate.execute(
                any(RedisScript.class), anyList()))
                .thenReturn(valueCaptor.getValue());
        assertThat(store.consume(rawCode)).isEqualTo(payload);
    }

    private Duration eqDuration(Duration expected) {
        return org.mockito.ArgumentMatchers.eq(expected);
    }
}
