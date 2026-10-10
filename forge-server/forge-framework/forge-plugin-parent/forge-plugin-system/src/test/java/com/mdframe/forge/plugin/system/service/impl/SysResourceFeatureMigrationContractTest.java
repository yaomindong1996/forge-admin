package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mdframe.forge.plugin.system.entity.SysResource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** MySQL 专用 DDL 只做静态契约验证；实际 information_schema/DDL 执行仍需真实 MySQL 验收。 */
class SysResourceFeatureMigrationContractTest {

    private static final Path MIGRATIONS = Path.of("../../../db/migration");
    private static final String VERSION = "V1.0.209__add_resource_feature_code.sql";

    @Test
    void shouldGuardColumnAdditionInCurrentSchemaAndKeepExistingRows() throws IOException {
        String sql = Files.readString(MIGRATIONS.resolve(VERSION));
        assertThat(sql).contains("FROM information_schema.COLUMNS", "TABLE_SCHEMA = DATABASE()",
                "TABLE_NAME = 'sys_resource'", "COLUMN_NAME = 'feature_code'", "COUNT(*) = 0", "'SELECT 1'");
        assertThat(sql).contains("ADD COLUMN `feature_code` VARCHAR(64) NULL", "AFTER `perms`");
        assertThat(sql).doesNotContain("UPDATE ", "DELETE ", "TRUNCATE ", "${");
    }

    @Test
    void shouldUseUniqueVersionAndReleasePreparedStatement() throws IOException {
        try (var files = Files.list(MIGRATIONS)) {
            assertThat(files.filter(path -> path.getFileName().toString().startsWith("V1.0.209__")))
                    .map(path -> path.getFileName().toString()).containsExactly(VERSION);
        }
        String sql = Files.readString(MIGRATIONS.resolve(VERSION));
        assertThat(sql).contains("PREPARE resource_feature_code_stmt FROM @resource_feature_code_sql;",
                "EXECUTE resource_feature_code_stmt;", "DEALLOCATE PREPARE resource_feature_code_stmt;");
    }

    @Test
    void shouldMapFeatureCodeToTheNewDatabaseColumn() {
        ResourceFeatureTestSupport.initializeMetadata(SysResource.class);
        assertThat(TableInfoHelper.getTableInfo(SysResource.class).getFieldList())
                .filteredOn(field -> field.getProperty().equals("featureCode"))
                .extracting(field -> field.getColumn()).containsExactly("feature_code");
    }
}
