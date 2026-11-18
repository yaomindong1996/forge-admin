package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SQL Server runtime dialect contracts")
class SqlServerRuntimeDatabaseDialectTest {

    private final SqlServerRuntimeDatabaseDialect dialect = new SqlServerRuntimeDatabaseDialect();

    @Test
    @DisplayName("supports SQLServer aliases used by admin UI and JDBC")
    void supportsAliases() {
        assertTrue(dialect.supports("SQLServer"));
        assertTrue(dialect.supports("sqlserver"));
        assertTrue(dialect.supports("mssql"));
        assertTrue(dialect.supports("Microsoft SQL Server"));
        assertFalse(dialect.supports("mysql"));
    }

    @Test
    @DisplayName("maps lowcode types to SQL Server sql types")
    void resolveSqlTypes() {
        assertEquals("datetime2", dialect.resolveSqlType("datetime", 0, null));
        assertEquals("tinyint", dialect.resolveSqlType("tinyint", 0, null));
        assertEquals("nvarchar(max)", dialect.resolveSqlType("json", 0, null));
        assertEquals("nvarchar(max)", dialect.resolveSqlType("longtext", 0, null));
        assertEquals("nvarchar(64)", dialect.resolveSqlType("varchar", 64, null));
        assertEquals("decimal(18,2)", dialect.resolveSqlType("decimal", 0, "18,2"));
    }

    @Test
    @DisplayName("identifiers use bracket quoting")
    void quoteIdentifiers() {
        assertEquals("[biz_order]", dialect.quote("biz_order"));
        assertEquals("[odd]]name]", dialect.quote("odd]name"));
    }

    @Test
    @DisplayName("paginate uses OFFSET FETCH")
    void paginate() {
        assertEquals("SELECT 1 OFFSET 10 ROWS FETCH NEXT 20 ROWS ONLY",
                dialect.paginate("SELECT 1", 10, 20));
    }

    @Test
    @DisplayName("string aggregate uses STRING_AGG")
    void stringAggregate() {
        assertEquals(
                "STRING_AGG(CAST([name] AS nvarchar(max)), '、') WITHIN GROUP (ORDER BY [id])",
                dialect.stringAggregate("[name]", "[id]", "、"));
    }

    @Test
    @DisplayName("boolean switch default becomes numeric 1 for tinyint")
    void booleanDefaultUsesNumeric() {
        String definition = dialect.columnDefinition(new RuntimeDatabaseDialect.DdlColumn(
                "field_switch",
                "tinyint",
                true,
                true,
                null,
                "开关",
                false
        ));
        assertTrue(definition.contains("DEFAULT 1"), definition);
        assertFalse(definition.contains("DEFAULT N'true'"), definition);
        assertFalse(definition.contains("IDENTITY"), definition);
    }

    @Test
    @DisplayName("create table + index after statements")
    void createTableSnapshot() {
        var id = new RuntimeDatabaseDialect.DdlColumn("id", "bigint", true, null, null, "主键", false);
        var name = new RuntimeDatabaseDialect.DdlColumn("name", "nvarchar(64)", true, null, null, "名称", false);
        String create = dialect.createTableSql("biz_order", List.of(
                dialect.columnDefinition(id),
                dialect.columnDefinition(name),
                dialect.primaryKeyConstraint("biz_order", "id")
        ), "业务订单");
        assertTrue(create.startsWith("CREATE TABLE [biz_order]"), create);
        assertTrue(create.contains("[name] nvarchar(64)"), create);

        List<String> after = dialect.afterCreateTableSql(
                "biz_order",
                "业务订单",
                List.of(id, name),
                List.of(new RuntimeDatabaseDialect.IndexDefinition("idx_biz_order_name", List.of("name"), false))
        );
        assertTrue(after.stream().anyMatch(sql -> sql.contains("CREATE INDEX [idx_biz_order_name] ON [biz_order]")),
                after.toString());
    }

    @Test
    @DisplayName("factory resolves SQLServer")
    void factoryResolves() {
        RuntimeDatabaseDialectFactory factory = new RuntimeDatabaseDialectFactory(List.of(
                new MySqlRuntimeDatabaseDialect(),
                new PostgreSqlRuntimeDatabaseDialect(),
                new SqlServerRuntimeDatabaseDialect()
        ));
        assertEquals("SQLServer", factory.resolve("SQLServer").dbType());
    }
}
