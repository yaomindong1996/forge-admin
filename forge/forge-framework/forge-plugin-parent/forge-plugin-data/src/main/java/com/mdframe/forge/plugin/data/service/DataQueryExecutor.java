package com.mdframe.forge.plugin.data.service;

import com.mdframe.forge.plugin.data.dto.DataDatasetQueryDTO;
import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.plugin.data.entity.DataDataset;
import com.mdframe.forge.plugin.data.entity.DataDatasetField;
import com.mdframe.forge.plugin.data.entity.DataDimensionItem;
import com.mdframe.forge.plugin.data.mapper.DataDimensionItemMapper;
import com.mdframe.forge.plugin.data.support.*;
import com.mdframe.forge.plugin.data.vo.DataDatasetFieldVO;
import com.mdframe.forge.plugin.data.vo.DataDatasetQueryResultVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataQueryExecutor {

    private final JdbcDataSourceProvider dataSourceProvider;
    private final DbDialectFactory dialectFactory;
    private final SqlSafetyValidator sqlSafetyValidator;
    private final SqlParameterBinder parameterBinder;
    private final DatasetParamSchemaParser datasetParamSchemaParser;
    private final DataDimensionItemMapper dimensionItemMapper;
    private final DataDatasetFieldViewAssembler fieldViewAssembler;
    private final DataDatasetRowScopeService rowScopeService;

    public DataDatasetQueryResultVO execute(DataDataset dataset, DataConnection connection, 
            List<DataDatasetField> fieldConfigs, DataDatasetQueryDTO query) {
        DataDatasetQueryResultVO result = new DataDatasetQueryResultVO();
        
        int maxRows = query.getMaxRows() != null ? Math.min(query.getMaxRows(), dataset.getMaxRows()) : dataset.getMaxRows();
        int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null ? Math.min(query.getPageSize(), maxRows) : maxRows;
        
        List<DataDatasetField> displayFields = fieldConfigs.stream()
                .filter(f -> f.getDisplayEnabled() == 1)
                .filter(f -> !"HIDDEN".equals(f.getSensitiveLevel()))
                .collect(Collectors.toList());

        List<DataDatasetField> queryFields = fieldConfigs.stream()
                .filter(f -> f.getQueryEnabled() == null || f.getQueryEnabled() == 1)
                .collect(Collectors.toList());

        if (queryFields.isEmpty()) {
            queryFields = fieldConfigs;
        }
        
        if (query.getFields() != null && !query.getFields().isEmpty()) {
            Set<String> requestedFields = new HashSet<>(query.getFields());
            displayFields = displayFields.stream()
                    .filter(f -> requestedFields.contains(f.getFieldName()))
                    .collect(Collectors.toList());
        }
        
        if (displayFields.isEmpty()) {
            log.warn("No display fields available for dataset {}", dataset.getId());
            result.setDimensions(new ArrayList<>());
            result.setSource(new ArrayList<>());
            result.setTotal(0L);
            result.setFields(new ArrayList<>());
            return result;
        }
        
        List<String> dimensions = displayFields.stream()
                .map(DataDatasetField::getFieldName)
                .collect(Collectors.toList());
        
        DbDialect dialect = dialectFactory.getDialect(connection.getDbType());
        QueryBuildResult buildResult;
        if ("TABLE".equals(dataset.getDatasetType())) {
            buildResult = buildTableQuerySql(dataset, connection, displayFields, queryFields, query.getParams());
        } else {
            buildResult = buildSqlQuery(dataset, query.getParams());
        }
        buildResult = applyRowScope(dataset, dialect, buildResult);

        String sql = dialect.buildLimitSql(buildResult.getSql(), pageSize);
        logQuerySql(dataset, query, buildResult.getParams(), sql);

        List<Map<String, Object>> rows = executeQuery(connection, sql, buildResult.getParams(), dataset.getTimeoutSeconds());
        
        rows = applyDimensionTranslation(rows, displayFields);
        rows = applyMasking(rows, displayFields);
        
        result.setDimensions(dimensions);
        result.setSource(rows);
        result.setTotal((long) rows.size());
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setFields(convertToFieldVOList(displayFields));
        
        return result;
    }

    private QueryBuildResult buildTableQuerySql(DataDataset dataset, DataConnection connection,
            List<DataDatasetField> selectFields, List<DataDatasetField> queryFields, Map<String, Object> params) {
        DbDialect dialect = dialectFactory.getDialect(connection.getDbType());
        StringBuilder sql = new StringBuilder();
        Map<String, Object> boundParams = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
        sql.append("SELECT ");
        List<String> fieldNames = selectFields.stream()
                .map(f -> dialect.quoteIdentifier(f.getFieldName()))
                .collect(Collectors.toList());
        sql.append(String.join(", ", fieldNames));
        sql.append(" FROM ").append(dialect.quoteIdentifier(dataset.getTableName()));
        sql.append(" WHERE 1 = 1");

        List<DatasetParamSchemaItem> paramSchemaItems = datasetParamSchemaParser.parse(dataset.getParamSchemaJson());
        if (!paramSchemaItems.isEmpty()) {
            appendSchemaConditions(dataset, dialect, sql, queryFields, paramSchemaItems, boundParams);
            return new QueryBuildResult(sql.toString(), boundParams);
        }

        Set<String> matchedParams = new LinkedHashSet<>();
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String paramName = entry.getKey();
                Object value = entry.getValue();
                if (value != null) {
                    DataDatasetField field = queryFields.stream()
                            .filter(f -> f.getFieldName().equals(paramName))
                            .findFirst()
                            .orElse(null);
                    if (field != null) {
                        sql.append(" AND ").append(dialect.quoteIdentifier(field.getFieldName()));
                        sql.append(" = :").append(paramName);
                        matchedParams.add(paramName);
                    }
                }
            }
            Set<String> ignoredParams = params.keySet().stream()
                    .filter(paramName -> !matchedParams.contains(paramName))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!ignoredParams.isEmpty()) {
                log.warn("Dataset {} ignored query params in TABLE mode, no matching query field found: {}",
                        dataset.getId(), ignoredParams);
            }
        }
        return new QueryBuildResult(sql.toString(), boundParams);
    }

    private QueryBuildResult buildSqlQuery(DataDataset dataset, Map<String, Object> params) {
        String sql = dataset.getSqlText();
        sqlSafetyValidator.validate(sql);
        return new QueryBuildResult(sql, params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>());
    }

    private QueryBuildResult applyRowScope(DataDataset dataset, DbDialect dialect, QueryBuildResult buildResult) {
        DataDatasetRowScopeCondition rowScopeCondition = rowScopeService.buildCondition(dataset, dialect);
        if (rowScopeCondition == null || !rowScopeCondition.isEnabled()) {
            return buildResult;
        }
        Map<String, Object> params = new LinkedHashMap<>(buildResult.getParams());
        params.putAll(rowScopeCondition.getParams());
        String conditionSql = rowScopeCondition.getConditionSql();
        String scopedSql;
        if ("SQL".equals(dataset.getDatasetType())) {
            String sql = buildResult.getSql();
            if (sql == null || !sql.contains("/*DATA_SCOPE*/")) {
                throw new BusinessException("SQL数据集启用行权限时，SQL中必须包含 /*DATA_SCOPE*/ 占位符");
            }
            scopedSql = sql.replace("/*DATA_SCOPE*/", conditionSql);
            sqlSafetyValidator.validate(scopedSql);
            return new QueryBuildResult(scopedSql, params);
        }
        scopedSql = buildResult.getSql() + " " + conditionSql;
        return new QueryBuildResult(scopedSql, params);
    }

    private List<Map<String, Object>> executeQuery(DataConnection connection, String sql, 
            Map<String, Object> params, int timeoutSeconds) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try {
            Connection conn = dataSourceProvider.getConnection(connection);
            try {
                if (params != null && !params.isEmpty()) {
                    List<String> namedParams = parameterBinder.extractNamedParams(sql);
                    if (!namedParams.isEmpty()) {
                        String preparedSql = parameterBinder.convertToPreparedStatement(sql);
                        PreparedStatement ps = conn.prepareStatement(preparedSql);
                        try {
                            ps.setQueryTimeout(timeoutSeconds);
                            Map<Integer, Object> indexMap = parameterBinder.buildParamIndexMap(sql, params);
                            for (Map.Entry<Integer, Object> entry : indexMap.entrySet()) {
                                ps.setObject(entry.getKey(), entry.getValue());
                            }
                            ResultSet rs = ps.executeQuery();
                            rows = resultSetToMaps(rs);
                            rs.close();
                        } finally {
                            ps.close();
                        }
                    } else {
                        PreparedStatement ps = conn.prepareStatement(sql);
                        try {
                            ps.setQueryTimeout(timeoutSeconds);
                            ResultSet rs = ps.executeQuery();
                            rows = resultSetToMaps(rs);
                            rs.close();
                        } finally {
                            ps.close();
                        }
                    }
                } else {
                    PreparedStatement ps = conn.prepareStatement(sql);
                    try {
                        ps.setQueryTimeout(timeoutSeconds);
                        ResultSet rs = ps.executeQuery();
                        rows = resultSetToMaps(rs);
                        rs.close();
                    } finally {
                        ps.close();
                    }
                }
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            log.warn("Execute query failed, sql={}, params={}, error={}", sql, params, e.getMessage(), e);
        }
        return rows;
    }

    private void logQuerySql(DataDataset dataset, DataDatasetQueryDTO query, Map<String, Object> params, String sql) {
        String debugSql = parameterBinder.renderDebugSql(sql, params);
        log.info("Dataset runtime query: datasetId={}, datasetType={}, pageNum={}, pageSize={}, params={}, sql={}",
                dataset.getId(),
                dataset.getDatasetType(),
                query.getPageNum(),
                query.getPageSize(),
                params,
                debugSql);
    }

    private void appendSchemaConditions(DataDataset dataset, DbDialect dialect, StringBuilder sql,
            List<DataDatasetField> queryFields, List<DatasetParamSchemaItem> paramSchemaItems, Map<String, Object> boundParams) {
        Map<String, DataDatasetField> queryFieldMap = queryFields.stream()
                .collect(Collectors.toMap(DataDatasetField::getFieldName, field -> field, (left, right) -> left, LinkedHashMap::new));
        Set<String> inputParamNames = new LinkedHashSet<>(boundParams.keySet());
        Set<String> matchedParams = new LinkedHashSet<>();
        for (DatasetParamSchemaItem item : paramSchemaItems) {
            if (item == null || isBlank(item.getParamName())) {
                continue;
            }
            Object value = resolveSchemaParamValue(item, boundParams);
            if (isEmptyValue(value)) {
                if (Boolean.TRUE.equals(item.getRequired())) {
                    throw new BusinessException("查询参数[" + resolveSchemaLabel(item) + "]不能为空");
                }
                continue;
            }
            DataDatasetField field = queryFieldMap.get(item.getFieldName());
            if (field == null) {
                throw new BusinessException("数据集参数映射字段不存在或不可筛选: " + item.getFieldName());
            }
            String operator = datasetParamSchemaParser.normalizeOperator(item.getOperator());
            String columnName = !isBlank(field.getSourceColumn()) ? field.getSourceColumn() : field.getFieldName();
            String paramName = item.getParamName();
            if ("LIKE".equals(operator)) {
                paramName = paramName + "_like";
                boundParams.put(paramName, "%" + value + "%");
            } else {
                boundParams.put(paramName, value);
            }
            sql.append(" AND ").append(dialect.quoteIdentifier(columnName)).append(" ")
                    .append(operator).append(" :").append(paramName);
            matchedParams.add(item.getParamName());
        }

        Set<String> unknownParams = inputParamNames.stream()
                .filter(paramName -> paramSchemaItems.stream()
                        .noneMatch(item -> item != null && item.getParamName() != null && item.getParamName().equals(paramName)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!unknownParams.isEmpty()) {
            log.warn("Dataset {} ignored query params not declared in paramSchemaJson: {}", dataset.getId(), unknownParams);
        }
    }

    private Object resolveSchemaParamValue(DatasetParamSchemaItem item, Map<String, Object> boundParams) {
        Object value = boundParams.get(item.getParamName());
        return isEmptyValue(value) ? item.getDefaultValue() : value;
    }

    private String resolveSchemaLabel(DatasetParamSchemaItem item) {
        return !isBlank(item.getLabel()) ? item.getLabel() : item.getParamName();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isEmptyValue(Object value) {
        return value == null || (value instanceof String && ((String) value).trim().isEmpty());
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class QueryBuildResult {
        private String sql;
        private Map<String, Object> params;
    }

    private List<Map<String, Object>> resultSetToMaps(ResultSet rs) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String label = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);
                row.put(label, value);
            }
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> applyMasking(List<Map<String, Object>> rows, List<DataDatasetField> fields) {
        Map<String, DataDatasetField> fieldMap = fields.stream()
                .collect(Collectors.toMap(DataDatasetField::getFieldName, f -> f));
        
        for (Map<String, Object> row : rows) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                DataDatasetField field = fieldMap.get(fieldName);
                if (field != null && "MASK".equals(field.getSensitiveLevel()) && value != null) {
                    String maskRule = field.getMaskRule();
                    entry.setValue(applyMaskRule(value.toString(), maskRule));
                }
            }
        }
        return rows;
    }

    private List<Map<String, Object>> applyDimensionTranslation(List<Map<String, Object>> rows, List<DataDatasetField> fields) {
        if (rows == null || rows.isEmpty() || fields == null || fields.isEmpty()) {
            return rows;
        }

        List<DataDatasetField> dimensionFields = fields.stream()
                .filter(field -> field.getDimensionId() != null)
                .collect(Collectors.toList());
        if (dimensionFields.isEmpty()) {
            return rows;
        }

        Set<Long> dimensionIds = dimensionFields.stream()
                .map(DataDatasetField::getDimensionId)
                .collect(Collectors.toSet());
        if (dimensionIds.isEmpty()) {
            return rows;
        }

        List<DataDimensionItem> items = dimensionItemMapper.selectEnabledItemsByDimensionIds(SessionHelper.getTenantId(), dimensionIds);
        if (items == null || items.isEmpty()) {
            return rows;
        }

        Map<Long, Map<String, String>> labelMap = items.stream()
                .collect(Collectors.groupingBy(
                        DataDimensionItem::getDimensionId,
                        LinkedHashMap::new,
                        Collectors.toMap(
                                DataDimensionItem::getItemValue,
                                DataDimensionItem::getItemLabel,
                                (left, right) -> left,
                                LinkedHashMap::new
                        )
                ));

        for (Map<String, Object> row : rows) {
            for (DataDatasetField field : dimensionFields) {
                String fieldName = field.getFieldName();
                if (!row.containsKey(fieldName)) {
                    continue;
                }
                Object rawValue = row.get(fieldName);
                if (rawValue == null) {
                    continue;
                }
                if (!"MASK".equals(field.getSensitiveLevel())) {
                    row.put(fieldName + "Raw", rawValue);
                }
                Map<String, String> dimensionLabels = labelMap.get(field.getDimensionId());
                String label = dimensionLabels != null ? dimensionLabels.get(String.valueOf(rawValue)) : null;
                if (label != null) {
                    row.put(fieldName, label);
                }
            }
        }
        return rows;
    }

    private String applyMaskRule(String value, String maskRule) {
        if (value == null) return null;
        if (maskRule == null || maskRule.isEmpty()) {
            int len = value.length();
            if (len <= 4) return value;
            return value.substring(0, 2) + "****" + value.substring(len - 2);
        }
        return value.replaceAll(maskRule, "****");
    }

    private List<DataDatasetFieldVO> convertToFieldVOList(List<DataDatasetField> fields) {
        return fieldViewAssembler.toVOList(fields);
    }
}
