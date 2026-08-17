package com.mdframe.forge.starter.cache.managed.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.cache.managed.model.CacheControlMessage;
import com.mdframe.forge.starter.cache.managed.model.CacheDefinition;
import com.mdframe.forge.starter.cache.managed.model.CachePolicyOverride;
import com.mdframe.forge.starter.cache.managed.model.ManagedCacheValue;
import org.redisson.client.codec.Codec;
import org.redisson.codec.TypedJsonJacksonCodec;

import java.util.Objects;

/**
 * Managed cache Redis structures use concrete envelope types instead of relying on global default typing.
 */
public final class ManagedCacheCodecs {

    private ManagedCacheCodecs() {
    }

    public static Codec values(ObjectMapper objectMapper) {
        return mapValues(ManagedCacheValue.class, objectMapper);
    }

    public static Codec definitions(ObjectMapper objectMapper) {
        return mapValues(CacheDefinition.class, objectMapper);
    }

    public static Codec policies(ObjectMapper objectMapper) {
        return mapValues(CachePolicyOverride.class, objectMapper);
    }

    public static Codec controlMessages(ObjectMapper objectMapper) {
        return new TypedJsonJacksonCodec(
                CacheControlMessage.class,
                Objects.requireNonNull(objectMapper, "objectMapper不能为空"));
    }

    private static Codec mapValues(Class<?> valueType, ObjectMapper objectMapper) {
        return new TypedJsonJacksonCodec(
                String.class,
                valueType,
                Objects.requireNonNull(objectMapper, "objectMapper不能为空"));
    }
}
