package com.mdframe.forge.starter.orm.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SQL Server 下将 {@code FOR UPDATE} / {@code LIMIT} 改写为 {@code WITH (UPDLOCK)} / {@code OFFSET FETCH}。
 * <p>MySQL / PostgreSQL 不改写。挂在租户等自定义拦截器之后、分页插件之前。
 */
@Slf4j
public class SqlServerDialectSqlInterceptor implements InnerInterceptor {

    private final ConcurrentHashMap<String, Boolean> productCache = new ConcurrentHashMap<>();

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        try {
            PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(sh);
            MappedStatement ms = mpSh.mappedStatement();
            SqlCommandType type = ms.getSqlCommandType();
            if (type != SqlCommandType.SELECT && type != SqlCommandType.UPDATE) {
                return;
            }
            if (!isSqlServer(connection, ms.getConfiguration().getDatabaseId())) {
                return;
            }
            PluginUtils.MPBoundSql mpBs = mpSh.mPBoundSql();
            String original = mpBs.sql();
            SqlServerDialectSqlRewriter.RewriteResult result = SqlServerDialectSqlRewriter.rewriteDetailed(original);
            if (!result.sql().equals(original)) {
                mpBs.sql(result.sql());
                if (result.swapLastTwoLimitOffsetParams()) {
                    swapLastTwoParameterMappings(mpBs);
                }
                if (log.isDebugEnabled()) {
                    log.debug("SQL Server dialect rewrite for {}: {} -> {}", ms.getId(), original, result.sql());
                }
            }
        } catch (Exception ex) {
            log.warn("SQL Server dialect rewrite skipped: {}", ex.toString());
        }
    }

    private void swapLastTwoParameterMappings(PluginUtils.MPBoundSql mpBs) {
        List<ParameterMapping> mappings = mpBs.parameterMappings();
        if (mappings == null || mappings.size() < 2) {
            return;
        }
        List<ParameterMapping> copy = new ArrayList<>(mappings);
        Collections.swap(copy, copy.size() - 2, copy.size() - 1);
        mpBs.parameterMappings(copy);
    }

    private boolean isSqlServer(Connection connection, String databaseId) {
        if (databaseId != null && "sqlserver".equalsIgnoreCase(databaseId.trim())) {
            return true;
        }
        if (connection == null) {
            return false;
        }
        try {
            DatabaseMetaData meta = connection.getMetaData();
            String url = meta.getURL();
            String cacheKey = url != null ? url : meta.getDatabaseProductName();
            return productCache.computeIfAbsent(cacheKey, key -> {
                try {
                    String product = meta.getDatabaseProductName();
                    if (product != null && product.toLowerCase(Locale.ROOT).contains("sql server")) {
                        return true;
                    }
                    return url != null && url.toLowerCase(Locale.ROOT).contains("jdbc:sqlserver:");
                } catch (SQLException e) {
                    return false;
                }
            });
        } catch (SQLException e) {
            return false;
        }
    }
}
