package com.mdframe.forge.plugin.capability.builtin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.capability.model.CapabilityBehavior;
import com.mdframe.forge.plugin.capability.model.CapabilityDefinition;
import com.mdframe.forge.plugin.capability.model.CapabilityQuery;
import com.mdframe.forge.plugin.capability.model.CapabilityRiskLevel;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.spi.CapabilitySource;

import java.util.Collection;
import java.util.List;

public final class PingCapabilitySource implements CapabilitySource {

    public static final String CAPABILITY_CODE = "capability.ping";
    public static final String VERSION = "1.0.0";

    private final CapabilityDefinition definition;

    public PingCapabilitySource(ObjectMapper objectMapper) {
        ObjectNode inputSchema = objectMapper.createObjectNode();
        inputSchema.put("$schema", CapabilitySchemaValidator.DRAFT_2020_12);
        inputSchema.put("type", "object");
        inputSchema.set("properties", objectMapper.createObjectNode());
        inputSchema.set("required", objectMapper.createArrayNode());
        inputSchema.put("additionalProperties", false);

        ObjectNode outputSchema = objectMapper.createObjectNode();
        outputSchema.put("$schema", CapabilitySchemaValidator.DRAFT_2020_12);
        outputSchema.put("type", "object");
        outputSchema.put("additionalProperties", false);
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("status", stringSchema(objectMapper));
        properties.set("requestId", stringSchema(objectMapper));
        properties.set("serverTime", stringSchema(objectMapper));
        outputSchema.set("properties", properties);
        ArrayNode required = objectMapper.createArrayNode();
        required.add("status").add("requestId").add("serverTime");
        outputSchema.set("required", required);

        this.definition = new CapabilityDefinition(
                CAPABILITY_CODE,
                CAPABILITY_CODE,
                VERSION,
                CapabilityBehavior.READ_ONLY,
                CapabilityRiskLevel.LOW,
                "检查 Forge AI 能力服务是否可用，不访问业务数据或外部系统",
                inputSchema,
                outputSchema);
    }

    @Override
    public Collection<CapabilityDefinition> load(CapabilityQuery query) {
        return List.of(definition);
    }

    private ObjectNode stringSchema(ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "string");
        schema.put("minLength", 1);
        return schema;
    }
}
