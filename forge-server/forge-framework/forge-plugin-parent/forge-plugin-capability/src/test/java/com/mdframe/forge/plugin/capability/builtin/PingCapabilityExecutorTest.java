package com.mdframe.forge.plugin.capability.builtin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.capability.model.CapabilityInvocation;
import com.mdframe.forge.plugin.capability.model.CapabilityResult;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PingCapabilityExecutorTest {

    @Test
    void shouldReturnOnlyStableHealthFields() {
        ObjectMapper objectMapper = new ObjectMapper();
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T12:00:00Z"), ZoneOffset.UTC);
        PingCapabilitySource source = new PingCapabilitySource(objectMapper);
        PingCapabilityExecutor executor = new PingCapabilityExecutor(objectMapper, clock);
        CapabilityInvocation invocation = new CapabilityInvocation(
                "request-ping", "capability.ping", "1.0.0",
                new CapabilityCallerContext("test-client", 1L, null, null,
                        Set.of("capability:discover", "capability:invoke")),
                objectMapper.createObjectNode());

        CapabilityResult result = executor.invoke(source.load(null).iterator().next(), invocation);

        assertThat(result.data().fieldNames()).toIterable()
                .containsExactlyInAnyOrder("status", "requestId", "serverTime");
        assertThat(result.data().path("status").asText()).isEqualTo("ok");
        assertThat(result.data().path("requestId").asText()).isEqualTo("request-ping");
        assertThat(result.data().path("serverTime").asText()).isEqualTo("2026-07-11T12:00:00Z");
    }
}
