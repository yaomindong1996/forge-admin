package com.mdframe.forge.starter.tenant.handler;

import com.mdframe.forge.starter.tenant.config.TenantProperties;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 租户表结构检测器
 * 在应用启动时自动扫描数据库表结构，检测哪些表包含租户字段
 * 
 * <p>使用 SmartInitializingSingleton 确保在所有单例 Bean 初始化完成后立即执行，
 * 早于 ApplicationReadyEvent，避免启动过程中的 SQL 被错误拦截</p>
 * 
 * @author forge
 */
@Slf4j
@RequiredArgsConstructor
public class TenantTableChecker implements SmartInitializingSingleton {

    private final DataSource dataSource;
    private final TenantProperties tenantProperties;

    /**
     * 包含租户字段的表名集合（缓存）
     */
    private final Set<String> tablesWithTenantColumn = ConcurrentHashMap.newKeySet();

    /**
     * 不包含租户字段的表名集合（缓存）
     */
    private final Set<String> tablesWithoutTenantColumn = ConcurrentHashMap.newKeySet();

    /**
     * 是否已初始化
     */
    private volatile boolean initialized = false;

    /**
     * 是否正在初始化
     */
    private volatile boolean initializing = false;

    @Override
    public void afterSingletonsInstantiated() {
        init();
    }

    /**
     * 初始化表结构检测
     */
    public void init() {
        if (!tenantProperties.getAutoDetectTenantColumn()) {
            log.info("自动检测租户字段已禁用，跳过表结构扫描");
            initialized = true;
            return;
        }

        if (initialized || initializing) {
            return;
        }

        synchronized (this) {
            if (initialized || initializing) {
                return;
            }
            initializing = true;
        }

        log.info("开始扫描数据库表结构，检测租户字段: {}", tenantProperties.getColumn());
        long startTime = System.currentTimeMillis();

        // 在初始化过程中禁用租户拦截，避免扫描过程中的 SQL 被错误处理
        TenantContextHolder.setIgnore(true);
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String tenantColumn = tenantProperties.getColumn().toLowerCase();
            String catalog = connection.getCatalog();
            String schema = getSchemaName(metaData, connection);

            // 获取所有表
            try (ResultSet tables = metaData.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    
                    // 检查表中是否有租户字段
                    boolean hasTenantColumn = checkTableHasColumn(metaData, catalog, schema, tableName, tenantColumn);
                    
                    if (hasTenantColumn) {
                        tablesWithTenantColumn.add(tableName);
                        tablesWithTenantColumn.add(tableName.toLowerCase());
                        tablesWithTenantColumn.add(tableName.toUpperCase());
                    } else {
                        tablesWithoutTenantColumn.add(tableName);
                        tablesWithoutTenantColumn.add(tableName.toLowerCase());
                        tablesWithoutTenantColumn.add(tableName.toUpperCase());
                        log.debug("表 {} 不包含租户字段 {}，将自动忽略租户过滤", tableName, tenantColumn);
                    }
                }
            }

            initialized = true;
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("租户表结构扫描完成，耗时: {}ms，包含租户字段的表: {} 个，不包含租户字段的表: {} 个",
                    elapsed, tablesWithTenantColumn.size() / 3, tablesWithoutTenantColumn.size() / 3);

        } catch (Exception e) {
            log.error("扫描数据库表结构失败", e);
            // 初始化失败时，默认所有表都需要租户过滤（安全策略）
            initialized = true;
        } finally {
            TenantContextHolder.setIgnore(false);
            initializing = false;
        }
    }

    /**
     * 获取数据库 schema 名称
     */
    private String getSchemaName(DatabaseMetaData metaData, Connection connection) {
        try {
            // Oracle 和 PostgreSQL 使用 schema
            String databaseProductName = metaData.getDatabaseProductName().toUpperCase();
            if (databaseProductName.contains("ORACLE") || databaseProductName.contains("POSTGRE")) {
                return connection.getSchema();
            }
            // MySQL 使用 catalog，schema 传 null
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查表中是否包含指定字段
     */
    private boolean checkTableHasColumn(DatabaseMetaData metaData, String catalog, String schema, 
                                        String tableName, String columnName) {
        try (ResultSet columns = metaData.getColumns(catalog, schema, tableName, null)) {
            while (columns.next()) {
                String colName = columns.getString("COLUMN_NAME");
                if (colName != null && colName.toLowerCase().equals(columnName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("检查表 {} 的字段时出错: {}", tableName, e.getMessage());
        }
        return false;
    }

    /**
     * 判断表是否包含租户字段
     *
     * @param tableName 表名
     * @return true-包含租户字段，false-不包含
     */
    public boolean hasTenantColumn(String tableName) {
        // 如果未启用自动检测，默认返回 true（需要租户过滤）
        if (!tenantProperties.getAutoDetectTenantColumn()) {
            return true;
        }

        // 如果还未初始化完成，等待初始化完成
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    init();
                }
            }
        }

        // 先检查缓存
        if (tablesWithTenantColumn.contains(tableName)) {
            return true;
        }
        if (tablesWithoutTenantColumn.contains(tableName)) {
            return false;
        }

        // 缓存未命中，临时查询数据库（需要在忽略租户模式下执行）
        return checkTableHasTenantColumnFromDb(tableName);
    }

    /**
     * 从数据库实时查询表是否包含租户字段
     */
    private boolean checkTableHasTenantColumnFromDb(String tableName) {
        String tenantColumn = tenantProperties.getColumn().toLowerCase();
        
        // 设置忽略租户，避免查询被拦截
        TenantContextHolder.setIgnore(true);
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            String schema = getSchemaName(metaData, connection);
            
            boolean hasColumn = checkTableHasColumn(metaData, catalog, schema, tableName, tenantColumn);
            
            // 更新缓存
            if (hasColumn) {
                tablesWithTenantColumn.add(tableName);
            } else {
                tablesWithoutTenantColumn.add(tableName);
            }
            
            return hasColumn;
        } catch (Exception e) {
            log.warn("检查表 {} 是否包含租户字段时出错，默认需要租户过滤: {}", tableName, e.getMessage());
            return true;
        } finally {
            TenantContextHolder.setIgnore(false);
        }
    }

    /**
     * 刷新缓存
     */
    public void refresh() {
        tablesWithTenantColumn.clear();
        tablesWithoutTenantColumn.clear();
        initialized = false;
        init();
    }

    /**
     * 获取包含租户字段的表名集合（只读）
     */
    public Set<String> getTablesWithTenantColumn() {
        return Set.copyOf(tablesWithTenantColumn);
    }

    /**
     * 获取不包含租户字段的表名集合（只读）
     */
    public Set<String> getTablesWithoutTenantColumn() {
        return Set.copyOf(tablesWithoutTenantColumn);
    }

    /**
     * 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
}