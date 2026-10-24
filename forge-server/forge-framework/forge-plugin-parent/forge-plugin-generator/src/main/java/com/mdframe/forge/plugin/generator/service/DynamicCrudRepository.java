package com.mdframe.forge.plugin.generator.service;

import com.mdframe.forge.plugin.generator.enums.DataAuditSourceType;
import com.mdframe.forge.plugin.generator.service.audit.DataAuditTenantSupport;
import com.mdframe.forge.plugin.generator.service.audit.DataAuditTransactionHolder;
import com.mdframe.forge.plugin.generator.dto.CustomQueryConditionDTO;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialectFactory;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeJdbcTemplateProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

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
    private final RuntimeJdbcTemplateProvider jdbcTemplateProvider;
    private final RuntimeDatabaseDialectFactory dialectFactory;

    private static final String DEFAULT_PRIMARY_KEY = "id";
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,63}$");
    /** 动态 CRUD 查询硬超时，避免 /page 因探库/锁等待一直 pending */
    private static final int QUERY_TIMEOUT_SECONDS = 15;

    public record JoinField(String fieldName, String tableAlias, String columnName) {
    }

    public record JoinSpec(String tableName, String tableAlias, String joinColumn, String mainColumn) {
    }

    public record SqlCondition(String sql, Map<String, Object> params) {
    }

    private volatile DynamicCrudTableMetadataGateway tableMetadataGateway;

    NamedParameterJdbcTemplate jdbc() {
        LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
        return context == null ? namedJdbcTemplate : jdbcTemplateProvider.namedJdbcTemplate(context);
    }

    private Long queryForLong(String sql, MapSqlParameterSource params) {
        Long value = jdbc().execute(sql, params, ps -> {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long number = rs.getLong(1);
                    return rs.wasNull() ? null : number;
                }
                return null;
            }
        });
        return value;
    }

    private List<Map<String, Object>> queryForList(String sql, MapSqlParameterSource params) {
        return jdbc().execute(sql, params, ps -> {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                org.springframework.jdbc.core.ColumnMapRowMapper mapper =
                        new org.springframework.jdbc.core.ColumnMapRowMapper();
                List<Map<String, Object>> rows = new java.util.ArrayList<>();
                int rowNum = 0;
                while (rs.next()) {
                    rows.add(mapper.mapRow(rs, rowNum++));
                }
                return rows;
            }
        });
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
        Long total = queryForLong(countSql, params);

        String dataSql = buildPageDataSql(tableName, whereClause, orderBy, pageNum, pageSize);

        List<Map<String, Object>> records = queryForList(dataSql, params);

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

        DynamicCrudJoinQueryCompiler.QueryPlan joinedSql = joinQueryCompiler()
                .resolveListPlan(mainTableName, selectFields, joins, aggregateChildren);
        String countSql = (joinedSql.distinctMainRows() ? "SELECT COUNT(DISTINCT " + qualifyPrimaryKey("t0") + ") " : "SELECT COUNT(*) ")
                + joinedSql.fromClause() + buildWhereSql(whereClause);
        Long total = queryForLong(countSql, params);

        String dataSql = paginateSql(buildJoinSelectClause(selectFields, joinedSql.distinctMainRows()) + " " + joinedSql.fromClause()
                + buildWhereSql(whereClause) + buildOrderByClause(orderBy), pageNum, pageSize);
        List<Map<String, Object>> records = queryForList(dataSql, params);

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
        sqlSupport().appendBaseQueryConditions(
                whereClause, new MapSqlParameterSource(), mainTableName, "t0");
        MapSqlParameterSource params = buildBaseQueryParams("t0");
        params.addValue("id", id);
        appendSqlCondition(whereClause, params, dataScopeCondition);

        String sql = buildJoinSelectClause(selectFields) + " " + joinQueryCompiler().buildJoinedFromClause(mainTableName, joins)
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

        DynamicCrudJoinQueryCompiler.QueryPlan joinedSql = joinQueryCompiler()
                .resolveListPlan(mainTableName, selectFields, joins, aggregateChildren);
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

        DynamicCrudJoinQueryCompiler.QueryPlan joinedSql = joinQueryCompiler()
                .resolveListPlan(mainTableName, selectFields, joins, aggregateChildren);
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

        DynamicCrudJoinQueryCompiler.QueryPlan joinedSql = joinQueryCompiler()
                .resolveListPlan(mainTableName, selectFields, joins, aggregateChildren);
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
    private void appendSearchConditions(StringBuilder whereClause, MapSqlParameterSource params,
                                        Map<String, Object> searchParams,
                                        Set<String> allowedSearchFields,
                                        Map<String, String> searchTypeMap,
                                        Map<String, String> columnMapping) {
        queryConditionCompiler().appendSearchConditions(
                whereClause, params, searchParams, allowedSearchFields, searchTypeMap, columnMapping);
    }

    private void appendCustomConditions(StringBuilder whereClause, MapSqlParameterSource params,
                                        List<CustomQueryConditionDTO> conditions,
                                        Set<String> allowedFields,
                                        Map<String, String> columnMapping) {
        queryConditionCompiler().appendCustomConditions(
                whereClause, params, conditions, allowedFields, columnMapping);
    }

    private String buildCustomSelectClause(List<String> selectedFields, Set<String> allowedFields,
                                           Map<String, String> columnMapping) {
        return queryConditionCompiler().buildCustomSelectClause(selectedFields, allowedFields, columnMapping);
    }

    private DynamicCrudQueryConditionCompiler queryConditionCompiler() {
        return new DynamicCrudQueryConditionCompiler(
                this::validateIdentifier, this::primaryKeyField, this::primaryKeyColumn);
    }

    private StringBuilder buildBaseWhereClause(String tableName) {
        return sqlSupport().buildBaseWhereClause(tableName, null);
    }

    private StringBuilder buildBaseWhereClause(String tableName, String tableAlias) {
        return sqlSupport().buildBaseWhereClause(tableName, tableAlias);
    }

    MapSqlParameterSource buildBaseQueryParams() {
        return sqlSupport().buildBaseQueryParams(null);
    }

    private MapSqlParameterSource buildBaseQueryParams(String tableAlias) {
        return sqlSupport().buildBaseQueryParams(tableAlias);
    }

    private String buildPageDataSql(String tableName, StringBuilder whereClause, String orderBy, int pageNum, int pageSize) {
        return sqlSupport().buildPageDataSql(tableName, whereClause, orderBy, pageNum, pageSize);
    }

    String buildOrderByClause(String orderBy) {
        return sqlSupport().buildOrderByClause(orderBy);
    }

    StringBuilder buildIdWhereClause(String tableName, String primaryKeyColumn) {
        return sqlSupport().buildIdWhereClause(tableName, primaryKeyColumn);
    }

    MapSqlParameterSource buildIdQueryParams(Object id) {
        return sqlSupport().buildIdQueryParams(id);
    }

    void appendBaseQueryConditions(StringBuilder whereClause, MapSqlParameterSource params, String tableName) {
        sqlSupport().appendBaseQueryConditions(whereClause, params, tableName, null);
    }

    /**
     * 根据ID查询
     */
    public Map<String, Object> selectById(String tableName, Object id) {
        return recordQueryExecutor().selectById(tableName, primaryKeyColumn(), id, null);
    }

    public Map<String, Object> selectById(String tableName, Object id, SqlCondition dataScopeCondition) {
        return recordQueryExecutor().selectById(tableName, primaryKeyColumn(), id, dataScopeCondition);
    }

    public Map<String, Object> selectById(String tableName,
                                          String primaryKeyColumn,
                                          Object id,
                                          SqlCondition dataScopeCondition) {
        return recordQueryExecutor().selectById(tableName, primaryKeyColumn, id, dataScopeCondition);
    }

    /**
     * 批量查询：IN (:ids) 一次取回多条记录
     */
    public List<Map<String, Object>> selectByIds(String tableName,
                                                  String primaryKeyColumn,
                                                  List<?> ids,
                                                  SqlCondition dataScopeCondition) {
        return recordQueryExecutor().selectByIds(tableName, primaryKeyColumn, ids, dataScopeCondition);
    }

    /** 事务命令状态门禁使用的行锁读取，条件与普通详情查询完全一致。 */
    public Map<String, Object> selectByIdForUpdate(String tableName,
                                                   String primaryKeyColumn,
                                                   Object id,
                                                   SqlCondition dataScopeCondition) {
        return recordQueryExecutor().selectByIdForUpdate(tableName, primaryKeyColumn, id, dataScopeCondition);
    }

    public List<Map<String, Object>> selectListByColumn(String tableName, String columnName, Object value) {
        return recordQueryExecutor().selectListByColumn(tableName, columnName, value);
    }

    public List<Map<String, Object>> selectListByColumnIn(String tableName,
                                                          String columnName,
                                                          Collection<?> values) {
        return recordQueryExecutor().selectListByColumnIn(tableName, columnName, values, null);
    }

    public List<Map<String, Object>> selectListByColumnIn(String tableName,
                                                          String columnName,
                                                          Collection<?> values,
                                                          SqlCondition dataScopeCondition) {
        return recordQueryExecutor().selectListByColumnIn(
                tableName, columnName, values, dataScopeCondition);
    }

    public List<Map<String, Object>> selectTreeChildren(String tableName,
                                                        String parentColumn,
                                                        Object parentValue,
                                                        String orderBy,
                                                        int limit) {
        return recordQueryExecutor().selectTreeChildren(
                tableName, parentColumn, parentValue, orderBy, limit, null);
    }

    public List<Map<String, Object>> selectTreeChildren(String tableName,
                                                        String parentColumn,
                                                        Object parentValue,
                                                        String orderBy,
                                                        int limit,
                                                        SqlCondition dataScopeCondition) {
        return recordQueryExecutor().selectTreeChildren(
                tableName, parentColumn, parentValue, orderBy, limit, dataScopeCondition);
    }

    public boolean existsByColumn(String tableName, String columnName, Object value) {
        return recordQueryExecutor().existsByColumn(tableName, columnName, value, null);
    }

    public boolean existsByColumn(String tableName, String columnName, Object value, SqlCondition dataScopeCondition) {
        return recordQueryExecutor().existsByColumn(tableName, columnName, value, dataScopeCondition);
    }

    public boolean existsByColumns(String tableName,
                                   Map<String, Object> columnValues,
                                   Object excludeId) {
        return recordQueryExecutor().existsByColumns(
                tableName, columnValues, primaryKeyColumn(), excludeId, null);
    }

    public boolean existsByColumns(String tableName,
                                   Map<String, Object> columnValues,
                                   Object excludeId,
                                   SqlCondition dataScopeCondition) {
        return recordQueryExecutor().existsByColumns(
                tableName, columnValues, primaryKeyColumn(), excludeId, dataScopeCondition);
    }

    public boolean existsByColumns(String tableName,
                                   Map<String, Object> columnValues,
                                   String primaryKeyColumn,
                                   Object excludeId,
                                   SqlCondition dataScopeCondition) {
        return recordQueryExecutor().existsByColumns(
                tableName, columnValues, primaryKeyColumn, excludeId, dataScopeCondition);
    }

    private DynamicCrudRecordQueryExecutor recordQueryExecutor() {
        return new DynamicCrudRecordQueryExecutor(this);
    }

    // ==================== 新增操作 ====================

    /**
     * 新增记录
     */
    public int insert(String tableName, Map<String, Object> data) {
        return mutationExecutor().insert(tableName, data);
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
        return mutationExecutor().insertReturningKey(
                tableName, data, primaryKeyColumn, autoIncrement);
    }

    // ==================== 更新操作 ====================

    /**
     * 根据ID更新
     */
    public int updateById(String tableName, Object id, Map<String, Object> data) {
        return mutationExecutor().updateById(tableName, primaryKeyColumn(), id, data, null);
    }

    public int updateById(String tableName, Object id, Map<String, Object> data, SqlCondition dataScopeCondition) {
        return mutationExecutor().updateById(
                tableName, primaryKeyColumn(), id, data, dataScopeCondition);
    }

    public int updateById(String tableName,
                          String primaryKeyColumn,
                          Object id,
                          Map<String, Object> data,
                          SqlCondition dataScopeCondition) {
        return mutationExecutor().updateById(
                tableName, primaryKeyColumn, id, data, dataScopeCondition);
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
        return mutationExecutor().adjustNumbersById(
                tableName, primaryKeyColumn, id, deltas, minimums, maximums, condition);
    }

    public Long selectFirstIdByColumn(String tableName, String columnName, Object value) {
        return recordQueryExecutor().selectFirstIdByColumn(tableName, columnName, value);
    }

    // ==================== 删除操作 ====================

    /**
     * 根据ID删除
     */
    public int deleteById(String tableName, Object id, boolean logicDelete) {
        return mutationExecutor().deleteById(
                tableName, primaryKeyColumn(), id, logicDelete, null);
    }

    public int deleteById(String tableName, Object id, boolean logicDelete, SqlCondition dataScopeCondition) {
        return mutationExecutor().deleteById(
                tableName, primaryKeyColumn(), id, logicDelete, dataScopeCondition);
    }

    public int deleteById(String tableName,
                          String primaryKeyColumn,
                          Object id,
                          boolean logicDelete,
                          SqlCondition dataScopeCondition) {
        return mutationExecutor().deleteById(
                tableName, primaryKeyColumn, id, logicDelete, dataScopeCondition);
    }

    /**
     * 批量删除（IN 子句），一条 SQL 处理多条记录
     */
    public int deleteByIds(String tableName,
                           String primaryKeyColumn,
                           List<?> ids,
                           boolean logicDelete,
                           SqlCondition dataScopeCondition) {
        return mutationExecutor().deleteByIds(
                tableName, primaryKeyColumn, ids, logicDelete, dataScopeCondition, null);
    }

    /**
     * @param beforeById 可选：已查询的删除前快照（key 为规范化 recordId），有则跳过逐条 FOR UPDATE 再读
     */
    public int deleteByIds(String tableName,
                           String primaryKeyColumn,
                           List<?> ids,
                           boolean logicDelete,
                           SqlCondition dataScopeCondition,
                           Map<String, Map<String, Object>> beforeById) {
        return mutationExecutor().deleteByIds(
                tableName, primaryKeyColumn, ids, logicDelete, dataScopeCondition, beforeById);
    }

    public int deleteByColumn(String tableName, String columnName, Object value, boolean logicDelete) {
        return mutationExecutor().deleteByColumn(tableName, columnName, value, logicDelete);
    }

    private DynamicCrudMutationExecutor mutationExecutor() {
        return new DynamicCrudMutationExecutor(this);
    }

    // ==================== 工具方法 ====================

    /**
     * 检查表是否存在
     */
    public boolean tableExists(String tableName) {
        return tableMetadataGateway().tableExists(tableName);
    }

    /**
     * 检查表是否有del_flag列（带缓存）
     */
    public boolean hasDelFlag(String tableName) {
        DynamicCrudWritePolicy policy = writePolicy();
        return tableMetadataGateway().hasLogicDeleteColumn(
                tableName, policy.logicDeleteColumn(), policy.logicDeleteEnabled(),
                () -> getTableColumns(tableName));
    }

    /**
     * 获取表的所有列名（带缓存）
     */
    public Set<String> getTableColumns(String tableName) {
        return tableMetadataGateway().columns(tableName);
    }

    /**
     * 获取表的字段映射（camelCase -> snake_case）
     */
    public Map<String, String> getColumnMapping(String tableName) {
        return tableMetadataGateway().columnMapping(tableName, () -> getTableColumns(tableName));
    }

    Object normalizeColumnQueryValue(String tableName, String columnName, Object value) {
        return tableMetadataGateway().normalizeQueryValue(tableName, columnName, value);
    }

    void captureColumnDelete(String tableName, String columnName, Object value) {
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

    /**
     * DDL 执行后清理动态表结构缓存，避免追加字段后运行时继续使用旧列集合。
     */
    public void clearTableMetadataCache(String tableName) {
        tableMetadataGateway().clear(tableName);
    }

    public void clearTableMetadataCache(LowcodeRuntimeDataSourceContext context, String tableName) {
        tableMetadataGateway().clear(context, tableName);
    }

    private DynamicCrudTableMetadataGateway tableMetadataGateway() {
        DynamicCrudTableMetadataGateway gateway = tableMetadataGateway;
        if (gateway == null) {
            synchronized (this) {
                gateway = tableMetadataGateway;
                if (gateway == null) {
                    gateway = new DynamicCrudTableMetadataGateway(jdbcTemplateProvider, dialectFactory);
                    tableMetadataGateway = gateway;
                }
            }
        }
        return gateway;
    }

    /**
     * 校验表名
     */
    void validateTableName(String tableName) {
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

    String buildSelectSql(String selectClause, String tableName, StringBuilder whereClause) {
        return sqlSupport().buildSelectSql(selectClause, tableName, whereClause);
    }

    private String buildWhereSql(StringBuilder whereClause) {
        return sqlSupport().buildWhereSql(whereClause);
    }

    private String buildJoinSelectClause(List<JoinField> fields) {
        return joinQueryCompiler().buildSelectClause(fields);
    }

    private String buildJoinSelectClause(List<JoinField> fields, boolean distinct) {
        return joinQueryCompiler().buildSelectClause(fields, distinct);
    }

    private void validateJoinQuery(String mainTableName, List<JoinField> selectFields, List<JoinSpec> joins) {
        joinQueryCompiler().validate(mainTableName, selectFields, joins);
    }

    private DynamicCrudJoinQueryCompiler joinQueryCompiler() {
        return DynamicCrudJoinQueryCompiler.create(this, dialectFactory);
    }

    private String qualifyPrimaryKey(String tableAlias) {
        return sqlSupport().qualifyColumn(tableAlias, primaryKeyColumn());
    }

    String primaryKeyColumn() {
        return sqlSupport().primaryKeyColumn();
    }

    private String primaryKeyField() {
        return sqlSupport().primaryKeyField();
    }

    private String paginateSql(String sql, int pageNum, int pageSize) {
        return sqlSupport().paginate(sql, pageNum, pageSize);
    }

    String limitSql(String sql, int limit) {
        return sqlSupport().limit(sql, limit);
    }

    void appendWhereCondition(StringBuilder whereClause, String condition) {
        sqlSupport().appendWhereCondition(whereClause, condition);
    }

    void appendSqlCondition(StringBuilder whereClause, MapSqlParameterSource params, SqlCondition condition) {
        sqlSupport().appendSqlCondition(whereClause, params, condition);
    }

    String appendTenantCondition(String sql, MapSqlParameterSource params, String tableName) {
        return sqlSupport().appendTenantCondition(sql, params, tableName);
    }

    String appendLogicActiveCondition(String sql, MapSqlParameterSource params, String tableName) {
        return sqlSupport().appendLogicActiveCondition(sql, params, tableName);
    }

    String appendSqlCondition(String sql, MapSqlParameterSource params, SqlCondition condition) {
        return sqlSupport().appendSqlCondition(sql, params, condition);
    }

    String buildInsertSql(String tableName, Map<String, Object> data) {
        return sqlSupport().buildInsertSql(tableName, data);
    }

    String buildUpdateSql(String tableName, Map<String, Object> data, String primaryKeyColumn) {
        return sqlSupport().buildUpdateSql(tableName, data, primaryKeyColumn);
    }

    String buildDeleteSql(String tableName, boolean logicDelete, String primaryKeyColumn) {
        return sqlSupport().buildDeleteSql(tableName, logicDelete, primaryKeyColumn, false);
    }

    String buildBatchDeleteSql(String tableName, boolean logicDelete, String primaryKeyColumn) {
        return sqlSupport().buildDeleteSql(tableName, logicDelete, primaryKeyColumn, true);
    }

    String logicDeleteSetClause(String tableName) {
        return writePolicy().logicDeleteSetClause(getTableColumns(tableName));
    }

    MapSqlParameterSource toSqlParams(Map<String, Object> data) {
        return sqlSupport().toSqlParams(data);
    }

    MapSqlParameterSource toSqlParams(Map<String, Object> data, Object id) {
        return sqlSupport().toSqlParams(data, id);
    }

    MapSqlParameterSource toIdParam(Object id) {
        return sqlSupport().toIdParam(id);
    }

    private void logDynamicSql(String scene, String sql, MapSqlParameterSource params) {
        log.info("[DynamicCrudRepository] {} SQL: {}", scene, sql);
        log.info("[DynamicCrudRepository] {} 参数: {}", scene, params == null ? Map.of() : params.getValues());
    }

    Map<String, Object> prepareInsertData(String tableName, Map<String, Object> data) {
        return writePolicy().prepareInsert(data, getTableColumns(tableName));
    }

    Map<String, Object> prepareUpdateData(String tableName, Map<String, Object> data, String primaryKeyColumn) {
        return writePolicy().prepareUpdate(data, getTableColumns(tableName), primaryKeyColumn);
    }

    Object logicDeletedValue() {
        return writePolicy().logicDeletedValue();
    }

    DynamicCrudWritePolicy writePolicy() {
        return new DynamicCrudWritePolicy(this::validateIdentifier);
    }

    private DynamicCrudSqlSupport sqlSupport() {
        return new DynamicCrudSqlSupport(this, dialectFactory);
    }
}
