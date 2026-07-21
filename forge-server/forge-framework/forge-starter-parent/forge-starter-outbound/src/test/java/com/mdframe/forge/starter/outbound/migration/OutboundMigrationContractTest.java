package com.mdframe.forge.starter.outbound.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboundMigrationContractTest {

    private static final String MIGRATION = "V1.0.47__add_outbound_security_policy.sql";

    @Test
    void shouldCreateLogicalDeleteAwareWhitelistTable() throws IOException {
        String sql = readMigration();

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `sys_outbound_whitelist`"));
        assertTrue(sql.contains("`tenant_id` bigint NOT NULL DEFAULT 1"));
        assertTrue(sql.contains("`create_dept` bigint"));
        assertTrue(sql.contains("`del_flag` tinyint NOT NULL DEFAULT 0"));
        assertTrue(sql.contains("`logic_delete_active` tinyint GENERATED ALWAYS AS"));
        assertTrue(sql.contains("IF(`del_flag` = 0, 1, NULL)"));
        assertFalse(sql.contains("IF(`del_flag` = 0, 0, `id`)"));
        assertTrue(sql.contains("uk_outbound_whitelist_active"));
        assertFalse(sql.contains("tenant_id = 0"));
    }

    @Test
    void shouldPreventJobWebhookPrivateExceptionAtDatabaseBoundary() throws IOException {
        String sql = readMigration();

        assertTrue(sql.contains("chk_outbound_private_scene"));
        assertTrue(sql.contains("`allow_private` = 0 OR `scene` = 'FLOW_API'"));
        assertTrue(sql.contains("'JOB_WEBHOOK'"));
        assertTrue(sql.contains("'FLOW_API'"));
    }

    @Test
    void shouldCreateAssignablePermissionsWithoutRoleExpansion() throws IOException {
        String sql = readMigration();

        assertTrue(sql.contains("'system:outboundWhitelist:list'"));
        assertTrue(sql.contains("'system:outboundWhitelist:add'"));
        assertTrue(sql.contains("'system:outboundWhitelist:edit'"));
        assertTrue(sql.contains("'system:outboundWhitelist:remove'"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
        assertFalse(sql.contains("INSERT INTO sys_role_resource"));
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
