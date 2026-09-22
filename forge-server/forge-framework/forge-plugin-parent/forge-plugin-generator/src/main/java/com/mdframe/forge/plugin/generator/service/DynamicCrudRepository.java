package com.mdframe.forge.plugin.generator.service;

import cn.dev33.satoken.exception.SaTokenContextException;
import com.mdframe.forge.plugin.generator.enums.DataAuditSourceType;
import com.mdframe.forge.plugin.generator.service.audit.DataAuditTenantSupport;
import com.mdframe.forge.plugin.generator.service.audit.DataAuditTransactionHolder;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.plugin.generator.dto.CustomQueryConditionDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAuditStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeLogicDeleteStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTenantStrategy;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.OracleRuntimeDatabaseDialect;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.PostgreSqlRuntimeDatabaseDialect;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialect;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialectFactory;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeJdbcTemplateProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.math.BigDecimal;
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

    private static final String OR_LIKE_SEARCH_KEY = "__orLike";

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final RuntimeJdbcTemplateProvider jdbcTemplateProvider;
    private final RuntimeDatabaseDialectFactory dialectFactory;

    private static final String DEFAULT_PRIMARY_KEY = "id";
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

    // 缓存：表名 -> {column -> JDBC type} 映射
    private final ConcurrentHashMap<String, Map<String, Integer>> columnTypesCache = new ConcurrentHashMap<>();

    // 缓存：表名 -> {camelCase -> snake_case} 映射
    private final ConcurrentHashMap<String, Map<String, String>> columnMappingCache = new ConcurrentHashMap<>();

    private NamedParameterJdbcTemplate jdbc() {
        LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
        return context == null ? namedJdbcTemplate : jdbcTemplateProvider.namedJdbcTemplate(context);
    }

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
        Long total = jdbc().queryForObject(countSql, params, Long.class);

        String dataSql = buildPageDataSql(tableName, whereClause, orderBy, pageNum, pageSize);

        List<Map<String, Object>> records = jdbc().queryForList(dataSql, params);

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
                allowedSearchFields, searchTypeMap, fieldColumnMapping, orderBy, null, true);
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
                                                      SqlCondition dataScopeCondition,
                                                      boolean aggregateChildren) {
        validateJoinQuery(mainTableName, selectFields, joins);

        StringBuilder whereClause = buildBaseWhereClause(mainTableName, "t0");
        MapSqlParameterSource params = buildBaseQueryParams("t0");
        appendSqlCondition(whereClause, params, dataScopeCondition);
        appendSearchConditions(whereClause, params, searchParams, allowedSearchFields, searchTypeMap, fieldColumnMapping);

        JoinedListSql joinedSql = resolveListJoinSql(mainTableName, selectFields, joins, aggregateChildren);
        String countSql = (joinedSql.distinctMainRows() ? "SELECT COUNT(DISTINCT " + qualifyPrimaryKey("t0") + ") " : "SELECT COUNT(*) ")
                + joinedSql.fromClause() + buildWhereSql(whereClause);
        Long total = jdbc().queryForObject(countSql, params, Long.class);

        String dataSql = paginateSql(buildJoinSelectClause(selectFields, joinedSql.distinctMainRows()) + " " + joinedSql.fromClause()
                + buildWhereSql(whereClause) + buildOrderByClause(orderBy), pageNum, pageSize);
        List<Map<String, Object>> records = jdbc().queryForList(dataSql, params);

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize, total != null ? total : 0);
        page.setRecords(records);
        return page;
    }

    /**
     * 多模型左连接详情查询。
     */
    public Map<String, Object> selectJoinedById(String mainTableName,
                                                Object id,
                                                List<JoinField> selectFields,
                                                List<JoinSpec> joins) {
        return selectJoinedById(mainTableName, id, selectFields, joins, null);
    }

    public Map<String, Object> selectJoinedById(String mainTableName,
                                                Object id,
                                                List<JoinField> selectFields,
                                                List<JoinSpec> joins,
                                                SqlCondition dataScopeCondition) {
        validateJoinQuery(mainTableName, selectFields, joins);

        StringBuilder whereClause = new StringBuilder(qualifyPrimaryKey("t0") + " = :id");
        appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), mainTableName, "t0");
        MapSqlParameterSource params = buildBaseQueryParams("t0");
        appendIdParam(params, id);
        appendSqlCondition(whereClause, params, dataScopeCondition);

        String sql = buildJoinSelectClause(selectFields) + " " + buildJoinedFromClause(mainTableName, joins)
                + buildWhereSql(whereClause);
        sql = limitSql(sql, 1);
        List<Map<String, Object>> results = jdbc().queryForList(sql, params);
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
        dataSql = limitSql(dataSql, Math.max(1, limit));

        return jdbc().queryForList(dataSql, params);
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
        Long total = jdbc().queryForObject(countSql, params, Long.class);
        return total != null ? total : 0L;
    }

    /**
     * 判断指定列是否存在非空数据（含租户隔离与逻辑删除过滤）。
     * 使用 LIMIT 1 存在性探测，避免大表 COUNT 全扫的性能开销。
     * 用于字段删除守卫：只需知道有无数据，无需精确计数。
     */
    public boolean hasColumnData(String tableName, String columnName) {
        validateTableName(tableName);
        if (StringUtils.isBlank(columnName) || !SAFE_IDENTIFIER.matcher(columnName).matches()) {
            throw new BusinessException("非法列名: " + columnName);
        }
        StringBuilder whereClause = buildBaseWhereClause(tableName);
        appendWhereCondition(whereClause, columnName + " IS NOT NULL");
        MapSqlParameterSource params = buildBaseQueryParams();
        String sql = buildSelectSql("SELECT 1", tableName, whereClause) + " LIMIT 1";
        return !jdbc().queryForList(sql, params).isEmpty();
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

        String dataSql = buildPageDataSql(tableName, whereClause, orderBy, pageNum, pageSize);
        return jdbc().queryForList(dataSql, params);
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
        return countJoined(mainTableName, selectFields, joins, searchParams, allowedSearchFields,
                searchTypeMap, fieldColumnMapping, dataScopeCondition, true);
    }

    public long countJoined(String mainTableName,
                            List<JoinField> selectFields,
                            List<JoinSpec> joins,
                            Map<String, Object> searchParams,
                            Set<String> allowedSearchFields,
                            Map<String, String> searchTypeMap,
                            Map<String, String> fieldColumnMapping,
                            SqlCondition dataScopeCondition,
                            boolean aggregateChildren) {
        validateJoinQuery(mainTableName, selectFields, joins);

        StringBuilder whereClause = buildBaseWhereClause(mainTableName, "t0");
        MapSqlParameterSource params = buildBaseQueryParams("t0");
        appendSqlCondition(whereClause, params, dataScopeCondition);
        appendSearchConditions(whereClause, params, searchParams, allowedSearchFields, searchTypeMap, fieldColumnMapping);

        JoinedListSql joinedSql = resolveListJoinSql(mainTableName, selectFields, joins, aggregateChildren);
        String countSql = (joinedSql.distinctMainRows() ? "SELECT COUNT(DISTINCT " + qualifyPrimaryKey("t0") + ") " : "SELECT COUNT(*) ")
                + joinedSql.fromClause() + buildWhereSql(whereClause);
        Long total = jdbc().queryForObject(countSql, params, Long.class);
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
        return selectJoinedPageRecords(mainTableName, selectFields, joins, pageNum, pageSize, searchParams,
                allowedSearchFields, searchTypeMap, fieldColumnMapping, orderBy, dataScopeCondition, true);
    }

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
                                                             SqlCondition dataScopeCondition,
                                                             boolean aggregateChildren) {
        validateJoinQuery(mainTableName, selectFields, joins);

        StringBuilder whereClause = buildBaseWhereClause(mainTableName, "t0");
        MapSqlParameterSource params = buildBaseQueryParams("t0");
        appendSqlCondition(whereClause, params, dataScopeCondition);
        appendSearchConditions(whereClause, params, searchParams, allowedSearchFields, searchTypeMap, fieldColumnMapping);

        JoinedListSql joinedSql = resolveListJoinSql(mainTableName, selectFields, joins, aggregateChildren);
        String dataSql = paginateSql(buildJoinSelectClause(selectFields, joinedSql.distinctMainRows()) + " " + joinedSql.fromClause()
                + buildWhereSql(whereClause) + buildOrderByClause(orderBy), pageNum, pageSize);
        return jdbc().queryForList(dataSql, params);
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
        Long total = jdbc().queryForObject(countSql, params, Long.class);

        String dataSql = buildSelectSql(
                buildCustomSelectClause(selectedFields, allowedFields, columnMapping),
                tableName,
                whereClause
        );
        dataSql += buildOrderByClause(orderBy);
        dataSql = paginateSql(dataSql, pageNum, pageSize);
        logDynamicSql("自定义查询数据", dataSql, params);

        List<Map<String, Object>> records = jdbc().queryForList(dataSql, params);

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
                allowedFields, fieldColumnMapping, orderBy, null, true);
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
                                                            SqlCondition dataScopeCondition,
                                                            boolean aggregateChildren) {
        validateJoinQuery(mainTableName, selectFields, joins);

        StringBuilder whereClause = buildBaseWhereClause(mainTableName, "t0");
        MapSqlParameterSource params = buildBaseQueryParams("t0");
        appendSqlCondition(whereClause, params, dataScopeCondition);
        appendCustomConditions(whereClause, params, conditions, allowedFields, fieldColumnMapping);

        JoinedListSql joinedSql = resolveListJoinSql(mainTableName, selectFields, joins, aggregateChildren);
        String countSql = (joinedSql.distinctMainRows() ? "SELECT COUNT(DISTINCT " + qualifyPrimaryKey("t0") + ") " : "SELECT COUNT(*) ")
                + joinedSql.fromClause() + buildWhereSql(whereClause);
        logDynamicSql("自定义查询统计(左连接)", countSql, params);
        Long total = jdbc().queryForObject(countSql, params, Long.class);

        String dataSql = paginateSql(buildJoinSelectClause(selectFields, joinedSql.distinctMainRows()) + " " + joinedSql.fromClause()
                + buildWhereSql(whereClause) + buildOrderByClause(orderBy), pageNum, pageSize);
        logDynamicSql("自定义查询数据(左连接)", dataSql, params);
        List<Map<String, Object>> records = jdbc().queryForList(dataSql, params);

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
            if (OR_LIKE_SEARCH_KEY.equals(fieldName)) {
                appendOrLikeConditions(whereClause, params, value, allowedSearchFields, columnMapping);
                continue;
            }
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

    private void appendOrLikeConditions(StringBuilder whereClause,
                                        MapSqlParameterSource params,
                                        Object rawConditions,
                                        Set<String> allowedSearchFields,
                                        Map<String, String> columnMapping) {
        if (!(rawConditions instanceof List<?> conditions) || conditions.isEmpty()) {
            return;
        }
        StringBuilder orClause = new StringBuilder();
        int index = 0;
        for (Object rawCondition : conditions) {
            if (!(rawCondition instanceof Map<?, ?> condition)) {
                continue;
            }
            String fieldName = StringUtils.trimToNull(String.valueOf(condition.get("field")));
            Object value = condition.get("value");
            if (shouldSkipSearchField(fieldName, value, allowedSearchFields)) {
                continue;
            }
            String columnName = resolveSearchColumn(fieldName, columnMapping);
            if (columnName == null || !isKnownColumn(columnName, columnMapping)) {
                continue;
            }
            if (orClause.length() > 0) {
                orClause.append(" OR ");
            }
            String paramName = "or_like_" + index + "_" + columnName.replace(".", "_");
            orClause.append(columnName).append(" LIKE :").append(paramName);
            params.addValue(paramName, "%" + value + "%");
            index++;
        }
        if (orClause.length() == 0) {
            return;
        }
        appendWhereJoiner(whereClause);
        whereClause.append("(").append(orClause).append(")");
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
        addSelectedColumn(columns, primaryKeyField(), allowedFields, columnMapping);
        addSelectedColumn(columns, primaryKeyColumn(), allowedFields, columnMapping);
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
        appendLogicDeleteParam(params);
        return params;
    }

    private String buildPageDataSql(String tableName, StringBuilder whereClause, String orderBy, int pageNum, int pageSize) {
        String dataSql = buildSelectSql("SELECT *", tableName, whereClause);
        dataSql += buildOrderByClause(orderBy);
        return paginateSql(dataSql, pageNum, pageSize);
    }

    private String buildOrderByClause(String orderBy) {
        if (StringUtils.isNotBlank(orderBy)) {
            return " ORDER BY " + orderBy;
        }
        return " ORDER BY " + primaryKeyColumn() + " DESC";
    }

    private StringBuilder buildIdWhereClause(String tableName) {
        return buildIdWhereClause(tableName, primaryKeyColumn());
    }

    private StringBuilder buildIdWhereClause(String tableName, String primaryKeyColumn) {
        validateIdentifier(primaryKeyColumn);
        StringBuilder whereClause = new StringBuilder(primaryKeyColumn + " = :id");
        appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);
        return whereClause;
    }

    private MapSqlParameterSource buildIdQueryParams(Object id) {
        MapSqlParameterSource params = buildBaseQueryParams();
        appendIdParam(params, id);
        return params;
    }

    private void appendBaseQueryConditions(StringBuilder whereClause, MapSqlParameterSource params, String tableName) {
        appendBaseQueryConditions(whereClause, params, tableName, null);
    }

    private void appendBaseQueryConditions(StringBuilder whereClause, MapSqlParameterSource params,
                                           String tableName, String tableAlias) {
        String tenantColumn = tenantColumn();
        if (tenantStrategyEnabled() && (tableName == null || getTableColumns(tableName).contains(tenantColumn))) {
            appendTenantWhereClause(whereClause, params, tableAlias);
        }
        if (tableName != null && hasDelFlag(tableName)) {
            appendWhereCondition(whereClause, qualifyColumn(tableAlias, logicDeleteColumn()) + " = :logicActiveValue");
            appendLogicDeleteParam(params);
        }
    }

    /**
     * 根据ID查询
     */
    public Map<String, Object> selectById(String tableName, Object id) {
        return selectById(tableName, id, null);
    }

    public Map<String, Object> selectById(String tableName, Object id, SqlCondition dataScopeCondition) {
        return selectById(tableName, primaryKeyColumn(), id, dataScopeCondition);
    }

    public Map<String, Object> selectById(String tableName,
                                          String primaryKeyColumn,
                                          Object id,
                                          SqlCondition dataScopeCondition) {
        validateTableName(tableName);
        validateIdentifier(primaryKeyColumn);

        StringBuilder whereClause = buildIdWhereClause(tableName, primaryKeyColumn);
        MapSqlParameterSource params = buildIdQueryParams(id);
        appendSqlCondition(whereClause, params, dataScopeCondition);

        String sql = buildSelectSql("SELECT *", tableName, whereClause);
        List<Map<String, Object>> results = jdbc().queryForList(sql, params);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 批量查询：IN (:ids) 一次取回多条记录
     */
    public List<Map<String, Object>> selectByIds(String tableName,
                                                  String primaryKeyColumn,
                                                  List<?> ids,
                                                  SqlCondition dataScopeCondition) {
        validateTableName(tableName);
        validateIdentifier(primaryKeyColumn);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        MapSqlParameterSource params = buildBaseQueryParams();
        params.addValue("ids", ids);
        StringBuilder whereClause = new StringBuilder(primaryKeyColumn + " IN (:ids)");
        appendBaseQueryConditions(whereClause, params, tableName);
        appendSqlCondition(whereClause, params, dataScopeCondition);

        String sql = buildSelectSql("SELECT *", tableName, whereClause);
        return jdbc().queryForList(sql, params);
    }

    /** 事务命令状态门禁使用的行锁读取，条件与普通详情查询完全一致。 */
    public Map<String, Object> selectByIdForUpdate(String tableName,
                                                   String primaryKeyColumn,
                                                   Object id,
                                                   SqlCondition dataScopeCondition) {
        validateTableName(tableName);
        validateIdentifier(primaryKeyColumn);
        StringBuilder whereClause = buildIdWhereClause(tableName, primaryKeyColumn);
        MapSqlParameterSource params = buildIdQueryParams(id);
        appendSqlCondition(whereClause, params, dataScopeCondition);
        String sql = buildSelectSql("SELECT *", tableName, whereClause) + " FOR UPDATE";
        List<Map<String, Object>> results = jdbc().queryForList(sql, params);
        return results.isEmpty() ? null : results.get(0);
    }

    public List<Map<String, Object>> selectListByColumn(String tableName, String columnName, Object value) {
        validateTableName(tableName);
        validateIdentifier(columnName);
        if (value == null) {
            return List.of();
        }

        Object queryValue = normalizeColumnQueryValue(tableName, columnName, value);
        StringBuilder whereClause = new StringBuilder(columnName + " = :value");
        appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);
        MapSqlParameterSource params = buildBaseQueryParams();
        params.addValue("value", queryValue);
        String orderColumn = getTableColumns(tableName).contains(primaryKeyColumn()) ? primaryKeyColumn() : columnName;
        String sql = buildSelectSql("SELECT *", tableName, whereClause) + " ORDER BY " + orderColumn + " ASC";
        return jdbc().queryForList(sql, params);
    }

    public List<Map<String, Object>> selectListByColumnIn(String tableName,
                                                          String columnName,
                                                          Collection<?> values) {
        return selectListByColumnIn(tableName, columnName, values, null);
    }

    public List<Map<String, Object>> selectListByColumnIn(String tableName,
                                                          String columnName,
                                                          Collection<?> values,
                                                          SqlCondition dataScopeCondition) {
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
        List<Object> queryValues = nonNullValues.stream()
                .map(value -> normalizeColumnQueryValue(tableName, columnName, value))
                .distinct()
                .toList();

        StringBuilder whereClause = new StringBuilder(columnName + " IN (:values)");
        appendBaseQueryConditions(whereClause, new MapSqlParameterSource(), tableName);
        MapSqlParameterSource params = buildBaseQueryParams();
        params.addValue("values", queryValues);
        appendSqlCondition(whereClause, params, dataScopeCondition);
        String orderColumn = getTableColumns(tableName).contains(primaryKeyColumn()) ? primaryKeyColumn() : columnName;
        String sql = buildSelectSql("SELECT *", tableName, whereClause) + " ORDER BY " + orderColumn + " ASC";
        return jdbc().queryForList(sql, params);
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
        String sql = buildSelectSql("SELECT *", tableName, whereClause)
                + buildOrderByClause(orderBy);
        sql = limitSql(sql, Math.max(1, limit));
        return jdbc().queryForList(sql, params);
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
        String sql = buildSelectSql("SELECT COUNT(1)", tableName, whereClause);
        Long count = jdbc().queryForObject(sql, params, Long.class);
        return count != null && count > 0;
    }

    public boolean existsByColumns(String tableName,
                                   Map<String, Object> columnValues,
                                   Object excludeId) {
        return existsByColumns(tableName, columnValues, excludeId, null);
    }

    public boolean existsByColumns(String tableName,
                                   Map<String, Object> columnValues,
                                   Object excludeId,
                                   SqlCondition dataScopeCondition) {
        return existsByColumns(tableName, columnValues, primaryKeyColumn(), excludeId, dataScopeCondition);
    }

    public boolean existsByColumns(String tableName,
                                   Map<String, Object> columnValues,
                                   String primaryKeyColumn,
                                   Object excludeId,
                                   SqlCondition dataScopeCondition) {
        validateTableName(tableName);
        validateIdentifier(primaryKeyColumn);
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
            appendWhereCondition(whereClause, primaryKeyColumn + " <> :excludeId");
            params.addValue("excludeId", excludeId);
        }
        appendSqlCondition(whereClause, params, dataScopeCondition);
        String sql = buildSelectSql("SELECT COUNT(1)", tableName, whereClause);
        Long count = jdbc().queryForObject(sql, params, Long.class);
        return count != null && count > 0;
    }

    // ==================== 新增操作 ====================

    /**
     * 新增记录
     */
    public int insert(String tableName, Map<String, Object> data) {
        validateTableName(tableName);

        Map<String, Object> insertData = prepareInsertData(tableName, data);
        DataAuditTransactionHolder.prepareWrite(tableName, primaryKeyColumn(), insertData.get(primaryKeyColumn()),
                DataAuditTransactionHolder.WriteKind.INSERT);
        int affected = jdbc().update(buildInsertSql(tableName, insertData), toSqlParams(insertData));
        DataAuditTransactionHolder.afterWrite(tableName, insertData.get(primaryKeyColumn()), insertData,
                DataAuditTransactionHolder.currentFieldSource(DataAuditSourceType.FORM),
                DataAuditTransactionHolder.WriteKind.INSERT, affected);
        return affected;
    }

    /**
     * 新增记录并返回自增主键。
     */
    public Long insertReturningId(String tableName, Map<String, Object> data) {
        Object key = insertReturningKey(tableName, data, DEFAULT_PRIMARY_KEY, true);
        if (key == null) {
            return null;
        }
        if (key instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(key));
    }

    /**
     * 新增记录并返回运行时主键。非自增主键直接返回入参中的主键值。
     */
    public Object insertReturningKey(String tableName,
                                     Map<String, Object> data,
                                     String primaryKeyColumn,
                                     boolean autoIncrement) {
        validateTableName(tableName);
        validateIdentifier(primaryKeyColumn);

        Map<String, Object> insertData = prepareInsertData(tableName, data);
        if (!autoIncrement && !insertData.containsKey(primaryKeyColumn)) {
            throw new BusinessException("新增操作缺少主键字段: " + primaryKeyColumn);
        }
        DataAuditTransactionHolder.prepareWrite(tableName, primaryKeyColumn, insertData.get(primaryKeyColumn),
                DataAuditTransactionHolder.WriteKind.INSERT);
        Object generatedKey;
        if (!autoIncrement) {
            jdbc().update(buildInsertSql(tableName, insertData), toSqlParams(insertData));
            generatedKey = insertData.get(primaryKeyColumn);
        } else {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc().update(buildInsertSql(tableName, insertData), toSqlParams(insertData), keyHolder, new String[] { primaryKeyColumn });
            Number key = keyHolder.getKey();
            generatedKey = key == null ? insertData.get(primaryKeyColumn) : key;
        }
        DataAuditTransactionHolder.afterWrite(tableName, generatedKey, insertData,
                DataAuditTransactionHolder.currentFieldSource(DataAuditSourceType.FORM),
                DataAuditTransactionHolder.WriteKind.INSERT, 1);
        return generatedKey;
    }

    // ==================== 更新操作 ====================

    /**
     * 根据ID更新
     */
    public int updateById(String tableName, Object id, Map<String, Object> data) {
        return updateById(tableName, id, data, null);
    }

    public int updateById(String tableName, Object id, Map<String, Object> data, SqlCondition dataScopeCondition) {
        return updateById(tableName, primaryKeyColumn(), id, data, dataScopeCondition);
    }

    public int updateById(String tableName,
                          String primaryKeyColumn,
                          Object id,
                          Map<String, Object> data,
                          SqlCondition dataScopeCondition) {
        validateTableName(tableName);
        validateIdentifier(primaryKeyColumn);

        Map<String, Object> updateData = prepareUpdateData(tableName, data, primaryKeyColumn);
        DataAuditTransactionHolder.prepareWrite(tableName, primaryKeyColumn, id, DataAuditTransactionHolder.WriteKind.UPDATE);
        MapSqlParameterSource params = toSqlParams(updateData, id);
        String sql = appendTenantCondition(buildUpdateSql(tableName, updateData, primaryKeyColumn), params, tableName);
        sql = appendLogicActiveCondition(sql, params, tableName);
        sql = appendSqlCondition(sql, params, dataScopeCondition);
        int affected = jdbc().update(sql, params);
        DataAuditTransactionHolder.afterWrite(tableName, id, updateData,
                DataAuditTransactionHolder.currentFieldSource(DataAuditSourceType.FORM),
                DataAuditTransactionHolder.WriteKind.UPDATE, affected);
        return affected;
    }

    /**
     * 在单条 UPDATE 中原子调整多个数值列，并把上下界和期望状态放进同一 WHERE。
     */
    public int adjustNumbersById(String tableName,
                                 String primaryKeyColumn,
                                 Object id,
                                 Map<String, BigDecimal> deltas,
                                 Map<String, BigDecimal> minimums,
                                 Map<String, BigDecimal> maximums,
                                 SqlCondition condition) {
        validateTableName(tableName);
        validateIdentifier(primaryKeyColumn);
        if (id == null) {
            throw new BusinessException("数值调整缺少目标记录 ID");
        }
        if (deltas == null || deltas.isEmpty()) {
            throw new BusinessException("数值调整字段不能为空");
        }
        Set<String> tableColumns = getTableColumns(tableName);
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        List<String> setClauses = new ArrayList<>();
        List<String> boundConditions = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, BigDecimal> entry : deltas.entrySet()) {
            String column = entry.getKey();
            validateIdentifier(column);
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
            BigDecimal minimum = minimums == null ? null : minimums.get(column);
            if (minimum != null) {
                String minParam = "adjustMin" + index;
                params.addValue(minParam, minimum);
                boundConditions.add(column + " + :" + deltaParam + " >= :" + minParam);
            }
            BigDecimal maximum = maximums == null ? null : maximums.get(column);
            if (maximum != null) {
                String maxParam = "adjustMax" + index;
                params.addValue(maxParam, maximum);
                boundConditions.add(column + " + :" + deltaParam + " <= :" + maxParam);
            }
            index++;
        }

        Map<String, Object> auditValues = new LinkedHashMap<>();
        fillUpdateAuditFields(auditValues, tableColumns);
        auditValues.forEach((column, value) -> {
            if (!deltas.containsKey(column)) {
                validateIdentifier(column);
                setClauses.add(column + " = :" + column);
                params.addValue(column, value);
            }
        });

        String sql = "UPDATE " + tableName + " SET " + String.join(", ", setClauses)
                + " WHERE " + primaryKeyColumn + " = :id";
        sql = appendTenantCondition(sql, params, tableName);
        sql = appendLogicActiveCondition(sql, params, tableName);
        sql = appendSqlCondition(sql, params, condition);
        for (String bound : boundConditions) {
            sql += " AND (" + bound + ")";
        }
        DataAuditTransactionHolder.prepareWrite(tableName, primaryKeyColumn, id, DataAuditTransactionHolder.WriteKind.UPDATE);
        int affected = jdbc().update(sql, params);
        DataAuditTransactionHolder.afterWrite(tableName, id, Map.of(),
                DataAuditTransactionHolder.currentFieldSource(DataAuditSourceType.BUSINESS_ACTION),
                DataAuditTransactionHolder.WriteKind.UPDATE, affected);
        return affected;
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
        String sql = buildSelectSql("SELECT id", tableName, whereClause) + " ORDER BY id ASC";
        sql = limitSql(sql, 1);
        List<Long> ids = jdbc().queryForList(sql, params, Long.class);
        return ids.isEmpty() ? null : ids.get(0);
    }

    // ==================== 删除操作 ====================

    /**
     * 根据ID删除
     */
    public int deleteById(String tableName, Object id, boolean logicDelete) {
        return deleteById(tableName, id, logicDelete, null);
    }

    public int deleteById(String tableName, Object id, boolean logicDelete, SqlCondition dataScopeCondition) {
        return deleteById(tableName, primaryKeyColumn(), id, logicDelete, dataScopeCondition);
    }

    public int deleteById(String tableName,
                          String primaryKeyColumn,
                          Object id,
                          boolean logicDelete,
                          SqlCondition dataScopeCondition) {
        validateTableName(tableName);
        validateIdentifier(primaryKeyColumn);

        MapSqlParameterSource params = toIdParam(id);
        if (logicDelete) {
            params.addValue("deletedValue", logicDeletedValue());
        }
        DataAuditTransactionHolder.prepareWrite(tableName, primaryKeyColumn, id, DataAuditTransactionHolder.WriteKind.DELETE);
        String sql = appendTenantCondition(buildDeleteSql(tableName, logicDelete, primaryKeyColumn), params, tableName);
        sql = appendSqlCondition(sql, params, dataScopeCondition);
        int affected = jdbc().update(sql, params);
        DataAuditTransactionHolder.afterWrite(tableName, id, Map.of(),
                DataAuditTransactionHolder.currentFieldSource(DataAuditSourceType.FORM),
                DataAuditTransactionHolder.WriteKind.DELETE, affected);
        return affected;
    }

    /**
     * 批量删除（IN 子句），一条 SQL 处理多条记录
     */
    public int deleteByIds(String tableName,
                           String primaryKeyColumn,
                           List<?> ids,
                           boolean logicDelete,
                           SqlCondition dataScopeCondition) {
        validateTableName(tableName);
        validateIdentifier(primaryKeyColumn);
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ids", ids);
        if (logicDelete) {
            params.addValue("deletedValue", logicDeletedValue());
        }
        for (Object id : ids) {
            DataAuditTransactionHolder.prepareWrite(tableName, primaryKeyColumn, id, DataAuditTransactionHolder.WriteKind.DELETE);
        }
        String sql = appendTenantCondition(buildBatchDeleteSql(tableName, logicDelete, primaryKeyColumn), params, tableName);
        sql = appendSqlCondition(sql, params, dataScopeCondition);
        int affected = jdbc().update(sql, params);
        for (Object id : ids) {
            DataAuditTransactionHolder.afterWrite(tableName, id, Map.of(),
                    DataAuditTransactionHolder.currentFieldSource(DataAuditSourceType.FORM),
                    DataAuditTransactionHolder.WriteKind.DELETE, affected > 0 ? 1 : 0);
        }
        return affected;
    }

    public int deleteByColumn(String tableName, String columnName, Object value, boolean logicDelete) {
        validateTableName(tableName);
        validateIdentifier(columnName);
        if (value == null) {
            return 0;
        }
        captureColumnDelete(tableName, columnName, value);
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("value", value);
        String sql;
        if (logicDelete) {
            params.addValue("deletedValue", logicDeletedValue());
            sql = "UPDATE " + tableName + " SET " + logicDeleteSetClause(tableName)
                    + " WHERE " + columnName + " = :value";
        } else {
            sql = "DELETE FROM " + tableName + " WHERE " + columnName + " = :value";
        }
        return jdbc().update(appendTenantCondition(sql, params, tableName), params);
    }

    // ==================== 工具方法 ====================

    /**
     * 检查表是否存在
     */
    public boolean tableExists(String tableName) {
        try {
            LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
            RuntimeDatabaseDialect dialect = dialectFactory.resolve(context);
            Integer count = jdbcTemplateProvider.jdbcTemplate(context)
                    .queryForObject(dialect.tableExistsSql(), Integer.class, tableName);
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
        String cacheKey = metadataCacheKey(tableName) + ":logic:" + logicDeleteColumn() + ":" + logicDeleteEnabled();
        return delFlagCache.computeIfAbsent(cacheKey, key -> {
            try {
                return logicDeleteEnabled() && getTableColumns(tableName).contains(logicDeleteColumn());
            } catch (Exception e) {
                log.warn("[DynamicCrudRepository] 检查del_flag失败, tableName={}", tableName, e);
                return false;
            }
        });
    }

    /**
     * 获取表的所有列名（带缓存）
     */
    public Set<String> getTableColumns(String tableName) {
        String cacheKey = metadataCacheKey(tableName);
        return tableColumnsCache.computeIfAbsent(cacheKey, key -> {
            try {
                LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
                RuntimeDatabaseDialect dialect = dialectFactory.resolve(context);
                List<String> columns = jdbcTemplateProvider.jdbcTemplate(context)
                        .queryForList(dialect.listColumnsSql(), String.class, tableName);
                return columns.stream()
                        .map(column -> StringUtils.defaultString(column).toLowerCase(Locale.ROOT))
                        .collect(Collectors.toCollection(HashSet::new));
            } catch (Exception e) {
                log.warn("[DynamicCrudRepository] 获取表列名失败, tableName={}", tableName, e);
                return Collections.emptySet();
            }
        });
    }

    /**
     * 获取表的字段映射（camelCase -> snake_case）
     */
    public Map<String, String> getColumnMapping(String tableName) {
        String cacheKey = metadataCacheKey(tableName);
        return columnMappingCache.computeIfAbsent(cacheKey, key -> {
            Map<String, String> mapping = new HashMap<>();
            Set<String> columns = getTableColumns(tableName);
            for (String column : columns) {
                String camelName = DynamicQueryGenerator.snakeToCamel(column);
                mapping.put(camelName, column);
                mapping.put(column, column);
            }
            return mapping;
        });
    }

    private Object normalizeColumnQueryValue(String tableName, String columnName, Object value) {
        if (value == null) {
            return null;
        }
        Integer jdbcType = getColumnJdbcTypes(tableName).get(StringUtils.defaultString(columnName).toLowerCase(Locale.ROOT));
        if (jdbcType != null && isCharacterJdbcType(jdbcType)) {
            return String.valueOf(value);
        }
        return value;
    }

    private Map<String, Integer> getColumnJdbcTypes(String tableName) {
        String cacheKey = metadataCacheKey(tableName);
        return columnTypesCache.computeIfAbsent(cacheKey, key -> {
            try {
                LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
                return jdbcTemplateProvider.jdbcTemplate(context).execute((ConnectionCallback<Map<String, Integer>>) connection -> {
                    Map<String, Integer> types = new HashMap<>();
                    DatabaseMetaData metadata = connection.getMetaData();
                    try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, tableName, null)) {
                        while (columns.next()) {
                            String columnName = columns.getString("COLUMN_NAME");
                            if (StringUtils.isNotBlank(columnName)) {
                                types.put(columnName.toLowerCase(Locale.ROOT), columns.getInt("DATA_TYPE"));
                            }
                        }
                    }
                    return types;
                });
            } catch (Exception e) {
                log.warn("[DynamicCrudRepository] 获取表字段类型失败, tableName={}", tableName, e);
                return Collections.emptyMap();
            }
        });
    }

    private void captureColumnDelete(String tableName, String columnName, Object value) {
        if (DataAuditTransactionHolder.index().findTable(DataAuditTenantSupport.currentTenantIdOrNull(), tableName) == null) {
            return;
        }
        List<Map<String, Object>> rows = selectListByColumn(tableName, columnName, value);
        for (Map<String, Object> row : rows) {
            Object id = row.get(primaryKeyColumn());
            if (id == null) {
                id = row.get("id");
            }
            if (id == null) {
                id = row.get("ID");
            }
            DataAuditTransactionHolder.prepareWrite(tableName, primaryKeyColumn(), id, DataAuditTransactionHolder.WriteKind.DELETE);
            DataAuditTransactionHolder.afterWrite(tableName, id, row,
                    DataAuditTransactionHolder.currentFieldSource(DataAuditSourceType.FORM),
                    DataAuditTransactionHolder.WriteKind.DELETE, 1);
        }
    }

    private boolean isCharacterJdbcType(int jdbcType) {
        return jdbcType == Types.CHAR
                || jdbcType == Types.VARCHAR
                || jdbcType == Types.LONGVARCHAR
                || jdbcType == Types.NCHAR
                || jdbcType == Types.NVARCHAR
                || jdbcType == Types.LONGNVARCHAR;
    }

    /**
     * DDL 执行后清理动态表结构缓存，避免追加字段后运行时继续使用旧列集合。
     */
    public void clearTableMetadataCache(String tableName) {
        String suffix = ":" + tableName;
        delFlagCache.keySet().removeIf(key -> key.endsWith(suffix));
        tableColumnsCache.keySet().removeIf(key -> key.endsWith(suffix));
        columnTypesCache.keySet().removeIf(key -> key.endsWith(suffix));
        columnMappingCache.keySet().removeIf(key -> key.endsWith(suffix));
    }

    public void clearTableMetadataCache(LowcodeRuntimeDataSourceContext context, String tableName) {
        String cacheKey = metadataCacheKey(context, tableName);
        delFlagCache.remove(cacheKey);
        tableColumnsCache.remove(cacheKey);
        columnTypesCache.remove(cacheKey);
        columnMappingCache.remove(cacheKey);
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
            selectItems.add(qualifyColumn(field.tableAlias(), field.columnName()) + " AS " + quoteIdentifier(field.fieldName()));
        }
        return "SELECT " + (distinct ? "DISTINCT " : "") + String.join(", ", selectItems);
    }

    private boolean selectsOnlyMainTable(List<JoinField> fields) {
        return fields != null && fields.stream().allMatch(field -> "t0".equals(field.tableAlias()));
    }

    private record JoinedListSql(String fromClause, boolean distinctMainRows) {
    }

    /**
     * 列表查出子表列时，按子表外键聚合成一行再左连接，避免一对多把主表行乘开。
     */
    private JoinedListSql resolveListJoinSql(String mainTableName,
                                             List<JoinField> selectFields,
                                             List<JoinSpec> joins,
                                             boolean aggregateChildren) {
        boolean shouldAggregate = aggregateChildren && !selectsOnlyMainTable(selectFields);
        String fromClause = shouldAggregate
                ? buildAggregatedJoinedFromClause(mainTableName, joins, selectFields)
                : buildJoinedFromClause(mainTableName, joins);
        return new JoinedListSql(fromClause, shouldAggregate || selectsOnlyMainTable(selectFields));
    }

    private String buildAggregatedJoinedFromClause(String mainTableName, List<JoinSpec> joins, List<JoinField> selectFields) {
        StringBuilder sql = new StringBuilder("FROM ")
                .append(mainTableName)
                .append(" t0");
        for (JoinSpec join : joins) {
            sql.append(" LEFT JOIN (")
                    .append(buildAggregatedChildSelect(join, selectFields))
                    .append(") ")
                    .append(join.tableAlias())
                    .append(" ON ")
                    .append(qualifyColumn(join.tableAlias(), join.joinColumn()))
                    .append(" = ")
                    .append(qualifyColumn("t0", join.mainColumn()));
        }
        return sql.toString();
    }

    private String buildAggregatedChildSelect(JoinSpec join, List<JoinField> selectFields) {
        LinkedHashSet<String> columns = new LinkedHashSet<>();
        columns.add(join.joinColumn());
        if (selectFields != null) {
            for (JoinField field : selectFields) {
                if (field != null && join.tableAlias().equals(field.tableAlias()) && StringUtils.isNotBlank(field.columnName())) {
                    columns.add(field.columnName());
                }
            }
        }
        Set<String> tableColumns = getTableColumns(join.tableName());
        boolean hasId = tableColumns.contains("id");
        StringBuilder sql = new StringBuilder("SELECT ");
        boolean first = true;
        for (String column : columns) {
            if (!first) {
                sql.append(", ");
            }
            first = false;
            if (column.equals(join.joinColumn())) {
                sql.append(quoteIdentifier(column));
                continue;
            }
            sql.append(aggregateExpression(column, hasId)).append(" AS ").append(quoteIdentifier(column));
        }
        sql.append(" FROM ").append(join.tableName());
        StringBuilder where = new StringBuilder();
        Long tenantId = TenantContextHolder.getTenantId();
        String tenantColumn = tenantColumn();
        if (tenantId != null && tenantStrategyEnabled() && tableColumns.contains(tenantColumn)) {
            where.append(quoteIdentifier(tenantColumn)).append(" = :tenantId");
        }
        if (hasDelFlag(join.tableName())) {
            if (!where.isEmpty()) {
                where.append(" AND ");
            }
            where.append(quoteIdentifier(logicDeleteColumn())).append(" = :logicActiveValue");
        }
        if (!where.isEmpty()) {
            sql.append(" WHERE ").append(where);
        }
        sql.append(" GROUP BY ").append(quoteIdentifier(join.joinColumn()));
        return sql.toString();
    }

    private String aggregateExpression(String column, boolean hasId) {
        String quoted = quoteIdentifier(column);
        String orderColumn = hasId ? quoteIdentifier("id") : quoted;
        RuntimeDatabaseDialect dialect = dialectFactory.resolve(LowcodeRuntimeDataSourceContextHolder.get());
        if (dialect instanceof PostgreSqlRuntimeDatabaseDialect) {
            return "string_agg(" + quoted + "::text, '、' ORDER BY " + orderColumn + ")";
        }
        if (dialect instanceof OracleRuntimeDatabaseDialect) {
            return "LISTAGG(" + quoted + ", '、') WITHIN GROUP (ORDER BY " + orderColumn + ")";
        }
        return "GROUP_CONCAT(" + quoted + " ORDER BY " + orderColumn + " SEPARATOR '、')";
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
            String tenantColumn = tenantColumn();
            if (tenantId != null && tenantStrategyEnabled() && getTableColumns(join.tableName()).contains(tenantColumn)) {
                sql.append(" AND ").append(qualifyColumn(join.tableAlias(), tenantColumn)).append(" = :tenantId");
            }
            if (hasDelFlag(join.tableName())) {
                sql.append(" AND ").append(qualifyColumn(join.tableAlias(), logicDeleteColumn())).append(" = :logicActiveValue");
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

    private String qualifyPrimaryKey(String tableAlias) {
        return qualifyColumn(tableAlias, primaryKeyColumn());
    }

    private String primaryKeyColumn() {
        LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
        String columnName = context == null || context.getPrimaryKey() == null
                ? DEFAULT_PRIMARY_KEY
                : context.getPrimaryKey().getColumnName();
        columnName = StringUtils.defaultIfBlank(columnName, DEFAULT_PRIMARY_KEY);
        validateIdentifier(columnName);
        return columnName;
    }

    private String primaryKeyField() {
        LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
        String field = context == null || context.getPrimaryKey() == null
                ? DEFAULT_PRIMARY_KEY
                : context.getPrimaryKey().getField();
        field = StringUtils.defaultIfBlank(field, DEFAULT_PRIMARY_KEY);
        validateIdentifier(field);
        return field;
    }

    private String quoteIdentifier(String identifier) {
        validateAlias(identifier);
        return dialectFactory.resolve(LowcodeRuntimeDataSourceContextHolder.get()).quote(identifier);
    }

    private String paginateSql(String sql, int pageNum, int pageSize) {
        long limit = Math.max(1, pageSize);
        long offset = Math.max(0, pageNum - 1L) * limit;
        return dialectFactory.resolve(LowcodeRuntimeDataSourceContextHolder.get()).paginate(sql, offset, limit);
    }

    private String limitSql(String sql, int limit) {
        return dialectFactory.resolve(LowcodeRuntimeDataSourceContextHolder.get())
                .paginate(sql, 0, Math.max(1, limit));
    }

    private void appendTenantWhereClause(StringBuilder whereClause, MapSqlParameterSource params) {
        appendTenantWhereClause(whereClause, params, null);
    }

    private void appendTenantWhereClause(StringBuilder whereClause, MapSqlParameterSource params, String tableAlias) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || !tenantStrategyEnabled()) {
            return;
        }
        appendWhereCondition(whereClause, qualifyColumn(tableAlias, tenantColumn()) + " = :tenantId");
        params.addValue("tenantId", tenantId);
    }

    private void appendLogicDeleteParam(MapSqlParameterSource params) {
        if (params != null && logicDeleteEnabled()) {
            params.addValue("logicActiveValue", logicActiveValue());
        }
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
        return appendTenantCondition(sql, params, null);
    }

    private String appendTenantCondition(String sql, MapSqlParameterSource params, String tableName) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || !tenantStrategyEnabled()) {
            return sql;
        }
        String tenantColumn = tenantColumn();
        if (tableName != null && !getTableColumns(tableName).contains(tenantColumn)) {
            return sql;
        }
        params.addValue("tenantId", tenantId);
        return sql + " AND " + tenantColumn + " = :tenantId";
    }

    private String appendLogicActiveCondition(String sql, MapSqlParameterSource params, String tableName) {
        if (tableName == null || !hasDelFlag(tableName)) {
            return sql;
        }
        appendLogicDeleteParam(params);
        return sql + " AND " + logicDeleteColumn() + " = :logicActiveValue";
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

    private String buildUpdateSql(String tableName, Map<String, Object> data, String primaryKeyColumn) {
        String setClauses = data.entrySet().stream()
                .map(entry -> entry.getKey() + " = :" + entry.getKey())
                .collect(Collectors.joining(", "));
        return "UPDATE " + tableName + " SET " + setClauses + " WHERE " + primaryKeyColumn + " = :id";
    }

    private String buildDeleteSql(String tableName, boolean logicDelete, String primaryKeyColumn) {
        if (logicDelete) {
            return "UPDATE " + tableName + " SET " + logicDeleteSetClause(tableName) + " WHERE "
                    + primaryKeyColumn + " = :id";
        }
        return "DELETE FROM " + tableName + " WHERE " + primaryKeyColumn + " = :id";
    }

    private String buildBatchDeleteSql(String tableName, boolean logicDelete, String primaryKeyColumn) {
        if (logicDelete) {
            return "UPDATE " + tableName + " SET " + logicDeleteSetClause(tableName) + " WHERE "
                    + primaryKeyColumn + " IN (:ids)";
        }
        return "DELETE FROM " + tableName + " WHERE " + primaryKeyColumn + " IN (:ids)";
    }

    private String logicDeleteSetClause(String tableName) {
        StringBuilder setClause = new StringBuilder(logicDeleteColumn()).append(" = :deletedValue");
        String updateTimeColumn = auditUpdateTimeColumn(auditStrategy());
        if (auditStrategyEnabled() && getTableColumns(tableName).contains(updateTimeColumn)) {
            setClause.append(", ").append(updateTimeColumn).append(" = CURRENT_TIMESTAMP");
        }
        return setClause.toString();
    }

    private MapSqlParameterSource toSqlParams(Map<String, Object> data) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        data.forEach(params::addValue);
        return params;
    }

    private MapSqlParameterSource toSqlParams(Map<String, Object> data, Object id) {
        MapSqlParameterSource params = toSqlParams(data);
        appendIdParam(params, id);
        return params;
    }

    private MapSqlParameterSource toIdParam(Object id) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        appendIdParam(params, id);
        return params;
    }

    private void appendIdParam(MapSqlParameterSource params, Object id) {
        params.addValue("id", id);
    }

    private String metadataCacheKey(String tableName) {
        return metadataCacheKey(LowcodeRuntimeDataSourceContextHolder.get(), tableName);
    }

    private String metadataCacheKey(LowcodeRuntimeDataSourceContext context, String tableName) {
        String datasourceKey = context == null || context.isMaster()
                ? "master"
                : String.valueOf(context.getDatasourceId());
        return datasourceKey + ":" + tableName;
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
        return prepareUpdateData(tableName, data, DEFAULT_PRIMARY_KEY);
    }

    private Map<String, Object> prepareUpdateData(String tableName, Map<String, Object> data, String primaryKeyColumn) {
        Map<String, Object> updateData = prepareWriteData(data, "没有可更新的字段");
        removeImmutableFields(updateData, DEFAULT_PRIMARY_KEY, primaryKeyColumn, "tenant_id", tenantColumn());
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
        Long userId = auditSessionValue(SessionHelper::getUserId);
        Long mainOrgId = auditSessionValue(SessionHelper::getMainOrgId);

        if (tenantStrategyEnabled()) {
            putIfColumnExists(data, columns, tenantColumn(), tenantId);
        }
        if (logicDeleteEnabled()) {
            putIfColumnExists(data, columns, logicDeleteColumn(), logicActiveValue());
        }
        if (!shouldFillAuditColumns(columns)) {
            return;
        }
        LowcodeAuditStrategy auditStrategy = effectiveAuditStrategy(columns);
        putIfColumnExists(data, columns, auditCreateByColumn(auditStrategy), userId);
        putIfColumnExists(data, columns, auditCreateDeptColumn(auditStrategy), mainOrgId);
        putIfColumnExists(data, columns, auditCreateTimeColumn(auditStrategy), now);
        putIfColumnExists(data, columns, auditUpdateByColumn(auditStrategy), userId);
        putIfColumnExists(data, columns, auditUpdateTimeColumn(auditStrategy), now);
    }

    private void fillUpdateAuditFields(Map<String, Object> data, Set<String> columns) {
        if (!shouldFillAuditColumns(columns)) {
            return;
        }
        Date now = new Date();
        Long userId = auditSessionValue(SessionHelper::getUserId);
        LowcodeAuditStrategy auditStrategy = effectiveAuditStrategy(columns);

        putIfColumnExists(data, columns, auditUpdateByColumn(auditStrategy), userId);
        putIfColumnExists(data, columns, auditUpdateTimeColumn(auditStrategy), now);
    }

    /**
     * 导入已有表时，若策略误标 NONE 但物理表仍有 Forge 标准审计列，按列存在情况补齐。
     */
    private boolean shouldFillAuditColumns(Set<String> columns) {
        if (auditStrategyEnabled()) {
            return true;
        }
        return hasStandardAuditColumn(columns);
    }

    private LowcodeAuditStrategy effectiveAuditStrategy(Set<String> columns) {
        LowcodeAuditStrategy strategy = auditStrategy();
        if (auditStrategyEnabled()) {
            return strategy;
        }
        LowcodeAuditStrategy fallback = new LowcodeAuditStrategy();
        fallback.setMode("FORGE_COLUMNS");
        if (columns == null || columns.isEmpty()) {
            fallback.setCreateByColumn("create_by");
            fallback.setCreateTimeColumn("create_time");
            fallback.setCreateDeptColumn("create_dept");
            fallback.setUpdateByColumn("update_by");
            fallback.setUpdateTimeColumn("update_time");
            return fallback;
        }
        if (columns.contains("create_by")) {
            fallback.setCreateByColumn("create_by");
        }
        if (columns.contains("create_time")) {
            fallback.setCreateTimeColumn("create_time");
        }
        if (columns.contains("create_dept")) {
            fallback.setCreateDeptColumn("create_dept");
        }
        if (columns.contains("update_by")) {
            fallback.setUpdateByColumn("update_by");
        }
        if (columns.contains("update_time")) {
            fallback.setUpdateTimeColumn("update_time");
        }
        return fallback;
    }

    private boolean hasStandardAuditColumn(Set<String> columns) {
        if (columns == null || columns.isEmpty()) {
            return false;
        }
        return columns.contains("create_by")
                || columns.contains("create_time")
                || columns.contains("create_dept")
                || columns.contains("update_by")
                || columns.contains("update_time");
    }

    private Long auditSessionValue(java.util.function.Supplier<Long> supplier) {
        try {
            // SessionHelper 优先读取显式执行身份，其次才是 Web 登录会话。
            return supplier.get();
        } catch (SaTokenContextException exception) {
            // 流程消息/定时任务没有 Web 会话，不伪造操作者；系统审计单独记录来源。
            // 只处理缺少请求上下文，其他异常仍向上传播；不影响租户或数据权限条件。
            log.debug("[DynamicCrudRepository] 后台写入无 Web 审计会话");
            return null;
        }
    }

    private boolean tenantStrategyEnabled() {
        LowcodeTenantStrategy strategy = tenantStrategy();
        return strategy == null || !isNoneMode(strategy.getMode());
    }

    private String tenantColumn() {
        LowcodeTenantStrategy strategy = tenantStrategy();
        String column = strategy == null ? null : strategy.getColumnName();
        column = StringUtils.defaultIfBlank(column, "tenant_id");
        validateIdentifier(column);
        return column;
    }

    private LowcodeTenantStrategy tenantStrategy() {
        LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
        return context == null ? null : context.getTenantStrategy();
    }

    private boolean auditStrategyEnabled() {
        LowcodeAuditStrategy strategy = auditStrategy();
        return strategy == null || !isNoneMode(strategy.getMode());
    }

    private LowcodeAuditStrategy auditStrategy() {
        LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
        return context == null ? null : context.getAuditStrategy();
    }

    private String auditCreateByColumn(LowcodeAuditStrategy strategy) {
        return auditColumn(strategy == null ? null : strategy.getCreateByColumn(), "create_by");
    }

    private String auditCreateTimeColumn(LowcodeAuditStrategy strategy) {
        return auditColumn(strategy == null ? null : strategy.getCreateTimeColumn(), "create_time");
    }

    private String auditCreateDeptColumn(LowcodeAuditStrategy strategy) {
        return auditColumn(strategy == null ? null : strategy.getCreateDeptColumn(), "create_dept");
    }

    private String auditUpdateByColumn(LowcodeAuditStrategy strategy) {
        return auditColumn(strategy == null ? null : strategy.getUpdateByColumn(), "update_by");
    }

    private String auditUpdateTimeColumn(LowcodeAuditStrategy strategy) {
        return auditColumn(strategy == null ? null : strategy.getUpdateTimeColumn(), "update_time");
    }

    private String auditColumn(String configuredColumn, String defaultColumn) {
        String column = StringUtils.defaultIfBlank(configuredColumn, defaultColumn);
        validateIdentifier(column);
        return column;
    }

    private boolean logicDeleteEnabled() {
        LowcodeLogicDeleteStrategy strategy = logicDeleteStrategy();
        return strategy == null || !isNoneMode(strategy.getMode());
    }

    private String logicDeleteColumn() {
        LowcodeLogicDeleteStrategy strategy = logicDeleteStrategy();
        String column = strategy == null ? null : strategy.getColumnName();
        column = StringUtils.defaultIfBlank(column, "del_flag");
        validateIdentifier(column);
        return column;
    }

    private Object logicActiveValue() {
        LowcodeLogicDeleteStrategy strategy = logicDeleteStrategy();
        return StringUtils.defaultIfBlank(strategy == null ? null : strategy.getActiveValue(), "0");
    }

    private Object logicDeletedValue() {
        LowcodeLogicDeleteStrategy strategy = logicDeleteStrategy();
        return StringUtils.defaultIfBlank(strategy == null ? null : strategy.getDeletedValue(), "1");
    }

    private LowcodeLogicDeleteStrategy logicDeleteStrategy() {
        LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
        return context == null ? null : context.getLogicDeleteStrategy();
    }

    private boolean isNoneMode(String mode) {
        return "NONE".equalsIgnoreCase(StringUtils.defaultString(mode));
    }

    private void putIfColumnExists(Map<String, Object> data, Set<String> columns, String column, Object value) {
        if (value == null || StringUtils.isBlank(column)) {
            return;
        }
        // 元数据查询失败时 columns 为空：仍尝试写入策略列，避免导入表审计字段静默丢失。
        if (columns != null && !columns.isEmpty() && !columns.contains(column)) {
            return;
        }
        if (!data.containsKey(column) || data.get(column) == null) {
            data.put(column, value);
        }
    }
}
