package com.mdframe.forge.plugin.mcp.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.exception.CapabilityDefinitionException;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpToolSchemaProjectorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final McpToolSchemaProjector projector = new McpToolSchemaProjector(objectMapper);
    private final CapabilitySchemaValidator validator = new CapabilitySchemaValidator();

    @Test
    void shouldProjectClosedObjectSchemaWithoutDraftMetadata() throws Exception {
        McpSchema.JsonSchema projected = projector.toInputSchema(validator.validateDefinition(objectMapper.readTree("""
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "required": ["mode"],
                  "additionalProperties": false,
                  "properties": {"mode": {"type": "string", "enum": ["safe"]}}
                }
                """)));

        assertThat(projected.type()).isEqualTo("object");
        assertThat(projected.required()).containsExactly("mode");
        assertThat(projected.additionalProperties()).isFalse();
        assertThat(projected.properties()).containsKey("mode");
    }

    @Test
    void shouldFailClosedWhenMcpProjectionCannotRepresentNullableType() throws Exception {
        assertThatThrownBy(() -> projector.toInputSchema(validator.validateDefinition(objectMapper.readTree("""
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {"name": {"type": ["string", "null"]}}
                }
                """))))
                .isInstanceOf(CapabilityDefinitionException.class)
                .hasMessageContaining("nullable");
    }
}
