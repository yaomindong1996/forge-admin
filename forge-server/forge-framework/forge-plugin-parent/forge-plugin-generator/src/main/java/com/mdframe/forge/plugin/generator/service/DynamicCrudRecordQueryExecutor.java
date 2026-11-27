package com.mdframe.forge.plugin.generator.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 动态记录查询执行器。
 *
 * <p>按 CQRS 将详情、批量、树节点和唯一性探测从写仓储门面中分离，
 * 复用 Repository 的数据源、租户、逻辑删除与数据权限基础设施。</p>
 */
@RequiredArgsConstructor
final class DynamicCrudRecordQueryExecutor {

    private final DynamicCrudRepository support;

    Map<String, Object> selectById(String tableName,
                                   String primaryKeyColumn,
                                   Object id,
                                   DynamicCrudRepository.SqlCondition dataScopeCondition) {
        support.validateTableName(tableName);
        support.validateIdentifier(primaryKeyColumn);
        StringBuilder whereClause = support.buildIdWhereClause(tableName, primaryKeyColumn);
        MapSqlParameterSource params = support.buildIdQueryParams(tableName, primaryKeyColumn, id);
        support.appendSqlCondition(whereClause, params, dataScopeCondition);
        String sql = support.buildSelectSql("SELECT *", tableName, whereClause);
        List<Map<String, Object>> results = support.jdbc().queryForList(sql, params);
        return results.isEmpty() ? null : results.get(0);
    }

    List<Map<String, Object>> selectByIds(String tableName,
                                          String primaryKeyColumn,
                                          List<?> ids,
                                          DynamicCrudRepository.SqlCondition dataScopeCondition) {
        support.validateTableName(tableName);
        support.validateIdentifier(primaryKeyColumn);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource params = support.buildBaseQueryParams();
        params.addValue("ids", support.coercePrimaryKeyValues(tableName, primaryKeyColumn, ids));
        StringBuilder whereClause = new StringBuilder(primaryKeyColumn + " IN (:ids)");
        support.appendBaseQueryConditions(whereClause, params, tableName);
        support.appendSqlCondition(whereClause, params, dataScopeCondition);
        return support.jdbc().queryForList(support.buildSelectSql("SELECT *", tableName, whereClause), params);
    }

    Map<String, Object> selectByIdForUpdate(String tableName,
                                            String primaryKeyColumn,
                                            Object id,
                                            DynamicCrudRepository.SqlCondition dataScopeCondition) {
        support.validateTableName(tableName);
        support.validateIdentifier(primaryKeyColumn);
        StringBuilder whereClause = support.buildIdWhereClause(tableName, primaryKeyColumn);
        MapSqlParameterSource params = support.buildIdQueryParams(tableName, primaryKeyColumn, id);
        support.appendSqlCondition(whereClause, params, dataScopeCondition);
        String sql = support.buildSelectSql("SELECT *", tableName, whereClause) + " FOR UPDATE";
        List<Map<String, Object>> results = support.jdbc().queryForList(sql, params);
        return results.isEmpty() ? null : results.get(0);
    }

    List<Map<String, Object>> selectListByColumn(String tableName, String columnName, Object value) {
        support.validateTableName(tableName);
        support.validateIdentifier(columnName);
        if (value == null) {
            return List.of();
        }
        Object queryValue = support.normalizeColumnQueryValue(tableName, columnName, value);
        StringBuilder whereClause = new StringBuilder(columnName + " = :value");
        support.appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);
        MapSqlParameterSource params = support.buildBaseQueryParams();
        params.addValue("value", queryValue);
        String primaryKey = support.primaryKeyColumn();
        String orderColumn = support.getTableColumns(tableName).contains(primaryKey) ? primaryKey : columnName;
        String sql = support.buildSelectSql("SELECT *", tableName, whereClause)
            + " ORDER BY " + orderColumn + " ASC";
        return support.jdbc().queryForList(sql, params);
    }

    List<Map<String, Object>> selectListByColumnIn(String tableName,
                                                   String columnName,
                                                   Collection<?> values,
                                                   DynamicCrudRepository.SqlCondition dataScopeCondition) {
        support.validateTableName(tableName);
        support.validateIdentifier(columnName);
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<Object> nonNullValues = values.stream()
            .filter(Objects::nonNull)
            .distinct()
            .map(Object.class::cast)
            .toList();
        if (nonNullValues.isEmpty()) {
            return List.of();
        }
        List<Object> queryValues = nonNullValues.stream()
            .map(value -> support.normalizeColumnQueryValue(tableName, columnName, value))
            .distinct()
            .toList();
        StringBuilder whereClause = new StringBuilder(columnName + " IN (:values)");
        support.appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);
        MapSqlParameterSource params = support.buildBaseQueryParams();
        params.addValue("values", queryValues);
        support.appendSqlCondition(whereClause, params, dataScopeCondition);
        String primaryKey = support.primaryKeyColumn();
        String orderColumn = support.getTableColumns(tableName).contains(primaryKey) ? primaryKey : columnName;
        String sql = support.buildSelectSql("SELECT *", tableName, whereClause)
            + " ORDER BY " + orderColumn + " ASC";
        return support.jdbc().queryForList(sql, params);
    }

    List<Map<String, Object>> selectTreeChildren(String tableName,
                                                 String parentColumn,
                                                 Object parentValue,
                                                 String orderBy,
                                                 int limit,
                                                 DynamicCrudRepository.SqlCondition dataScopeCondition) {
        support.validateTableName(tableName);
        support.validateIdentifier(parentColumn);
        boolean rootQuery = parentValue == null || StringUtils.isBlank(String.valueOf(parentValue));
        StringBuilder whereClause = rootQuery
            ? new StringBuilder("(" + parentColumn + " IS NULL OR " + parentColumn
                + " = :zeroValue OR " + parentColumn + " = :emptyValue)")
            : new StringBuilder(parentColumn + " = :parentValue");
        support.appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);
        MapSqlParameterSource params = support.buildBaseQueryParams();
        if (rootQuery) {
            params.addValue("zeroValue", "0");
            params.addValue("emptyValue", "");
        } else {
            params.addValue("parentValue", parentValue);
        }
        support.appendSqlCondition(whereClause, params, dataScopeCondition);
        String sql = support.buildSelectSql("SELECT *", tableName, whereClause)
            + support.buildOrderByClause(orderBy);
        return support.jdbc().queryForList(support.limitSql(sql, Math.max(1, limit)), params);
    }

    boolean existsByColumn(String tableName,
                           String columnName,
                           Object value,
                           DynamicCrudRepository.SqlCondition dataScopeCondition) {
        support.validateTableName(tableName);
        support.validateIdentifier(columnName);
        if (value == null) {
            return false;
        }
        StringBuilder whereClause = new StringBuilder(columnName + " = :value");
        support.appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);
        MapSqlParameterSource params = support.buildBaseQueryParams();
        params.addValue("value", value);
        support.appendSqlCondition(whereClause, params, dataScopeCondition);
        Long count = support.jdbc().queryForObject(
            support.buildSelectSql("SELECT COUNT(1)", tableName, whereClause), params, Long.class);
        return count != null && count > 0;
    }

    Long selectFirstIdByColumn(String tableName, String columnName, Object value) {
        support.validateTableName(tableName);
        support.validateIdentifier(columnName);
        if (value == null) {
            return null;
        }
        StringBuilder whereClause = new StringBuilder(columnName + " = :value");
        support.appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);
        MapSqlParameterSource params = support.buildBaseQueryParams();
        params.addValue("value", value);
        String sql = support.buildSelectSql("SELECT id", tableName, whereClause) + " ORDER BY id ASC";
        List<Long> ids = support.jdbc().queryForList(support.limitSql(sql, 1), params, Long.class);
        return ids.isEmpty() ? null : ids.get(0);
    }

    boolean existsByColumns(String tableName,
                            Map<String, Object> columnValues,
                            String primaryKeyColumn,
                            Object excludeId,
                            DynamicCrudRepository.SqlCondition dataScopeCondition) {
        support.validateTableName(tableName);
        support.validateIdentifier(primaryKeyColumn);
        if (columnValues == null || columnValues.isEmpty()) {
            return false;
        }
        StringBuilder whereClause = new StringBuilder();
        MapSqlParameterSource params = support.buildBaseQueryParams();
        int index = 0;
        for (Map.Entry<String, Object> entry : columnValues.entrySet()) {
            String columnName = entry.getKey();
            support.validateIdentifier(columnName);
            String paramName = "uniqueValue" + index++;
            if (entry.getValue() == null) {
                support.appendWhereCondition(whereClause, columnName + " IS NULL");
            } else {
                support.appendWhereCondition(whereClause, columnName + " = :" + paramName);
                params.addValue(paramName, entry.getValue());
            }
        }
        support.appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);
        if (excludeId != null) {
            support.appendWhereCondition(whereClause, primaryKeyColumn + " <> :excludeId");
            params.addValue("excludeId", excludeId);
        }
        support.appendSqlCondition(whereClause, params, dataScopeCondition);
        Long count = support.jdbc().queryForObject(
            support.buildSelectSql("SELECT COUNT(1)", tableName, whereClause), params, Long.class);
        return count != null && count > 0;
    }
}
