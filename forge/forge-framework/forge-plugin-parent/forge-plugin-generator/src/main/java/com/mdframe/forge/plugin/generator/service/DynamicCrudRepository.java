package com.mdframe.forge.plugin.generator.service;

import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 动态CRUD数据访问层
 * 使用NamedParameterJdbcTemplate防止SQL注入，支持多种数据库
 * 
 * @author forge
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DynamicCrudRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,63}$");
    
    // 缓存：表名 -> 是否有del_flag列
    private final ConcurrentHashMap<String, Boolean> delFlagCache = new ConcurrentHashMap<>();
    
    // 缓存：表名 -> 列名集合
    private final ConcurrentHashMap<String, Set<String>> tableColumnsCache = new ConcurrentHashMap<>();
    
    // 缓存：表名 -> {camelCase -> snake_case} 映射
    private final ConcurrentHashMap<String, Map<String, String>> columnMappingCache = new ConcurrentHashMap<>();

    // ==================== 查询操作 ====================

    /**
     * 分页查询
     */
    public Page<Map<String, Object>> selectPage(String tableName, int pageNum, int pageSize,
                                                  Map<String, Object> searchParams,
                                                  Set<String> allowedSearchFields,
                                                  Map<String, String> searchTypeMap,
                                                  Map<String, String> columnMapping,
                                                  String orderBy) {
        validateTableName(tableName);

        StringBuilder whereClause = buildBaseWhereClause(tableName);
        MapSqlParameterSource params = buildBaseQueryParams();
        appendSearchConditions(whereClause, params, searchParams, allowedSearchFields, searchTypeMap, columnMapping);

        String countSql = buildSelectSql("SELECT COUNT(*)", tableName, whereClause);
        Long total = namedJdbcTemplate.queryForObject(countSql, params, Long.class);

        String dataSql = buildPageDataSql(tableName, whereClause, orderBy);
        appendPageParams(params, pageNum, pageSize);
        
        List<Map<String, Object>> records = namedJdbcTemplate.queryForList(dataSql, params);
        
        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize, total != null ? total : 0);
        page.setRecords(records);
        return page;
    }

    /**
     * 添加搜索条件
     */
    private void addSearchCondition(StringBuilder whereClause, MapSqlParameterSource params,
                                     String columnName, String searchType, Object value) {
        String paramName = "param_" + columnName.replace(".", "_");
        
        switch (searchType.toLowerCase()) {
            case "like":
                addLikeCondition(whereClause, params, columnName, paramName, "%" + value + "%");
                break;
            case "left_like":
                addLikeCondition(whereClause, params, columnName, paramName, "%" + value);
                break;
            case "right_like":
                addLikeCondition(whereClause, params, columnName, paramName, value + "%");
                break;
            case "eq":
                addBinaryCondition(whereClause, params, columnName, "=", paramName, value);
                break;
            case "ne":
                addBinaryCondition(whereClause, params, columnName, "!=", paramName, value);
                break;
            case "gt":
                addBinaryCondition(whereClause, params, columnName, ">", paramName, value);
                break;
            case "ge":
            case "gte":
                addBinaryCondition(whereClause, params, columnName, ">=", paramName, value);
                break;
            case "lt":
                addBinaryCondition(whereClause, params, columnName, "<", paramName, value);
                break;
            case "le":
            case "lte":
                addBinaryCondition(whereClause, params, columnName, "<=", paramName, value);
                break;
            case "in":
                addInCondition(whereClause, params, columnName, paramName, value);
                break;
            case "between":
                addBetweenCondition(whereClause, params, columnName, paramName, value);
                break;
            case "is_null":
                whereClause.append(columnName).append(" IS NULL");
                break;
            case "is_not_null":
                whereClause.append(columnName).append(" IS NOT NULL");
                break;
            default:
                addBinaryCondition(whereClause, params, columnName, "=", paramName, value);
                break;
        }
    }

    private void addLikeCondition(StringBuilder whereClause, MapSqlParameterSource params,
                                  String columnName, String paramName, Object value) {
        addBinaryCondition(whereClause, params, columnName, "LIKE", paramName, value);
    }

    private void addBinaryCondition(StringBuilder whereClause, MapSqlParameterSource params,
                                    String columnName, String operator, String paramName, Object value) {
        whereClause.append(columnName).append(" ").append(operator).append(" :").append(paramName);
        params.addValue(paramName, value);
    }

    private void addInCondition(StringBuilder whereClause, MapSqlParameterSource params,
                                String columnName, String paramName, Object value) {
        List<?> values = normalizeInValues(value);
        if (values == null) {
            addBinaryCondition(whereClause, params, columnName, "=", paramName, value);
            return;
        }
        whereClause.append(columnName).append(" IN (:").append(paramName).append(")");
        params.addValue(paramName, values);
    }

    private List<?> normalizeInValues(Object value) {
        if (value instanceof List) {
            return (List<?>) value;
        }
        if (value instanceof String) {
            return Arrays.asList(((String) value).split(","));
        }
        return null;
    }

    private void addBetweenCondition(StringBuilder whereClause, MapSqlParameterSource params,
                                     String columnName, String paramName, Object value) {
        if (!(value instanceof List)) {
            return;
        }
        List<?> range = (List<?>) value;
        if (range.size() < 2) {
            return;
        }
        whereClause.append(columnName).append(" BETWEEN :").append(paramName).append("_start AND :")
                .append(paramName).append("_end");
        params.addValue(paramName + "_start", range.get(0));
        params.addValue(paramName + "_end", range.get(1));
    }

    private void appendSearchConditions(StringBuilder whereClause, MapSqlParameterSource params,
                                        Map<String, Object> searchParams,
                                        Set<String> allowedSearchFields,
                                        Map<String, String> searchTypeMap,
                                        Map<String, String> columnMapping) {
        if (searchParams == null || searchParams.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : searchParams.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            if (shouldSkipSearchField(fieldName, value, allowedSearchFields)) {
                continue;
            }

            String columnName = resolveSearchColumn(fieldName, columnMapping);
            if (columnName == null) {
                continue;
            }

            appendWhereJoiner(whereClause);
            addSearchCondition(whereClause, params, columnName, resolveSearchType(fieldName, searchTypeMap), value);
        }
    }

    private boolean shouldSkipSearchField(String fieldName, Object value, Set<String> allowedSearchFields) {
        if (!allowedSearchFields.contains(fieldName)) {
            return true;
        }
        return value == null || (value instanceof String && StringUtils.isBlank((String) value));
    }

    private String resolveSearchColumn(String fieldName, Map<String, String> columnMapping) {
        String columnName = columnMapping.getOrDefault(fieldName, DynamicQueryGenerator.camelToSnake(fieldName));
        if (DynamicQueryGenerator.containsSqlInjection(columnName)) {
            log.warn("[DynamicCrudRepository] 检测到SQL注入尝试, fieldName={}", fieldName);
            return null;
        }
        return columnName;
    }

    private String resolveSearchType(String fieldName, Map<String, String> searchTypeMap) {
        return searchTypeMap.getOrDefault(fieldName, "eq");
    }

    private StringBuilder buildBaseWhereClause(String tableName) {
        StringBuilder whereClause = new StringBuilder();
        appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);
        return whereClause;
    }

    private MapSqlParameterSource buildBaseQueryParams() {
        MapSqlParameterSource params = new MapSqlParameterSource();
        appendBaseQueryConditions(new StringBuilder(), params, null);
        return params;
    }

    private String buildPageDataSql(String tableName, StringBuilder whereClause, String orderBy) {
        String dataSql = buildSelectSql("SELECT *", tableName, whereClause);
        dataSql += buildOrderByClause(orderBy);
        return dataSql + " LIMIT :limit OFFSET :offset";
    }

    private String buildOrderByClause(String orderBy) {
        if (StringUtils.isNotBlank(orderBy)) {
            return " ORDER BY " + orderBy;
        }
        return " ORDER BY id DESC";
    }

    private void appendPageParams(MapSqlParameterSource params, int pageNum, int pageSize) {
        params.addValue("limit", pageSize);
        params.addValue("offset", (pageNum - 1) * pageSize);
    }

    private StringBuilder buildIdWhereClause(String tableName) {
        StringBuilder whereClause = new StringBuilder("id = :id");
        appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);
        return whereClause;
    }

    private MapSqlParameterSource buildIdQueryParams(Long id) {
        MapSqlParameterSource params = buildBaseQueryParams();
        appendIdParam(params, id);
        return params;
    }

    private void appendBaseQueryConditions(StringBuilder whereClause, MapSqlParameterSource params, String tableName) {
        appendTenantWhereClause(whereClause, params);
        if (tableName != null && hasDelFlag(tableName)) {
            appendWhereCondition(whereClause, "del_flag = '0'");
        }
    }

    /**
     * 根据ID查询
     */
    public Map<String, Object> selectById(String tableName, Long id) {
        validateTableName(tableName);

        StringBuilder whereClause = buildIdWhereClause(tableName);
        MapSqlParameterSource params = buildIdQueryParams(id);

        String sql = buildSelectSql("SELECT *", tableName, whereClause);
        List<Map<String, Object>> results = namedJdbcTemplate.queryForList(sql, params);
        return results.isEmpty() ? null : results.get(0);
    }

    // ==================== 新增操作 ====================

    /**
     * 新增记录
     */
    public int insert(String tableName, Map<String, Object> data) {
        validateTableName(tableName);

        Map<String, Object> insertData = prepareInsertData(tableName, data);
        return namedJdbcTemplate.update(buildInsertSql(tableName, insertData), toSqlParams(insertData));
    }

    // ==================== 更新操作 ====================

    /**
     * 根据ID更新
     */
    public int updateById(String tableName, Long id, Map<String, Object> data) {
        validateTableName(tableName);

        Map<String, Object> updateData = prepareUpdateData(tableName, data);
        MapSqlParameterSource params = toSqlParams(updateData, id);
        String sql = appendTenantCondition(buildUpdateSql(tableName, updateData), params);
        return namedJdbcTemplate.update(sql, params);
    }

    // ==================== 删除操作 ====================

    /**
     * 根据ID删除
     */
    public int deleteById(String tableName, Long id, boolean logicDelete) {
        validateTableName(tableName);

        MapSqlParameterSource params = toIdParam(id);
        String sql = appendTenantCondition(buildDeleteSql(tableName, logicDelete), params);
        return namedJdbcTemplate.update(sql, params);
    }

    // ==================== 工具方法 ====================

    /**
     * 检查表是否存在
     */
    public boolean tableExists(String tableName) {
        try {
            Integer count = queryInformationSchemaCount(
                    "tables", "table_name = :tableName", "tableName", tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("[DynamicCrudRepository] 检查表是否存在失败, tableName={}", tableName, e);
            return false;
        }
    }

    /**
     * 检查表是否有del_flag列（带缓存）
     */
    public boolean hasDelFlag(String tableName) {
        return delFlagCache.computeIfAbsent(tableName, key -> {
            try {
                Integer count = queryInformationSchemaCount(
                        "columns", "table_name = :tableName AND column_name = 'del_flag'", "tableName", key);
                return count != null && count > 0;
            } catch (Exception e) {
                log.warn("[DynamicCrudRepository] 检查del_flag失败, tableName={}", key, e);
                return false;
            }
        });
    }

    /**
     * 获取表的所有列名（带缓存）
     */
    public Set<String> getTableColumns(String tableName) {
        return tableColumnsCache.computeIfAbsent(tableName, key -> {
            try {
                List<String> columns = queryInformationSchemaList(
                        "column_name", "columns", "table_name = :tableName", "tableName", key);
                return new HashSet<>(columns);
            } catch (Exception e) {
                log.warn("[DynamicCrudRepository] 获取表列名失败, tableName={}", key, e);
                return Collections.emptySet();
            }
        });
    }

    /**
     * 获取表的字段映射（camelCase -> snake_case）
     */
    public Map<String, String> getColumnMapping(String tableName) {
        return columnMappingCache.computeIfAbsent(tableName, key -> {
            Map<String, String> mapping = new HashMap<>();
            Set<String> columns = getTableColumns(key);
            for (String column : columns) {
                String camelName = DynamicQueryGenerator.snakeToCamel(column);
                mapping.put(camelName, column);
                mapping.put(column, column);
            }
            return mapping;
        });
    }

    /**
     * 校验表名
     */
    private void validateTableName(String tableName) {
        if (StringUtils.isBlank(tableName) || !SAFE_IDENTIFIER.matcher(tableName).matches()) {
            throw new BusinessException("非法表名: " + tableName);
        }
        if (!tableExists(tableName)) {
            throw new BusinessException("数据表不存在: " + tableName);
        }
    }

    /**
     * 校验标识符
     */
    public void validateIdentifier(String identifier) {
        if (StringUtils.isBlank(identifier) || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new BusinessException("非法标识符: " + identifier);
        }
    }

    private String buildSelectSql(String selectClause, String tableName, StringBuilder whereClause) {
        String sql = selectClause + " FROM " + tableName;
        if (whereClause.length() > 0) {
            sql += " WHERE " + whereClause;
        }
        return sql;
    }

    private void appendTenantWhereClause(StringBuilder whereClause, MapSqlParameterSource params) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return;
        }
        appendWhereCondition(whereClause, "tenant_id = :tenantId");
        params.addValue("tenantId", tenantId);
    }

    private void appendWhereCondition(StringBuilder whereClause, String condition) {
        appendWhereJoiner(whereClause);
        whereClause.append(condition);
    }

    private void appendWhereJoiner(StringBuilder whereClause) {
        if (whereClause.length() > 0) {
            whereClause.append(" AND ");
        }
    }

    private String appendTenantCondition(String sql, MapSqlParameterSource params) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return sql;
        }
        params.addValue("tenantId", tenantId);
        return sql + " AND tenant_id = :tenantId";
    }

    private String buildInsertSql(String tableName, Map<String, Object> data) {
        String columns = String.join(", ", data.keySet());
        String placeholders = data.keySet().stream()
                .map(col -> ":" + col)
                .collect(Collectors.joining(", "));
        return "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
    }

    private String buildUpdateSql(String tableName, Map<String, Object> data) {
        String setClauses = data.entrySet().stream()
                .map(entry -> entry.getKey() + " = :" + entry.getKey())
                .collect(Collectors.joining(", "));
        return "UPDATE " + tableName + " SET " + setClauses + " WHERE id = :id";
    }

    private String buildDeleteSql(String tableName, boolean logicDelete) {
        if (logicDelete) {
            return "UPDATE " + tableName + " SET del_flag = '1', update_time = NOW() WHERE id = :id";
        }
        return "DELETE FROM " + tableName + " WHERE id = :id";
    }

    private MapSqlParameterSource toSqlParams(Map<String, Object> data) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        data.forEach(params::addValue);
        return params;
    }

    private MapSqlParameterSource toSqlParams(Map<String, Object> data, Long id) {
        MapSqlParameterSource params = toSqlParams(data);
        appendIdParam(params, id);
        return params;
    }

    private MapSqlParameterSource toIdParam(Long id) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        appendIdParam(params, id);
        return params;
    }

    private void appendIdParam(MapSqlParameterSource params, Long id) {
        params.addValue("id", id);
    }

    private Integer queryInformationSchemaCount(String table, String condition, String paramName, Object value) {
        String sql = "SELECT COUNT(*) FROM information_schema." + table
                + " WHERE " + condition + " AND table_schema = (SELECT DATABASE())";
        return namedJdbcTemplate.queryForObject(sql, singleParam(paramName, value), Integer.class);
    }

    private List<String> queryInformationSchemaList(String selectColumn, String table,
                                                    String condition, String paramName, Object value) {
        String sql = "SELECT " + selectColumn + " FROM information_schema." + table
                + " WHERE " + condition + " AND table_schema = (SELECT DATABASE())";
        return namedJdbcTemplate.queryForList(sql, singleParam(paramName, value), String.class);
    }

    private MapSqlParameterSource singleParam(String paramName, Object value) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(paramName, value);
        return params;
    }

    private Map<String, Object> prepareInsertData(String tableName, Map<String, Object> data) {
        Map<String, Object> insertData = prepareWriteData(data, "没有可写入的字段");
        fillInsertAuditFields(insertData, getTableColumns(tableName));
        return insertData;
    }

    private Map<String, Object> prepareUpdateData(String tableName, Map<String, Object> data) {
        Map<String, Object> updateData = prepareWriteData(data, "没有可更新的字段");
        removeImmutableFields(updateData, "id", "tenant_id");
        fillUpdateAuditFields(updateData, getTableColumns(tableName));
        return updateData;
    }

    private Map<String, Object> prepareWriteData(Map<String, Object> data, String emptyMessage) {
        if (data == null || data.isEmpty()) {
            throw new BusinessException(emptyMessage);
        }
        return data;
    }

    private void removeImmutableFields(Map<String, Object> data, String... fields) {
        for (String field : fields) {
            data.remove(field);
        }
    }

    private void fillInsertAuditFields(Map<String, Object> data, Set<String> columns) {
        Date now = new Date();
        Long tenantId = TenantContextHolder.getTenantId();
        Long userId = SessionHelper.getUserId();
        Long mainOrgId = SessionHelper.getMainOrgId();

        putIfColumnExists(data, columns, "tenant_id", tenantId);
        putIfColumnExists(data, columns, "create_by", userId);
        putIfColumnExists(data, columns, "create_dept", mainOrgId);
        putIfColumnExists(data, columns, "create_time", now);
        putIfColumnExists(data, columns, "update_by", userId);
        putIfColumnExists(data, columns, "update_time", now);
    }

    private void fillUpdateAuditFields(Map<String, Object> data, Set<String> columns) {
        Date now = new Date();
        Long userId = SessionHelper.getUserId();

        putIfColumnExists(data, columns, "update_by", userId);
        putIfColumnExists(data, columns, "update_time", now);
    }

    private void putIfColumnExists(Map<String, Object> data, Set<String> columns, String column, Object value) {
        if (value == null || !columns.contains(column)) {
            return;
        }
        if (!data.containsKey(column) || data.get(column) == null) {
            data.put(column, value);
        }
    }
}
