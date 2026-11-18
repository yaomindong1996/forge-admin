package com.mdframe.forge.plugin.job.config;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Locale;

/**
 * 按 JDBC 产品名选择 Quartz JobStore Delegate。
 */
public final class QuartzDriverDelegateResolver {

    public static final String MYSQL = "org.quartz.impl.jdbcjobstore.StdJDBCDelegate";
    public static final String POSTGRES = "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate";
    public static final String SQLSERVER = "org.quartz.impl.jdbcjobstore.MSSQLDelegate";

    private QuartzDriverDelegateResolver() {
    }

    public static String resolve(DataSource dataSource) {
        if (dataSource == null) {
            return MYSQL;
        }
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            String product = meta.getDatabaseProductName();
            String url = meta.getURL();
            return resolve(product, url);
        } catch (Exception ex) {
            return MYSQL;
        }
    }

    static String resolve(String productName, String jdbcUrl) {
        String product = productName == null ? "" : productName.toLowerCase(Locale.ROOT);
        String url = jdbcUrl == null ? "" : jdbcUrl.toLowerCase(Locale.ROOT);
        if (product.contains("sql server") || url.contains("jdbc:sqlserver:")) {
            return SQLSERVER;
        }
        if (product.contains("postgresql") || url.contains("jdbc:postgresql:")) {
            return POSTGRES;
        }
        return MYSQL;
    }
}
