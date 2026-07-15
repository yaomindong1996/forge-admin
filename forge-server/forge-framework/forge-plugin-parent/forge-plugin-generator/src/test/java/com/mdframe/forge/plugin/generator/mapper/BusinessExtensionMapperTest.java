package com.mdframe.forge.plugin.generator.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessExtensionMapper XML contract")
class BusinessExtensionMapperTest {

    @Test
    @DisplayName("extension queries are tenant scoped, logic-delete aware and deterministically ordered")
    void extensionQueriesAreGoverned() throws Exception {
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/BusinessExtensionMapper.xml"), StandardCharsets.UTF_8);

        assertTrue(xml.contains("e.tenant_id = #{tenantId}"));
        assertTrue(xml.contains("e.del_flag = '0'"));
        assertTrue(xml.contains("ORDER BY e.sort_order ASC, e.extension_code ASC, e.id ASC"));
        assertTrue(xml.contains("lock_token_hash"));
        assertFalse(xml.contains("lock_token ="));
        assertFalse(xml.contains("Class.forName"));
        assertFalse(xml.contains("getBean("));
    }
}
