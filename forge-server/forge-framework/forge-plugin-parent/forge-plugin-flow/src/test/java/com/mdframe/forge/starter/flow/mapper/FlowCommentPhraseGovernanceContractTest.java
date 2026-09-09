package com.mdframe.forge.starter.flow.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FlowCommentPhraseGovernanceContractTest {

    private static final Path MAPPER = Path.of("src/main/resources/mapper/FlowCommentPhraseMapper.xml");
    private static final Path SERVICE = Path.of(
            "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowCommentPhraseServiceImpl.java");
    private static final Path MIGRATION = Path.of("../../../db/migration/V1.0.156__add_flow_comment_phrase.sql");

    @Test
    void queriesMustBeTenantScopedAndFilterLogicalDeletes() throws IOException {
        String mapper = Files.readString(MAPPER);
        assertThat(mapper).contains(
                "p.tenant_id = #{tenantId}",
                "p.del_flag = 0",
                "p.content LIKE CONCAT('%', #{keyword}, '%')",
                "(p.owner_type = 0 AND p.user_id = 0)",
                "(p.owner_type = 1 AND p.user_id = #{userId})",
                "p.scene = #{scene} OR p.scene = 'ALL'",
                "SET del_flag = id",
                "LIMIT 50");
    }

    @Test
    void serviceMustCapRecordsAndIsolatePersonalPhrases() throws IOException {
        String service = Files.readString(SERVICE);
        assertThat(service).contains(
                "private static final int MAX_PERSONAL = 30",
                "private static final int MAX_TENANT = 100",
                "无法确定当前租户，禁止管理常用审批意见",
                "EnableStatus.ENABLED.getCode()",
                "FlowCommentPhraseOwnerType.USER",
                "requireManagePermission()",
                "existing.getUserId().equals(userId)");
        assertThat(service).doesNotContain("LambdaQueryWrapper");
    }

    @Test
    void migrationMustUseLogicalDeleteTombstonesAndIdempotentResources() throws IOException {
        String sql = Files.readString(MIGRATION);
        assertThat(sql).contains(
                "CREATE TABLE IF NOT EXISTS `sys_flow_comment_phrase`",
                "UNIQUE KEY `uk_flow_comment_phrase_content`",
                "NOT EXISTS",
                "tenant_id = 1",
                "flow:comment-phrase:view",
                "flow:comment-phrase:manage",
                "'同意'",
                "'请补充材料'");
        assertThat(sql).doesNotContain("${");
    }
}
