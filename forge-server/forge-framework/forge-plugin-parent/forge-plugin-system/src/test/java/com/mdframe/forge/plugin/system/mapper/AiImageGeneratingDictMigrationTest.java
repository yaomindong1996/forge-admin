package com.mdframe.forge.plugin.system.mapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiImageGeneratingDictMigrationTest {

    @Test
    void should_addOnlyExistingStatusForDefaultTenant_and_beRepeatable() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
             var statement = connection.createStatement()) {
            schema(statement);
            statement.execute("INSERT INTO sys_dict_type VALUES (1, 'ai_image_generate_status', 0)");
            statement.execute("INSERT INTO sys_dict_type VALUES (7, 'ai_image_generate_status', 0)");
            String sql = migration();
            statement.execute(sql);
            statement.execute(sql);
            try (var result = statement.executeQuery("SELECT * FROM sys_dict_data")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong("tenant_id")).isEqualTo(1L);
                assertThat(result.getString("dict_type")).isEqualTo("ai_image_generate_status");
                assertThat(result.getString("dict_value")).isEqualTo("generating");
                assertThat(result.getString("dict_label")).isEqualTo("生成中");
                assertThat(result.getString("list_class")).isEqualTo("info");
                assertThat(result.getLong("del_flag")).isZero();
                assertThat(result.next()).isFalse();
            }
            assertThat(sql).doesNotContain("${", "sys_resource", "ALTER TABLE");
        }
    }

    @Test
    void should_notInventDictionary_when_moduleIsAbsentOrDeleted() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
             var statement = connection.createStatement()) {
            schema(statement);
            statement.execute("INSERT INTO sys_dict_type VALUES (1, 'ai_image_generate_status', 1)");
            statement.execute(migration());
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM sys_dict_data")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
        }
    }

    private static String migration() throws Exception {
        return Files.readString(Path.of("../../../db/migration/V1.0.218__complete_ai_image_generating_dict.sql"));
    }

    private static void schema(Statement statement) throws Exception {
        statement.execute("CREATE TABLE sys_dict_type (tenant_id BIGINT, dict_type VARCHAR(100), del_flag BIGINT)");
        statement.execute("""
                CREATE TABLE sys_dict_data (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT, dict_sort INT,
                    dict_label VARCHAR(100), dict_value VARCHAR(100), dict_type VARCHAR(100),
                    list_class VARCHAR(30), is_default VARCHAR(1), dict_status INT,
                    create_by BIGINT, create_time TIMESTAMP, update_by BIGINT,
                    update_time TIMESTAMP, create_dept BIGINT, del_flag BIGINT
                )
                """);
    }
}
