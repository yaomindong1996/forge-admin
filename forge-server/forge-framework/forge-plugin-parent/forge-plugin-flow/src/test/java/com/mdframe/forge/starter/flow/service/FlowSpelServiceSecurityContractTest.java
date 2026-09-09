package com.mdframe.forge.starter.flow.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowSpelServiceSecurityContractTest {

    @Test
    void runtimeSpelHelpersMustUseTenantAwareOrganizationGateway() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/mdframe/forge/starter/flow/service/FlowSpelService.java"));

        assertTrue(source.contains("getUserIdsByRoleCode"));
        assertTrue(source.contains("getUserIdsByRegionCode"));
        assertTrue(source.contains("getUserIdsByDeptAndRoleCode"));
        assertTrue(source.contains("MAX_RESULT_USERS = 200"));
        assertFalse(source.contains("LambdaQueryWrapper"));
        assertFalse(source.contains("log.info(\"SPEL: 流程变量"));
        assertFalse(source.contains("log.info(\"SPEL: 查找角色用户"));
        assertFalse(source.contains("log.info(\"SPEL: 查找区域负责人"));
    }
}
