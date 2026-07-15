package com.mdframe.forge.plugin.generator.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessApplication Phase 5 persistence contract")
class BusinessApplicationPhaseFiveMapperTest {

    @Test
    @DisplayName("application versions expose no update statement")
    void immutableVersionMapper() throws Exception {
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/BusinessApplicationVersionMapper.xml"), StandardCharsets.UTF_8);

        assertTrue(xml.contains("ai_business_application_version"));
        assertTrue(xml.contains("tenant_id = #{tenantId}"));
        assertTrue(xml.contains("del_flag = '0'"));
        assertFalse(xml.contains("<update"));
        assertFalse(xml.contains("UPDATE ai_business_application_version"));
    }

    @Test
    @DisplayName("publish run claims created rows and records recoverable progress")
    void recoverableRunMapper() throws Exception {
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/BusinessApplicationPublishRunMapper.xml"), StandardCharsets.UTF_8);

        assertTrue(xml.contains("run_status = 'RUNNING'"));
        assertTrue(xml.contains("run_status = 'CREATED'"));
        assertTrue(xml.contains("attempt_count = attempt_count + 1"));
        assertTrue(xml.contains("FOR UPDATE"));
        assertTrue(xml.contains("idempotency_key = #{idempotencyKey}"));
    }
}
