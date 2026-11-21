package com.mdframe.forge.plugin.system.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 套餐解析结果模型：二期升降级将复用同一对象，首期仅校验组成字段语义。
 */
class TenantPackageResolutionTest {

    @Test
    void builderKeepsBootstrapAndQuota() {
        TenantPackageResolution resolution = TenantPackageResolution.builder()
                .packageId(10L)
                .packageCode("minimal")
                .packageName("精简版")
                .bootstrapPackages(List.of(
                        TenantBootstrapPackages.DICT_AND_CONFIG,
                        TenantBootstrapPackages.ROOT_ORG,
                        TenantBootstrapPackages.TENANT_ADMIN_ROLE))
                .resourceIds(Set.of())
                .userLimit(50)
                .build();

        assertEquals(10L, resolution.getPackageId());
        assertEquals("minimal", resolution.getPackageCode());
        assertEquals(3, resolution.getBootstrapPackages().size());
        assertTrue(resolution.getResourceIds().isEmpty());
        assertEquals(50, resolution.getUserLimit());
    }
}
