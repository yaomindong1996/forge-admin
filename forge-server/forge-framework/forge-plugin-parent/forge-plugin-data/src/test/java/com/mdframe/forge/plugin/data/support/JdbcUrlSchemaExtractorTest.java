package com.mdframe.forge.plugin.data.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcUrlSchemaExtractorTest {

    @Test
    void extractsDatabaseFromMysqlUrlWithQuery() {
        assertThat(JdbcUrlSchemaExtractor.extract(
                "jdbc:mysql://127.0.0.1:3306/forge_admin?useSSL=false&serverTimezone=Asia/Shanghai"))
                .isEqualTo("forge_admin");
    }

    @Test
    void extractsWhenPasswordContainsQuestionMark() {
        // 旧实现 indexOf('?') 会命中密码里的 ?，再配合 lastIndexOf('/') 导致 substring 起止颠倒
        assertThat(JdbcUrlSchemaExtractor.extract(
                "jdbc:mysql://app:p?ssw0rd@10.0.0.8:3306/biz_db?useUnicode=true&characterEncoding=utf8"))
                .isEqualTo("biz_db");
    }

    @Test
    void ignoresSlashInsideQueryString() {
        assertThat(JdbcUrlSchemaExtractor.extract(
                "jdbc:mysql://127.0.0.1:3306/forge_admin?rewriteBatchedStatements=true&sessionVariables=sql_mode=''"))
                .isEqualTo("forge_admin");
        assertThat(JdbcUrlSchemaExtractor.extract(
                "jdbc:mysql://127.0.0.1:3306/forge_admin?x=a/b&y=1"))
                .isEqualTo("forge_admin");
    }

    @Test
    void returnsNullWhenNoPathDatabase() {
        assertThat(JdbcUrlSchemaExtractor.extract("jdbc:sqlserver://localhost:1433;databaseName=demo"))
                .isNull();
        assertThat(JdbcUrlSchemaExtractor.extract(null)).isNull();
        assertThat(JdbcUrlSchemaExtractor.extract("")).isNull();
    }
}
