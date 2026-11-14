package com.mdframe.forge.plugin.external.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalConnectorMigrationContractTest {

    private static final String MIGRATION = "V1.0.101__add_external_connector_outbound_scene.sql";

    @Test
    void shouldAddIdempotentTenantOneOutboundSceneWithoutSensitiveDefaults() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("'sys_outbound_scene'"));
        assertTrue(sql.contains("'EXTERNAL_CONNECTOR'"));
        assertTrue(sql.contains("SELECT 1, 4, '外部连接器'"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
        assertTrue(sql.contains("data.tenant_id = 1"));
        assertFalse(sql.contains("tenant_id = 0"));
        assertFalse(sql.contains("INSERT INTO sys_outbound_whitelist"));
        assertFalse(sql.contains("${"));
    }

    private Path resolveMigration() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("db/mysql/migration").resolve(MIGRATION);
            if (Files.exists(candidate)) {
                return candidate;
            }
            candidate = current.resolve("forge-server/db/mysql/migration").resolve(MIGRATION);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("找不到迁移脚本: " + MIGRATION);
    }
}
