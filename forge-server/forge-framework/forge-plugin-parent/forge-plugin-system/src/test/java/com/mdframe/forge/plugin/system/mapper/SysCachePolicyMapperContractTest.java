package com.mdframe.forge.plugin.system.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SysCachePolicyMapperContractTest {

    @Test
    void activeQueriesMustUseTrustedTenantAndLogicDeleteFilters() throws IOException {
        String mapper = Files.readString(Path.of("src/main/resources/mapper/SysCachePolicyMapper.xml"));

        assertThat(statement(mapper, "selectActivePolicies", "select"))
                .contains("tenant_id = #{tenantId}", "del_flag = 0")
                .doesNotContain("${");
        assertThat(statement(mapper, "selectByIdentity", "select"))
                .contains("tenant_id = #{tenantId}", "application_code = #{applicationCode}",
                        "cache_name = #{cacheName}", "del_flag = 0")
                .doesNotContain("${");
    }

    @Test
    void writesMustUseOptimisticVersionAndPrimaryKeyTombstone() throws IOException {
        String mapper = Files.readString(Path.of("src/main/resources/mapper/SysCachePolicyMapper.xml"));

        assertThat(statement(mapper, "updateWithVersion", "update"))
                .contains("policy_version = #{policy.policyVersion}",
                        "policy_version = #{expectedVersion}", "del_flag = 0", "tenant_id = #{policy.tenantId}")
                .doesNotContain("${");
        assertThat(statement(mapper, "logicalDeleteWithVersion", "update"))
                .contains("SET del_flag = id", "policy_version = policy_version + 1",
                        "policy_version = #{expectedVersion}", "tenant_id = #{tenantId}", "del_flag = 0")
                .doesNotContain("${");
    }

    private String statement(String mapper, String id, String element) {
        int start = mapper.indexOf("id=\"" + id + "\"");
        int end = mapper.indexOf("</" + element + ">", start);
        assertThat(start).as(id).isGreaterThanOrEqualTo(0);
        assertThat(end).as(id).isGreaterThan(start);
        return mapper.substring(start, end);
    }
}
