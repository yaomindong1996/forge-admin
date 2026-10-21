package com.mdframe.forge.plugin.generator.mapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 内存数据库只验证字典脚本语法、存储码和幂等，不连接真实库。 */
class FlowModelPublicationDictionaryMigrationTest {
    @Test
    void publicationDictionaryCanRunTwiceWithoutDuplicateRows() throws Exception {
        String sql = migration();
        String common = "tenant_id bigint, create_by bigint, create_time timestamp, update_by bigint, "
                + "update_time timestamp, create_dept bigint, del_flag int";
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:dict_" + UUID.randomUUID() + ";MODE=MySQL");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE sys_dict_type (dict_name varchar(128), dict_type varchar(128), "
                    + "dict_status int, " + common + ")");
            statement.execute("CREATE TABLE sys_dict_data (dict_sort int, dict_label varchar(128), "
                    + "dict_value varchar(128), dict_type varchar(128), list_class varchar(32), "
                    + "is_default varchar(1), dict_status int, " + common + ")");
            for (int run = 0; run < 2; run++) {
                for (String command : sql.split(";")) {
                    if (!command.isBlank()) {
                        statement.execute(command);
                    }
                }
            }
            Set<String> codes = new HashSet<>();
            try (var result = statement.executeQuery("SELECT tenant_id, dict_value FROM sys_dict_data")) {
                while (result.next()) {
                    assertThat(result.getLong("tenant_id")).isEqualTo(1L);
                    assertThat(codes.add(result.getString("dict_value"))).isTrue();
                }
            }
            assertThat(codes).containsExactlyInAnyOrder("UNPUBLISHED", "SYNCED", "CHANGED", "UNKNOWN");
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM sys_dict_type")) {
                result.next();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
        }
    }

    private String migration() throws Exception {
        Path cursor = Path.of("").toAbsolutePath();
        while (cursor != null) {
            Path file = cursor.resolve("db/migration/V1.0.228__add_flow_model_publication_state_dict.sql");
            if (Files.isRegularFile(file)) {
                return Files.readString(file);
            }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("未找到发布状态字典增量脚本");
    }
}
