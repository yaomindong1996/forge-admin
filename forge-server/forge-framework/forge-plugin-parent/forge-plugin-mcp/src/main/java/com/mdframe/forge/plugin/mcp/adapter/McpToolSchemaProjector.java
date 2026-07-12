package com.mdframe.forge.plugin.mcp.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.exception.CapabilityDefinitionException;
import com.mdframe.forge.plugin.capability.model.CapabilityErrorCode;
import com.mdframe.forge.plugin.capability.schema.ValidatedCapabilitySchema;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;

public final class McpToolSchemaProjector {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public McpToolSchemaProjector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public McpSchema.JsonSchema toInputSchema(ValidatedCapabilitySchema validatedSchema) {
        JsonNode schema = validatedSchema.schema();
        rejectNullableTypes(schema, "$");
        String type = schema.path("type").asText();
        Map<String, Object> properties = schema.has("properties")
                ? objectMapper.convertValue(schema.path("properties"), MAP_TYPE)
                : Map.of();
        List<String> required = schema.has("required")
                ? objectMapper.convertValue(schema.path("required"), new TypeReference<>() {
                })
                : List.of();
        Boolean additionalProperties = schema.has("additionalProperties")
                ? schema.path("additionalProperties").asBoolean()
                : null;
        return new McpSchema.JsonSchema(
                type, properties, required, additionalProperties, Map.of(), Map.of());
    }

    public Map<String, Object> toOutputSchema(ValidatedCapabilitySchema validatedSchema) {
        JsonNode schema = validatedSchema.schema();
        rejectNullableTypes(schema, "$");
        Map<String, Object> projected = objectMapper.convertValue(schema, MAP_TYPE);
        projected.remove("$schema");
        return Map.copyOf(projected);
    }

    private void rejectNullableTypes(JsonNode node, String path) {
        JsonNode type = node.get("type");
        if (type != null && type.isArray()) {
            throw new CapabilityDefinitionException(CapabilityErrorCode.SCHEMA_UNSUPPORTED,
                    "当前 MCP SDK 无法无损投影 nullable 类型，路径：" + path + ".type");
        }
        JsonNode properties = node.get("properties");
        if (properties != null && properties.isObject()) {
            properties.properties().forEach(entry ->
                    rejectNullableTypes(entry.getValue(), path + ".properties." + entry.getKey()));
        }
        JsonNode items = node.get("items");
        if (items != null && items.isObject()) {
            rejectNullableTypes(items, path + ".items");
        }
    }
}
