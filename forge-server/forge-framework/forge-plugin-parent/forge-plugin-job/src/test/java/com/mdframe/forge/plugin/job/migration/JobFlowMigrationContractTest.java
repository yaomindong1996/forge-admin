package com.mdframe.forge.plugin.job.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobFlowMigrationContractTest {

    @Test
    void shouldAddInvokeModeAndImmutableFlowBindingSnapshot() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("COLUMN_NAME = 'invoke_mode'"));
        assertTrue(sql.contains("invoke_mode varchar(16) NOT NULL DEFAULT ''SINGLE''"));
        assertTrue(sql.contains("COLUMN_NAME = 'flow_model_key'"));
        assertTrue(sql.contains("COLUMN_NAME = 'flow_model_version'"));
        assertTrue(sql.contains("COLUMN_NAME = 'flow_deployment_id'"));
        assertTrue(sql.contains("COLUMN_NAME = 'flow_process_definition_id'"));
    }

    @Test
    void shouldAddProcessInstanceTraceAndIndex() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("COLUMN_NAME = 'process_instance_id'"));
        assertTrue(sql.contains("process_instance_id varchar(64) NULL"));
        assertTrue(sql.contains("INDEX_NAME = 'idx_job_log_process_instance'"));
        assertTrue(sql.contains("process_instance_id, del_flag"));
        assertTrue(sql.contains("information_schema.STATISTICS"));
    }

    @Test
    void shouldSeedInvokeModeDictionaryForDefaultTenant() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("'sys_job_invoke_mode'"));
        assertTrue(sql.contains("'SINGLE'"));
        assertTrue(sql.contains("'FLOW'"));
        assertTrue(sql.contains("SELECT 1 tenant_id"));
        assertFalse(sql.contains("tenant_id = 0"));
    }

    @Test
    void shouldUseIdempotentGuardsAndBackfillExistingJobs() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("information_schema.TABLES"));
        assertTrue(sql.contains("information_schema.COLUMNS"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
        assertTrue(sql.contains("invoke_mode = ''SINGLE''"));
    }

    private Path resolveMigration() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(
                    "db/migration/V1.0.48__add_job_flow_orchestration.sql");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位定时任务 Flowable 编排迁移脚本");
    }
}
