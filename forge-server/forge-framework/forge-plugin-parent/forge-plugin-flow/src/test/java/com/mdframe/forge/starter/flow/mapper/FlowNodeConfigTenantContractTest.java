package com.mdframe.forge.starter.flow.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowNodeConfigTenantContractTest {

    @Test
    void runtimeNodeConfigLookupMustCarryTenant() throws IOException {
        String mapper = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/mapper/FlowNodeConfigMapper.java"));
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/FlowNodeConfigMapper.xml"));
        String service = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowNodeConfigServiceImpl.java"));

        assertTrue(mapper.contains("@Param(\"tenantId\") Long tenantId"));
        assertTrue(xml.contains("c.tenant_id = #{tenantId}"));
        assertTrue(xml.contains("m.tenant_id = #{tenantId}"));
        assertTrue(xml.contains("m.id = #{modelRef} OR m.model_key = #{modelRef}"));
        assertTrue(xml.contains("AND tenant_id = #{tenantId}"));
        assertTrue(mapper.contains("selectByModelRef"));
        assertTrue(mapper.contains("selectByIdAndTenant"));
        assertTrue(service.contains("SessionHelper.getTenantId()"));
        assertTrue(service.contains("TenantContextHolder.getTenantId()"));
        assertTrue(service.contains("selectByModelKeyAndNode"));
        assertTrue(service.contains("selectByModelRef"));
        assertTrue(service.contains("getOwnedNodeConfig"));
    }
}
