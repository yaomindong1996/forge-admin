package com.mdframe.forge.admin.migration;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogicDeleteUniqueIndexPortabilityMigrationTest {

    private static final String MIGRATION =
            "V1.0.51__replace_logic_delete_generated_columns.sql";

    private static final Set<String> TABLES = Set.of(
            "ai_agent",
            "ai_business_app",
            "ai_business_application",
            "ai_business_application_object",
            "ai_business_application_publish_run",
            "ai_business_application_version",
            "ai_business_extension",
            "ai_business_extension_version",
            "ai_business_object",
            "ai_business_suite",
            "ai_capability",
            "ai_capability_access_token",
            "ai_capability_approval",
            "ai_capability_client",
            "ai_capability_flow_action_log",
            "ai_capability_grant",
            "ai_capability_oauth_redirect_uri",
            "ai_capability_policy",
            "ai_capability_version",
            "ai_code_rule",
            "ai_code_rule_segment",
            "ai_crud_config",
            "ai_lowcode_domain",
            "ai_lowcode_model",
            "ai_model",
            "ai_model_capability",
            "ai_model_route_policy",
            "ai_model_route_target",
            "ai_page_template",
            "ai_prompt_template",
            "ai_report_data_business_definition",
            "ai_report_data_connection",
            "ai_report_data_dataset",
            "ai_report_data_dataset_category",
            "ai_report_data_dimension",
            "sample_purchase_order",
            "sys_api_config",
            "sys_config",
            "sys_data_scope_config",
            "sys_dict_data",
            "sys_dict_type",
            "sys_employee",
            "sys_flow_node_config",
            "sys_job_api_idempotency",
            "sys_job_api_token",
            "sys_job_config",
            "sys_message_biz_type",
            "sys_message_template",
            "sys_org",
            "sys_outbound_whitelist",
            "sys_post",
            "sys_resource",
            "sys_role",
            "sys_tenant",
            "sys_user"
    );

    private static final Set<String> INDEXES = Set.of(
            "uk_agent_code_active",
            "uk_ai_business_app_code_active",
            "uk_ai_business_application_code_active",
            "uk_ai_business_application_object_active",
            "uk_ai_business_publish_run_key_active",
            "uk_ai_business_publish_run_version_active",
            "uk_ai_business_application_version_active",
            "uk_ai_business_extension_code_active",
            "uk_ai_business_extension_version_active",
            "uk_ai_business_object_code_active",
            "uk_ai_business_suite_code_active",
            "uk_ai_capability_code_active",
            "uk_ai_capability_tool_active",
            "uk_ai_capability_token_key_active",
            "uk_capability_approval_idempotency",
            "uk_capability_approval_request",
            "uk_ai_capability_client_code_active",
            "uk_ai_capability_client_key_active",
            "uk_cap_flow_action_idempotency",
            "uk_cap_flow_action_request",
            "uk_ai_capability_grant_active",
            "uk_ai_capability_redirect_active",
            "uk_capability_policy_version",
            "uk_ai_capability_version_active",
            "uk_ai_code_rule_code_active",
            "uk_ai_code_rule_segment_key_active",
            "uk_config_key_active",
            "uk_ai_lowcode_domain_code_active",
            "uk_ai_lowcode_domain_name_active",
            "uk_ai_lowcode_model_code_active",
            "uk_ai_model_provider_model_active",
            "uk_ai_model_capability_active",
            "uk_ai_route_policy_code_active",
            "uk_ai_route_target_active",
            "uk_template_key_active",
            "uk_ai_prompt_template_code_active",
            "uk_data_business_code_tenant_active",
            "uk_data_connection_code_tenant_active",
            "uk_data_dataset_code_tenant_active",
            "uk_data_dataset_category_code_tenant_active",
            "uk_data_dimension_code_tenant_active",
            "uk_sample_purchase_order_business_key_active",
            "uk_sample_purchase_order_no_active",
            "uk_method_url_active",
            "uk_tenant_config_key_active",
            "uk_tenant_mapper_active",
            "uk_tenant_dict_data_active",
            "uk_tenant_dict_type_active",
            "uk_emp_no_active",
            "uk_model_node_active",
            "uk_job_api_idempotency_active",
            "uk_job_api_token_key_active",
            "uk_job_name_group_active",
            "uk_tenant_type_active",
            "uk_tenant_code_active",
            "uk_tenant_org_name_active",
            "uk_outbound_whitelist_active",
            "uk_tenant_org_post_active",
            "uk_tenant_post_code_active",
            "uk_tenant_resource_active",
            "uk_tenant_role_key_active",
            "uk_tenant_role_name_active",
            "uk_tenant_name_active",
            "sys_user_unique_active"
    );

    @Test
    void shouldReplaceEveryVisibleHelperColumnWithPortableDeleteMarkers()
            throws IOException {
        String sql = Files.readString(resolveMigration());

        assertEquals(55, TABLES.size());
        assertEquals(64, INDEXES.size());
        assertEquals(55, count(sql, "FROM information_schema.COLUMNS"));
        assertEquals(55, count(sql, "FROM information_schema.STATISTICS"));
        assertEquals(55, count(sql, "DROP COLUMN `logic_delete_active`"));
        assertEquals(64, count(sql, "ADD UNIQUE INDEX"));
        assertEquals(54, count(sql, "MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0"));
        assertEquals(1, count(sql,
                "MODIFY COLUMN `del_flag` varchar(64) NOT NULL DEFAULT ''0''"));
        assertEquals(55, count(sql, "SET `del_flag` = `"));
        assertFalse(sql.contains("GENERATED ALWAYS"));
        assertFalse(sql.contains("CASE WHEN `del_flag`"));

        TABLES.forEach(table ->
                assertTrue(sql.contains("ALTER TABLE `" + table + "`"), table));
        INDEXES.forEach(index -> {
            assertTrue(sql.contains("'" + index + "'"), index);
            assertTrue(sql.contains("ADD UNIQUE INDEX `" + index + "`"), index);
        });

        assertEquals(64, count(sql, "`del_flag`)"));
    }

    @Test
    void shouldMakeTheMigrationSafeToReRunAfterTheHelperColumnIsGone() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertEquals(55, count(sql, "information_schema.COLUMNS"));
        assertEquals(55, count(sql, "information_schema.STATISTICS"));
        assertEquals(55, count(sql, "SET @logic_delete_active_exists ="));
        assertEquals(55, count(sql, "SET @drop_index_clauses ="));
        assertEquals(55, count(sql, "CONCAT(@drop_index_clauses, ', ')"));
        assertEquals(165, count(sql, "IF(@logic_delete_active_exists > 0,"));
        assertEquals(165, count(sql, "'SELECT 1'"));
    }

    @Test
    void shouldOnlyDropLegacyIndexesThatStillExist() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("COLUMN_NAME = 'logic_delete_active'"));
        assertTrue(sql.contains("INDEX_NAME IN ('uk_ai_code_rule_code_active', "
                + "'uk_ai_code_rule_code')"));
        assertFalse(sql.contains("ALTER TABLE `ai_code_rule` DROP INDEX "
                + "`uk_ai_code_rule_code_active`"));
        assertFalse(sql.contains("ADD UNIQUE INDEX `uk_ai_code_rule_code` "));
    }

    @Test
    void shouldLetMyBatisPlusUseTheNumericPrimaryKeyAsDeleteValue() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(NumericLogicEntity.class.getName());
        TableInfo tableInfo = TableInfoHelper.initTableInfo(assistant, NumericLogicEntity.class);

        assertEquals("del_flag=id", tableInfo.getLogicDeleteSql(false, false));
        assertEquals("del_flag=0", tableInfo.getLogicDeleteSql(false, true));
    }

    @Test
    void shouldDocumentThatLogicalDeleteDoesNotAlwaysNeedAnActiveUniqueKey()
            throws IOException {
        String agents = Files.readString(resolveRepositoryFile("AGENTS.md"));
        String codegenSkill = Files.readString(resolveRepositoryFile(
                ".agents/skills/forge-codegen-crud/SKILL.md"));
        String sqlReference = Files.readString(resolveRepositoryFile(
                ".agents/skills/forge-codegen-crud/references/sql-seeds.md"));
        String projectContext = Files.readString(resolveRepositoryFile(
                "code-copilot/rules/project-context.md"));

        assertTrue(agents.contains("不是所有逻辑删除表都需要删除标记唯一索引"));
        assertFalse(agents.contains("推荐新增生成列 `logic_delete_active`"));
        assertTrue(codegenSkill.contains("Never generate `logic_delete_active`"));
        assertTrue(codegenSkill.contains("@TableLogic(value = \"0\", delval ="));
        assertTrue(sqlReference.contains("禁止创建可见的 `logic_delete_active` 生成列"));
        assertTrue(sqlReference.contains("MySQL 唯一索引允许多个 `NULL`"));
        assertTrue(projectContext.contains("逻辑删除与唯一键（强制）"));
        assertTrue(projectContext.contains(
                "@TableLogic(value = \"0\", delval = \"主键数据库列名\")"));
    }

    private int count(String source, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }

    private Path resolveMigration() {
        return resolveRepositoryFile("forge-server/db/mysql/migration/" + MIGRATION);
    }

    private Path resolveRepositoryFile(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 10 && current != null; depth++) {
            Path candidate = current.resolve(relativePath);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Path.of(relativePath);
    }

    @TableName("logic_delete_contract")
    private static class NumericLogicEntity {

        @TableId
        private Long id;

        @TableLogic(value = "0", delval = "id")
        private Long delFlag;
    }
}
