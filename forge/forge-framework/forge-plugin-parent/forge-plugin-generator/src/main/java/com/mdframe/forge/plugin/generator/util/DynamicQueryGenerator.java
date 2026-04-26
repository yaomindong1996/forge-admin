package com.mdframe.forge.plugin.generator.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 动态表查询条件生成器
 * 参考JeecgBoot的QueryGenerator实现，支持多种查询规则
 * 
 * @author forge
 */
@Slf4j
public class DynamicQueryGenerator {

    /** 区间查询后缀 - 开始 */
    private static final String BEGIN = "_begin";
    /** 区间查询后缀 - 结束 */
    private static final String END = "_end";
    /** 多值查询后缀 */
    private static final String MULTI = "_MultiString";
    /** 通配符 */
    private static final String STAR = "*";
    /** 逗号 */
    private static final String COMMA = ",";
    /** 不等于前缀 */
    private static final String NOT_EQUAL = "!";
    /** 查询分隔符 */
    private static final String QUERY_SEPARATE_KEYWORD = " ";

    /** SQL注入检测正则 */
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        ".*(?:insert|update|delete|drop|truncate|exec|execute|union|select|into|from|where|and|or|--|;|'|\"|\\\\).*",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 查询规则枚举
     */
    public enum QueryRuleEnum {
        EQ("=", "等于"),
        NE("!=", "不等于"),
        GT(">", "大于"),
        GE(">=", "大于等于"),
        LT("<", "小于"),
        LE("<=", "小于等于"),
        LIKE("LIKE", "模糊匹配"),
        LEFT_LIKE("LEFT_LIKE", "左模糊"),
        RIGHT_LIKE("RIGHT_LIKE", "右模糊"),
        IN("IN", "包含"),
        BETWEEN("BETWEEN", "区间"),
        IS_NULL("IS_NULL", "为空"),
        IS_NOT_NULL("IS_NOT_NULL", "不为空");

        private final String value;
        private final String condition;

        QueryRuleEnum(String value, String condition) {
            this.value = value;
            this.condition = condition;
        }

        public String getValue() {
            return value;
        }

        public String getCondition() {
            return condition;
        }

        public static QueryRuleEnum getByValue(String value) {
            for (QueryRuleEnum rule : values()) {
                if (rule.getValue().equals(value)) {
                    return rule;
                }
            }
            return null;
        }
    }

    /**
     * 根据搜索参数构建QueryWrapper
     * 
     * @param searchParams 搜索参数（camelCase字段名 -> 值）
     * @param allowedFields 允许搜索的字段集合
     * @param searchTypeMap 搜索类型映射（字段名 -> 搜索类型）
     * @param columnMapping 字段映射（camelCase -> snake_case）
     * @return QueryWrapper
     */
    public static QueryWrapper<Map<String, Object>> buildQueryWrapper(
            Map<String, Object> searchParams,
            Set<String> allowedFields,
            Map<String, String> searchTypeMap,
            Map<String, String> columnMapping) {
        
        QueryWrapper<Map<String, Object>> queryWrapper = new QueryWrapper<>();
        
        if (searchParams == null || searchParams.isEmpty()) {
            return queryWrapper;
        }

        for (Map.Entry<String, Object> entry : searchParams.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            // 检查是否允许搜索
            if (!allowedFields.contains(fieldName)) {
                continue;
            }

            // 跳过空值
            if (value == null || (value instanceof String && StringUtils.isBlank((String) value))) {
                continue;
            }

            // 转换为snake_case列名
            String columnName = columnMapping.getOrDefault(fieldName, camelToSnake(fieldName));
            
            // SQL注入检测
            if (containsSqlInjection(columnName)) {
                log.warn("[DynamicQueryGenerator] 检测到SQL注入尝试, fieldName={}", fieldName);
                continue;
            }

            // 获取搜索类型
            String searchType = searchTypeMap.getOrDefault(fieldName, "eq");
            
            // 构建查询条件
            addQueryCondition(queryWrapper, columnName, searchType, value);
        }

        return queryWrapper;
    }

    /**
     * 添加查询条件
     */
    private static void addQueryCondition(QueryWrapper<Map<String, Object>> queryWrapper, 
                                           String columnName, String searchType, Object value) {
        switch (searchType.toLowerCase()) {
            case "like":
                queryWrapper.like(columnName, value);
                break;
            case "left_like":
                queryWrapper.likeLeft(columnName, value);
                break;
            case "right_like":
                queryWrapper.likeRight(columnName, value);
                break;
            case "eq":
                queryWrapper.eq(columnName, value);
                break;
            case "ne":
                queryWrapper.ne(columnName, value);
                break;
            case "gt":
                queryWrapper.gt(columnName, value);
                break;
            case "ge":
            case "gte":
                queryWrapper.ge(columnName, value);
                break;
            case "lt":
                queryWrapper.lt(columnName, value);
                break;
            case "le":
            case "lte":
                queryWrapper.le(columnName, value);
                break;
            case "in":
                if (value instanceof List) {
                    queryWrapper.in(columnName, (List<?>) value);
                } else if (value instanceof String) {
                    queryWrapper.in(columnName, Arrays.asList(((String) value).split(COMMA)));
                } else {
                    queryWrapper.in(columnName, value);
                }
                break;
            case "between":
                if (value instanceof List) {
                    List<?> range = (List<?>) value;
                    if (range.size() >= 2) {
                        queryWrapper.between(columnName, range.get(0), range.get(1));
                    }
                }
                break;
            case "is_null":
                queryWrapper.isNull(columnName);
                break;
            case "is_not_null":
                queryWrapper.isNotNull(columnName);
                break;
            default:
                // 默认精确匹配
                queryWrapper.eq(columnName, value);
                break;
        }
    }

    /**
     * 构建排序子句
     * 
     * @param orderByColumn 排序字段（camelCase）
     * @param isAsc 是否升序
     * @param columnMapping 字段映射
     * @return ORDER BY子句（不含ORDER BY关键字）
     */
    public static String buildOrderByClause(String orderByColumn, String isAsc, Map<String, String> columnMapping) {
        if (StringUtils.isBlank(orderByColumn)) {
            return "id DESC";
        }

        // 多字段排序支持：column1,column2
        String[] columns = orderByColumn.split(COMMA);
        List<String> orderClauses = new ArrayList<>();

        for (String col : columns) {
            String trimmedCol = col.trim();
            if (StringUtils.isBlank(trimmedCol)) {
                continue;
            }

            // 转换为snake_case
            String columnName = columnMapping.getOrDefault(trimmedCol, camelToSnake(trimmedCol));
            
            // SQL注入检测
            if (containsSqlInjection(columnName)) {
                log.warn("[DynamicQueryGenerator] 检测到排序字段SQL注入, column={}", trimmedCol);
                continue;
            }

            orderClauses.add(columnName);
        }

        if (orderClauses.isEmpty()) {
            return "id DESC";
        }

        String direction = "asc".equalsIgnoreCase(isAsc) ? "ASC" : "DESC";
        return String.join(", ", orderClauses) + " " + direction;
    }

    /**
     * 根据值自动推断查询规则（参考JeecgBoot的convert2Rule）
     * 
     * @param value 查询值
     * @return 查询规则
     */
    public static QueryRuleEnum inferQueryRule(Object value) {
        if (value == null) {
            return QueryRuleEnum.EQ;
        }

        String val = value.toString().trim();
        if (val.isEmpty()) {
            return QueryRuleEnum.EQ;
        }

        // 1. 检查 >= <= > <
        if (val.length() >= 3 && QUERY_SEPARATE_KEYWORD.equals(val.substring(2, 3))) {
            QueryRuleEnum rule = QueryRuleEnum.getByValue(val.substring(0, 2));
            if (rule != null) {
                return rule;
            }
        }
        if (val.length() >= 2 && QUERY_SEPARATE_KEYWORD.equals(val.substring(1, 2))) {
            QueryRuleEnum rule = QueryRuleEnum.getByValue(val.substring(0, 1));
            if (rule != null) {
                return rule;
            }
        }

        // 2. 检查模糊查询 *
        if (val.equals(STAR)) {
            return QueryRuleEnum.EQ;
        }
        if (val.contains(STAR)) {
            if (val.startsWith(STAR) && val.endsWith(STAR)) {
                return QueryRuleEnum.LIKE;
            } else if (val.startsWith(STAR)) {
                return QueryRuleEnum.LEFT_LIKE;
            } else if (val.endsWith(STAR)) {
                return QueryRuleEnum.RIGHT_LIKE;
            }
        }

        // 3. 检查IN查询（逗号分隔）
        if (val.contains(COMMA)) {
            return QueryRuleEnum.IN;
        }

        // 4. 检查不等于 !
        if (val.startsWith(NOT_EQUAL)) {
            return QueryRuleEnum.NE;
        }

        return QueryRuleEnum.EQ;
    }

    /**
     * 清理值中的规则前缀
     * 
     * @param rule 查询规则
     * @param value 原始值
     * @return 清理后的值
     */
    public static Object cleanRuleValue(QueryRuleEnum rule, Object value) {
        if (rule == null || !(value instanceof String)) {
            return value;
        }

        String val = value.toString().trim();

        switch (rule) {
            case LIKE:
                // 移除前后 *
                return val.substring(1, val.length() - 1);
            case LEFT_LIKE:
            case NE:
                // 移除前缀
                return val.substring(1);
            case RIGHT_LIKE:
                // 移除后缀
                return val.substring(0, val.length() - 1);
            case IN:
                // 按逗号分割
                return val.split(COMMA);
            default:
                // 移除规则前缀（如 >= ）
                if (val.startsWith(rule.getValue())) {
                    return val.substring(rule.getValue().length());
                }
                return value;
        }
    }

    // ==================== 工具方法 ====================

    /**
     * snake_case 转 camelCase
     */
    public static String snakeToCamel(String snake) {
        if (StringUtils.isBlank(snake)) {
            return snake;
        }
        StringBuilder camel = new StringBuilder();
        boolean nextUpper = false;
        for (int i = 0; i < snake.length(); i++) {
            char c = snake.charAt(i);
            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    camel.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    camel.append(c);
                }
            }
        }
        return camel.toString();
    }

    /**
     * camelCase 转 snake_case
     */
    public static String camelToSnake(String camel) {
        if (StringUtils.isBlank(camel)) {
            return camel;
        }
        StringBuilder snake = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    snake.append('_');
                }
                snake.append(Character.toLowerCase(c));
            } else {
                snake.append(c);
            }
        }
        return snake.toString();
    }

    /**
     * 将Map的key从snake_case转换为camelCase，同时将 Boolean 值转换为 0/1
     * <p>
     * MySQL Connector/J 默认将 tinyint(1) 映射为 Boolean，
     * 此处统一转换为 Integer，避免前端收到 true/false。
     */
    public static Map<String, Object> convertMapToCamelCase(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            // tinyint(1) 被 JDBC 驱动映射为 Boolean，统一还原为 Integer(0/1)
            if (value instanceof Boolean boolVal) {
                value = boolVal ? 1 : 0;
            }
            result.put(snakeToCamel(entry.getKey()), value);
        }
        return result;
    }

    /**
     * 将List<Map>的key从snake_case转换为camelCase
     */
    public static List<Map<String, Object>> convertListToCamelCase(List<Map<String, Object>> list) {
        if (list == null) {
            return null;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> map : list) {
            result.add(convertMapToCamelCase(map));
        }
        return result;
    }

    /**
     * SQL注入检测
     */
    public static boolean containsSqlInjection(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        return SQL_INJECTION_PATTERN.matcher(value).matches();
    }

    // ==================== Schema解析方法 ====================

    /**
     * 解析Schema中的字段名
     * 
     * @param schemaJson Schema JSON字符串
     * @param objectMapper ObjectMapper实例
     * @return 字段名集合
     */
    public static Set<String> extractFieldNames(String schemaJson, ObjectMapper objectMapper) {
        if (StringUtils.isBlank(schemaJson)) {
            return Collections.emptySet();
        }
        try {
            JsonNode node = objectMapper.readTree(schemaJson);
            Set<String> fields = new HashSet<>();
            if (node.isArray()) {
                for (JsonNode item : node) {
                    // 支持 field、dataIndex、key 三种属性名
                    JsonNode fieldNode = item.get("field");
                    if (fieldNode != null && !fieldNode.isNull()) {
                        fields.add(fieldNode.asText());
                    }
                    JsonNode dataIndexNode = item.get("dataIndex");
                    if (dataIndexNode != null && !dataIndexNode.isNull()) {
                        fields.add(dataIndexNode.asText());
                    }
                    JsonNode keyNode = item.get("key");
                    if (keyNode != null && !keyNode.isNull()) {
                        fields.add(keyNode.asText());
                    }
                }
            }
            return fields;
        } catch (Exception e) {
            log.warn("[DynamicQueryGenerator] 解析schema字段名失败", e);
            return Collections.emptySet();
        }
    }

    /**
     * 解析搜索类型映射
     * 
     * @param schemaJson Schema JSON字符串
     * @param objectMapper ObjectMapper实例
     * @return 搜索类型映射（字段名 -> 搜索类型）
     */
    public static Map<String, String> extractSearchTypeMap(String schemaJson, ObjectMapper objectMapper) {
        Map<String, String> typeMap = new HashMap<>();
        if (StringUtils.isBlank(schemaJson)) {
            return typeMap;
        }
        try {
            JsonNode node = objectMapper.readTree(schemaJson);
            if (node.isArray()) {
                for (JsonNode item : node) {
                    JsonNode fieldNode = item.get("field");
                    if (fieldNode == null || fieldNode.isNull()) continue;
                    String field = fieldNode.asText();
                    
                    // 优先使用searchType字段
                    JsonNode searchTypeNode = item.get("searchType");
                    if (searchTypeNode != null && !searchTypeNode.isNull()) {
                        typeMap.put(field, searchTypeNode.asText());
                    } else {
                        // 根据type自动推断
                        String type = item.has("type") ? item.get("type").asText("") : "";
                        if ("daterange".equals(type)) {
                            typeMap.put(field, "between");
                        } else if ("select".equals(type)) {
                            JsonNode multipleNode = item.get("multiple");
                            if (multipleNode != null && multipleNode.asBoolean(false)) {
                                typeMap.put(field, "in");
                            } else {
                                typeMap.put(field, "eq");
                            }
                        } else if ("number".equals(type)) {
                            typeMap.put(field, "eq");
                        } else {
                            typeMap.put(field, "like");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicQueryGenerator] 解析searchTypeMap失败", e);
        }
        return typeMap;
    }
}
