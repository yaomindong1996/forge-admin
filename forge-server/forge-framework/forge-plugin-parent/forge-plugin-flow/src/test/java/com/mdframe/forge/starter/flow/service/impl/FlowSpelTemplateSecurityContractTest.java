package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowSpelTemplateSecurityContractTest {

    @Test
    void templateQueriesAndMutationsMustBeTenantScoped() throws IOException {
        String mapper = Files.readString(Path.of("src/main/java/com/mdframe/forge/starter/flow/mapper/FlowSpelTemplateMapper.java"));
        String xml = Files.readString(Path.of("src/main/resources/mapper/FlowSpelTemplateMapper.xml"));
        String service = Files.readString(Path.of("src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowSpelTemplateServiceImpl.java"));
        String controller = Files.readString(Path.of("../../../forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowSpelTemplateController.java"));

        assertTrue(mapper.contains("selectByIdAndTenant"));
        assertTrue(mapper.contains("logicallyDeleteByIdAndTenant"));
        assertTrue(xml.contains("tenant_id = #{tenantId}"));
        assertTrue(xml.contains("SET deleted = 1"));
        assertTrue(service.contains("requireTenantId()"));
        assertFalse(service.contains("LambdaQueryWrapper"));
        assertTrue(controller.contains("FlowSpelTemplateCreateDTO"));
        assertTrue(controller.contains("FlowSpelTemplateUpdateDTO"));
        assertFalse(controller.contains("@RequestBody FlowSpelTemplate template"));
    }

    @Test
    void templateListMustHaveBoundedPageAndEnabledResult() throws IOException {
        String service = Files.readString(Path.of("src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowSpelTemplateServiceImpl.java"));
        String xml = Files.readString(Path.of("src/main/resources/mapper/FlowSpelTemplateMapper.xml"));

        assertTrue(service.contains("MAX_PAGE_SIZE = 100"));
        assertTrue(service.contains("Math.min(pageSize, MAX_PAGE_SIZE)"));
        assertTrue(xml.contains("LIMIT 200"));
    }
}
