package com.mdframe.forge.plugin.generator.service;

import com.mdframe.forge.plugin.generator.service.lowcode.runtime.MySqlRuntimeDatabaseDialect;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.OracleRuntimeDatabaseDialect;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.PostgreSqlRuntimeDatabaseDialect;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.SqlServerRuntimeDatabaseDialect;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicCrudJoinQueryCompilerTest {

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void compilesAggregatedChildQueryPlanWithTenantAndLogicDeleteFilters() {
        TenantContextHolder.setTenantId(1L);
        DynamicCrudJoinQueryCompiler compiler = compiler();
        List<DynamicCrudRepository.JoinField> fields = List.of(
            new DynamicCrudRepository.JoinField("id", "t0", "id"),
            new DynamicCrudRepository.JoinField("itemName", "t1", "item_name")
        );
        List<DynamicCrudRepository.JoinSpec> joins = List.of(
            new DynamicCrudRepository.JoinSpec("test_detail", "t1", "order_id", "id")
        );

        DynamicCrudJoinQueryCompiler.QueryPlan plan = compiler.resolveListPlan(
            "test_order", fields, joins, true);

        assertTrue(plan.distinctMainRows());
        assertTrue(plan.fromClause().contains("LEFT JOIN (SELECT `order_id`, "
            + "GROUP_CONCAT(`item_name` ORDER BY `id` SEPARATOR '、') AS `item_name`"));
        assertTrue(plan.fromClause().contains("`tenant_id` = :tenantId"));
        assertTrue(plan.fromClause().contains("`del_flag` = :logicActiveValue"));
        assertTrue(plan.fromClause().contains("GROUP BY `order_id`) t1 ON t1.order_id = t0.id"));
        assertEquals("SELECT DISTINCT t0.id AS `id`, t1.item_name AS `itemName`",
            compiler.buildSelectClause(fields, true));
    }

    @Test
    void preservesRawJoinPlanWhenChildAggregationIsDisabled() {
        TenantContextHolder.setTenantId(1L);
        DynamicCrudJoinQueryCompiler.QueryPlan plan = compiler().resolveListPlan(
            "test_order",
            List.of(new DynamicCrudRepository.JoinField("itemName", "t1", "item_name")),
            List.of(new DynamicCrudRepository.JoinSpec("test_detail", "t1", "order_id", "id")),
            false
        );

        assertFalse(plan.distinctMainRows());
        assertEquals("FROM test_order t0 LEFT JOIN test_detail t1 ON t1.order_id = t0.id"
            + " AND t1.tenant_id = :tenantId AND t1.del_flag = :logicActiveValue", plan.fromClause());
    }

    @Test
    void rejectsUnsafeResultAliasBeforeSqlCompilation() {
        DynamicCrudJoinQueryCompiler compiler = compiler();

        assertThrows(BusinessException.class, () -> compiler.validate(
            "test_order",
            List.of(new DynamicCrudRepository.JoinField("name, password", "t0", "name")),
            List.of()
        ));
    }

    @Test
    void delegatesTextAggregationSyntaxToDatabaseDialectStrategy() {
        assertEquals("GROUP_CONCAT(`name` ORDER BY `id` SEPARATOR '、')",
            new MySqlRuntimeDatabaseDialect().stringAggregate("`name`", "`id`", "、"));
        assertEquals("string_agg(\"name\"::text, '、' ORDER BY \"id\")",
            new PostgreSqlRuntimeDatabaseDialect().stringAggregate("\"name\"", "\"id\"", "、"));
        assertEquals("LISTAGG(\"name\", '、') WITHIN GROUP (ORDER BY \"id\")",
            new OracleRuntimeDatabaseDialect().stringAggregate("\"name\"", "\"id\"", "、"));
        assertEquals("STRING_AGG(CAST([name] AS nvarchar(max)), '、') WITHIN GROUP (ORDER BY [id])",
            new SqlServerRuntimeDatabaseDialect().stringAggregate("[name]", "[id]", "、"));
    }

    private DynamicCrudJoinQueryCompiler compiler() {
        return new DynamicCrudJoinQueryCompiler(
            table -> {
            },
            identifier -> {
            },
            table -> Set.of("id", "order_id", "item_name", "tenant_id", "del_flag"),
            table -> "test_detail".equals(table),
            MySqlRuntimeDatabaseDialect::new,
            () -> true,
            () -> "tenant_id",
            () -> "del_flag"
        );
    }
}
