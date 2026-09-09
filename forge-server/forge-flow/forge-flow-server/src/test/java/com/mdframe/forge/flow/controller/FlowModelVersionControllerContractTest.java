package com.mdframe.forge.flow.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FlowModelVersionControllerContractTest {

    @Test
    void cleanupMustHaveDedicatedPermissionAndMigrationResource() throws IOException {
        String controller = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/flow/controller/FlowModelVersionController.java"));
        String migration = Files.readString(Path.of(
                "../../db/migration/V1.0.150__add_flow_model_version_cleanup_permission.sql"));

        assertThat(controller).contains("@SaCheckPermission(\"flow:model:version:cleanup\")",
                "@PostMapping(\"/cleanup\")", "VersionCleanupDTO dto");
        assertThat(migration).contains("flow:model:version:cleanup",
                "flow:model:version:cleanup:api", "NOT EXISTS", "tenant_id = 1")
                .doesNotContain("sys_role_resource", "INSERT INTO sys_role");
    }
}
