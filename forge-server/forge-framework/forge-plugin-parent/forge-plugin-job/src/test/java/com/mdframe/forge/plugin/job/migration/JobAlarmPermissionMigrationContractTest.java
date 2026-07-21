package com.mdframe.forge.plugin.job.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobAlarmPermissionMigrationContractTest {

    @Test
    void shouldAddGuardedAlarmColumnsAndSupportedChannelDictionary() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("COLUMN_NAME = 'alarm_enabled'"));
        assertTrue(sql.contains("COLUMN_NAME = 'alarm_channels'"));
        assertTrue(sql.contains("COLUMN_NAME = 'alarm_recipient_user_ids'"));
        assertTrue(sql.contains("information_schema.COLUMNS"));
        assertTrue(sql.contains("'sys_job_alarm_channel'"));
        assertTrue(sql.contains("'WEB' dict_value"));
        assertTrue(sql.contains("'EMAIL'"));
        assertFalse(sql.contains("'WEBHOOK'"));
    }

    @Test
    void shouldSeedAssignablePermissionsWithoutRoleTemplateOrHardCodedMenuId() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("path = '/system/job-config'"));
        assertTrue(sql.contains("system:jobConfig:dangerous"));
        assertTrue(sql.contains("system:jobLog:detail"));
        assertTrue(sql.contains("resource_type = 4"));
        assertTrue(sql.contains("'/job/config/**'"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
        assertFalse(sql.contains("INSERT INTO sys_role_resource"));
        assertFalse(sql.contains("tenant_id = 0"));
        assertFalse(sql.contains("MES"));
    }

    private Path resolveMigration() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(
                    "db/migration/V1.0.45__add_job_alarm_permissions.sql");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位定时任务告警权限迁移脚本");
    }
}
