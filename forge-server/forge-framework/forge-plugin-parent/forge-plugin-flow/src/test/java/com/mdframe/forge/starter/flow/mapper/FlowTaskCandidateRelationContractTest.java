package com.mdframe.forge.starter.flow.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowTaskCandidateRelationContractTest {

    private static final Path MAPPER = Path.of("src/main/resources/mapper/FlowTaskMapper.xml");
    private static final Path CANDIDATE_MAPPER = Path.of("src/main/resources/mapper/FlowTaskCandidateMapper.xml");
    private static final Path MIGRATION = Path.of(
            "../../../db/mysql/migration/V1.0.147__add_flow_task_candidate_relation.sql");

    @Test
    void candidateQueriesMustUseTenantScopedIndexedRelationsWithCsvFallback() throws IOException {
        String xml = Files.readString(MAPPER);
        assertTrue(xml.contains("sys_flow_task_candidate"));
        assertTrue(xml.contains("tc.tenant_id = t.tenant_id"));
        assertTrue(xml.contains("tc.candidate_type = 'USER'"));
        assertTrue(xml.contains("tc.status = 1"));
        assertTrue(xml.contains("CsvTokenContains"),
                "CSV fallback must use portable CONCAT+LIKE fragment");
        assertFalse(xml.contains("FIND_IN_SET"), "must not use MySQL-only FIND_IN_SET");
        assertFalse(xml.contains("string_to_array"), "must not use PG-only string_to_array");
        assertFalse(xml.contains("_databaseId"), "FlowTaskMapper CSV predicates must not branch by databaseId");
    }

    @Test
    void candidateRelationWritesMustStayDialectFree() throws IOException {
        String xml = Files.readString(CANDIDATE_MAPPER);
        assertTrue(xml.contains("<insert id=\"insertPlain\""));
        assertTrue(xml.contains("<update id=\"updateUpsertByUniqueKey\""));
        assertFalse(xml.contains("ON DUPLICATE KEY"), "upsert dialect belongs in DuplicateKeyActions");
        assertFalse(xml.contains("ON CONFLICT"), "upsert dialect belongs in DuplicateKeyActions");
        assertFalse(xml.contains("_databaseId"));
        assertTrue(xml.contains("<update id=\"activate\""));
        assertTrue(xml.contains("<update id=\"deactivate\""));
        assertTrue(xml.contains("tenant_id = #{tenantId}"));
    }

    @Test
    void dynamicSignRelationsMustResolveTenantMemberNames() throws IOException {
        String xml = Files.readString(CANDIDATE_MAPPER);
        int start = xml.indexOf("<select id=\"selectDynamicSignRelations\"");
        String select = xml.substring(start, xml.indexOf("</select>", start));
        assertTrue(select.contains("AS targetUserName"));
        assertTrue(select.contains("LEFT JOIN sys_user target_user"));
        assertTrue(select.contains("CONCAT(target_user.id, '') = NULLIF(tc.candidate_value, '')"),
                "user join must stay portable (no MySQL REGEXP / UNSIGNED)");
        assertFalse(select.contains("REGEXP"), "must not use MySQL-only REGEXP");
        assertFalse(select.contains("UNSIGNED"), "must not use MySQL-only UNSIGNED cast");
        assertTrue(select.contains("target_user.del_flag = 0"));
        assertTrue(select.contains("target_membership.tenant_id = tc.tenant_id"));
        assertTrue(select.contains("tc.tenant_id = #{tenantId}"));
        assertTrue(select.contains("LIMIT 50"));
        assertFalse(select.contains("${"));
    }

    @Test
    void migrationMustBackfillBothCandidateTypesIdempotently() throws IOException {
        String sql = Files.readString(MIGRATION);
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS"));
        assertTrue(sql.contains("uk_flow_task_candidate"));
        assertTrue(sql.contains("idx_flow_task_candidate_lookup"));
        assertTrue(sql.contains("INSERT IGNORE INTO `sys_flow_task_candidate`"));
        assertTrue(sql.contains("'USER'"));
        assertTrue(sql.contains("'GROUP'"));
        assertTrue(sql.contains("JSON_TABLE"));
    }
}
