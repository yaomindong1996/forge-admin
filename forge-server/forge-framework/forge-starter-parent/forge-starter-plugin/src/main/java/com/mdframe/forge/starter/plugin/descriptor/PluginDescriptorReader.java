package com.mdframe.forge.starter.plugin.descriptor;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.LogicalType;

import java.io.IOException;

/** classpath 和源码包共享严格 JSON 边界，业务 ObjectMapper 的宽松配置不会影响交付协议。 */
public final class PluginDescriptorReader {
    private static final JsonMapper MAPPER = mapper();
    private static final int MAX_BYTES = 64 * 1024;

    private PluginDescriptorReader() {
    }

    public static PluginDescriptor read(byte[] bytes) {
        requireSize(bytes);
        try {
            return MAPPER.readValue(bytes, PluginDescriptor.class);
        } catch (IOException exception) {
            throw new IllegalArgumentException("插件描述 JSON 无法解析", exception);
        }
    }

    public static boolean identical(byte[] left, byte[] right) {
        requireSize(left);
        requireSize(right);
        try {
            JsonNode first = MAPPER.readTree(left);
            return first.equals(MAPPER.readTree(right));
        } catch (IOException exception) {
            throw new IllegalArgumentException("插件描述 JSON 无法解析", exception);
        }
    }

    private static void requireSize(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("插件描述为空或超过 64 KiB");
        }
    }

    private static JsonMapper mapper() {
        JsonMapper mapper = JsonMapper.builder().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        mapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
        return mapper;
    }
}
