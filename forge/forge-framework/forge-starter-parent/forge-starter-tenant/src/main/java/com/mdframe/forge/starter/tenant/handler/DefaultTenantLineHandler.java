package com.mdframe.forge.starter.tenant.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.mdframe.forge.starter.tenant.config.TenantProperties;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;

import java.util.HashSet;
import java.util.Set;

/**
 * 多租户处理器
 * 实现MyBatis-Plus的租户拦截器，自动在SQL中添加租户条件
 * 
 * <p>支持两种忽略表的方式：</p>
 * <ol>
 *     <li>自动检测：通过 {@link TenantTableChecker} 扫描数据库表结构，
 *         自动判断表是否包含租户字段</li>
 *     <li>手动配置：通过配置文件或 {@link #addIgnoreTable(String)} 指定忽略的表</li>
 * </ol>
 */
@Slf4j
public class DefaultTenantLineHandler implements TenantLineHandler {

    private final TenantProperties tenantProperties;

    /**
     * 租户表结构检测器（可选，用于自动检测）
     */
    @Setter
    private TenantTableChecker tenantTableChecker;

    /**
     * 忽略租户的表集合（手动配置的缓存）
     */
    private final Set<String> ignoreTableCache = new HashSet<>();

    public DefaultTenantLineHandler(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            // 如果没有租户ID，返回NULL（这样不会过滤任何数据，但在某些场景下可能需要抛出异常）
            log.warn("当前上下文中没有租户ID，请检查租户设置");
            return new NullValue();
        }
        return new LongValue(tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return tenantProperties.getColumn();
    }

    @Override
    public boolean ignoreTable(String tableName) {
        // 1. 如果上下文设置了忽略租户，则跳过
        if (TenantContextHolder.isIgnore()) {
            return true;
        }

        // 2. 检查是否在手动配置的忽略表列表中
        if (tenantProperties.getIgnoreTables() != null &&
            tenantProperties.getIgnoreTables().contains(tableName)) {
            return true;
        }

        // 3. 检查手动添加到缓存的忽略表
        if (ignoreTableCache.contains(tableName)) {
            return true;
        }

        // 4. 自动检测：如果启用了自动检测，检查表是否包含租户字段
        if (tenantProperties.getAutoDetectTenantColumn() && tenantTableChecker != null) {
            // 不包含租户字段的表需要忽略
            return !tenantTableChecker.hasTenantColumn(tableName);
        }

        // 默认不忽略
        return false;
    }

    /**
     * 添加忽略的表到缓存
     *
     * @param tableName 表名
     */
    public void addIgnoreTable(String tableName) {
        ignoreTableCache.add(tableName);
    }

    /**
     * 移除忽略的表
     *
     * @param tableName 表名
     */
    public void removeIgnoreTable(String tableName) {
        ignoreTableCache.remove(tableName);
    }

    /**
     * 清空忽略表缓存
     */
    public void clearIgnoreTableCache() {
        ignoreTableCache.clear();
    }

    /**
     * 刷新表结构检测缓存（如果启用了自动检测）
     */
    public void refreshTableCheckCache() {
        if (tenantTableChecker != null) {
            tenantTableChecker.refresh();
        }
    }
}