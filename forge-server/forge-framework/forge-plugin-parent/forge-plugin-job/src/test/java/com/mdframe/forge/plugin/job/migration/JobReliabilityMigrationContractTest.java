package com.mdframe.forge.plugin.job.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobReliabilityMigrationContractTest {

    private static final String MIGRATION = "V1.0.40__harden_job_scheduler_reliability.sql";

    @Test
    void shouldAddMinimalJobConfigSyncColumns() throws IOException {
        String sql = readMigration();

        assertTrue(sql.contains("COLUMN_NAME = 'sync_status'"));
        assertTrue(sql.contains("sync_status varchar(20) NOT NULL DEFAULT ''PENDING''"));
        assertTrue(sql.contains("COLUMN_NAME = 'sync_error'"));
        assertTrue(sql.contains("sync_error varchar(1000)"));
        assertTrue(sql.contains("COLUMN_NAME = 'sync_time'"));
        assertTrue(sql.contains("sync_time datetime"));
        assertTrue(sql.contains("COLUMN_NAME = 'version'"));
        assertTrue(sql.contains("version int NOT NULL DEFAULT 0"));
    }

    @Test
    void shouldAddJobLogAssociationFields() throws IOException {
        String sql = readMigration();

        assertTrue(sql.contains("COLUMN_NAME = 'job_config_id'"));
        assertTrue(sql.contains("job_config_id bigint"));
        assertTrue(sql.contains("COLUMN_NAME = 'trigger_type'"));
        assertTrue(sql.contains("trigger_type varchar(20) NOT NULL DEFAULT ''UNKNOWN''"));
        assertTrue(sql.contains("SET job_log.job_config_id = job_config.id"));
    }

    @Test
    void shouldCreateRequiredIndexesAndBackfillCompatibilityValues() throws IOException {
        String sql = readMigration();

        assertTrue(sql.contains("INDEX_NAME = 'idx_job_sync_status_del'"));
        assertTrue(sql.contains("CREATE INDEX idx_job_sync_status_del ON sys_job_config (sync_status, del_flag)"));
        assertTrue(sql.contains("INDEX_NAME = 'idx_job_log_config_trigger'"));
        assertTrue(sql.contains("CREATE INDEX idx_job_log_config_trigger ON sys_job_log (job_config_id, trigger_time)"));
        assertTrue(sql.contains("sync_status = ''PENDING''"));
        assertTrue(sql.contains("trigger_type = ''UNKNOWN''"));
    }

    @Test
    void shouldUseIdempotentGuards() throws IOException {
        String sql = readMigration();

        assertTrue(sql.contains("information_schema.TABLES"));
        assertTrue(sql.contains("information_schema.COLUMNS"));
        assertTrue(sql.contains("information_schema.STATISTICS"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
    }

    @Test
    void shouldSeedSyncStatusDictionaryWithTenantOne() throws IOException {
        String sql = readMigration();

        assertTrue(sql.contains("'sys_job_sync_status'"));
        assertTrue(sql.contains("'PENDING'"));
        assertTrue(sql.contains("'SYNCED'"));
        assertTrue(sql.contains("'FAILED'"));
        assertTrue(sql.contains("'DELETE_PENDING'"));
        assertTrue(sql.contains("SELECT 1,"));
    }

    @Test
    void shouldNotAddDeferredVersionFields() throws IOException {
        String sql = readMigration().toLowerCase();

        assertFalse(sql.contains("schedule_type"));
        assertFalse(sql.contains("fire_once_time"));
        assertFalse(sql.contains("timezone"));
        assertFalse(sql.contains("concurrent_policy"));
        assertFalse(sql.contains("misfire_policy"));
        assertFalse(sql.contains("alarm_channels"));
        assertFalse(sql.contains("sys_job_api_token"));
        assertFalse(sql.contains("flow_model_key"));
        assertFalse(sql.contains("process_instance_id"));
    }

    @Test
    void shouldPreserveLegacyStatusValues() throws IOException {
        String sql = readMigration().toLowerCase();

        assertFalse(sql.contains("update sys_job_config set status"));
        assertFalse(sql.contains("update sys_job_log set status"));
    }

    private String readMigration() throws IOException {
        return Files.readString(resolveMigration());
    }

    private Path resolveMigration() {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 8 && current != null; depth++) {
            Path candidate = current.resolve("db/migration").resolve(MIGRATION);
            if (Files.exists(candidate)) {
                return candidate;
            }
            candidate = current.resolve("forge-server/db/migration").resolve(MIGRATION);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Path.of("db/migration").resolve(MIGRATION);
    }
}
