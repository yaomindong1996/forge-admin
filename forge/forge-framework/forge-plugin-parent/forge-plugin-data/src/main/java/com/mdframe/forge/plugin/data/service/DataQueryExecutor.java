package com.mdframe.forge.plugin.data.service;

import com.mdframe.forge.plugin.data.dto.DataDatasetQueryDTO;
import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.plugin.data.entity.DataDataset;
import com.mdframe.forge.plugin.data.entity.DataDatasetField;
import com.mdframe.forge.plugin.data.support.*;
import com.mdframe.forge.plugin.data.vo.DataDatasetFieldVO;
import com.mdframe.forge.plugin.data.vo.DataDatasetQueryResultVO;
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
        
        String sql;
        if ("TABLE".equals(dataset.getDatasetType())) {
            sql = buildTableQuerySql(dataset, connection, displayFields, query.getParams());
        } else {
            sql = buildSqlQuery(dataset, query.getParams());
        }
        
        DbDialect dialect = dialectFactory.getDialect(connection.getDbType());
        sql = dialect.buildLimitSql(sql, pageSize);
        
        List<Map<String, Object>> rows = executeQuery(connection, sql, query.getParams(), dataset.getTimeoutSeconds());
        
        rows = applyMasking(rows, displayFields);
        
        result.setDimensions(dimensions);
        result.setSource(rows);
        result.setTotal((long) rows.size());
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setFields(convertToFieldVOList(displayFields));
        
        return result;
    }

    private String buildTableQuerySql(DataDataset dataset, DataConnection connection, 
            List<DataDatasetField> fields, Map<String, Object> params) {
        DbDialect dialect = dialectFactory.getDialect(connection.getDbType());
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        List<String> fieldNames = fields.stream()
                .map(f -> dialect.quoteIdentifier(f.getFieldName()))
                .collect(Collectors.toList());
        sql.append(String.join(", ", fieldNames));
        sql.append(" FROM ").append(dialect.quoteIdentifier(dataset.getTableName()));
        sql.append(" WHERE 1 = 1");
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String paramName = entry.getKey();
                Object value = entry.getValue();
                if (value != null) {
                    DataDatasetField field = fields.stream()
                            .filter(f -> f.getFieldName().equals(paramName))
                            .findFirst()
                            .orElse(null);
                    if (field != null) {
                        sql.append(" AND ").append(dialect.quoteIdentifier(field.getFieldName()));
                        sql.append(" = :").append(paramName);
                    }
                }
            }
        }
        return sql.toString();
    }

    private String buildSqlQuery(DataDataset dataset, Map<String, Object> params) {
        String sql = dataset.getSqlText();
        sqlSafetyValidator.validate(sql);
        return sql;
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
            log.warn("Execute query failed: {}", e.getMessage());
        }
        return rows;
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
        return fields.stream().map(f -> {
            DataDatasetFieldVO vo = new DataDatasetFieldVO();
            vo.setId(f.getId());
            vo.setFieldName(f.getFieldName());
            vo.setFieldLabel(f.getFieldLabel());
            vo.setDataType(f.getDataType());
            vo.setFieldRole(f.getFieldRole());
            return vo;
        }).collect(Collectors.toList());
    }
}