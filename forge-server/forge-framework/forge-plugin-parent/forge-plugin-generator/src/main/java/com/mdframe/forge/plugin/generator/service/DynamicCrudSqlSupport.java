package com.mdframe.forge.plugin.generator.service;

import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialectFactory;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 动态 CRUD SQL 片段与参数编译支持。
 *
 * <p>集中主键策略、分页方言、租户/逻辑删除谓词和 Mutation SQL 模板，
 * Query/Command Executor 通过 Repository 窄门面共享同一套规则。</p>
 */
@RequiredArgsConstructor
final class DynamicCrudSqlSupport {

    private static final String DEFAULT_PRIMARY_KEY = "id";

    private final DynamicCrudRepository repository;
    private final RuntimeDatabaseDialectFactory dialectFactory;

    String buildSelectSql(String selectClause, String tableName, StringBuilder whereClause) {
        String sql = selectClause + " FROM " + tableName;
        return whereClause.isEmpty() ? sql : sql + " WHERE " + whereClause;
    }

    StringBuilder buildBaseWhereClause(String tableName, String tableAlias) {
        StringBuilder whereClause = new StringBuilder();
        appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName, tableAlias);
        return whereClause;
    }

    MapSqlParameterSource buildBaseQueryParams(String tableAlias) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        appendBaseQueryConditions(new StringBuilder(), params, null, tableAlias);
        appendLogicDeleteParam(params);
        return params;
    }

    String buildPageDataSql(String tableName,
                            StringBuilder whereClause,
                            String orderBy,
                            int pageNum,
                            int pageSize) {
        return paginate(buildSelectSql("SELECT *", tableName, whereClause)
            + buildOrderByClause(orderBy), pageNum, pageSize);
    }

    String buildOrderByClause(String orderBy) {
        return StringUtils.isNotBlank(orderBy)
            ? " ORDER BY " + orderBy
            : " ORDER BY " + primaryKeyColumn() + " DESC";
    }

    StringBuilder buildIdWhereClause(String tableName, String primaryKeyColumn) {
        repository.validateIdentifier(primaryKeyColumn);
        StringBuilder whereClause = new StringBuilder(primaryKeyColumn + " = :id");
        appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName, null);
        return whereClause;
    }

    MapSqlParameterSource buildIdQueryParams(Object id) {
        return buildIdQueryParams(null, primaryKeyColumn(), id);
    }

    MapSqlParameterSource buildIdQueryParams(String tableName, String primaryKeyColumn, Object id) {
        MapSqlParameterSource params = buildBaseQueryParams(null);
        if (tableName != null) {
            appendLogicDeleteParam(params, tableName);
            params.addValue("id", repository.coercePrimaryKeyValue(tableName, primaryKeyColumn, id));
        } else {
            params.addValue("id", id);
        }
        return params;
    }

    void appendBaseQueryConditions(StringBuilder whereClause,
                                   MapSqlParameterSource params,
                                   String tableName,
                                   String tableAlias) {
        DynamicCrudWritePolicy policy = repository.writePolicy();
        String tenantColumn = policy.tenantColumn();
        if (policy.tenantStrategyEnabled()
            && (tableName == null || repository.getTableColumns(tableName).contains(tenantColumn))) {
            appendTenantWhereClause(whereClause, params, tableAlias);
        }
        if (tableName != null && repository.hasDelFlag(tableName)) {
            appendWhereCondition(whereClause,
                qualifyColumn(tableAlias, policy.logicDeleteColumn()) + " = :logicActiveValue");
            appendLogicDeleteParam(params, tableName);
        }
    }

    String buildWhereSql(StringBuilder whereClause) {
        return whereClause.isEmpty() ? "" : " WHERE " + whereClause;
    }

    String qualifyColumn(String tableAlias, String columnName) {
        return StringUtils.isBlank(tableAlias) ? columnName : tableAlias + "." + columnName;
    }

    String primaryKeyColumn() {
        LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
        String columnName = context == null || context.getPrimaryKey() == null
            ? DEFAULT_PRIMARY_KEY
            : context.getPrimaryKey().getColumnName();
        columnName = StringUtils.defaultIfBlank(columnName, DEFAULT_PRIMARY_KEY);
        repository.validateIdentifier(columnName);
        return columnName;
    }

    String primaryKeyField() {
        LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
        String field = context == null || context.getPrimaryKey() == null
            ? DEFAULT_PRIMARY_KEY
            : context.getPrimaryKey().getField();
        field = StringUtils.defaultIfBlank(field, DEFAULT_PRIMARY_KEY);
        repository.validateIdentifier(field);
        return field;
    }

    String paginate(String sql, int pageNum, int pageSize) {
        long limit = Math.max(1, pageSize);
        long offset = Math.max(0, pageNum - 1L) * limit;
        return dialectFactory.resolve(LowcodeRuntimeDataSourceContextHolder.get()).paginate(sql, offset, limit);
    }

    String limit(String sql, int limit) {
        return dialectFactory.resolve(LowcodeRuntimeDataSourceContextHolder.get())
            .paginate(sql, 0, Math.max(1, limit));
    }

    void appendTenantWhereClause(StringBuilder whereClause,
                                 MapSqlParameterSource params,
                                 String tableAlias) {
        Long tenantId = TenantContextHolder.getTenantId();
        DynamicCrudWritePolicy policy = repository.writePolicy();
        if (tenantId == null || !policy.tenantStrategyEnabled()) {
            return;
        }
        appendWhereCondition(whereClause,
            qualifyColumn(tableAlias, policy.tenantColumn()) + " = :tenantId");
        params.addValue("tenantId", tenantId);
    }

    void appendLogicDeleteParam(MapSqlParameterSource params) {
        appendLogicDeleteParam(params, null);
    }

    void appendLogicDeleteParam(MapSqlParameterSource params, String tableName) {
        DynamicCrudWritePolicy policy = repository.writePolicy();
        if (params == null || !policy.logicDeleteEnabled()) {
            return;
        }
        Object active = policy.logicActiveValue();
        if (tableName != null) {
            active = repository.coerceLogicFlagLiteral(tableName, policy.logicDeleteColumn(), active);
        }
        params.addValue("logicActiveValue", active);
    }

    void appendWhereCondition(StringBuilder whereClause, String condition) {
        if (!whereClause.isEmpty()) {
            whereClause.append(" AND ");
        }
        whereClause.append(condition);
    }

    void appendSqlCondition(StringBuilder whereClause,
                            MapSqlParameterSource params,
                            DynamicCrudRepository.SqlCondition condition) {
        if (condition == null || StringUtils.isBlank(condition.sql())) {
            return;
        }
        appendWhereCondition(whereClause, "(" + condition.sql() + ")");
        if (condition.params() != null) {
            condition.params().forEach(params::addValue);
        }
    }

    String appendTenantCondition(String sql, MapSqlParameterSource params, String tableName) {
        Long tenantId = TenantContextHolder.getTenantId();
        DynamicCrudWritePolicy policy = repository.writePolicy();
        if (tenantId == null || !policy.tenantStrategyEnabled()) {
            return sql;
        }
        String tenantColumn = policy.tenantColumn();
        if (tableName != null && !repository.getTableColumns(tableName).contains(tenantColumn)) {
            return sql;
        }
        params.addValue("tenantId", tenantId);
        return sql + " AND " + tenantColumn + " = :tenantId";
    }

    String appendLogicActiveCondition(String sql, MapSqlParameterSource params, String tableName) {
        if (tableName == null || !repository.hasDelFlag(tableName)) {
            return sql;
        }
        DynamicCrudWritePolicy policy = repository.writePolicy();
        appendLogicDeleteParam(params, tableName);
        return sql + " AND " + policy.logicDeleteColumn() + " = :logicActiveValue";
    }

    String appendSqlCondition(String sql,
                              MapSqlParameterSource params,
                              DynamicCrudRepository.SqlCondition condition) {
        if (condition == null || StringUtils.isBlank(condition.sql())) {
            return sql;
        }
        if (condition.params() != null) {
            condition.params().forEach(params::addValue);
        }
        return sql + " AND (" + condition.sql() + ")";
    }

    String buildInsertSql(String tableName, Map<String, Object> data) {
        String placeholders = data.keySet().stream()
            .map(column -> ":" + column)
            .collect(Collectors.joining(", "));
        return "INSERT INTO " + tableName + " (" + String.join(", ", data.keySet())
            + ") VALUES (" + placeholders + ")";
    }

    String buildUpdateSql(String tableName, Map<String, Object> data, String primaryKeyColumn) {
        String setClauses = data.keySet().stream()
            .map(column -> column + " = :" + column)
            .collect(Collectors.joining(", "));
        return "UPDATE " + tableName + " SET " + setClauses + " WHERE " + primaryKeyColumn + " = :id";
    }

    String buildDeleteSql(String tableName, boolean logicDelete, String primaryKeyColumn, boolean batch) {
        String predicate = batch ? " IN (:ids)" : " = :id";
        if (logicDelete) {
            String setClause = repository.writePolicy()
                .logicDeleteSetClause(repository.getTableColumns(tableName));
            return "UPDATE " + tableName + " SET " + setClause + " WHERE " + primaryKeyColumn + predicate;
        }
        return "DELETE FROM " + tableName + " WHERE " + primaryKeyColumn + predicate;
    }

    MapSqlParameterSource toSqlParams(Map<String, Object> data) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        data.forEach(params::addValue);
        return params;
    }

    MapSqlParameterSource toSqlParams(Map<String, Object> data, Object id) {
        return toSqlParams(data, null, primaryKeyColumn(), id);
    }

    MapSqlParameterSource toSqlParams(Map<String, Object> data,
                                      String tableName,
                                      String primaryKeyColumn,
                                      Object id) {
        MapSqlParameterSource params = toSqlParams(data);
        Object boundId = tableName == null
            ? id
            : repository.coercePrimaryKeyValue(tableName, primaryKeyColumn, id);
        return params.addValue("id", boundId);
    }

    MapSqlParameterSource toIdParam(Object id) {
        return toIdParam(null, primaryKeyColumn(), id);
    }

    MapSqlParameterSource toIdParam(String tableName, String primaryKeyColumn, Object id) {
        Object boundId = tableName == null
            ? id
            : repository.coercePrimaryKeyValue(tableName, primaryKeyColumn, id);
        return new MapSqlParameterSource().addValue("id", boundId);
    }

    List<?> coerceIds(String tableName, String primaryKeyColumn, List<?> ids) {
        return repository.coercePrimaryKeyValues(tableName, primaryKeyColumn, ids);
    }
}
