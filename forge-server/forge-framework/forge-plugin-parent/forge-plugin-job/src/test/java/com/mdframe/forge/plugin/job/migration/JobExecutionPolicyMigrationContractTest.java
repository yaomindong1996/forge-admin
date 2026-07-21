package com.mdframe.forge.plugin.job.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobExecutionPolicyMigrationContractTest {

    @Test
    void shouldAddExecutionPolicyAndLifecycleFields() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("COLUMN_NAME = 'concurrent_policy'"));
        assertTrue(sql.contains("concurrent_policy varchar(32) NOT NULL DEFAULT ''ALLOW''"));
        assertTrue(sql.contains("COLUMN_NAME = 'misfire_policy'"));
        assertTrue(sql.contains("misfire_policy varchar(32) NOT NULL DEFAULT ''DO_NOTHING''"));
        assertTrue(sql.contains("COLUMN_NAME = 'idempotent_flag'"));
        assertTrue(sql.contains("idempotent_flag tinyint NOT NULL DEFAULT 0"));
        assertTrue(sql.contains("COLUMN_NAME = 'scheduled_fire_time'"));
        assertTrue(sql.contains("scheduled_fire_time datetime NULL"));
        assertTrue(sql.contains("COLUMN_NAME = 'fire_instance_id'"));
        assertTrue(sql.contains("fire_instance_id varchar(200) NULL"));
    }

    @Test
    void shouldSeedPoliciesAndExtendedLogStatuses() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("'sys_job_concurrent_policy'"));
        assertTrue(sql.contains("'ALLOW'"));
        assertTrue(sql.contains("'SKIP_IF_RUNNING'"));
        assertTrue(sql.contains("'sys_job_misfire_policy'"));
        assertTrue(sql.contains("'FIRE_ONCE_NOW'"));
        assertTrue(sql.contains("'DO_NOTHING'"));
        assertTrue(sql.contains("'sys_job_log_status'"));
        assertTrue(sql.contains("'运行中'"));
        assertTrue(sql.contains("'已跳过'"));
        assertFalse(sql.contains("tenant_id = 0"));
    }

    @Test
    void shouldUseIdempotentGuardsAndBackfillCompatibilityDefaults() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("information_schema.TABLES"));
        assertTrue(sql.contains("information_schema.COLUMNS"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
        assertTrue(sql.contains("concurrent_policy = ''ALLOW''"));
        assertTrue(sql.contains("misfire_policy = ''DO_NOTHING''"));
        assertTrue(sql.contains("idempotent_flag = 0"));
    }

    private Path resolveMigration() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(
                    "db/migration/V1.0.43__add_job_execution_policies.sql");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位定时任务执行策略迁移脚本");
    }
}
