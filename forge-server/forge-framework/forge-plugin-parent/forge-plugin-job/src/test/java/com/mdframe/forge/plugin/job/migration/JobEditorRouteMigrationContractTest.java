package com.mdframe.forge.plugin.job.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobEditorRouteMigrationContractTest {

    @Test
    void shouldCreateHiddenEditorRoutesAndInheritExistingRoles() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("'/system/job-config/editor'"));
        assertTrue(sql.contains("'/system/job-config/editor/:id'"));
        assertTrue(sql.contains("'system/job-config.editor'"));
        assertTrue(sql.contains("'system/job-config.editor.[id]'"));
        assertTrue(sql.contains("visible, perms"));
        assertTrue(sql.contains("FROM sys_role_resource role_resource"));
        assertTrue(sql.contains("role_resource.resource_id = @job_config_resource_id"));
        assertTrue(sql.contains("NOT EXISTS"));
        assertFalse(sql.contains("tenant_id = 0"));
    }

    private Path resolveMigration() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("db/migration/V1.0.41__add_job_editor_routes.sql");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位定时任务工作台路由迁移脚本");
    }
}
