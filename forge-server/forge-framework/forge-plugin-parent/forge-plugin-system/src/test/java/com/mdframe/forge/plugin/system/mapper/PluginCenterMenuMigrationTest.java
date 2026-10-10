package com.mdframe.forge.plugin.system.mapper;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 执行真实增量 SQL，校验移动菜单不改变资源身份、访问边界和关联数据。 */
class PluginCenterMenuMigrationTest {
    private static final String MIGRATION = "V1.0.217__promote_plugin_center_to_top_level.sql";
    private Connection connection;
    private long menuId;

    @BeforeEach
    void prepare() throws Exception {
        var source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;NON_KEYWORDS=VALUE;DATABASE_TO_LOWER=TRUE");
        connection = source.getConnection();
        try (var fixture = getClass().getResourceAsStream("/plugin-task-migration-fixture.sql")) {
            RunScript.execute(connection, new InputStreamReader(fixture, StandardCharsets.UTF_8));
        }
        run("V1.0.210__add_plugin_center.sql");
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT id FROM sys_resource WHERE perms = 'system:plugin:view'")) {
            assertThat(result.next()).isTrue();
            menuId = result.getLong(1);
        }
        execute("CREATE TABLE sys_role_resource(tenant_id BIGINT, role_id BIGINT, resource_id BIGINT)");
        execute("INSERT INTO sys_role_resource VALUES(1, 1, " + menuId + ")");
    }

    @AfterEach
    void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void promotes_same_menu_without_changing_access_children_or_role_links() throws Exception {
        String protectedFields = "SELECT id, tenant_id, resource_type, sort, path, component, perms,"
                + " min_user_type, is_public, menu_status, visible, client_code, del_flag"
                + " FROM sys_resource ORDER BY id";
        var before = snapshot(protectedFields);
        var children = snapshot("SELECT id, parent_id FROM sys_resource WHERE parent_id = " + menuId);
        var roles = snapshot("SELECT * FROM sys_role_resource");
        run(MIGRATION);
        assertThat(snapshot(protectedFields)).isEqualTo(before);
        assertThat(snapshot("SELECT id, parent_id FROM sys_resource WHERE parent_id = " + menuId))
                .isEqualTo(children).hasSize(3);
        assertThat(snapshot("SELECT * FROM sys_role_resource")).isEqualTo(roles);
        assertThat(snapshot("SELECT parent_id, icon FROM sys_resource WHERE id = " + menuId))
                .containsExactly(List.of(0L, "i-streamline-plump-color:module"));
    }

    @Test
    void second_execution_is_a_noop_including_audit_time() throws Exception {
        run(MIGRATION);
        execute("UPDATE sys_resource SET update_time = TIMESTAMP '2026-01-01 00:00:00' WHERE id = " + menuId);
        var before = snapshot("SELECT * FROM sys_resource ORDER BY id");
        run(MIGRATION);
        assertThat(snapshot("SELECT * FROM sys_resource ORDER BY id")).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "tenant_id = 2", "client_code = 'h5'", "perms = 'customer:view'", "path = '/customer/plugin'",
            "component = 'customer/plugin'", "resource_type = 1", "min_user_type = 2", "del_flag = id"
    })
    void leaves_nonmatching_customer_resources_untouched(String change) throws Exception {
        // 这里只接受测试声明内的常量变体，覆盖每一个精确匹配约束。
        execute("UPDATE sys_resource SET " + change + " WHERE id = " + menuId);
        var before = snapshot("SELECT * FROM sys_resource ORDER BY id");
        run(MIGRATION);
        assertThat(snapshot("SELECT * FROM sys_resource ORDER BY id")).isEqualTo(before);
    }

    @Test
    void absent_menu_does_not_create_a_duplicate_or_move_other_resources() throws Exception {
        execute("DELETE FROM sys_resource WHERE id = " + menuId);
        var before = snapshot("SELECT * FROM sys_resource ORDER BY id");
        run(MIGRATION);
        assertThat(snapshot("SELECT * FROM sys_resource ORDER BY id")).isEqualTo(before);
    }

    private void run(String file) throws Exception {
        try (var reader = Files.newBufferedReader(Path.of("../../../db/migration", file))) {
            RunScript.execute(connection, reader);
        }
    }

    private void execute(String sql) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private List<List<Object>> snapshot(String sql) throws Exception {
        var rows = new ArrayList<List<Object>>();
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            while (result.next()) {
                var row = new ArrayList<>();
                for (int column = 1; column <= result.getMetaData().getColumnCount(); column++) {
                    row.add(result.getObject(column));
                }
                rows.add(row);
            }
        }
        return rows;
    }
}
