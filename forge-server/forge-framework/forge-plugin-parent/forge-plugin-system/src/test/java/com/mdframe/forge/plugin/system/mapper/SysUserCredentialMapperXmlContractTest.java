package com.mdframe.forge.plugin.system.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class SysUserCredentialMapperXmlContractTest {

    private static final Path XML = Path.of("src/main/resources/mapper/SysUserCredentialMapper.xml");

    @Test
    void everyMapperMethodHasXmlStatement() throws IOException {
        String xml = Files.readString(XML);

        assertThat(xml).contains("namespace=\"" + SysUserCredentialMapper.class.getName() + "\"");
        for (Method method : SysUserCredentialMapper.class.getDeclaredMethods()) {
            assertThat(xml).as("statement for %s", method.getName()).contains("id=\"" + method.getName() + "\"");
        }
    }

    @Test
    void credentialUpdateMustBumpVersionAndChangeTime() throws IOException {
        String update = statement("update", "updateCredential");

        assertThat(update)
                .contains("password = #{encodedPassword}")
                .contains("password_version = COALESCE(password_version, 0) + 1")
                .contains("password_changed_time = #{changedTime}")
                .contains("update_time = #{changedTime}")
                .contains("WHERE id = #{userId}")
                .contains("del_flag = 0");
        // PostgreSQL 的 force_password_change 是 smallint，不能绑定 Java 布尔值
        assertThat(update)
                .contains("force_password_change = 1")
                .contains("force_password_change = 0")
                .doesNotContain("force_password_change = #{");
    }

    @Test
    void credentialReadMustSkipDeletedUsers() throws IOException {
        assertThat(statement("select", "selectCredential"))
                .contains("FROM sys_user")
                .contains("WHERE id = #{userId}")
                .contains("del_flag = 0");
    }

    @Test
    void historyStatementsMustBeScopedByTenantAndUser() throws IOException {
        for (String[] target : new String[][] {
                {"select", "selectRecentPasswordHashes"},
                {"select", "selectHistoryIds"},
                {"delete", "deleteHistoryByIds"}}) {
            assertThat(statement(target[0], target[1]))
                    .as(target[1])
                    .contains("sys_user_password_history")
                    .contains("tenant_id = #{tenantId}")
                    .contains("user_id = #{userId}");
        }
        assertThat(statement("insert", "insertPasswordHistory"))
                .contains("(tenant_id, user_id, password_hash, changed_time, create_time, update_time)")
                .contains("#{tenantId}, #{userId}, #{passwordHash}, #{changedTime}");
        assertThat(statement("select", "selectRecentPasswordHashes"))
                .contains("ORDER BY changed_time DESC, id DESC")
                .contains("LIMIT #{limit}");
        assertThat(statement("select", "selectHistoryIds")).contains("ORDER BY changed_time DESC, id DESC");
    }

    @Test
    void sqlMustStayPortableAcrossMysqlAndPostgresql() throws IOException {
        String sql = Files.readString(XML).replaceAll("(?s)<!--.*?-->", "");

        assertThat(sql)
                .doesNotContain("`")
                .doesNotContain("${")
                .doesNotContainIgnoringCase("NOW()")
                .doesNotContainIgnoringCase("IFNULL(")
                .doesNotContainIgnoringCase("ON DUPLICATE KEY");
        assertThat(Pattern.compile("LIMIT\\s+[^\\s,]+\\s*,", Pattern.CASE_INSENSITIVE).matcher(sql).find())
                .as("MySQL-only LIMIT offset, count")
                .isFalse();
    }

    private String statement(String tag, String id) throws IOException {
        String xml = Files.readString(XML);
        Matcher matcher = Pattern.compile(
                "<" + tag + " id=\"" + id + "\"[^>]*>(.*?)</" + tag + ">", Pattern.DOTALL).matcher(xml);
        assertThat(matcher.find()).as("missing <%s id=\"%s\">", tag, id).isTrue();
        return matcher.group(1);
    }
}
