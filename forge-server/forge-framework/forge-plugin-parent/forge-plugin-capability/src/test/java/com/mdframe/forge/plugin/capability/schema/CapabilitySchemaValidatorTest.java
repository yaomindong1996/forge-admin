package com.mdframe.forge.plugin.capability.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.exception.CapabilityDefinitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapabilitySchemaValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CapabilitySchemaValidator validator = new CapabilitySchemaValidator();

    @Test
    void shouldValidateRequiredEnumNestedObjectAndArray() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "required": ["mode", "items"],
                  "additionalProperties": false,
                  "properties": {
                    "mode": {"type": "string", "enum": ["safe", "fast"]},
                    "meta": {
                      "type": ["object", "null"],
                      "additionalProperties": false,
                      "properties": {"name": {"type": "string", "minLength": 1}}
                    },
                    "items": {"type": "array", "minItems": 1, "items": {"type": "integer"}}
                  }
                }
                """);

        validator.validateDefinition(schema);
        assertThatCode(() -> validator.validateInstance(schema,
                objectMapper.readTree("{\"mode\":\"safe\",\"meta\":null,\"items\":[1,2]}")))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> validator.validateInstance(schema,
                objectMapper.readTree("{\"mode\":\"unknown\",\"items\":[],\"tenantId\":2}")))
                .isInstanceOf(CapabilitySchemaValidationException.class);
    }

    @Test
    void shouldRejectUnsupportedKeywordsWithoutDroppingSemantics() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "$ref": "#/$defs/value"
                }
                """);

        assertThatThrownBy(() -> validator.validateDefinition(schema))
                .isInstanceOf(CapabilityDefinitionException.class)
                .hasMessageContaining("$ref");
    }

    @Test
    void shouldRejectKeywordsThatDoNotApplyToDeclaredType() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "string",
                  "properties": {"ignored": {"type": "string"}}
                }
                """);

        assertThatThrownBy(() -> validator.validateDefinition(schema))
                .isInstanceOf(CapabilityDefinitionException.class)
                .hasMessageContaining("不适用于类型 string")
                .hasMessageContaining("properties");
    }

    @Test
    void shouldRejectInconsistentBoundsAndEnumTypes() throws Exception {
        JsonNode invalidBounds = objectMapper.readTree("""
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "string",
                  "minLength": 5,
                  "maxLength": 2
                }
                """);
        JsonNode invalidEnum = objectMapper.readTree("""
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "integer",
                  "enum": [1, "2"]
                }
                """);

        assertThatThrownBy(() -> validator.validateDefinition(invalidBounds))
                .isInstanceOf(CapabilityDefinitionException.class)
                .hasMessageContaining("minLength 不能大于 maxLength");
        assertThatThrownBy(() -> validator.validateDefinition(invalidEnum))
                .isInstanceOf(CapabilityDefinitionException.class)
                .hasMessageContaining("enum 值类型");
    }
}
