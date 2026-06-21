package com.mdframe.forge.plugin.generator.service;

import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.plugin.generator.dto.CustomQueryConditionDTO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
    private static final Pattern SAFE_ALIAS = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,127}$");

    public record JoinField(String fieldName, String tableAlias, String columnName) {
    }

    public record JoinSpec(String tableName, String tableAlias, String joinColumn, String mainColumn) {
    }

    public record SqlCondition(String sql, Map<String, Object> params) {
    }

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
        return selectPage(tableName, pageNum, pageSize, searchParams, allowedSearchFields,
                searchTypeMap, columnMapping, orderBy, null);
    }

    public Page<Map<String, Object>> selectPage(String tableName, int pageNum, int pageSize,
                                                  Map<String, Object> searchParams,
                                                  Set<String> allowedSearchFields,
                                                  Map<String, String> searchTypeMap,
                                                  Map<String, String> columnMapping,
                                                  String orderBy,
                                                  SqlCondition dataScopeCondition) {
        validateTableName(tableName);

        StringBuilder whereClause = buildBaseWhereClause(tableName);
        MapSqlParameterSource params = buildBaseQueryParams();
        appendSqlCondition(whereClause, params, dataScopeCondition);
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
     * 多模型左连接分页查询。selectFields 的 fieldName 会作为返回字段别名，调用方无需再做 snake_case 转换。
     */
    public Page<Map<String, Object>> selectJoinedPage(String mainTableName,
                                                      List<JoinField> selectFields,
                                                      List<JoinSpec> joins,
                                                      int pageNum,
                                                      int pageSize,
                                                      Map<String, Object> searchParams,
                                                      Set<String> allowedSearchFields,
                                                      Map<String, String> searchTypeMap,
                                                      Map<String, String> fieldColumnMapping,
                                                      String orderBy) {
        return selectJoinedPage(mainTableName, selectFields, joins, pageNum, pageSize, searchParams,
                allowedSearchFields, searchTypeMap, fieldColumnMapping, orderBy, null);
    }

    public Page<Map<String, Object>> selectJoinedPage(String mainTableName,
                                                      List<JoinField> selectFields,
                                                      List<JoinSpec> joins,
                                                      int pageNum,
                                                      int pageSize,
                                                      Map<String, Object> searchParams,
                                                      Set<String> allowedSearchFields,
                                                      Map<String, String> searchTypeMap,
                                                      Map<String, String> fieldColumnMapping,
                                                      String orderBy,
                                                      SqlCondition dataScopeCondition) {
        validateJoinQuery(mainTableName, selectFields, joins);

        StringBuilder whereClause = buildBaseWhereClause(mainTableName, "t0");
        MapSqlParameterSource params = buildBaseQueryParams("t0");
        appendSqlCondition(whereClause, params, dataScopeCondition);
        appendSearchConditions(whereClause, params, searchParams, allowedSearchFields, searchTypeMap, fieldColumnMapping);

        String fromClause = buildJoinedFromClause(mainTableName, joins);
        boolean distinctMainRows = selectsOnlyMainTable(selectFields);
        String countSql = (distinctMainRows ? "SELECT COUNT(DISTINCT t0.id) " : "SELECT COUNT(*) ")
                + fromClause + buildWhereSql(whereClause);
        Long total = namedJdbcTemplate.queryForObject(countSql, params, Long.class);

        String dataSql = buildJoinSelectClause(selectFields, distinctMainRows) + " " + fromClause + buildWhereSql(whereClause)
                + buildOrderByClause(orderBy) + " LIMIT :limit OFFSET :offset";
        appendPageParams(params, pageNum, pageSize);
        List<Map<String, Object>> records = namedJdbcTemplate.queryForList(dataSql, params);

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize, total != null ? total : 0);
        page.setRecords(records);
        return page;
    }

    /**
     * 多模型左连接详情查询。
     */
    public Map<String, Object> selectJoinedById(String mainTableName,
                                                Long id,
                                                List<JoinField> selectFields,
                                                List<JoinSpec> joins) {
        return selectJoinedById(mainTableName, id, selectFields, joins, null);
    }

    public Map<String, Object> selectJoinedById(String mainTableName,
                                                Long id,
                                                List<JoinField> selectFields,
                                                List<JoinSpec> joins,
                                                SqlCondition dataScopeCondition) {
        validateJoinQuery(mainTableName, selectFields, joins);

        StringBuilder whereClause = new StringBuilder("t0.id = :id");
        appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), mainTableName, "t0");
        MapSqlParameterSource params = buildBaseQueryParams("t0");
        appendIdParam(params, id);
        appendSqlCondition(whereClause, params, dataScopeCondition);

        String sql = buildJoinSelectClause(selectFields) + " " + buildJoinedFromClause(mainTableName, joins)
                + buildWhereSql(whereClause) + " LIMIT 1";
        List<Map<String, Object>> results = namedJdbcTemplate.queryForList(sql, params);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 限量查询列表，用于动态导出。
     */
    public List<Map<String, Object>> selectList(String tableName,
                                                Map<String, Object> searchParams,
                                                Set<String> allowedSearchFields,
                                                Map<String, String> searchTypeMap,
                                                Map<String, String> columnMapping,
                                                String orderBy,
                                                int limit) {
        return selectList(tableName, searchParams, allowedSearchFields, searchTypeMap, columnMapping, orderBy, limit, null);
    }

    public List<Map<String, Object>> selectList(String tableName,
                                                Map<String, Object> searchParams,
                                                Set<String> allowedSearchFields,
                                                Map<String, String> searchTypeMap,
                                                Map<String, String> columnMapping,
                                                String orderBy,
                                                int limit,
                                                SqlCondition dataScopeCondition) {
        validateTableName(tableName);

        StringBuilder whereClause = buildBaseWhereClause(tableName);
        MapSqlParameterSource params = buildBaseQueryParams();
        appendSqlCondition(whereClause, params, dataScopeCondition);
        appendSearchConditions(whereClause, params, searchParams, allowedSearchFields, searchTypeMap, columnMapping);

        String dataSql = buildSelectSql("SELECT *", tableName, whereClause);
        dataSql += buildOrderByClause(orderBy);
        dataSql += " LIMIT :limit";
        params.addValue("limit", Math.max(1, limit));

        return namedJdbcTemplate.queryForList(dataSql, params);
    }

    /**
     * 统计动态列表数据量，用于智能导出阈值判断。
     */
    public long countList(String tableName,
                          Map<String, Object> searchParams,
                          Set<String> allowedSearchFields,
                          Map<String, String> searchTypeMap,
                          Map<String, String> columnMapping,
                          SqlCondition dataScopeCondition) {
        validateTableName(tableName);

        StringBuilder whereClause = buildBaseWhereClause(tableName);
        MapSqlParameterSource params = buildBaseQueryParams();
        appendSqlCondition(whereClause, params, dataScopeCondition);
        appendSearchConditions(whereClause, params, searchParams, allowedSearchFields, searchTypeMap, columnMapping);

        String countSql = buildSelectSql("SELECT COUNT(*)", tableName, whereClause);
        Long total = namedJdbcTemplate.queryForObject(countSql, params, Long.class);
        return total != null ? total : 0L;
    }

    /**
     * 分页查询动态列表记录，不执行 count，用于异步导出分批读取。
     */
    public List<Map<String, Object>> selectPageRecords(String tableName,
                                                       int pageNum,
                                                       int pageSize,
                                                       Map<String, Object> searchParams,
                                                       Set<String> allowedSearchFields,
                                                       Map<String, String> searchTypeMap,
                                                       Map<String, String> columnMapping,
                                                       String orderBy,
                                                       SqlCondition dataScopeCondition) {
        validateTableName(tableName);

        StringBuilder whereClause = buildBaseWhereClause(tableName);
        MapSqlParameterSource params = buildBaseQueryParams();
        appendSqlCondition(whereClause, params, dataScopeCondition);
        appendSearchConditions(whereClause, params, searchParams, allowedSearchFields, searchTypeMap, columnMapping);

        String dataSql = buildPageDataSql(tableName, whereClause, orderBy);
        appendPageParams(params, pageNum, pageSize);
        return namedJdbcTemplate.queryForList(dataSql, params);
    }

    /**
     * 统计左连接动态列表数据量，用于智能导出阈值判断。
     */
    public long countJoined(String mainTableName,
                            List<JoinField> selectFields,
                            List<JoinSpec> joins,
                            Map<String, Object> searchParams,
                            Set<String> allowedSearchFields,
                            Map<String, String> searchTypeMap,
                            Map<String, String> fieldColumnMapping,
                            SqlCondition dataScopeCondition) {
        validateJoinQuery(mainTableName, selectFields, joins);

        StringBuilder whereClause = buildBaseWhereClause(mainTableName, "t0");
        MapSqlParameterSource params = buildBaseQueryParams("t0");
        appendSqlCondition(whereClause, params, dataScopeCondition);
        appendSearchConditions(whereClause, params, searchParams, allowedSearchFields, searchTypeMap, fieldColumnMapping);

        String fromClause = buildJoinedFromClause(mainTableName, joins);
        boolean distinctMainRows = selectsOnlyMainTable(selectFields);
        String countSql = (distinctMainRows ? "SELECT COUNT(DISTINCT t0.id) " : "SELECT COUNT(*) ")
                + fromClause + buildWhereSql(whereClause);
        Long total = namedJdbcTemplate.queryForObject(countSql, params, Long.class);
        return total != null ? total : 0L;
    }

    /**
     * 分页查询左连接动态列表记录，不执行 count，用于异步导出分批读取。
     */
    public List<Map<String, Object>> selectJoinedPageRecords(String mainTableName,
                                                             List<JoinField> selectFields,
                                                             List<JoinSpec> joins,
                                                             int pageNum,
                                                             int pageSize,
                                                             Map<String, Object> searchParams,
                                                             Set<String> allowedSearchFields,
                                                             Map<String, String> searchTypeMap,
                                                             Map<String, String> fieldColumnMapping,
                                                             String orderBy,
                                                             SqlCondition dataScopeCondition) {
        validateJoinQuery(mainTableName, selectFields, joins);

        StringBuilder whereClause = buildBaseWhereClause(mainTableName, "t0");
        MapSqlParameterSource params = buildBaseQueryParams("t0");
        appendSqlCondition(whereClause, params, dataScopeCondition);
        appendSearchConditions(whereClause, params, searchParams, allowedSearchFields, searchTypeMap, fieldColumnMapping);

        String fromClause = buildJoinedFromClause(mainTableName, joins);
        boolean distinctMainRows = selectsOnlyMainTable(selectFields);
        String dataSql = buildJoinSelectClause(selectFields, distinctMainRows) + " " + fromClause
                + buildWhereSql(whereClause) + buildOrderByClause(orderBy) + " LIMIT :limit OFFSET :offset";
        appendPageParams(params, pageNum, pageSize);
        return namedJdbcTemplate.queryForList(dataSql, params);
    }

    /**
     * 自定义分页查询。
     */
    public Page<Map<String, Object>> selectCustomPage(String tableName, int pageNum, int pageSize,
                                                      List<String> selectedFields,
                                                      List<CustomQueryConditionDTO> conditions,
                                                      Set<String> allowedFields,
                                                      Map<String, String> columnMapping,
                                                      String orderBy) {
        return selectCustomPage(tableName, pageNum, pageSize, selectedFields, conditions,
                allowedFields, columnMapping, orderBy, null);
    }

    public Page<Map<String, Object>> selectCustomPage(String tableName, int pageNum, int pageSize,
                                                      List<String> selectedFields,
                                                      List<CustomQueryConditionDTO> conditions,
                                                      Set<String> allowedFields,
                                                      Map<String, String> columnMapping,
                                                      String orderBy,
                                                      SqlCondition dataScopeCondition) {
        validateTableName(tableName);

        StringBuilder whereClause = buildBaseWhereClause(tableName);
        MapSqlParameterSource params = buildBaseQueryParams();
        appendSqlCondition(whereClause, params, dataScopeCondition);
        appendCustomConditions(whereClause, params, conditions, allowedFields, columnMapping);

        String countSql = buildSelectSql("SELECT COUNT(*)", tableName, whereClause);
        logDynamicSql("自定义查询统计", countSql, params);
        Long total = namedJdbcTemplate.queryForObject(countSql, params, Long.class);

        String dataSql = buildSelectSql(
                buildCustomSelectClause(selectedFields, allowedFields, columnMapping),
                tableName,
                whereClause
        );
        dataSql += buildOrderByClause(orderBy);
        dataSql += " LIMIT :limit OFFSET :offset";
        appendPageParams(params, pageNum, pageSize);
        logDynamicSql("自定义查询数据", dataSql, params);

        List<Map<String, Object>> records = namedJdbcTemplate.queryForList(dataSql, params);

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize, total != null ? total : 0);
        page.setRecords(records);
        return page;
    }

    /**
     * 多模型自定义分页查询，支持使用子表字段作为展示字段或查询条件。
     */
    public Page<Map<String, Object>> selectJoinedCustomPage(String mainTableName,
                                                            List<JoinField> selectFields,
                                                            List<JoinSpec> joins,
                                                            int pageNum,
                                                            int pageSize,
                                                            List<CustomQueryConditionDTO> conditions,
                                                            Set<String> allowedFields,
                                                            Map<String, String> fieldColumnMapping,
                                                            String orderBy) {
        return selectJoinedCustomPage(mainTableName, selectFields, joins, pageNum, pageSize, conditions,
                allowedFields, fieldColumnMapping, orderBy, null);
    }

    public Page<Map<String, Object>> selectJoinedCustomPage(String mainTableName,
                                                            List<JoinField> selectFields,
                                                            List<JoinSpec> joins,
                                                            int pageNum,
                                                            int pageSize,
                                                            List<CustomQueryConditionDTO> conditions,
                                                            Set<String> allowedFields,
                                                            Map<String, String> fieldColumnMapping,
                                                            String orderBy,
                                                            SqlCondition dataScopeCondition) {
        validateJoinQuery(mainTableName, selectFields, joins);

        StringBuilder whereClause = buildBaseWhereClause(mainTableName, "t0");
        MapSqlParameterSource params = buildBaseQueryParams("t0");
        appendSqlCondition(whereClause, params, dataScopeCondition);
        appendCustomConditions(whereClause, params, conditions, allowedFields, fieldColumnMapping);

        String fromClause = buildJoinedFromClause(mainTableName, joins);
        boolean distinctMainRows = selectsOnlyMainTable(selectFields);
        String countSql = (distinctMainRows ? "SELECT COUNT(DISTINCT t0.id) " : "SELECT COUNT(*) ")
                + fromClause + buildWhereSql(whereClause);
        logDynamicSql("自定义查询统计(左连接)", countSql, params);
        Long total = namedJdbcTemplate.queryForObject(countSql, params, Long.class);

        String dataSql = buildJoinSelectClause(selectFields, distinctMainRows) + " " + fromClause
                + buildWhereSql(whereClause) + buildOrderByClause(orderBy) + " LIMIT :limit OFFSET :offset";
        appendPageParams(params, pageNum, pageSize);
        logDynamicSql("自定义查询数据(左连接)", dataSql, params);
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
            List<?> values = ((List<?>) value).stream()
                    .filter(Objects::nonNull)
                    .filter(item -> !(item instanceof String) || StringUtils.isNotBlank((String) item))
                    .toList();
            return values.isEmpty() ? null : values;
        }
        if (value instanceof String) {
            List<String> values = Arrays.stream(((String) value).split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .toList();
            return values.isEmpty() ? null : values;
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

    private void appendCustomConditions(StringBuilder whereClause, MapSqlParameterSource params,
                                        List<CustomQueryConditionDTO> conditions,
                                        Set<String> allowedFields,
                                        Map<String, String> columnMapping) {
        if (conditions == null || conditions.isEmpty()) {
            return;
        }

        StringBuilder customClause = new StringBuilder();
        int index = 0;
        for (CustomQueryConditionDTO condition : conditions) {
            if (condition == null || shouldSkipCustomCondition(condition, allowedFields)) {
                continue;
            }

            String columnName = resolveSearchColumn(condition.getField(), columnMapping);
            if (columnName == null || !isKnownColumn(columnName, columnMapping)) {
                continue;
            }

            String operator = normalizeOperator(condition.getOperator());
            String conditionSql = buildCustomConditionSql(params, columnName, operator, condition, index);
            if (StringUtils.isBlank(conditionSql)) {
                continue;
            }

            if (customClause.length() > 0) {
                customClause.append(" ").append(resolveRelation(condition.getRelation())).append(" ");
            }
            customClause.append(conditionSql);
            index++;
        }

        if (customClause.length() > 0) {
            appendWhereJoiner(whereClause);
            whereClause.append("(").append(customClause).append(")");
        }
    }

    private boolean shouldSkipCustomCondition(CustomQueryConditionDTO condition, Set<String> allowedFields) {
        if (StringUtils.isBlank(condition.getField()) || !allowedFields.contains(condition.getField())) {
            return true;
        }
        String operator = normalizeOperator(condition.getOperator());
        if ("is_null".equals(operator) || "is_not_null".equals(operator)) {
            return false;
        }
        Object value = condition.getValue();
        return value == null || (value instanceof String && StringUtils.isBlank((String) value));
    }

    private String normalizeOperator(String operator) {
        String normalized = StringUtils.defaultIfBlank(operator, "eq").trim().toLowerCase();
        return switch (normalized) {
            case "=", "eq" -> "eq";
            case "!=", "<>", "ne" -> "ne";
            case "like", "left_like", "right_like", "gt", "ge", "gte", "lt", "le", "lte",
                    "in", "between", "is_null", "is_not_null" -> normalized;
            case ">", "<" -> "gt".equals(normalized) ? "gt" : normalized;
            default -> throw new BusinessException("不支持的查询操作符: " + operator);
        };
    }

    private String resolveRelation(String relation) {
        return "OR".equalsIgnoreCase(relation) ? "OR" : "AND";
    }

    private String buildCustomConditionSql(MapSqlParameterSource params, String columnName, String operator,
                                           CustomQueryConditionDTO condition, int index) {
        String paramName = "custom_" + index + "_" + columnName.replace(".", "_");
        Object value = condition.getValue();

        return switch (operator) {
            case "like" -> addCustomBinaryCondition(params, columnName, "LIKE", paramName, "%" + value + "%");
            case "left_like" -> addCustomBinaryCondition(params, columnName, "LIKE", paramName, "%" + value);
            case "right_like" -> addCustomBinaryCondition(params, columnName, "LIKE", paramName, value + "%");
            case "eq" -> addCustomBinaryCondition(params, columnName, "=", paramName, value);
            case "ne" -> addCustomBinaryCondition(params, columnName, "!=", paramName, value);
            case "gt", ">" -> addCustomBinaryCondition(params, columnName, ">", paramName, value);
            case "ge", "gte" -> addCustomBinaryCondition(params, columnName, ">=", paramName, value);
            case "lt", "<" -> addCustomBinaryCondition(params, columnName, "<", paramName, value);
            case "le", "lte" -> addCustomBinaryCondition(params, columnName, "<=", paramName, value);
            case "in" -> addCustomInCondition(params, columnName, paramName, value);
            case "between" -> addCustomBetweenCondition(params, columnName, paramName, condition);
            case "is_null" -> columnName + " IS NULL";
            case "is_not_null" -> columnName + " IS NOT NULL";
            default -> throw new BusinessException("不支持的查询操作符: " + operator);
        };
    }

    private String addCustomBinaryCondition(MapSqlParameterSource params, String columnName, String operator,
                                            String paramName, Object value) {
        params.addValue(paramName, value);
        return columnName + " " + operator + " :" + paramName;
    }

    private String addCustomInCondition(MapSqlParameterSource params, String columnName, String paramName, Object value) {
        List<?> values = normalizeInValues(value);
        if (values == null || values.isEmpty()) {
            return null;
        }
        params.addValue(paramName, values);
        return columnName + " IN (:" + paramName + ")";
    }

    private String addCustomBetweenCondition(MapSqlParameterSource params, String columnName, String paramName,
                                             CustomQueryConditionDTO condition) {
        List<?> range = normalizeBetweenValues(condition);
        if (range == null || range.size() < 2) {
            return null;
        }
        params.addValue(paramName + "_start", range.get(0));
        params.addValue(paramName + "_end", range.get(1));
        return columnName + " BETWEEN :" + paramName + "_start AND :" + paramName + "_end";
    }

    private List<?> normalizeBetweenValues(CustomQueryConditionDTO condition) {
        Object value = condition.getValue();
        if (value instanceof List && ((List<?>) value).size() >= 2) {
            return (List<?>) value;
        }
        if (condition.getValueEnd() == null
                || (condition.getValueEnd() instanceof String && StringUtils.isBlank((String) condition.getValueEnd()))) {
            return null;
        }
        return Arrays.asList(value, condition.getValueEnd());
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

    private String buildCustomSelectClause(List<String> selectedFields, Set<String> allowedFields,
                                           Map<String, String> columnMapping) {
        if (selectedFields == null || selectedFields.isEmpty()) {
            return "SELECT *";
        }
        LinkedHashSet<String> columns = new LinkedHashSet<>();
        addSelectedColumn(columns, "id", allowedFields, columnMapping);
        for (String fieldName : selectedFields) {
            addSelectedColumn(columns, fieldName, allowedFields, columnMapping);
        }
        if (columns.isEmpty()) {
            return "SELECT *";
        }
        return "SELECT " + String.join(", ", columns);
    }

    private void addSelectedColumn(Set<String> columns, String fieldName, Set<String> allowedFields,
                                   Map<String, String> columnMapping) {
        if (StringUtils.isBlank(fieldName) || !allowedFields.contains(fieldName)) {
            return;
        }
        String columnName = columnMapping.getOrDefault(fieldName, DynamicQueryGenerator.camelToSnake(fieldName));
        if (!isKnownColumn(columnName, columnMapping) || DynamicQueryGenerator.containsSqlInjection(columnName)) {
            return;
        }
        validateIdentifier(columnName);
        columns.add(columnName);
    }

    private boolean isKnownColumn(String columnName, Map<String, String> columnMapping) {
        return columnMapping.containsValue(columnName);
    }

    private String resolveSearchType(String fieldName, Map<String, String> searchTypeMap) {
        return searchTypeMap.getOrDefault(fieldName, "eq");
    }

    private StringBuilder buildBaseWhereClause(String tableName) {
        return buildBaseWhereClause(tableName, null);
    }

    private StringBuilder buildBaseWhereClause(String tableName, String tableAlias) {
        StringBuilder whereClause = new StringBuilder();
        appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName, tableAlias);
        return whereClause;
    }

    private MapSqlParameterSource buildBaseQueryParams() {
        return buildBaseQueryParams(null);
    }

    private MapSqlParameterSource buildBaseQueryParams(String tableAlias) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        appendBaseQueryConditions(new StringBuilder(), params, null, tableAlias);
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
        appendBaseQueryConditions(whereClause, params, tableName, null);
    }

    private void appendBaseQueryConditions(StringBuilder whereClause, MapSqlParameterSource params,
                                           String tableName, String tableAlias) {
        if (tableName == null || getTableColumns(tableName).contains("tenant_id")) {
            appendTenantWhereClause(whereClause, params, tableAlias);
        }
        if (tableName != null && hasDelFlag(tableName)) {
            appendWhereCondition(whereClause, qualifyColumn(tableAlias, "del_flag") + " = '0'");
        }
    }

    /**
     * 根据ID查询
     */
    public Map<String, Object> selectById(String tableName, Long id) {
        return selectById(tableName, id, null);
    }

    public Map<String, Object> selectById(String tableName, Long id, SqlCondition dataScopeCondition) {
        validateTableName(tableName);

        StringBuilder whereClause = buildIdWhereClause(tableName);
        MapSqlParameterSource params = buildIdQueryParams(id);
        appendSqlCondition(whereClause, params, dataScopeCondition);

        String sql = buildSelectSql("SELECT *", tableName, whereClause);
        List<Map<String, Object>> results = namedJdbcTemplate.queryForList(sql, params);
        return results.isEmpty() ? null : results.get(0);
    }

    public List<Map<String, Object>> selectListByColumn(String tableName, String columnName, Object value) {
        validateTableName(tableName);
        validateIdentifier(columnName);
        if (value == null) {
            return List.of();
        }

        StringBuilder whereClause = new StringBuilder(columnName + " = :value");
        appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);
        MapSqlParameterSource params = buildBaseQueryParams();
        params.addValue("value", value);
        String orderColumn = getTableColumns(tableName).contains("id") ? "id" : columnName;
        String sql = buildSelectSql("SELECT *", tableName, whereClause) + " ORDER BY " + orderColumn + " ASC";
        return namedJdbcTemplate.queryForList(sql, params);
    }

    public List<Map<String, Object>> selectListByColumnIn(String tableName,
                                                          String columnName,
                                                          Collection<?> values) {
        validateTableName(tableName);
        validateIdentifier(columnName);
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

        StringBuilder whereClause = new StringBuilder(columnName + " IN (:values)");
        appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);
        MapSqlParameterSource params = buildBaseQueryParams();
        params.addValue("values", nonNullValues);
        String orderColumn = getTableColumns(tableName).contains("id") ? "id" : columnName;
        String sql = buildSelectSql("SELECT *", tableName, whereClause) + " ORDER BY " + orderColumn + " ASC";
        return namedJdbcTemplate.queryForList(sql, params);
    }

    public List<Map<String, Object>> selectTreeChildren(String tableName,
                                                        String parentColumn,
                                                        Object parentValue,
                                                        String orderBy,
                                                        int limit) {
        return selectTreeChildren(tableName, parentColumn, parentValue, orderBy, limit, null);
    }

    public List<Map<String, Object>> selectTreeChildren(String tableName,
                                                        String parentColumn,
                                                        Object parentValue,
                                                        String orderBy,
                                                        int limit,
                                                        SqlCondition dataScopeCondition) {
        validateTableName(tableName);
        validateIdentifier(parentColumn);

        boolean rootQuery = parentValue == null || StringUtils.isBlank(String.valueOf(parentValue));
        StringBuilder whereClause = rootQuery
                ? new StringBuilder("(" + parentColumn + " IS NULL OR " + parentColumn + " = :zeroValue OR " + parentColumn + " = :emptyValue)")
                : new StringBuilder(parentColumn + " = :parentValue");
        appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);

        MapSqlParameterSource params = buildBaseQueryParams();
        if (rootQuery) {
            params.addValue("zeroValue", "0");
            params.addValue("emptyValue", "");
        } else {
            params.addValue("parentValue", parentValue);
        }
        appendSqlCondition(whereClause, params, dataScopeCondition);
        params.addValue("limit", Math.max(1, limit));

        String sql = buildSelectSql("SELECT *", tableName, whereClause)
                + buildOrderByClause(orderBy) + " LIMIT :limit";
        return namedJdbcTemplate.queryForList(sql, params);
    }

    public boolean existsByColumn(String tableName, String columnName, Object value) {
        return existsByColumn(tableName, columnName, value, null);
    }

    public boolean existsByColumn(String tableName, String columnName, Object value, SqlCondition dataScopeCondition) {
        validateTableName(tableName);
        validateIdentifier(columnName);
        if (value == null) {
            return false;
        }

        StringBuilder whereClause = new StringBuilder(columnName + " = :value");
        appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);
        MapSqlParameterSource params = buildBaseQueryParams();
        params.addValue("value", value);
        appendSqlCondition(whereClause, params, dataScopeCondition);
        String sql = buildSelectSql("SELECT COUNT(1)", tableName, whereClause) + " LIMIT 1";
        Long count = namedJdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null && count > 0;
    }

    public boolean existsByColumns(String tableName,
                                   Map<String, Object> columnValues,
                                   Long excludeId) {
        return existsByColumns(tableName, columnValues, excludeId, null);
    }

    public boolean existsByColumns(String tableName,
                                   Map<String, Object> columnValues,
                                   Long excludeId,
                                   SqlCondition dataScopeCondition) {
        validateTableName(tableName);
        if (columnValues == null || columnValues.isEmpty()) {
            return false;
        }

        StringBuilder whereClause = new StringBuilder();
        MapSqlParameterSource params = buildBaseQueryParams();
        int index = 0;
        for (Map.Entry<String, Object> entry : columnValues.entrySet()) {
            String columnName = entry.getKey();
            validateIdentifier(columnName);
            String paramName = "uniqueValue" + index++;
            Object value = entry.getValue();
            if (value == null) {
                appendWhereCondition(whereClause, columnName + " IS NULL");
            } else {
                appendWhereCondition(whereClause, columnName + " = :" + paramName);
                params.addValue(paramName, value);
            }
        }
        appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);
        if (excludeId != null) {
            appendWhereCondition(whereClause, "id <> :excludeId");
            params.addValue("excludeId", excludeId);
        }
        appendSqlCondition(whereClause, params, dataScopeCondition);
        String sql = buildSelectSql("SELECT COUNT(1)", tableName, whereClause) + " LIMIT 1";
        Long count = namedJdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null && count > 0;
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

    /**
     * 新增记录并返回自增主键。
     */
    public Long insertReturningId(String tableName, Map<String, Object> data) {
        validateTableName(tableName);

        Map<String, Object> insertData = prepareInsertData(tableName, data);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        namedJdbcTemplate.update(buildInsertSql(tableName, insertData), toSqlParams(insertData), keyHolder, new String[] { "id" });
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    // ==================== 更新操作 ====================

    /**
     * 根据ID更新
     */
    public int updateById(String tableName, Long id, Map<String, Object> data) {
        return updateById(tableName, id, data, null);
    }

    public int updateById(String tableName, Long id, Map<String, Object> data, SqlCondition dataScopeCondition) {
        validateTableName(tableName);

        Map<String, Object> updateData = prepareUpdateData(tableName, data);
        MapSqlParameterSource params = toSqlParams(updateData, id);
        String sql = appendTenantCondition(buildUpdateSql(tableName, updateData), params);
        sql = appendSqlCondition(sql, params, dataScopeCondition);
        return namedJdbcTemplate.update(sql, params);
    }

    public Long selectFirstIdByColumn(String tableName, String columnName, Object value) {
        validateTableName(tableName);
        validateIdentifier(columnName);
        if (value == null) {
            return null;
        }
        StringBuilder whereClause = new StringBuilder(columnName + " = :value");
        appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);
        MapSqlParameterSource params = buildBaseQueryParams();
        params.addValue("value", value);
        String sql = buildSelectSql("SELECT id", tableName, whereClause) + " ORDER BY id ASC LIMIT 1";
        List<Long> ids = namedJdbcTemplate.queryForList(sql, params, Long.class);
        return ids.isEmpty() ? null : ids.get(0);
    }

    // ==================== 删除操作 ====================

    /**
     * 根据ID删除
     */
    public int deleteById(String tableName, Long id, boolean logicDelete) {
        return deleteById(tableName, id, logicDelete, null);
    }

    public int deleteById(String tableName, Long id, boolean logicDelete, SqlCondition dataScopeCondition) {
        validateTableName(tableName);

        MapSqlParameterSource params = toIdParam(id);
        String sql = appendTenantCondition(buildDeleteSql(tableName, logicDelete), params);
        sql = appendSqlCondition(sql, params, dataScopeCondition);
        return namedJdbcTemplate.update(sql, params);
    }

    public int deleteByColumn(String tableName, String columnName, Object value, boolean logicDelete) {
        validateTableName(tableName);
        validateIdentifier(columnName);
        if (value == null) {
            return 0;
        }
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("value", value);
        String sql;
        if (logicDelete) {
            sql = "UPDATE " + tableName + " SET del_flag = '1', update_time = NOW() WHERE " + columnName + " = :value";
        } else {
            sql = "DELETE FROM " + tableName + " WHERE " + columnName + " = :value";
        }
        return namedJdbcTemplate.update(appendTenantCondition(sql, params), params);
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
     * DDL 执行后清理动态表结构缓存，避免追加字段后运行时继续使用旧列集合。
     */
    public void clearTableMetadataCache(String tableName) {
        delFlagCache.remove(tableName);
        tableColumnsCache.remove(tableName);
        columnMappingCache.remove(tableName);
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

    private String buildWhereSql(StringBuilder whereClause) {
        return whereClause.length() > 0 ? " WHERE " + whereClause : "";
    }

    private String buildJoinSelectClause(List<JoinField> fields) {
        return buildJoinSelectClause(fields, false);
    }

    private String buildJoinSelectClause(List<JoinField> fields, boolean distinct) {
        LinkedHashSet<String> selectItems = new LinkedHashSet<>();
        for (JoinField field : fields) {
            selectItems.add(qualifyColumn(field.tableAlias(), field.columnName()) + " AS `" + field.fieldName() + "`");
        }
        return "SELECT " + (distinct ? "DISTINCT " : "") + String.join(", ", selectItems);
    }

    private boolean selectsOnlyMainTable(List<JoinField> fields) {
        return fields != null && fields.stream().allMatch(field -> "t0".equals(field.tableAlias()));
    }

    private String buildJoinedFromClause(String mainTableName, List<JoinSpec> joins) {
        StringBuilder sql = new StringBuilder("FROM ")
                .append(mainTableName)
                .append(" t0");
        Long tenantId = TenantContextHolder.getTenantId();
        for (JoinSpec join : joins) {
            sql.append(" LEFT JOIN ")
                    .append(join.tableName())
                    .append(" ")
                    .append(join.tableAlias())
                    .append(" ON ")
                    .append(qualifyColumn(join.tableAlias(), join.joinColumn()))
                    .append(" = ")
                    .append(qualifyColumn("t0", join.mainColumn()));
            if (tenantId != null && getTableColumns(join.tableName()).contains("tenant_id")) {
                sql.append(" AND ").append(qualifyColumn(join.tableAlias(), "tenant_id")).append(" = :tenantId");
            }
            if (hasDelFlag(join.tableName())) {
                sql.append(" AND ").append(qualifyColumn(join.tableAlias(), "del_flag")).append(" = '0'");
            }
        }
        return sql.toString();
    }

    private void validateJoinQuery(String mainTableName, List<JoinField> selectFields, List<JoinSpec> joins) {
        validateTableName(mainTableName);
        validateIdentifier("t0");
        if (selectFields == null || selectFields.isEmpty()) {
            throw new BusinessException("左连接查询字段不能为空");
        }
        for (JoinField field : selectFields) {
            validateIdentifier(field.tableAlias());
            validateIdentifier(field.columnName());
            validateAlias(field.fieldName());
        }
        for (JoinSpec join : joins) {
            validateTableName(join.tableName());
            validateIdentifier(join.tableAlias());
            validateIdentifier(join.joinColumn());
            validateIdentifier(join.mainColumn());
        }
    }

    private void validateAlias(String alias) {
        if (StringUtils.isBlank(alias) || !SAFE_ALIAS.matcher(alias).matches()) {
            throw new BusinessException("非法字段别名: " + alias);
        }
    }

    private String qualifyColumn(String tableAlias, String columnName) {
        if (StringUtils.isBlank(tableAlias)) {
            return columnName;
        }
        return tableAlias + "." + columnName;
    }

    private void appendTenantWhereClause(StringBuilder whereClause, MapSqlParameterSource params) {
        appendTenantWhereClause(whereClause, params, null);
    }

    private void appendTenantWhereClause(StringBuilder whereClause, MapSqlParameterSource params, String tableAlias) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return;
        }
        appendWhereCondition(whereClause, qualifyColumn(tableAlias, "tenant_id") + " = :tenantId");
        params.addValue("tenantId", tenantId);
    }

    private void appendWhereCondition(StringBuilder whereClause, String condition) {
        appendWhereJoiner(whereClause);
        whereClause.append(condition);
    }

    private void appendSqlCondition(StringBuilder whereClause, MapSqlParameterSource params, SqlCondition condition) {
        if (condition == null || StringUtils.isBlank(condition.sql())) {
            return;
        }
        appendWhereCondition(whereClause, "(" + condition.sql() + ")");
        if (condition.params() != null) {
            condition.params().forEach(params::addValue);
        }
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

    private String appendSqlCondition(String sql, MapSqlParameterSource params, SqlCondition condition) {
        if (condition == null || StringUtils.isBlank(condition.sql())) {
            return sql;
        }
        if (condition.params() != null) {
            condition.params().forEach(params::addValue);
        }
        return sql + " AND (" + condition.sql() + ")";
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

    private void logDynamicSql(String scene, String sql, MapSqlParameterSource params) {
        log.info("[DynamicCrudRepository] {} SQL: {}", scene, sql);
        log.info("[DynamicCrudRepository] {} 参数: {}", scene, params == null ? Map.of() : params.getValues());
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
