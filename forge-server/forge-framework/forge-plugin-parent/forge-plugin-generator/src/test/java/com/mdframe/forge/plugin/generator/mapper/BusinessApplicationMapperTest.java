package com.mdframe.forge.plugin.generator.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessApplication Phase 2 mapper contract")
class BusinessApplicationMapperTest {

    @Test
    @DisplayName("application aggregate returns all overview counts in one query")
    void applicationAggregateReturnsOverviewCountsInOneQuery() throws IOException {
        String xml = resource("mapper/BusinessApplicationMapper.xml");

        assertTrue(xml.contains("AS objectCount"));
        assertTrue(xml.contains("AS entryCount"));
        assertTrue(xml.contains("AS flowCount"));
        assertTrue(xml.contains("AS extensionCount"));
        assertTrue(xml.contains("AS problemCount"));
        assertTrue(xml.contains("target_type = 'APPLICATION'"));
        assertFalse(xml.contains("target_type = 'APP'"));
    }

    @Test
    @DisplayName("application aggregate excludes deleted assets and uses stable ordering")
    void applicationAggregateExcludesDeletedAssetsAndUsesStableOrdering() throws IOException {
        String xml = resource("mapper/BusinessApplicationMapper.xml");

        assertTrue(xml.contains("o.del_flag = '0'"));
        assertTrue(xml.contains("ao.del_flag = '0'"));
        assertTrue(xml.contains("FROM ai_business_app"));
        assertTrue(xml.contains("WHERE del_flag = '0'"));
        assertTrue(xml.contains("ORDER BY a.update_time DESC, a.id DESC"));
    }

    @Test
    @DisplayName("suite mapper resolves a tenant-scoped recursive subtree")
    void suiteMapperResolvesTenantScopedRecursiveSubtree() throws IOException {
        String xml = resource("mapper/BusinessSuiteMapper.xml");

        assertTrue(xml.contains("WITH RECURSIVE suite_tree"));
        assertTrue(xml.contains("child.parent_id = parent.id"));
        assertTrue(xml.contains("child.tenant_id = #{tenantId}"));
        assertTrue(xml.contains("child.del_flag = '0'"));
    }

    private String resource(String path) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "找不到 Mapper XML: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
