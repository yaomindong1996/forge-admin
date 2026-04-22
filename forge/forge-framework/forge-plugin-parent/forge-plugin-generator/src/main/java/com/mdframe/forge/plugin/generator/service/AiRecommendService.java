package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.util.GenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendService {

    private static final String COLUMN_ADVISOR_AGENT = "codegen_column_advisor";
    private static final Set<String> VALID_HTML_TYPES = Set.of(
            "INPUT", "TEXTAREA", "SELECT", "RADIO", "CHECKBOX", "DATETIME", "IMAGE", "FILE", "EDITOR"
    );
    private static final Set<String> VALID_JAVA_TYPES = Set.of(
            "Integer", "Long", "String", "BigDecimal", "Float", "Double",
            "LocalDateTime", "LocalDate", "Boolean", "byte[]"
    );
    private static final Set<String> SUPER_COLUMNS = Set.of(
            "create_time", "create_by", "update_time", "update_by", "remark",
            "id", "tenant_id", "create_dept"
    );

    private final AiClientAdapter aiClientAdapter;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Set<String> existingDictTypes = new HashSet<>();

    public List<GenTableColumn> recommendColumns(Long tableId, List<GenTableColumn> columns, GenTable table) {
        if (columns == null || columns.isEmpty()) {
            return Collections.emptyList();
        }

        loadExistingDictTypes();
        String dictList = truncateDictList(queryDictTypeList());
        String prompt = buildColumnRecommendPrompt(table, columns, dictList);

        Map<String, String> contextVars = new HashMap<>();
        contextVars.put("dictList", dictList);

        AiClientAdapter.AiClientResult result = aiClientAdapter.call(COLUMN_ADVISOR_AGENT, prompt, contextVars);

        if (result.isFallback()) {
            log.warn("[AiRecommendService] AI推荐降级, reason={}", result.getFallbackReason());
            return fallbackToRuleBased(columns);
        }

        try {
            List<Map<String, Object>> recommendations = parseAiResponse(result.getContent());
            return validateAndMerge(columns, recommendations);
        } catch (Exception e) {
            log.error("[AiRecommendService] 解析AI推荐结果失败", e);
            return fallbackToRuleBased(columns);
        }
    }

    private String buildColumnRecommendPrompt(GenTable table, List<GenTableColumn> columns, String dictList) {
        StringBuilder sb = new StringBuilder();
        sb.append("表名：").append(table.getTableName()).append("\n");
        sb.append("表注释：").append(table.getTableComment()).append("\n\n");
        sb.append("字段列表：\n");

        for (GenTableColumn col : columns) {
            sb.append("- 字段名：").append(col.getColumnName());
            sb.append("，数据库类型：").append(col.getColumnType());
            sb.append("，注释：").append(col.getColumnComment() != null ? col.getColumnComment() : "");
            if (StringUtils.hasText(col.getDictType())) {
                sb.append("，当前字典：").append(col.getDictType());
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String queryDictTypeList() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT dict_type, dict_name FROM sys_dict_type WHERE dict_status = '0' LIMIT 200"
            );
            return rows.stream()
                    .map(row -> row.get("dict_type") + "(" + row.get("dict_name") + ")")
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            log.debug("[AiRecommendService] 查询字典列表失败，跳过字典注入");
            return "";
        }
    }

    private String truncateDictList(String dictList) {
        if (dictList.length() > 3000) {
            dictList = dictList.substring(0, 3000);
            log.debug("[AiRecommendService] 字典列表过长，截断至3000字符");
        }
        return dictList;
    }

    private List<Map<String, Object>> parseAiResponse(String content) throws Exception {
        String json = content.trim();
        if (json.startsWith("```json")) {
            json = json.substring(7);
        }
        if (json.startsWith("```")) {
            json = json.substring(3);
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }
        json = json.trim();
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private List<GenTableColumn> validateAndMerge(List<GenTableColumn> original, List<Map<String, Object>> recommendations) {
        Map<String, Map<String, Object>> recMap = recommendations.stream()
                .filter(r -> r.containsKey("columnName"))
                .collect(Collectors.toMap(
                        r -> String.valueOf(r.get("columnName")).toLowerCase(),
                        r -> r,
                        (a, b) -> b
                ));

        for (GenTableColumn col : original) {
            String colNameLower = col.getColumnName().toLowerCase();
            Map<String, Object> rec = recMap.get(colNameLower);

            if (rec == null) {
                continue;
            }

            if (SUPER_COLUMNS.contains(colNameLower)) {
                col.setIsInsert(0);
                col.setIsEdit(0);
                col.setIsList(0);
                col.setIsQuery(0);
                continue;
            }

            String recJavaType = getStr(rec, "javaType");
            String recHtmlType = getStr(rec, "htmlType");
            String recDictType = getStr(rec, "dictType");
            String recQueryType = getStr(rec, "queryType");
            Object reqRequired = rec.get("isRequired");
            String recValidateRule = getStr(rec, "validateRule");

            boolean userModified = col.getAiRecommended() != null && col.getAiRecommended() == 0
                    && hasUserCustomization(col);
            if (userModified) {
                continue;
            }

            if (VALID_JAVA_TYPES.contains(recJavaType)) {
                col.setJavaType(recJavaType);
            }
            if (VALID_HTML_TYPES.contains(recHtmlType)) {
                col.setHtmlType(recHtmlType);
            }
            if (StringUtils.hasText(recDictType) && existingDictTypes.contains(recDictType)) {
                col.setDictType(recDictType);
            }
            if (StringUtils.hasText(recQueryType)) {
                col.setQueryType(recQueryType);
            }
            if (reqRequired != null) {
                try {
                    col.setIsRequired(Integer.parseInt(String.valueOf(reqRequired)));
                } catch (NumberFormatException ignored) {}
            }
            if (StringUtils.hasText(recValidateRule)) {
                col.setValidateRule(recValidateRule);
            }
            col.setAiRecommended(1);
        }

        return original;
    }

    private List<GenTableColumn> fallbackToRuleBased(List<GenTableColumn> columns) {
        for (GenTableColumn col : columns) {
            GenUtils.initColumnField(col);
            col.setAiRecommended(0);
        }
        return columns;
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    private boolean hasUserCustomization(GenTableColumn col) {
        return StringUtils.hasText(col.getDictType())
                || !"INPUT".equals(col.getHtmlType())
                || !"EQ".equals(col.getQueryType())
                || StringUtils.hasText(col.getValidateRule());
    }

    private void loadExistingDictTypes() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT dict_type FROM sys_dict_type WHERE dict_status = '0'"
            );
            existingDictTypes.clear();
            for (Map<String, Object> row : rows) {
                existingDictTypes.add(String.valueOf(row.get("dict_type")));
            }
        } catch (Exception e) {
            log.debug("[AiRecommendService] 加载字典类型失败");
        }
    }
}
