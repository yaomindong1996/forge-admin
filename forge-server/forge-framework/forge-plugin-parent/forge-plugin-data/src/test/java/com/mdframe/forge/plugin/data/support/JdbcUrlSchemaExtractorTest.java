package com.mdframe.forge.plugin.data.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcUrlSchemaExtractorTest {

    @Test
    void prefersConfiguredSchemaThenCatalogThenUrl() {
        AtomicReference<String> catalog = new AtomicReference<>("from_catalog");
        AtomicReference<String> schema = new AtomicReference<>("from_schema");
        Connection connection = stubConnection(catalog, schema);
        String url = "jdbc:mysql://127.0.0.1:3306/from_url?serverTimezone=Asia/Shanghai";

        assertThat(JdbcUrlSchemaExtractor.resolve("configured", connection, url)).isEqualTo("configured");
        assertThat(JdbcUrlSchemaExtractor.resolve("  ", connection, url)).isEqualTo("from_catalog");
        catalog.set(null);
        assertThat(JdbcUrlSchemaExtractor.resolve(null, connection, url)).isEqualTo("from_schema");
        schema.set(null);
        assertThat(JdbcUrlSchemaExtractor.resolve(null, connection, url)).isEqualTo("from_url");
    }

    private static Connection stubConnection(AtomicReference<String> catalog, AtomicReference<String> schema) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (proxy, method, args) -> {
                    if ("getCatalog".equals(method.getName())) {
                        return catalog.get();
                    }
                    if ("getSchema".equals(method.getName())) {
                        return schema.get();
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("toString".equals(method.getName())) {
                        return "stub-connection";
                    }
                    return null;
                });
    }

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
