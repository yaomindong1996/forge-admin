package com.mdframe.forge.starter.tenant.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.mdframe.forge.starter.tenant.config.TenantProperties;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;

import java.util.HashSet;
import java.util.Set;

/**
 * 多租户处理器
 * 实现MyBatis-Plus的租户拦截器，自动在SQL中添加租户条件
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultTenantLineHandler implements TenantLineHandler {

    private final TenantProperties tenantProperties;

    /**
     * 忽略租户的表集合（缓存）
     */
    private final Set<String> ignoreTableCache = new HashSet<>();

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

        // 2. 检查是否在忽略表列表中
        if (tenantProperties.getIgnoreTables() != null &&
            tenantProperties.getIgnoreTables().contains(tableName)) {
            return true;
        }

        // 3. 使用缓存优化性能
        return ignoreTableCache.contains(tableName);
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
}
