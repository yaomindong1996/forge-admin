package com.mdframe.forge.plugin.system.mapper;

import com.mdframe.forge.plugin.system.enums.RuntimeLicenseMode;
import com.mdframe.forge.starter.plugin.license.RuntimeLicenseState;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeLicenseDictionaryMigrationTest {
    @Test
    void actual_seed_is_idempotent_matches_enums_and_leaves_resources_unchanged() throws Exception {
        var source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;NON_KEYWORDS=VALUE;DATABASE_TO_LOWER=TRUE");
        try (var connection = source.getConnection();
             var fixture = getClass().getResourceAsStream("/plugin-task-migration-fixture.sql")) {
            RunScript.execute(connection, new InputStreamReader(fixture, StandardCharsets.UTF_8));
            for (int count = 0; count < 2; count++) {
                try (var sql = Files.newBufferedReader(Path.of("../../../db/migration",
                        "V1.0.215__add_runtime_license_diagnostics_dict.sql"))) {
                    RunScript.execute(connection, sql);
                }
            }
            assertCodes(connection, "sys_runtime_license_mode", java.util.Arrays.stream(RuntimeLicenseMode.values())
                    .map(RuntimeLicenseMode::getCode).toList());
            assertCodes(connection, "sys_runtime_license_state", java.util.Arrays.stream(RuntimeLicenseState.values())
                    .map(RuntimeLicenseState::getCode).toList());
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM sys_resource")) {
                result.next();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM sys_dict_type")) {
                result.next();
                assertThat(result.getInt(1)).isEqualTo(2);
            }
        }
    }

    private void assertCodes(java.sql.Connection connection, String type, java.util.List<String> expected)
            throws Exception {
        var values = new ArrayList<String>();
        try (var statement = connection.prepareStatement("SELECT dict_value FROM sys_dict_data"
                + " WHERE tenant_id = 1 AND del_flag = 0 AND dict_type = ?")) {
            statement.setString(1, type);
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    values.add(result.getString(1));
                }
            }
        }
        assertThat(values).containsExactlyInAnyOrderElementsOf(expected);
    }
}
