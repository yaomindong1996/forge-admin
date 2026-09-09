package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowModelVersionGovernanceContractTest {

    @Test
    void versionQueriesAndDeletesMustBeTenantScoped() throws IOException {
        String mapper = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/mapper/FlowModelVersionMapper.java"));
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/FlowModelVersionMapper.xml"));
        String service = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowModelVersionServiceImpl.java"));

        assertTrue(mapper.contains("@Param(\"tenantId\") Long tenantId"));
        assertTrue(mapper.contains("logicalDeleteByIdAndTenant"));
        assertTrue(mapper.contains("updateVersionTagByIdAndTenant"));
        assertTrue(xml.contains("AND v.tenant_id = #{tenantId}"));
        assertTrue(xml.contains("SET del_flag = 1"));
        assertTrue(service.contains("requireTenantId()"));
        assertTrue(service.contains("仍有运行中的流程实例，禁止删除"));
        assertTrue(service.contains("运行时服务未初始化，无法确认版本引用"));
        assertTrue(service.contains("selectByIdAndTenant(dto.getModelId(), tenantId)"));
        assertTrue(service.contains("newVersionRecord.setTenantId(tenantId)"));
    }

    @Test
    void versionListMustCapPageSize() throws IOException {
        String controller = Files.readString(Path.of(
                "../../../forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowModelVersionController.java"));
        String modelService = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowModelServiceImpl.java"));
        assertTrue(controller.contains("Math.min(pageSize, 100)"));
        assertTrue(modelService.contains("MAX_MODEL_VERSIONS"));
        assertTrue(modelService.contains("listPage(0, MAX_MODEL_VERSIONS)"));
    }
}
