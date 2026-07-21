package com.mdframe.forge.plugin.job.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobObservabilityMigrationContractTest {

    @Test
    void shouldAddConsecutiveFailuresAndObservabilityIndexWithGuards() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("COLUMN_NAME = 'consecutive_failures'"));
        assertTrue(sql.contains("consecutive_failures int NOT NULL DEFAULT 0"));
        assertTrue(sql.contains("INDEX_NAME = 'idx_job_log_observability'"));
        assertTrue(sql.contains("job_config_id, status, trigger_type, trigger_time"));
        assertTrue(sql.contains("information_schema.COLUMNS"));
        assertTrue(sql.contains("information_schema.STATISTICS"));
    }

    @Test
    void shouldBackfillFailureCountAndSeedSafeExportColumns() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("'sys_job_log_export'"));
        assertTrue(sql.contains("'sysJobLogService'"));
        assertTrue(sql.contains("'selectExportList'"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
        assertTrue(sql.contains("last_success_time"));
        assertTrue(sql.contains("failed_log.status = 0"));
        assertTrue(sql.contains("'triggerType'"));
        assertTrue(sql.contains("'status'"));
        assertFalse(sql.contains("'jobParam'"));
        assertFalse(sql.contains("'result' field_name"));
        assertFalse(sql.contains("'exceptionMsg'"));
        assertFalse(sql.contains("tenant_id = 0"));
    }

    private Path resolveMigration() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(
                    "db/migration/V1.0.44__add_job_log_observability.sql");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位定时任务运维日志监控迁移脚本");
    }
}
