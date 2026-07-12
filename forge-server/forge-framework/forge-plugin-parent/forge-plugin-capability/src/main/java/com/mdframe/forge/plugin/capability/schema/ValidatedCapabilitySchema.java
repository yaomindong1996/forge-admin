package com.mdframe.forge.plugin.capability.schema;

import com.fasterxml.jackson.databind.JsonNode;

public record ValidatedCapabilitySchema(JsonNode schema) {

    public ValidatedCapabilitySchema {
        schema = schema.deepCopy();
    }

    @Override
    public JsonNode schema() {
        return schema.deepCopy();
    }
}
