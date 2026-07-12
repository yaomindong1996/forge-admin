package com.mdframe.forge.plugin.capability.controlplane.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapability;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityVersion;
import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityPublishDTO;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityMapper;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityVersionMapper;
import com.mdframe.forge.plugin.capability.naming.CapabilityToolNameMapper;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CapabilityCatalogServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiCapabilityMapper capabilityMapper = mock(AiCapabilityMapper.class);
    private final AiCapabilityVersionMapper versionMapper = mock(AiCapabilityVersionMapper.class);
    private final CapabilityCatalogService service = new CapabilityCatalogService(
            capabilityMapper,
            versionMapper,
            new CapabilityToolNameMapper(),
            new CapabilitySchemaValidator(),
            objectMapper);

    @Test
    void shouldRejectChangingSourceSnapshotOfPublishedVersion() throws Exception {
        JsonNode inputSchema = schema();
        JsonNode outputSchema = schema();
        String checksum = new CapabilitySchemaChecksum(objectMapper)
                .calculate(inputSchema, outputSchema, null, fingerprint("system.user"));
        AiCapability capability = new AiCapability();
        capability.setId(10L);
        capability.setTenantId(1L);
        capability.setCapabilityCode("system.user.search");
        capability.setProtocolToolName("system.user.search");
        AiCapabilityVersion existingVersion = new AiCapabilityVersion();
        existingVersion.setSourceVersion("source-v1");
        existingVersion.setSourceType("LOW_CODE_CRUD");
        existingVersion.setSourceKey("system.user");
        existingVersion.setBehavior("READ_ONLY");
        existingVersion.setRiskLevel("LOW");
        existingVersion.setVisibility("PRIVATE");
        existingVersion.setSchemaChecksum(checksum);
        when(capabilityMapper.selectByCode(1L, "system.user.search")).thenReturn(capability);
        when(capabilityMapper.selectByToolName(1L, "system.user.search")).thenReturn(capability);
        when(versionMapper.selectVersion(1L, 10L, "1.0.0")).thenReturn(existingVersion);

        CapabilityPublishDTO dto = new CapabilityPublishDTO(
                "system.user.search",
                "system.user.search",
                "用户查询",
                "安全只读用户查询",
                "LOW_CODE_CRUD",
                "system.user",
                "source-v2",
                "1.0.0",
                "READ_ONLY",
                "LOW",
                "PRIVATE",
                inputSchema,
                outputSchema,
                null);

        assertThatThrownBy(() -> service.publish(1L, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可修改");
        verify(capabilityMapper, never()).updateById(capability);
        verify(versionMapper, never()).insert(
                org.mockito.ArgumentMatchers.any(AiCapabilityVersion.class));
    }

    @Test
    void shouldRejectChangingSourceKeyWithoutNewVersion() throws Exception {
        JsonNode inputSchema = schema();
        JsonNode outputSchema = schema();
        AiCapability capability = new AiCapability();
        capability.setId(10L);
        capability.setTenantId(1L);
        capability.setCapabilityCode("system.user.search");
        capability.setProtocolToolName("system.user.search");
        AiCapabilityVersion existingVersion = new AiCapabilityVersion();
        existingVersion.setSourceType("LOW_CODE_CRUD");
        existingVersion.setSourceKey("system.user");
        existingVersion.setSourceVersion("source-v1");
        existingVersion.setBehavior("READ_ONLY");
        existingVersion.setRiskLevel("LOW");
        existingVersion.setVisibility("PRIVATE");
        existingVersion.setSchemaChecksum(new CapabilitySchemaChecksum(objectMapper)
                .calculate(inputSchema, outputSchema, null, fingerprint("system.user")));
        when(capabilityMapper.selectByCode(1L, "system.user.search")).thenReturn(capability);
        when(capabilityMapper.selectByToolName(1L, "system.user.search")).thenReturn(capability);
        when(versionMapper.selectVersion(1L, 10L, "1.0.0")).thenReturn(existingVersion);
        CapabilityPublishDTO changed = dto(inputSchema, outputSchema, "system.employee", "source-v1");

        assertThatThrownBy(() -> service.publish(1L, changed))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可修改");
        verify(capabilityMapper, never()).updateById(capability);
    }

    private CapabilityPublishDTO dto(
            JsonNode inputSchema,
            JsonNode outputSchema,
            String sourceKey,
            String sourceVersion) {
        return new CapabilityPublishDTO(
                "system.user.search", "system.user.search", "用户查询", "安全只读用户查询",
                "LOW_CODE_CRUD", sourceKey, sourceVersion, "1.0.0",
                "READ_ONLY", "LOW", "PRIVATE", inputSchema, outputSchema, null);
    }

    private CapabilityVersionFingerprint fingerprint(String sourceKey) {
        return new CapabilityVersionFingerprint(
                "LOW_CODE_CRUD", sourceKey, "source-v1", "READ_ONLY", "LOW", "PRIVATE");
    }

    private JsonNode schema() throws Exception {
        return objectMapper.readTree("""
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {},
                  "additionalProperties": false
                }
                """);
    }
}
