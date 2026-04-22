package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.dto.NlToSchemaResult;
import com.mdframe.forge.plugin.generator.dto.SchemaColumn;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.mapper.GenTableColumnMapper;
import com.mdframe.forge.plugin.generator.mapper.GenTableMapper;
import com.mdframe.forge.plugin.generator.config.GeneratorConfig;
import com.mdframe.forge.plugin.generator.util.GenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSchemaService {

    private static final String SCHEMA_BUILDER_AGENT = "codegen_schema_builder";
    private static final List<SchemaColumn> BASE_COLUMNS = List.of(
            createBaseColumn("id", "主键", "bigint", "Long", "id", null, null, 0, null),
            createBaseColumn("tenant_id", "租户ID", "bigint", "Long", "tenantId", null, null, 0, null),
            createBaseColumn("create_by", "创建者", "bigint", "Long", "createBy", null, null, 0, null),
            createBaseColumn("create_time", "创建时间", "datetime", "LocalDateTime", "createTime", null, null, 0, null),
            createBaseColumn("create_dept", "创建部门", "bigint", "Long", "createDept", null, null, 0, null),
            createBaseColumn("update_by", "更新者", "bigint", "Long", "updateBy", null, null, 0, null),
            createBaseColumn("update_time", "更新时间", "datetime", "LocalDateTime", "updateTime", null, null, 0, null)
    );

    private final AiClientAdapter aiClientAdapter;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final GenTableMapper genTableMapper;
    private final GenTableColumnMapper genTableColumnMapper;
    private final GeneratorConfig generatorConfig;

    public NlToSchemaResult nlToSchema(String description) {
        if (!StringUtils.hasText(description)) {
            return errorResult("描述不能为空");
        }
        if (description.length() > 2000) {
            description = description.substring(0, 2000);
        }

        String dictList = queryDictTypeList();
        String prompt = "请根据以下描述推断数据表结构：\n\n" + description;

        Map<String, String> contextVars = new HashMap<>();
        contextVars.put("dictList", dictList);

        AiClientAdapter.AiClientResult result = aiClientAdapter.call(SCHEMA_BUILDER_AGENT, prompt, contextVars);

        if (result.isFallback()) {
            log.warn("[AiSchemaService] AI Schema推断降级, reason={}", result.getFallbackReason());
            return errorResult("AI 不可用，请通过导入表方式创建");
        }

        return parseSchemaResponse(result.getContent());
    }

    public NlToSchemaResult nlToSchemaRefine(String currentSchemaJson, String message, String sessionId) {
        if (!StringUtils.hasText(currentSchemaJson) || !StringUtils.hasText(message)) {
            return errorResult("当前Schema和追问消息不能为空");
        }

        String prompt = "当前Schema如下：\n" + currentSchemaJson +
                "\n\n请根据以下追问优化Schema：\n" + message;

        String dictList = queryDictTypeList();
        Map<String, String> contextVars = new HashMap<>();
        contextVars.put("dictList", dictList);

        AiClientAdapter.AiClientResult result = aiClientAdapter.call(SCHEMA_BUILDER_AGENT, prompt, contextVars);

        if (result.isFallback()) {
            log.warn("[AiSchemaService] AI Schema追问优化降级, reason={}", result.getFallbackReason());
            return errorResult("AI 不可用，请稍后重试");
        }

        return parseSchemaResponse(result.getContent());
    }

    private NlToSchemaResult parseSchemaResponse(String content) {
        try {
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

            Map<String, Object> schemaMap = objectMapper.readValue(json, new TypeReference<>() {});
            NlToSchemaResult result = new NlToSchemaResult();
            result.setTableName(getStr(schemaMap, "tableName"));
            result.setTableComment(getStr(schemaMap, "tableComment"));

            Object columnsObj = schemaMap.get("columns");
            if (columnsObj instanceof List) {
                String columnsJson = objectMapper.writeValueAsString(columnsObj);
                List<SchemaColumn> columns = objectMapper.readValue(columnsJson, new TypeReference<>() {});
                appendBaseColumns(columns);
                result.setColumns(columns);
            }

            result.setRawResponse(content);
            return result;
        } catch (Exception e) {
            log.error("[AiSchemaService] 解析Schema响应失败", e);
            NlToSchemaResult error = new NlToSchemaResult();
            error.setRawResponse(content);
            return error;
        }
    }

    private void appendBaseColumns(List<SchemaColumn> columns) {
        Set<String> existingNames = columns.stream()
                .map(c -> c.getColumnName().toLowerCase())
                .collect(Collectors.toSet());

        List<SchemaColumn> baseToAdd = new ArrayList<>();
        for (SchemaColumn base : BASE_COLUMNS) {
            if (!existingNames.contains(base.getColumnName().toLowerCase())) {
                baseToAdd.add(base);
            }
        }
        if (!baseToAdd.isEmpty()) {
            columns.addAll(baseToAdd);
        }
    }

    private String queryDictTypeList() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT dict_type, dict_name FROM sys_dict_type WHERE dict_status = '0' LIMIT 200"
            );
            String dictList = rows.stream()
                    .map(row -> row.get("dict_type") + "(" + row.get("dict_name") + ")")
                    .collect(Collectors.joining(", "));
            if (dictList.length() > 3000) {
                dictList = dictList.substring(0, 3000);
                log.debug("[AiSchemaService] 字典列表过长，截断至3000字符");
            }
            return dictList;
        } catch (Exception e) {
            log.debug("[AiSchemaService] 查询字典列表失败，跳过字典注入");
            return "";
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void importSchema(NlToSchemaResult schema) {
        if (schema == null || !StringUtils.hasText(schema.getTableName())) {
            throw new RuntimeException("Schema 无效：缺少表名");
        }

        GenTable genTable = new GenTable();
        genTable.setTableName(schema.getTableName());
        genTable.setTableComment(schema.getTableComment());
        GenUtils.initTable(genTable, generatorConfig);

        genTableMapper.insert(genTable);

        if (schema.getColumns() != null) {
            int sort = 1;
            for (SchemaColumn sc : schema.getColumns()) {
                GenTableColumn col = new GenTableColumn();
                col.setTableId(genTable.getTableId());
                col.setColumnName(sc.getColumnName());
                col.setColumnComment(sc.getColumnComment());
                col.setColumnType(sc.getColumnType());
                col.setJavaType(sc.getJavaType() != null ? sc.getJavaType() : GenUtils.convertDbTypeToJavaType(sc.getColumnType()));
                col.setJavaField(sc.getJavaField() != null ? sc.getJavaField() : GenUtils.convertColumnNameToJavaField(sc.getColumnName()));
                col.setHtmlType(sc.getHtmlType());
                col.setDictType(sc.getDictType());
                col.setIsRequired(sc.getIsRequired() != null ? sc.getIsRequired() : 0);
                col.setValidateRule(sc.getValidateRule());
                col.setAiRecommended(1);
                col.setIsPk("id".equals(sc.getColumnName()) ? 1 : 0);
                col.setIsIncrement("id".equals(sc.getColumnName()) ? 1 : 0);

                boolean isSuper = GenUtils.isSuperColumn(sc.getColumnName())
                        || "id".equals(sc.getColumnName())
                        || "tenant_id".equals(sc.getColumnName());
                col.setIsInsert(isSuper ? 0 : 1);
                col.setIsEdit(isSuper ? 0 : 1);
                col.setIsList(isSuper ? 0 : 1);
                col.setIsQuery(isSuper ? 0 : 1);

                col.setQueryType("String".equals(col.getJavaType()) ? "LIKE" : "EQ");
                col.setSort(sort++);
                genTableColumnMapper.insert(col);
            }
        }
    }

    private NlToSchemaResult errorResult(String msg) {
        NlToSchemaResult result = new NlToSchemaResult();
        result.setRawResponse(msg);
        return result;
    }

    private static SchemaColumn createBaseColumn(String name, String comment, String colType,
                                                  String javaType, String javaField, String htmlType,
                                                  String dictType, int isRequired, String validateRule) {
        SchemaColumn col = new SchemaColumn();
        col.setColumnName(name);
        col.setColumnComment(comment);
        col.setColumnType(colType);
        col.setJavaType(javaType);
        col.setJavaField(javaField);
        col.setHtmlType(htmlType);
        col.setDictType(dictType);
        col.setIsRequired(isRequired);
        col.setValidateRule(validateRule);
        return col;
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : null;
    }
}
