package com.mdframe.forge.plugin.generator.service;

import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialect;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialectFactory;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeJdbcTemplateProvider;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.ConnectionCallback;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 动态表结构元数据网关。
 *
 * <p>使用 Cache-Aside 隔离数据源级表存在性、列、类型和字段映射读取，
 * DDL 完成后由调用方通过明确的失效入口清理缓存。</p>
 */
@Slf4j
@RequiredArgsConstructor
final class DynamicCrudTableMetadataGateway {

    private final RuntimeJdbcTemplateProvider jdbcTemplateProvider;
    private final RuntimeDatabaseDialectFactory dialectFactory;

    private final ConcurrentHashMap<String, Boolean> logicDeleteColumnCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> tableColumnsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, Integer>> columnTypesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, String>> columnMappingCache = new ConcurrentHashMap<>();

    boolean tableExists(String tableName) {
        try {
            LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
            RuntimeDatabaseDialect dialect = dialectFactory.resolve(context);
            Integer count = jdbcTemplateProvider.jdbcTemplate(context)
                .queryForObject(dialect.tableExistsSql(), Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("[DynamicCrudTableMetadataGateway] 检查表是否存在失败, tableName={}", tableName, e);
            return false;
        }
    }

    boolean hasLogicDeleteColumn(String tableName,
                                 String columnName,
                                 boolean enabled,
                                 Supplier<Set<String>> columnsSupplier) {
        String cacheKey = cacheKey(tableName) + ":logic:" + columnName + ":" + enabled;
        return logicDeleteColumnCache.computeIfAbsent(cacheKey, key -> {
            try {
                return enabled && columnsSupplier.get().contains(columnName);
            } catch (Exception e) {
                log.warn("[DynamicCrudTableMetadataGateway] 检查逻辑删除列失败, tableName={}", tableName, e);
                return false;
            }
        });
    }

    Set<String> columns(String tableName) {
        return tableColumnsCache.computeIfAbsent(cacheKey(tableName), key -> {
            try {
                LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
                RuntimeDatabaseDialect dialect = dialectFactory.resolve(context);
                List<String> columns = jdbcTemplateProvider.jdbcTemplate(context)
                    .queryForList(dialect.listColumnsSql(), String.class, tableName);
                return columns.stream()
                    .map(column -> StringUtils.defaultString(column).toLowerCase(Locale.ROOT))
                    .collect(Collectors.toCollection(HashSet::new));
            } catch (Exception e) {
                log.warn("[DynamicCrudTableMetadataGateway] 获取表列名失败, tableName={}", tableName, e);
                return Collections.emptySet();
            }
        });
    }

    Map<String, String> columnMapping(String tableName, Supplier<Set<String>> columnsSupplier) {
        return columnMappingCache.computeIfAbsent(cacheKey(tableName), key -> {
            Map<String, String> mapping = new HashMap<>();
            for (String column : columnsSupplier.get()) {
                mapping.put(DynamicQueryGenerator.snakeToCamel(column), column);
                mapping.put(column, column);
            }
            return mapping;
        });
    }

    Object normalizeQueryValue(String tableName, String columnName, Object value) {
        if (value == null) {
            return null;
        }
        Integer jdbcType = columnJdbcType(tableName, columnName);
        if (jdbcType == null) {
            // 禁止无元数据时猜类型（尤其是把 "0"/"1" 抬成 Long）
            return value;
        }
        if (isCharacterJdbcType(jdbcType)) {
            return String.valueOf(value);
        }
        if (isNumericJdbcType(jdbcType)) {
            return coerceNumericLiteral(value);
        }
        return value;
    }

    /**
     * 路径/查询主键：纯数字串在数值列（或尚无列元数据）时提升为 Long，避免 PG {@code bigint = varchar}。
     * 元数据明确为字符列时不抬升。
     */
    Object coercePrimaryKeyValue(String tableName, String columnName, Object value) {
        if (value == null) {
            return null;
        }
        Integer jdbcType = columnJdbcType(tableName, columnName);
        if (jdbcType != null && isCharacterJdbcType(jdbcType)) {
            return String.valueOf(value);
        }
        if (jdbcType == null || isNumericJdbcType(jdbcType)) {
            return coerceNumericLiteral(value);
        }
        return value;
    }

    /**
     * 逻辑删除字面量：无列元数据时保持策略原值（通常 {@code "0"}/{@code "1"}）；
     * 有元数据再按 JDBC 类型归一。
     */
    Object coerceLogicFlagLiteral(String tableName, String columnName, Object value) {
        if (value == null) {
            return null;
        }
        Integer jdbcType = columnJdbcType(tableName, columnName);
        if (jdbcType == null) {
            return value;
        }
        if (isCharacterJdbcType(jdbcType)) {
            return String.valueOf(value);
        }
        if (isNumericJdbcType(jdbcType)) {
            Object coerced = coerceNumericLiteral(value);
            return coerced != null ? coerced : value;
        }
        return value;
    }

    Integer columnJdbcType(String tableName, String columnName) {
        return columnTypes(tableName)
            .get(StringUtils.defaultString(columnName).toLowerCase(Locale.ROOT));
    }

    void clear(String tableName) {
        String suffix = ":" + tableName;
        logicDeleteColumnCache.keySet().removeIf(key -> key.contains(suffix + ":logic:"));
        tableColumnsCache.keySet().removeIf(key -> key.endsWith(suffix));
        columnTypesCache.keySet().removeIf(key -> key.endsWith(suffix));
        columnMappingCache.keySet().removeIf(key -> key.endsWith(suffix));
    }

    void clear(LowcodeRuntimeDataSourceContext context, String tableName) {
        String cacheKey = cacheKey(context, tableName);
        logicDeleteColumnCache.keySet().removeIf(key -> key.startsWith(cacheKey + ":logic:"));
        tableColumnsCache.remove(cacheKey);
        columnTypesCache.remove(cacheKey);
        columnMappingCache.remove(cacheKey);
    }

    private Map<String, Integer> columnTypes(String tableName) {
        return columnTypesCache.computeIfAbsent(cacheKey(tableName), key -> {
            try {
                LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
                return jdbcTemplateProvider.jdbcTemplate(context)
                    .execute((ConnectionCallback<Map<String, Integer>>) connection -> {
                        Map<String, Integer> types = new HashMap<>();
                        DatabaseMetaData metadata = connection.getMetaData();
                        // 禁止用 getCatalog() 过滤：PG apps 等非 public schema 会被扫丢
                        String schema = resolveConnectionSchema(connection);
                        String lookupName = StringUtils.defaultString(tableName);
                        readColumnTypes(metadata, schema, lookupName, types);
                        if (types.isEmpty() && !lookupName.equals(lookupName.toLowerCase(Locale.ROOT))) {
                            readColumnTypes(metadata, schema, lookupName.toLowerCase(Locale.ROOT), types);
                        }
                        return types;
                    });
            } catch (Exception e) {
                log.warn("[DynamicCrudTableMetadataGateway] 获取表字段类型失败, tableName={}", tableName, e);
                return Collections.emptyMap();
            }
        });
    }

    private static void readColumnTypes(DatabaseMetaData metadata,
                                        String schema,
                                        String tableName,
                                        Map<String, Integer> types) throws SQLException {
        try (ResultSet columns = metadata.getColumns(null, schema, tableName, null)) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                if (StringUtils.isNotBlank(columnName)) {
                    types.put(columnName.toLowerCase(Locale.ROOT), columns.getInt("DATA_TYPE"));
                }
            }
        }
    }

    private static String resolveConnectionSchema(java.sql.Connection connection) {
        try {
            String schema = connection.getSchema();
            return StringUtils.isNotBlank(schema) ? schema : null;
        } catch (Exception ignored) {
            return null;
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

    private boolean isNumericJdbcType(int jdbcType) {
        return jdbcType == Types.TINYINT
            || jdbcType == Types.SMALLINT
            || jdbcType == Types.INTEGER
            || jdbcType == Types.BIGINT
            || jdbcType == Types.NUMERIC
            || jdbcType == Types.DECIMAL
            || jdbcType == Types.FLOAT
            || jdbcType == Types.REAL
            || jdbcType == Types.DOUBLE;
    }

    private static Object coerceNumericLiteral(Object value) {
        if (value instanceof Number) {
            return value;
        }
        if (!(value instanceof String text)) {
            return value;
        }
        String trimmed = text.trim();
        if (!isPureDigits(trimmed)) {
            return value;
        }
        try {
            return Long.valueOf(trimmed);
        } catch (NumberFormatException ex) {
            return value;
        }
    }

    private static boolean isPureDigits(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        int start = text.charAt(0) == '-' ? 1 : 0;
        if (start >= text.length()) {
            return false;
        }
        for (int i = start; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String cacheKey(String tableName) {
        return cacheKey(LowcodeRuntimeDataSourceContextHolder.get(), tableName);
    }

    private String cacheKey(LowcodeRuntimeDataSourceContext context, String tableName) {
        String datasourceKey = context == null || context.isMaster()
            ? "master"
            : String.valueOf(context.getDatasourceId());
        return datasourceKey + ":" + tableName;
    }
}
