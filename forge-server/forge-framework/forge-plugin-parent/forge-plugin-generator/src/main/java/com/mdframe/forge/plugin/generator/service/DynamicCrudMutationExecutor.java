package com.mdframe.forge.plugin.generator.service;

import com.mdframe.forge.plugin.generator.enums.DataAuditSourceType;
import com.mdframe.forge.plugin.generator.service.audit.DataAuditRecordIds;
import com.mdframe.forge.plugin.generator.service.audit.DataAuditTransactionHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 动态记录变更命令执行器。
 *
 * <p>按 CQRS/Command 将新增、更新、原子数值调整和删除命令从查询门面分离，
 * 同时固化“审计前置 → JDBC 副作用 → 审计后置”的执行模板。</p>
 */
@RequiredArgsConstructor
final class DynamicCrudMutationExecutor {

    private final DynamicCrudRepository support;

    int insert(String tableName, Map<String, Object> data) {
        support.validateTableName(tableName);
        Map<String, Object> insertData = support.prepareInsertData(tableName, data);
        String primaryKey = support.primaryKeyColumn();
        DataAuditTransactionHolder.prepareWrite(tableName, primaryKey, insertData.get(primaryKey),
            DataAuditTransactionHolder.WriteKind.INSERT);
        int affected = support.jdbc().update(
            support.buildInsertSql(tableName, insertData), support.toSqlParams(insertData));
        DataAuditTransactionHolder.afterWrite(tableName, insertData.get(primaryKey), insertData,
            DataAuditTransactionHolder.currentFieldSource(DataAuditSourceType.FORM),
            DataAuditTransactionHolder.WriteKind.INSERT, affected);
        return affected;
    }

    Object insertReturningKey(String tableName,
                              Map<String, Object> data,
                              String primaryKeyColumn,
                              boolean autoIncrement) {
        support.validateTableName(tableName);
        support.validateIdentifier(primaryKeyColumn);
        Map<String, Object> insertData = support.prepareInsertData(tableName, data);
        if (!autoIncrement && !insertData.containsKey(primaryKeyColumn)) {
            throw new BusinessException("新增操作缺少主键字段: " + primaryKeyColumn);
        }
        DataAuditTransactionHolder.prepareWrite(tableName, primaryKeyColumn, insertData.get(primaryKeyColumn),
            DataAuditTransactionHolder.WriteKind.INSERT);
        Object generatedKey;
        if (!autoIncrement) {
            support.jdbc().update(support.buildInsertSql(tableName, insertData), support.toSqlParams(insertData));
            generatedKey = insertData.get(primaryKeyColumn);
        } else {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            support.jdbc().update(support.buildInsertSql(tableName, insertData),
                support.toSqlParams(insertData), keyHolder, new String[]{primaryKeyColumn});
            Number key = keyHolder.getKey();
            generatedKey = key == null ? insertData.get(primaryKeyColumn) : key;
        }
        DataAuditTransactionHolder.afterWrite(tableName, generatedKey, insertData,
            DataAuditTransactionHolder.currentFieldSource(DataAuditSourceType.FORM),
            DataAuditTransactionHolder.WriteKind.INSERT, 1);
        return generatedKey;
    }

    int updateById(String tableName,
                   String primaryKeyColumn,
                   Object id,
                   Map<String, Object> data,
                   DynamicCrudRepository.SqlCondition dataScopeCondition) {
        support.validateTableName(tableName);
        support.validateIdentifier(primaryKeyColumn);
        Map<String, Object> updateData = support.prepareUpdateData(tableName, data, primaryKeyColumn);
        DataAuditTransactionHolder.prepareWrite(
            tableName, primaryKeyColumn, id, DataAuditTransactionHolder.WriteKind.UPDATE);
        MapSqlParameterSource params = support.toSqlParams(updateData, tableName, primaryKeyColumn, id);
        String sql = support.appendTenantCondition(
            support.buildUpdateSql(tableName, updateData, primaryKeyColumn), params, tableName);
        sql = support.appendLogicActiveCondition(sql, params, tableName);
        sql = support.appendSqlCondition(sql, params, dataScopeCondition);
        int affected = support.jdbc().update(sql, params);
        DataAuditTransactionHolder.afterWrite(tableName, id, updateData,
            DataAuditTransactionHolder.currentFieldSource(DataAuditSourceType.FORM),
            DataAuditTransactionHolder.WriteKind.UPDATE, affected);
        return affected;
    }

    int adjustNumbersById(String tableName,
                          String primaryKeyColumn,
                          Object id,
                          Map<String, BigDecimal> deltas,
                          Map<String, BigDecimal> minimums,
                          Map<String, BigDecimal> maximums,
                          DynamicCrudRepository.SqlCondition condition) {
        support.validateTableName(tableName);
        support.validateIdentifier(primaryKeyColumn);
        if (id == null) {
            throw new BusinessException("数值调整缺少目标记录 ID");
        }
        if (deltas == null || deltas.isEmpty()) {
            throw new BusinessException("数值调整字段不能为空");
        }
        Set<String> tableColumns = support.getTableColumns(tableName);
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", support.coercePrimaryKeyValue(tableName, primaryKeyColumn, id));
        List<String> setClauses = new ArrayList<>();
        List<String> boundConditions = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, BigDecimal> entry : deltas.entrySet()) {
            String column = entry.getKey();
            support.validateIdentifier(column);
            if (!tableColumns.contains(column)) {
                throw new BusinessException("数值调整字段不存在");
            }
            BigDecimal delta = entry.getValue();
            if (delta == null) {
                throw new BusinessException("数值调整量不能为空");
            }
            String deltaParam = "adjustDelta" + index;
            params.addValue(deltaParam, delta);
            setClauses.add(column + " = " + column + " + :" + deltaParam);
            appendBound(minimums, column, "adjustMin" + index, ">=", deltaParam, params, boundConditions);
            appendBound(maximums, column, "adjustMax" + index, "<=", deltaParam, params, boundConditions);
            index++;
        }
        Map<String, Object> auditValues = new LinkedHashMap<>();
        support.writePolicy().fillUpdateAuditFields(auditValues, tableColumns);
        auditValues.forEach((column, value) -> {
            if (!deltas.containsKey(column)) {
                support.validateIdentifier(column);
                setClauses.add(column + " = :" + column);
                params.addValue(column, value);
            }
        });
        String sql = "UPDATE " + tableName + " SET " + String.join(", ", setClauses)
            + " WHERE " + primaryKeyColumn + " = :id";
        sql = support.appendTenantCondition(sql, params, tableName);
        sql = support.appendLogicActiveCondition(sql, params, tableName);
        sql = support.appendSqlCondition(sql, params, condition);
        for (String bound : boundConditions) {
            sql += " AND (" + bound + ")";
        }
        DataAuditTransactionHolder.prepareWrite(
            tableName, primaryKeyColumn, id, DataAuditTransactionHolder.WriteKind.UPDATE);
        int affected = support.jdbc().update(sql, params);
        DataAuditTransactionHolder.afterWrite(tableName, id, Map.of(),
            DataAuditTransactionHolder.currentFieldSource(DataAuditSourceType.BUSINESS_ACTION),
            DataAuditTransactionHolder.WriteKind.UPDATE, affected);
        return affected;
    }

    int deleteById(String tableName,
                   String primaryKeyColumn,
                   Object id,
                   boolean logicDelete,
                   DynamicCrudRepository.SqlCondition dataScopeCondition) {
        support.validateTableName(tableName);
        support.validateIdentifier(primaryKeyColumn);
        MapSqlParameterSource params = support.toIdParam(tableName, primaryKeyColumn, id);
        if (logicDelete) {
            params.addValue("deletedValue",
                support.coerceLogicFlagLiteral(tableName, support.writePolicy().logicDeleteColumn(),
                    support.logicDeletedValue()));
        }
        DataAuditTransactionHolder.prepareWrite(
            tableName, primaryKeyColumn, id, DataAuditTransactionHolder.WriteKind.DELETE);
        String sql = support.appendTenantCondition(
            support.buildDeleteSql(tableName, logicDelete, primaryKeyColumn), params, tableName);
        sql = support.appendSqlCondition(sql, params, dataScopeCondition);
        int affected = support.jdbc().update(sql, params);
        DataAuditTransactionHolder.afterWrite(tableName, id, Map.of(),
            DataAuditTransactionHolder.currentFieldSource(DataAuditSourceType.FORM),
            DataAuditTransactionHolder.WriteKind.DELETE, affected);
        return affected;
    }

    int deleteByIds(String tableName,
                    String primaryKeyColumn,
                    List<?> ids,
                    boolean logicDelete,
                    DynamicCrudRepository.SqlCondition dataScopeCondition,
                    Map<String, Map<String, Object>> beforeById) {
        support.validateTableName(tableName);
        support.validateIdentifier(primaryKeyColumn);
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("ids", support.coercePrimaryKeyValues(tableName, primaryKeyColumn, ids));
        if (logicDelete) {
            params.addValue("deletedValue",
                support.coerceLogicFlagLiteral(tableName, support.writePolicy().logicDeleteColumn(),
                    support.logicDeletedValue()));
        }
        for (Object id : ids) {
            Map<String, Object> snapshot = null;
            if (beforeById != null) {
                String key = DataAuditRecordIds.normalize(id);
                if (key != null) {
                    snapshot = beforeById.get(key);
                }
            }
            DataAuditTransactionHolder.prepareWrite(
                tableName, primaryKeyColumn, id, DataAuditTransactionHolder.WriteKind.DELETE, snapshot);
        }
        String sql = support.appendTenantCondition(
            support.buildBatchDeleteSql(tableName, logicDelete, primaryKeyColumn), params, tableName);
        sql = support.appendSqlCondition(sql, params, dataScopeCondition);
        int affected = support.jdbc().update(sql, params);
        for (Object id : ids) {
            DataAuditTransactionHolder.afterWrite(tableName, id, Map.of(),
                DataAuditTransactionHolder.currentFieldSource(DataAuditSourceType.FORM),
                DataAuditTransactionHolder.WriteKind.DELETE, affected > 0 ? 1 : 0);
        }
        return affected;
    }

    int deleteByColumn(String tableName, String columnName, Object value, boolean logicDelete) {
        support.validateTableName(tableName);
        support.validateIdentifier(columnName);
        if (value == null) {
            return 0;
        }
        support.captureColumnDelete(tableName, columnName, value);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("value", value);
        String sql;
        if (logicDelete) {
            params.addValue("deletedValue", support.logicDeletedValue());
            sql = "UPDATE " + tableName + " SET " + support.logicDeleteSetClause(tableName)
                + " WHERE " + columnName + " = :value";
        } else {
            sql = "DELETE FROM " + tableName + " WHERE " + columnName + " = :value";
        }
        return support.jdbc().update(support.appendTenantCondition(sql, params, tableName), params);
    }

    private void appendBound(Map<String, BigDecimal> bounds,
                             String column,
                             String parameter,
                             String operator,
                             String deltaParameter,
                             MapSqlParameterSource params,
                             List<String> conditions) {
        BigDecimal value = bounds == null ? null : bounds.get(column);
        if (value != null) {
            params.addValue(parameter, value);
            conditions.add(column + " + :" + deltaParameter + " " + operator + " :" + parameter);
        }
    }
}
