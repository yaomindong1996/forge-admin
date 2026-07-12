package com.mdframe.forge.plugin.capability.controlplane.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

public final class CapabilitySchemaChecksum {

    private final ObjectMapper canonicalMapper;

    public CapabilitySchemaChecksum(ObjectMapper objectMapper) {
        this.canonicalMapper = objectMapper.copy()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public String calculate(
            JsonNode inputSchema,
            JsonNode outputSchema,
            JsonNode policySnapshot,
            CapabilityVersionFingerprint fingerprint) {
        try {
            Map<String, Object> material = Map.of(
                    "input", canonicalMapper.convertValue(inputSchema, Object.class),
                    "output", canonicalMapper.convertValue(outputSchema, Object.class),
                    "metadata", canonicalMapper.convertValue(fingerprint, Object.class),
                    "policy", policySnapshot == null
                            ? Map.of()
                            : canonicalMapper.convertValue(policySnapshot, Object.class));
            byte[] canonical = canonicalMapper.writeValueAsString(material)
                    .getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        }
        catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("无法计算能力 Schema 校验和", exception);
        }
    }
}
