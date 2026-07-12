package com.mdframe.forge.plugin.capability.controlplane.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilitySchemaChecksumTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldIgnoreJsonObjectFieldOrder() throws Exception {
        CapabilitySchemaChecksum checksum = new CapabilitySchemaChecksum(objectMapper);

        String first = checksum.calculate(
                objectMapper.readTree("{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"},\"b\":{\"type\":\"integer\"}}}"),
                objectMapper.readTree("{\"type\":\"object\"}"), null, fingerprint());
        String second = checksum.calculate(
                objectMapper.readTree("{\"properties\":{\"b\":{\"type\":\"integer\"},\"a\":{\"type\":\"string\"}},\"type\":\"object\"}"),
                objectMapper.readTree("{\"type\":\"object\"}"), null, fingerprint());

        assertThat(first).isEqualTo(second).matches("[0-9a-f]{64}");
    }

    private CapabilityVersionFingerprint fingerprint() {
        return new CapabilityVersionFingerprint(
                "LOW_CODE_CRUD", "system.user", "1", "READ_ONLY", "LOW", "PRIVATE");
    }
}
