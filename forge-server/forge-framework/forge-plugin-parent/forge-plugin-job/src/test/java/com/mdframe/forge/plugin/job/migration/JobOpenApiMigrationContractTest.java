package com.mdframe.forge.plugin.job.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobOpenApiMigrationContractTest {

    private static final String MIGRATION = "V1.0.46__add_job_open_api_credentials.sql";

    @Test
    void shouldStoreOnlyCredentialHashAndHashedIdempotencyKey() throws IOException {
        String sql = readMigration();

        assertTrue(sql.contains("`token_key_id` varchar(22)"));
        assertTrue(sql.contains("`token_prefix` varchar(32)"));
        assertTrue(sql.contains("`token_hash` char(64)"));
        assertTrue(sql.contains("`idempotency_key_hash` char(64)"));
        assertFalse(sql.contains("`raw_token`"));
        assertFalse(sql.contains("`plain_token`"));
        assertFalse(sql.contains("`idempotency_key` varchar"));
    }

    @Test
    void shouldUseTenantOneAndLogicalDeleteAwareUniqueKeys() throws IOException {
        String sql = readMigration();

        assertTrue(sql.contains("`tenant_id` bigint NOT NULL DEFAULT 1"));
        assertTrue(sql.contains("`del_flag` tinyint NOT NULL DEFAULT 0"));
        assertTrue(sql.contains("`logic_delete_active` tinyint GENERATED ALWAYS AS"));
        assertTrue(sql.contains("uk_job_api_token_key_active"));
        assertTrue(sql.contains("uk_job_api_idempotency_active"));
        assertFalse(sql.contains("tenant_id = 0"));
        assertFalse(sql.contains("SELECT 0 tenant_id"));
    }

    @Test
    void shouldSeedDictionariesAndAssignableManagementPermissions() throws IOException {
        String sql = readMigration();

        assertTrue(sql.contains("'sys_job_api_scope'"));
        assertTrue(sql.contains("'jobs:read'"));
        assertTrue(sql.contains("'jobs:trigger'"));
        assertTrue(sql.contains("'executions:read'"));
        assertTrue(sql.contains("'system:jobApiToken:list'"));
        assertTrue(sql.contains("'system:jobApiToken:add'"));
        assertTrue(sql.contains("'system:jobApiToken:revoke'"));
        assertTrue(sql.contains("'system:jobApiToken:rotate'"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
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
