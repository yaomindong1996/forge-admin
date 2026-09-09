package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FlowModelVersionCleanupContractTest {

    @Test
    void cleanupMustLockTenantVersionsAndProtectRuntimeReferences() throws IOException {
        String mapper = Files.readString(Path.of(
                "src/main/resources/mapper/FlowModelVersionMapper.xml"));
        String service = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowModelVersionServiceImpl.java"));
        String controller = Files.readString(Path.of(
                "../../../forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowModelVersionController.java"));
        String dto = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/dto/VersionCleanupDTO.java"));

        assertThat(mapper).contains("selectCleanupCandidates", "tenant_id = #{tenantId}",
                "del_flag = 0", "LIMIT 500", "FOR UPDATE");
        assertThat(service).contains("cleanupVersions(VersionCleanupDTO dto)",
                "selectCleanupCandidates(dto.getModelId(), tenantId)",
                "isReferencedByRunningInstance(version)",
                "logicalDeleteByIdAndTenant(version.getId(), tenantId)");
        assertThat(controller).contains("@PostMapping(\"/cleanup\")",
                "@RequestBody VersionCleanupDTO dto");
        assertThat(dto).contains("private String modelId", "private Integer retainLatest");
    }
}
