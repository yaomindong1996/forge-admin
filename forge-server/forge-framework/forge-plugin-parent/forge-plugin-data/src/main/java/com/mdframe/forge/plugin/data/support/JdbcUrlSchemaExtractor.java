package com.mdframe.forge.plugin.data.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析拉表/列元数据用的库名（MySQL catalog / 其它库 schema / SQL Server databaseName）。
 * <p>
 * 优先级：连接上显式配置的 Schema → 已打开连接的 {@link Connection#getCatalog()} /
 * {@link Connection#getSchema()} → 最后才从 JDBC URL 字符串兜底。
 * URL 截取不可靠（如 {@code serverTimezone=Asia/Shanghai} 含 {@code /}），仅作兜底。
 */
public final class JdbcUrlSchemaExtractor {

    private static final Pattern SQLSERVER_DATABASE_NAME =
            Pattern.compile("(?:^|[;])\\s*databaseName\\s*=\\s*([^;]+)", Pattern.CASE_INSENSITIVE);

    private JdbcUrlSchemaExtractor() {
    }

    /**
     * @param configuredSchema 连接配置里的 schemaName，可空
     * @param connection       已打开的 JDBC 连接，可空（空则跳过 catalog/schema）
     * @param jdbcUrl          JDBC URL，可空
     */
    public static String resolve(String configuredSchema, Connection connection, String jdbcUrl) {
        if (configuredSchema != null && !configuredSchema.isBlank()) {
            return configuredSchema.trim();
        }
        if (connection != null) {
            try {
                String catalog = connection.getCatalog();
                if (catalog != null && !catalog.isBlank()) {
                    return catalog.trim();
                }
            }
            catch (SQLException ignored) {
                // 部分驱动不支持，继续尝试 schema / URL
            }
            try {
                String schema = connection.getSchema();
                if (schema != null && !schema.isBlank()) {
                    return schema.trim();
                }
            }
            catch (SQLException ignored) {
                // fall through
            }
        }
        return extract(jdbcUrl);
    }

    /**
     * 从 JDBC URL 解析库名/schema 段。仅作兜底。
     * <ul>
     *   <li>MySQL/Postgres 等：{@code jdbc:xxx://host:port/db?...}</li>
     *   <li>SQL Server：{@code jdbc:sqlserver://host:1433;databaseName=db}</li>
     * </ul>
     */
    public static String extract(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return null;
        }
        String trimmed = jdbcUrl.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("jdbc:sqlserver:")) {
            return extractSqlServerDatabaseName(trimmed);
        }
        int schemeSep = trimmed.indexOf("://");
        int searchFrom = schemeSep >= 0 ? schemeSep + 3 : 0;
        // 主机段之后的第一个 '/' 才是库名起点，避免查询参数里的 '/' 干扰
        int start = trimmed.indexOf('/', searchFrom);
        if (start < 0 || start + 1 >= trimmed.length()) {
            return null;
        }
        int end = trimmed.indexOf('?', start + 1);
        if (end < 0) {
            end = trimmed.length();
        }
        if (start + 1 >= end) {
            return null;
        }
        String schema = trimmed.substring(start + 1, end).trim();
        return schema.isEmpty() ? null : schema;
    }

    private static String extractSqlServerDatabaseName(String jdbcUrl) {
        Matcher matcher = SQLSERVER_DATABASE_NAME.matcher(jdbcUrl);
        if (!matcher.find()) {
            return null;
        }
        String name = matcher.group(1).trim();
        return name.isEmpty() ? null : name;
    }
}
