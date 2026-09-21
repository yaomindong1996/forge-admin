package com.mdframe.forge.plugin.generator.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataAuditMapperContractTest {

    @Test
    void adminQueryUsesAuthorizedObjectsAndBusinessKeywords() throws IOException {
        String query = statement(resource("mapper/DataAuditEventMapper.xml"), "select", "selectEventPage");
        String where = statement(resource("mapper/DataAuditEventMapper.xml"), "sql", "Query_Where");

        assertTrue(where.contains("collection=\"objectIds\""));
        assertTrue(where.contains("e.record_label LIKE CONCAT('%', #{query.recordKeyword}, '%')"));
        assertTrue(where.contains("e.record_id LIKE CONCAT('%', #{query.recordKeyword}, '%')"));
        assertTrue(where.contains("f.field_code = #{query.fieldCode}"));
        assertTrue(where.contains("f.column_name = #{query.fieldCode}"));
        assertTrue(where.contains("f.field_label LIKE CONCAT('%', #{query.fieldKeyword}, '%')"));
        assertTrue(where.contains("f.field_code LIKE CONCAT('%', #{query.fieldKeyword}, '%')"));
        assertTrue(query.contains("ORDER BY e.occurred_at DESC, e.id DESC"));
    }

    @Test
    void policyIndexIncludesStoppedPoliciesAndDetailVisibility() throws IOException {
        String xml = resource("mapper/DataAuditPolicyMapper.xml");
        String configured = statement(xml, "select", "selectConfigured");

        assertTrue(xml.contains("show_in_detail"));
        assertTrue(configured.contains("tenant_id = #{tenantId}"));
        assertTrue(configured.contains("del_flag = 0"));
        assertFalse(configured.contains("enabled = 1"));
    }

    @Test
    void applicationPageOptionsAreRestrictedToAuthorizedObjects() throws IOException {
        String xml = resource("mapper/BusinessApplicationObjectMapper.xml");
        String query = statement(xml, "select", "selectApplicationIdsByObjectIds");

        assertTrue(query.contains("collection=\"objectIds\""));
        assertTrue(query.contains("ao.tenant_id = #{tenantId}"));
        assertTrue(query.contains("ao.del_flag = '0'"));
        assertTrue(query.contains("a.del_flag = '0'"));
    }

    private String resource(String path) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "找不到 Mapper XML: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String statement(String xml, String tag, String id) {
        String opening = "<" + tag + " id=\"" + id + "\"";
        int start = xml.indexOf(opening);
        int end = xml.indexOf("</" + tag + ">", start);
        assertTrue(start >= 0 && end > start, "找不到 Mapper statement: " + id);
        return xml.substring(start, end);
    }
}
