package com.mdframe.forge.plugin.generator.util;

import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源管理工具类
 */
@Slf4j
public class DynamicDataSourceUtil {

    /**
     * 数据源缓存池
     */
    private static final Map<Long, DataSource> DATA_SOURCE_POOL = new ConcurrentHashMap<>();

    /**
     * 获取数据源连接
     */
    public static Connection getConnection(GenDatasource datasource) throws SQLException {
        DataSource dataSource = getOrCreateDataSource(datasource);
        return dataSource.getConnection();
    }

    /**
     * 获取或创建数据源
     */
    private static DataSource getOrCreateDataSource(GenDatasource datasource) {
        return DATA_SOURCE_POOL.computeIfAbsent(datasource.getDatasourceId(), key -> {
            log.info("创建数据源连接池: {}", datasource.getDatasourceName());
            return createDataSource(datasource);
        });
    }

    /**
     * 创建数据源
     */
    private static DataSource createDataSource(GenDatasource datasource) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(datasource.getUrl());
        config.setUsername(datasource.getUsername());
        config.setPassword(datasource.getPassword());
        config.setDriverClassName(datasource.getDriverClassName());
        
        // 连接池配置
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setConnectionTestQuery(datasource.getTestQuery());
        
        return new HikariDataSource(config);
    }

    /**
     * 测试数据源连接
     */
    public static boolean testConnection(GenDatasource datasource) {
        try (Connection conn = getConnection(datasource);
             Statement stmt = conn.createStatement()) {
            stmt.execute(datasource.getTestQuery());
            log.info("数据源连接测试成功: {}", datasource.getDatasourceName());
            return true;
        } catch (Exception e) {
            log.error("数据源连接测试失败: {}, 错误: {}", datasource.getDatasourceName(), e.getMessage());
            return false;
        }
    }

    /**
     * 移除数据源（关闭连接池）
     */
    public static void removeDataSource(Long datasourceId) {
        DataSource dataSource = DATA_SOURCE_POOL.remove(datasourceId);
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
            log.info("关闭数据源连接池: datasourceId={}", datasourceId);
        }
    }

    /**
     * 清空所有数据源
     */
    public static void clearAll() {
        DATA_SOURCE_POOL.forEach((id, ds) -> {
            if (ds instanceof HikariDataSource) {
                ((HikariDataSource) ds).close();
            }
        });
        DATA_SOURCE_POOL.clear();
        log.info("已清空所有数据源连接池");
    }

    /**
     * 执行查询并返回ResultSet（调用者负责关闭资源）
     */
    public static ResultSet executeQuery(GenDatasource datasource, String sql) throws SQLException {
        Connection conn = getConnection(datasource);
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(sql);
    }
}
