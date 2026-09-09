package com.mdframe.forge.starter.flow.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FlowUserGroupGovernanceContractTest {

    private static final Path MAPPER = Path.of("src/main/resources/mapper/FlowUserGroupMapper.xml");
    private static final Path MAPPER_JAVA = Path.of(
            "src/main/java/com/mdframe/forge/starter/flow/mapper/FlowUserGroupMapper.java");
    private static final Path SERVICE = Path.of(
            "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowUserGroupServiceImpl.java");
    private static final Path MIGRATION = Path.of("../../../db/migration/V1.0.149__add_flow_user_group_management.sql");

    @Test
    void groupQueriesMustBeTenantScopedAndFilterLogicalDeletes() throws IOException {
        String mapper = Files.readString(MAPPER);
        assertThat(mapper).contains(
                "g.tenant_id = #{tenantId}",
                "g.del_flag = 0",
                "g.group_code LIKE CONCAT('%', #{keyword}, '%')",
                "g.group_name LIKE CONCAT('%', #{keyword}, '%')",
                "m.tenant_id = #{tenantId}",
                "m.del_flag = 0",
                "u.tenant_id = m.tenant_id",
                "u.user_status = 1",
                "LIMIT 200");
    }

    @Test
    void serviceMustCapMembersAndRejectMissingTenant() throws IOException {
        String service = Files.readString(SERVICE);
        assertThat(service).contains(
                "private static final int MAX_MEMBERS = 200",
                "无法确定当前租户，禁止管理流程用户组",
                "selectFlowAvailableUserIds(tenantId, userIds)",
                "单次最多维护200名成员",
                "EnableStatus.ENABLED.getCode()");
    }

    @Test
    void pageKeywordMustBeDeclaredAndForwardedToMapper() throws IOException {
        String mapperJava = Files.readString(MAPPER_JAVA);
        String service = Files.readString(SERVICE);
        assertThat(mapperJava).contains("@Param(\"keyword\") String keyword");
        assertThat(service).contains("trimToNull(safeQuery.getKeyword()),");
    }

    @Test
    void migrationMustUseLogicalDeleteTombstonesAndIdempotentResources() throws IOException {
        String sql = Files.readString(MIGRATION);
        assertThat(sql).contains(
                "CREATE TABLE IF NOT EXISTS `sys_flow_user_group`",
                "CREATE TABLE IF NOT EXISTS `sys_flow_user_group_member`",
                "UNIQUE KEY `uk_flow_user_group_code`",
                "UNIQUE KEY `uk_flow_user_group_member`",
                "NOT EXISTS",
                "tenant_id = 1",
                "flow:org:group:view",
                "flow:org:group:manage");
        assertThat(sql).doesNotContain("${");
    }
}
