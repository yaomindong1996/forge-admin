package com.mdframe.forge.plugin.capability.builtin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.capability.model.CapabilityDefinition;
import com.mdframe.forge.plugin.capability.model.CapabilityInvocation;
import com.mdframe.forge.plugin.capability.model.CapabilityResult;
import com.mdframe.forge.plugin.capability.spi.CapabilityExecutor;

import java.time.Clock;
import java.time.Instant;

public final class PingCapabilityExecutor implements CapabilityExecutor {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PingCapabilityExecutor(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public boolean supports(CapabilityDefinition definition) {
        return PingCapabilitySource.CAPABILITY_CODE.equals(definition.capabilityCode())
                && PingCapabilitySource.VERSION.equals(definition.version());
    }

    @Override
    public CapabilityResult invoke(CapabilityDefinition definition, CapabilityInvocation invocation) {
        long startedAt = System.nanoTime();
        ObjectNode data = objectMapper.createObjectNode();
        data.put("status", "ok");
        data.put("requestId", invocation.requestId());
        data.put("serverTime", Instant.now(clock).toString());
        long durationMs = Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
        return CapabilityResult.success(invocation.requestId(), definition.capabilityCode(), data, durationMs);
    }
}
