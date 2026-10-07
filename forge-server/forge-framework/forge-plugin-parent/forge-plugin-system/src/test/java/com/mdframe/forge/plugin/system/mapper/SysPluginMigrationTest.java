package com.mdframe.forge.plugin.system.mapper;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SysPluginMigrationTest {
    @Test
    void additive_migration_is_repeatable_with_real_permission_unique_key_and_dictionaries() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;NON_KEYWORDS=VALUE;DATABASE_TO_LOWER=TRUE");
        try (Connection connection = source.getConnection();
             var fixture = getClass().getResourceAsStream("/plugin-task-migration-fixture.sql")) {
            RunScript.execute(connection, new InputStreamReader(fixture, StandardCharsets.UTF_8));
            run(connection, "V1.0.210__add_plugin_center.sql");
            run(connection, "V1.0.211__add_plugin_install_workbench.sql");
            run(connection, "V1.0.211__add_plugin_install_workbench.sql");
            assertThat(count(connection, "SELECT COUNT(*) FROM sys_resource")).isEqualTo(15);
            assertThat(count(connection, "SELECT COUNT(*) FROM sys_resource WHERE min_user_type = 0"))
                    .isEqualTo(14);
            assertThat(count(connection, "SELECT COUNT(*) FROM sys_dict_type")).isEqualTo(6);
            assertThat(count(connection, "SELECT COUNT(*) FROM sys_dict_data")).isEqualTo(14);
            run(connection, "V1.0.212__add_plugin_build_leases.sql");
            run(connection, "V1.0.212__add_plugin_build_leases.sql");
            assertThat(count(connection, "SELECT COUNT(*) FROM sys_dict_type")).isEqualTo(7);
            assertThat(count(connection, "SELECT COUNT(*) FROM sys_dict_data")).isEqualTo(22);
            assertThat(count(connection, "SELECT COUNT(*) FROM sys_resource")).isEqualTo(15);
            assertThat(count(connection, "SELECT COUNT(*) FROM sys_resource WHERE perms = 'system:plugin:snapshot'"))
                    .isEqualTo(1);
            checkCustomerMenu(connection);
            checkPermissionCollision(connection);
        }
    }

    private void checkCustomerMenu(Connection connection) throws Exception {
        // 迁移不能向同路径、不同归属的客户菜单追加高权限动作。
        try (var statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE sys_resource");
            statement.execute("INSERT INTO sys_resource(tenant_id, resource_name, resource_type, path, perms)"
                    + " VALUES (1, '客户菜单', 2, '/system/plugin', 'customer:view')");
        }
        run(connection, "V1.0.211__add_plugin_install_workbench.sql");
        assertThat(count(connection, "SELECT COUNT(*) FROM sys_resource")).isEqualTo(1);
    }

    private void run(Connection connection, String file) throws Exception {
        try (var reader = Files.newBufferedReader(Path.of("../../../db/migration", file))) {
            RunScript.execute(connection, reader);
        }
    }

    private void checkPermissionCollision(Connection connection) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE sys_resource");
            statement.execute("INSERT INTO sys_resource(tenant_id, resource_type, path, perms, client_code,"
                    + " min_user_type) VALUES (1, 2, '/system/plugin', 'system:plugin:view', 'pc', 0)");
            statement.execute("INSERT INTO sys_resource(tenant_id, resource_type, perms, client_code, api_url)"
                    + " VALUES (1, 4, 'system:plugin:upload', 'pc', '/customer/plugin-upload')");
        }
        run(connection, "V1.0.211__add_plugin_install_workbench.sql");
        run(connection, "V1.0.211__add_plugin_install_workbench.sql");
        assertThat(count(connection, "SELECT COUNT(*) FROM sys_resource WHERE resource_type = 4"
                + " AND perms = 'system:plugin:upload' AND api_url = '/customer/plugin-upload'"))
                .isEqualTo(1);
    }

    private int count(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }
}
