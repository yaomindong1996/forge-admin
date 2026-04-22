package com.mdframe.forge.plugin.generator.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategy;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategyFactory;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeType;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicCrudService {

    private final JdbcTemplate jdbcTemplate;
    private final AiCrudConfigService configService;
    private final ObjectMapper objectMapper;
    private final DictValueProvider dictValueProvider;
    private final DesensitizeStrategyFactory desensitizeStrategyFactory = new DesensitizeStrategyFactory();

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,63}$");

    public Page<Map<String, Object>> selectPage(String configKey, PageQuery pageQuery, DynamicCrudQuery query) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        validateTableName(tableName);

        Set<String> allowedSearchFields = extractFieldNames(config.getSearchSchema());
        Map<String, String> searchTypeMap = extractSearchTypeMap(config.getSearchSchema());

        List<Object> params = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder();

        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            whereClause.append("tenant_id = ?");
            params.add(tenantId);
        }

        if (query != null && query.getSearchParams() != null && !query.getSearchParams().isEmpty()) {
            for (Map.Entry<String, Object> entry : query.getSearchParams().entrySet()) {
                String fieldName = entry.getKey();
                if (!allowedSearchFields.contains(fieldName)) {
                    continue;
                }
                validateIdentifier(fieldName);
                Object value = entry.getValue();
                if (value == null || (value instanceof String && StringUtils.isBlank((String) value))) {
                    continue;
                }

                if (whereClause.length() > 0) {
                    whereClause.append(" AND ");
                }

                String searchType = searchTypeMap.getOrDefault(fieldName, "eq");
                switch (searchType) {
                    case "like":
                        whereClause.append(fieldName).append(" LIKE ?");
                        params.add("%" + value + "%");
                        break;
                    case "between":
                        if (value instanceof List) {
                            List<?> range = (List<?>) value;
                            if (range.size() >= 2) {
                                whereClause.append(fieldName).append(" BETWEEN ? AND ?");
                                params.add(range.get(0));
                                params.add(range.get(1));
                            }
                        } else {
                            whereClause.append(fieldName).append(" = ?");
                            params.add(value);
                        }
                        break;
                    case "in":
                        if (value instanceof List) {
                            List<?> values = (List<?>) value;
                            String placeholders = String.join(",", Collections.nCopies(values.size(), "?"));
                            whereClause.append(fieldName).append(" IN (").append(placeholders).append(")");
                            params.addAll(values);
                        } else {
                            whereClause.append(fieldName).append(" = ?");
                            params.add(value);
                        }
                        break;
                    default:
                        whereClause.append(fieldName).append(" = ?");
                        params.add(value);
                        break;
                }
            }
        }

        String baseSql;
        if (whereClause.length() > 0) {
            baseSql = "SELECT * FROM " + tableName + " WHERE " + whereClause;
        } else {
            baseSql = "SELECT * FROM " + tableName;
        }

        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") t";
        Long total;
        if (params.isEmpty()) {
            total = jdbcTemplate.queryForObject(countSql, Long.class);
        } else {
            total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        }

        String dataSql = baseSql + " ORDER BY id DESC LIMIT ? OFFSET ?";
        params.add(pageQuery.getPageSize());
        params.add((pageQuery.getPageNum() - 1) * pageQuery.getPageSize());

        List<Map<String, Object>> records = jdbcTemplate.queryForList(dataSql, params.toArray());

        // 应用脱敏和字典翻译
        applyDesensitize(records, config.getDesensitizeConfig());
        applyDictTranslation(records, config.getTransConfig());

        Page<Map<String, Object>> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize(), total != null ? total : 0);
        page.setRecords(records);
        return page;
    }

    public Map<String, Object> selectById(String configKey, Long id) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        validateTableName(tableName);

        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        List<Object> params = new ArrayList<>();
        params.add(id);

        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            sql += " AND tenant_id = ?";
            params.add(tenantId);
        }

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, params.toArray());
        if (!results.isEmpty()) {
            Map<String, Object> record = results.get(0);
            applyDesensitize(Collections.singletonList(record), config.getDesensitizeConfig());
            applyDictTranslation(Collections.singletonList(record), config.getTransConfig());
            return record;
        }
        return null;
    }

    public void insert(String configKey, Map<String, Object> data) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        validateTableName(tableName);

        Set<String> allowedFields = extractFieldNames(config.getEditSchema());
        Map<String, Object> filtered = filterFields(data, allowedFields);

        if (filtered.isEmpty()) {
            throw new BusinessException("没有可写入的字段");
        }

        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            filtered.put("tenant_id", tenantId);
        }
        filtered.put("create_time", new java.util.Date());
        filtered.put("update_time", new java.util.Date());

        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, Object> entry : filtered.entrySet()) {
            validateIdentifier(entry.getKey());
            columns.add(entry.getKey());
            placeholders.add("?");
            values.add(entry.getValue());
        }

        String sql = "INSERT INTO " + tableName + " (" + String.join(", ", columns) + ") VALUES (" + String.join(", ", placeholders) + ")";
        jdbcTemplate.update(sql, values.toArray());
    }

    public void updateById(String configKey, Map<String, Object> data) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        validateTableName(tableName);

        Set<String> allowedFields = extractFieldNames(config.getEditSchema());
        Map<String, Object> filtered = filterFields(data, allowedFields);
        filtered.remove("id");
        filtered.remove("tenant_id");

        if (filtered.isEmpty()) {
            throw new BusinessException("没有可更新的字段");
        }

        filtered.put("update_time", new java.util.Date());

        List<String> setClauses = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, Object> entry : filtered.entrySet()) {
            validateIdentifier(entry.getKey());
            setClauses.add(entry.getKey() + " = ?");
            values.add(entry.getValue());
        }

        Object idValue = data.get("id");
        if (idValue == null) {
            throw new BusinessException("更新操作缺少id");
        }
        values.add(idValue);

        StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET " + String.join(", ", setClauses) + " WHERE id = ?");

        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            sql.append(" AND tenant_id = ?");
            values.add(tenantId);
        }

        jdbcTemplate.update(sql.toString(), values.toArray());
    }

    public void deleteById(String configKey, Long id) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        validateTableName(tableName);

        List<Object> params = new ArrayList<>();
        params.add(id);

        Long tenantId = TenantContextHolder.getTenantId();

        if (hasDelFlag(tableName)) {
            StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET del_flag = '1', update_time = ? WHERE id = ?");
            List<Object> delParams = new ArrayList<>();
            delParams.add(new java.util.Date());
            delParams.add(id);
            if (tenantId != null) {
                sql.append(" AND tenant_id = ?");
                delParams.add(tenantId);
            }
            jdbcTemplate.update(sql.toString(), delParams.toArray());
        } else {
            StringBuilder sql = new StringBuilder("DELETE FROM " + tableName + " WHERE id = ?");
            if (tenantId != null) {
                sql.append(" AND tenant_id = ?");
                params.add(tenantId);
            }
            jdbcTemplate.update(sql.toString(), params.toArray());
        }
    }

    public boolean hasDelFlag(String tableName) {
        try {
            String checkSql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = ? AND column_name = 'del_flag' AND table_schema = (SELECT DATABASE())";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 检查del_flag失败, tableName={}", tableName, e);
            return false;
        }
    }

    private void validateTableName(String tableName) {
        validateIdentifier(tableName);
        try {
            String checkSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ? AND table_schema = (SELECT DATABASE())";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName);
            if (count == null || count == 0) {
                throw new BusinessException("数据表不存在: " + tableName);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 校验表名失败, tableName={}", tableName, e);
            throw new BusinessException("校验表名失败: " + tableName);
        }
    }

    private AiCrudConfig getConfig(String configKey) {
        AiCrudConfig config = configService.getByConfigKey(configKey);
        if (config == null || "1".equals(config.getStatus())) {
            throw new BusinessException("CRUD配置不存在或已停用: " + configKey);
        }
        if (!"CONFIG".equals(config.getMode())) {
            throw new BusinessException("该配置不是配置驱动模式: " + configKey);
        }
        return config;
    }

    private Set<String> extractFieldNames(String schemaJson) {
        if (StringUtils.isBlank(schemaJson)) {
            return Collections.emptySet();
        }
        try {
            JsonNode node = objectMapper.readTree(schemaJson);
            Set<String> fields = new HashSet<>();
            if (node.isArray()) {
                for (JsonNode item : node) {
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
            log.warn("[DynamicCrudService] 解析schema字段名失败", e);
            return Collections.emptySet();
        }
    }

    private Map<String, String> extractSearchTypeMap(String schemaJson) {
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
                    JsonNode searchTypeNode = item.get("searchType");
                    if (searchTypeNode != null && !searchTypeNode.isNull()) {
                        typeMap.put(field, searchTypeNode.asText());
                    } else {
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
                        } else {
                            typeMap.put(field, "like");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析searchTypeMap失败", e);
        }
        return typeMap;
    }

    private Map<String, Object> filterFields(Map<String, Object> data, Set<String> allowedFields) {
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (allowedFields.contains(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    private void validateIdentifier(String identifier) {
        if (StringUtils.isBlank(identifier) || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new BusinessException("非法标识符: " + identifier);
        }
    }

    /**
     * 应用字段脱敏
     */
    private void applyDesensitize(List<Map<String, Object>> rows, String desensitizeConfigJson) {
        if (StringUtils.isBlank(desensitizeConfigJson) || rows == null || rows.isEmpty()) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(desensitizeConfigJson);
            if (!configNode.isObject()) return;

            for (Map<String, Object> row : rows) {
                Iterator<Map.Entry<String, JsonNode>> fields = configNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String fieldName = entry.getKey();
                    JsonNode ruleNode = entry.getValue();
                    if (!row.containsKey(fieldName) || row.get(fieldName) == null) continue;

                    String typeStr = ruleNode.has("type") ? ruleNode.get("type").asText("CUSTOM") : "CUSTOM";
                    DesensitizeType type = DesensitizeType.valueOf(typeStr);
                    DesensitizeStrategy strategy = desensitizeStrategyFactory.getStrategy(type);
                    if (strategy != null) {
                        String originalValue = String.valueOf(row.get(fieldName));
                        row.put(fieldName, strategy.desensitize(originalValue));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 脱敏处理失败", e);
        }
    }

    /**
     * 应用字典翻译
     */
    private void applyDictTranslation(List<Map<String, Object>> rows, String transConfigJson) {
        if (StringUtils.isBlank(transConfigJson) || rows == null || rows.isEmpty() || dictValueProvider == null) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(transConfigJson);
            if (!configNode.isObject()) return;

            for (Map<String, Object> row : rows) {
                Iterator<Map.Entry<String, JsonNode>> fields = configNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String sourceField = entry.getKey();
                    JsonNode ruleNode = entry.getValue();
                    if (!row.containsKey(sourceField) || row.get(sourceField) == null) continue;

                    String dictType = ruleNode.has("dictType") ? ruleNode.get("dictType").asText() : "";
                    String targetField = ruleNode.has("targetField") ? ruleNode.get("targetField").asText()
                            : sourceField + "Name";
                    if (StringUtils.isBlank(dictType)) continue;

                    String key = String.valueOf(row.get(sourceField));
                    String label = dictValueProvider.getLabel(dictType, key);
                    if (label != null) {
                        row.put(targetField, label);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 字典翻译失败", e);
        }
    }
}
