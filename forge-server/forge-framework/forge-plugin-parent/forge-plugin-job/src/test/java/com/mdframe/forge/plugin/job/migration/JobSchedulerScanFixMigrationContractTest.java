package com.mdframe.forge.plugin.job.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobSchedulerScanFixMigrationContractTest {

    private static final String MIGRATION = "V1.0.49__fix_job_scheduler_scan_findings.sql";

    @Test
    void shouldAddDedicatedJobRpcSceneWithPrivateNetworkGuard() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("'JOB_RPC'"));
        assertTrue(sql.contains("'sys_outbound_scene'"));
        assertTrue(sql.contains("`scene` IN ('FLOW_API', 'JOB_RPC')"));
        assertTrue(sql.contains("information_schema.TABLE_CONSTRAINTS"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
        assertTrue(sql.contains("heartbeat_time"));
        assertTrue(sql.contains("last_completion_time"));
        assertTrue(sql.contains("last_completion_execution_id"));
        assertFalse(sql.contains("tenant_id = 0"));
    }

    private Path resolveMigration() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
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
        throw new IllegalStateException("找不到迁移脚本: " + MIGRATION);
    }
}
