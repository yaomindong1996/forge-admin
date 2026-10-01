package com.mdframe.forge.plugin.print;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PrintStandaloneSourceMigrationContractTest {

    @Test
    void shouldCreateStandaloneSourceAndPreserveLegacyScope() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS sys_print_business_source");
        assertThat(sql).contains("UNIQUE KEY uk_print_business_source_code");
        assertThat(sql).contains("business_source_id");
        assertThat(sql).contains("template_version_id");
        assertThat(sql).contains("source_revision");
        assertThat(sql).contains("MODIFY COLUMN application_id BIGINT NULL");
        assertThat(sql).contains("uk_print_template_source_code");
        assertThat(sql).contains("uk_print_binding_source_template");
        assertThat(sql).contains("'print:source:view'");
        assertThat(sql).contains("'print:source:manage'");
        assertThat(sql).contains("resource_name = '打印中心', visible = 1");
        assertThat(sql).doesNotContain("tenant_id = 0");
        assertThat(sql).doesNotContain("${");
    }

    private Path resolveMigration() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("db/migration/V1.0.204__add_standalone_print_sources.sql");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位独立打印来源迁移脚本");
    }
}
