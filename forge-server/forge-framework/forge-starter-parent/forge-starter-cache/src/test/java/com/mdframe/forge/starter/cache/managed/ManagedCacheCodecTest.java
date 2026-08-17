package com.mdframe.forge.starter.cache.managed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mdframe.forge.starter.cache.managed.codec.ManagedCacheCodecs;
import com.mdframe.forge.starter.cache.managed.enums.CacheMode;
import com.mdframe.forge.starter.cache.managed.enums.CacheScope;
import com.mdframe.forge.starter.cache.managed.model.CacheControlAction;
import com.mdframe.forge.starter.cache.managed.model.CacheControlMessage;
import com.mdframe.forge.starter.cache.managed.model.CacheDefinition;
import com.mdframe.forge.starter.cache.managed.model.CachePolicyOverride;
import com.mdframe.forge.starter.cache.managed.model.ManagedCacheValue;
import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Test;
import org.redisson.client.codec.Codec;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ManagedCacheCodecTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void shouldRoundTripManagedValueWithConcreteBusinessType() throws Exception {
        CodecPayload payload = new CodecPayload("payload", LocalDateTime.of(2026, 8, 17, 12, 30));
        ManagedCacheValue value = new ManagedCacheValue(payload, false, 10L);

        ManagedCacheValue decoded = roundTripMapValue(ManagedCacheCodecs.values(objectMapper), value);

        assertInstanceOf(CodecPayload.class, decoded.value());
        assertEquals(value, decoded);
    }

    @Test
    void shouldRoundTripDefinitionPolicyAndControlMessage() throws Exception {
        CacheDefinition definition = definition();
        CachePolicyOverride policy = policy();
        CacheControlMessage message = new CacheControlMessage(
                CacheControlAction.APPLY, definition.applicationCode(), definition.cacheName(), policy);

        assertEquals(definition,
                roundTripMapValue(ManagedCacheCodecs.definitions(objectMapper), definition));
        assertEquals(policy,
                roundTripMapValue(ManagedCacheCodecs.policies(objectMapper), policy));
        assertEquals(message,
                roundTripValue(ManagedCacheCodecs.controlMessages(objectMapper), message));
    }

    @SuppressWarnings("unchecked")
    private <T> T roundTripMapValue(Codec codec, T value) throws Exception {
        ByteBuf encoded = codec.getMapValueEncoder().encode(value);
        try {
            return (T) codec.getMapValueDecoder().decode(encoded, null);
        } finally {
            encoded.release();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T roundTripValue(Codec codec, T value) throws Exception {
        ByteBuf encoded = codec.getValueEncoder().encode(value);
        try {
            return (T) codec.getValueDecoder().decode(encoded, null);
        } finally {
            encoded.release();
        }
    }

    private CacheDefinition definition() {
        return new CacheDefinition(
                "forge-admin", "test:cache", "Test cache", CacheMode.MULTI,
                List.of(CacheMode.LOCAL, CacheMode.REDIS, CacheMode.MULTI), CacheScope.TENANT,
                60, 300, 100, true, 10, getClass().getName());
    }

    private CachePolicyOverride policy() {
        return new CachePolicyOverride(
                "forge-admin", "test:cache", true, CacheMode.MULTI,
                60, 300, 100, true, 10, 2L);
    }

    private record CodecPayload(String name, LocalDateTime createdAt) {
    }
}
