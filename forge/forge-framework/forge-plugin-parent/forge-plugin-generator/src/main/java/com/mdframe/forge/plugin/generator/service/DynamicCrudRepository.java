package com.mdframe.forge.plugin.generator.service;

import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.starter.core.exception.BusinessException;
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
        
        // 构建WHERE子句和参数
        StringBuilder whereClause = new StringBuilder();
        MapSqlParameterSource params = new MapSqlParameterSource();
        
        // 租户隔离
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            whereClause.append("tenant_id = :tenantId");
            params.addValue("tenantId", tenantId);
        }
        
        // 逻辑删除过滤：如果表有 del_flag 列，只查未删除的数据
        if (hasDelFlag(tableName)) {
            if (whereClause.length() > 0) {
                whereClause.append(" AND ");
            }
            whereClause.append("del_flag = '0'");
        }
        
        // 构建搜索条件
        if (searchParams != null && !searchParams.isEmpty()) {
            for (Map.Entry<String, Object> entry : searchParams.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                
                // 检查是否允许搜索
                if (!allowedSearchFields.contains(fieldName)) {
                    continue;
                }
                
                // 跳过空值
                if (value == null || (value instanceof String && StringUtils.isBlank((String) value))) {
                    continue;
                }
                
                // 转换为snake_case列名
                String columnName = columnMapping.getOrDefault(fieldName, DynamicQueryGenerator.camelToSnake(fieldName));
                
                // SQL注入检测
                if (DynamicQueryGenerator.containsSqlInjection(columnName)) {
                    log.warn("[DynamicCrudRepository] 检测到SQL注入尝试, fieldName={}", fieldName);
                    continue;
                }
                
                // 获取搜索类型
                String searchType = searchTypeMap.getOrDefault(fieldName, "eq");
                
                // 添加查询条件
                if (whereClause.length() > 0) {
                    whereClause.append(" AND ");
                }
                
                addSearchCondition(whereClause, params, columnName, searchType, value);
            }
        }
        
        // 查询总数
        String countSql = "SELECT COUNT(*) FROM " + tableName;
        if (whereClause.length() > 0) {
            countSql += " WHERE " + whereClause;
        }
        Long total = namedJdbcTemplate.queryForObject(countSql, params, Long.class);
        
        // 查询数据
        String dataSql = "SELECT * FROM " + tableName;
        if (whereClause.length() > 0) {
            dataSql += " WHERE " + whereClause;
        }
        if (StringUtils.isNotBlank(orderBy)) {
            dataSql += " ORDER BY " + orderBy;
        } else {
            dataSql += " ORDER BY id DESC";
        }
        dataSql += " LIMIT :limit OFFSET :offset";
        
        params.addValue("limit", pageSize);
        params.addValue("offset", (pageNum - 1) * pageSize);
        
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
                whereClause.append(columnName).append(" LIKE :").append(paramName);
                params.addValue(paramName, "%" + value + "%");
                break;
            case "left_like":
                whereClause.append(columnName).append(" LIKE :").append(paramName);
                params.addValue(paramName, "%" + value);
                break;
            case "right_like":
                whereClause.append(columnName).append(" LIKE :").append(paramName);
                params.addValue(paramName, value + "%");
                break;
            case "eq":
                whereClause.append(columnName).append(" = :").append(paramName);
                params.addValue(paramName, value);
                break;
            case "ne":
                whereClause.append(columnName).append(" != :").append(paramName);
                params.addValue(paramName, value);
                break;
            case "gt":
                whereClause.append(columnName).append(" > :").append(paramName);
                params.addValue(paramName, value);
                break;
            case "ge":
            case "gte":
                whereClause.append(columnName).append(" >= :").append(paramName);
                params.addValue(paramName, value);
                break;
            case "lt":
                whereClause.append(columnName).append(" < :").append(paramName);
                params.addValue(paramName, value);
                break;
            case "le":
            case "lte":
                whereClause.append(columnName).append(" <= :").append(paramName);
                params.addValue(paramName, value);
                break;
            case "in":
                if (value instanceof List) {
                    List<?> values = (List<?>) value;
                    whereClause.append(columnName).append(" IN (:").append(paramName).append(")");
                    params.addValue(paramName, values);
                } else if (value instanceof String) {
                    List<String> values = Arrays.asList(((String) value).split(","));
                    whereClause.append(columnName).append(" IN (:").append(paramName).append(")");
                    params.addValue(paramName, values);
                } else {
                    whereClause.append(columnName).append(" = :").append(paramName);
                    params.addValue(paramName, value);
                }
                break;
            case "between":
                if (value instanceof List) {
                    List<?> range = (List<?>) value;
                    if (range.size() >= 2) {
                        whereClause.append(columnName).append(" BETWEEN :").append(paramName).append("_start AND :").append(paramName).append("_end");
                        params.addValue(paramName + "_start", range.get(0));
                        params.addValue(paramName + "_end", range.get(1));
                    }
                }
                break;
            case "is_null":
                whereClause.append(columnName).append(" IS NULL");
                break;
            case "is_not_null":
                whereClause.append(columnName).append(" IS NOT NULL");
                break;
            default:
                whereClause.append(columnName).append(" = :").append(paramName);
                params.addValue(paramName, value);
                break;
        }
    }

    /**
     * 根据ID查询
     */
    public Map<String, Object> selectById(String tableName, Long id) {
        validateTableName(tableName);
        
        String sql = "SELECT * FROM " + tableName + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        
        // 添加租户条件
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            sql += " AND tenant_id = :tenantId";
            params.addValue("tenantId", tenantId);
        }
        
        // 逻辑删除过滤
        if (hasDelFlag(tableName)) {
            sql += " AND del_flag = '0'";
        }
        
        List<Map<String, Object>> results = namedJdbcTemplate.queryForList(sql, params);
        return results.isEmpty() ? null : results.get(0);
    }

    // ==================== 新增操作 ====================

    /**
     * 新增记录
     */
    public int insert(String tableName, Map<String, Object> data) {
        validateTableName(tableName);
        
        if (data == null || data.isEmpty()) {
            throw new BusinessException("没有可写入的字段");
        }
        
        // 自动填充审计字段
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            data.put("tenant_id", tenantId);
        }
        data.put("create_time", new Date());
        data.put("update_time", new Date());
        
        String columns = String.join(", ", data.keySet());
        String placeholders = data.keySet().stream()
                .map(col -> ":" + col)
                .collect(Collectors.joining(", "));
        
        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
        
        MapSqlParameterSource params = new MapSqlParameterSource();
        data.forEach(params::addValue);
        
        return namedJdbcTemplate.update(sql, params);
    }

    // ==================== 更新操作 ====================

    /**
     * 根据ID更新
     */
    public int updateById(String tableName, Long id, Map<String, Object> data) {
        validateTableName(tableName);
        
        if (data == null || data.isEmpty()) {
            throw new BusinessException("没有可更新的字段");
        }
        
        // 移除不可更新字段
        data.remove("id");
        data.remove("tenant_id");
        
        // 自动填充更新时间
        data.put("update_time", new Date());
        
        String setClauses = data.entrySet().stream()
                .map(entry -> entry.getKey() + " = :" + entry.getKey())
                .collect(Collectors.joining(", "));
        
        String sql = "UPDATE " + tableName + " SET " + setClauses + " WHERE id = :id";
        
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        data.forEach(params::addValue);
        
        // 添加租户条件
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            sql += " AND tenant_id = :tenantId";
            params.addValue("tenantId", tenantId);
        }
        
        return namedJdbcTemplate.update(sql, params);
    }

    // ==================== 删除操作 ====================

    /**
     * 根据ID删除
     */
    public int deleteById(String tableName, Long id, boolean logicDelete) {
        validateTableName(tableName);
        
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        
        String sql;
        if (logicDelete) {
            sql = "UPDATE " + tableName + " SET del_flag = '1', update_time = NOW() WHERE id = :id";
        } else {
            sql = "DELETE FROM " + tableName + " WHERE id = :id";
        }
        
        // 添加租户条件
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            sql += " AND tenant_id = :tenantId";
            params.addValue("tenantId", tenantId);
        }
        
        return namedJdbcTemplate.update(sql, params);
    }

    // ==================== 工具方法 ====================

    /**
     * 检查表是否存在
     */
    public boolean tableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = :tableName AND table_schema = (SELECT DATABASE())";
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("tableName", tableName);
            Integer count = namedJdbcTemplate.queryForObject(sql, params, Integer.class);
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
                String sql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = :tableName AND column_name = 'del_flag' AND table_schema = (SELECT DATABASE())";
                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("tableName", key);
                Integer count = namedJdbcTemplate.queryForObject(sql, params, Integer.class);
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
                String sql = "SELECT column_name FROM information_schema.columns WHERE table_name = :tableName AND table_schema = (SELECT DATABASE())";
                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("tableName", key);
                List<String> columns = namedJdbcTemplate.queryForList(sql, params, String.class);
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
}
