package com.mdframe.forge.starter.flow.mapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 模型排序 SQL 必须保持租户、逻辑删除和并发锁边界。 */
class FlowModelSortingContractTest {

    @Test
    void sortMapperUsesTenantLockAndStableOrdering() throws Exception {
        String mapper = Files.readString(Path.of("src/main/resources/mapper/FlowModelMapper.xml"));
        assertTrue(mapper.contains("id=\"selectByIdsForUpdate\""));
        assertTrue(mapper.contains("m.tenant_id = #{tenantId}"));
        assertTrue(mapper.contains("m.del_flag = 0"));
        assertTrue(mapper.contains("FOR UPDATE"));
        assertTrue(mapper.contains("m.sort_order ASC"));
        assertFalse(statement(mapper, "selectByIdsForUpdate")
                .matches("(?s).*\\bFOR\\s+UPDATE\\b.*\\bORDER\\s+BY\\b.*"),
                "row-lock query must not place ORDER BY after FOR UPDATE after SQL parser rewriting");
        String updateStatement = statement(mapper, "updateSortOrder");
        assertTrue(updateStatement.contains("last_update_by = #{lastUpdateBy}"),
                "model sorting must update the existing last_update_by column");
        assertFalse(updateStatement.matches("(?sm).*^\\s+update_by\\s*=\\s*#\\{lastUpdateBy}.*"),
                "model sorting must not reference the absent generic update_by column");
    }

    @Test
    void migrationAddsGuardedSortColumnAndIndex() throws Exception {
        String sql = Files.readString(Path.of("../../../db/migration/V1.0.151__add_flow_model_sort_order.sql"));
        assertTrue(sql.contains("information_schema.COLUMNS"));
        assertTrue(sql.contains("sort_order"));
        assertTrue(sql.contains("idx_flow_model_tenant_sort"));
    }

    @Test
    void sortPermissionMigrationIsIndependentAndRepeatable() throws Exception {
        String sql = Files.readString(Path.of("../../../db/migration/V1.0.152__add_flow_model_sort_permission.sql"));
        String controller = Files.readString(Path.of(
                "../../../forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowModelController.java"));
        assertTrue(sql.contains("flow:model:sort"));
        assertTrue(sql.contains("flow:model:sort:api"));
        assertTrue(sql.contains("NOT EXISTS"));
        assertTrue(sql.contains("/api/flow/model/sort"));
        assertTrue(controller.contains("@SaCheckPermission(\"flow:model:sort\")"));
        assertTrue(controller.contains("FlowModelSortDTO"));
    }

    private static String statement(String xml, String id) {
        int start = xml.indexOf("id=\"" + id + "\"");
        int end = xml.indexOf("</select>", start);
        return start >= 0 && end > start ? xml.substring(start, end) : "";
    }
}
