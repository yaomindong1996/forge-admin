package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.dto.SchemaGenerateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaGenerator {

    private final ObjectMapper objectMapper;

    private static final Set<String> BASE_FIELDS = Set.of(
            "id", "tenant_id", "create_by", "create_time", "create_dept", "update_by", "update_time", "del_flag"
    );

    private static final Map<String, String> QUERY_TYPE_TO_SEARCH_TYPE = Map.ofEntries(
            Map.entry("EQ", "input"),
            Map.entry("LIKE", "input"),
            Map.entry("BETWEEN", "daterange"),
            Map.entry("GT", "number"),
            Map.entry("GTE", "number"),
            Map.entry("LT", "number"),
            Map.entry("LTE", "number"),
            Map.entry("IN", "select"),
            Map.entry("NE", "input")
    );

    private static final Map<String, String> HTML_TYPE_TO_EDIT_TYPE = Map.ofEntries(
            Map.entry("INPUT", "input"),
            Map.entry("TEXTAREA", "textarea"),
            Map.entry("SELECT", "select"),
            Map.entry("RADIO", "radio"),
            Map.entry("CHECKBOX", "checkbox"),
            Map.entry("DATETIME", "datetime"),
            Map.entry("DATE", "date"),
            Map.entry("SWITCH", "switch"),
            Map.entry("UPLOAD", "upload"),
            Map.entry("IMAGEUPLOAD", "imageUpload"),
            Map.entry("FILEUPLOAD", "fileUpload"),
            Map.entry("CASCADER", "cascader"),
            Map.entry("TREESELECT", "treeSelect"),
            Map.entry("NUMBER", "number")
    );

    public SchemaGenerateResult generate(String configKey, String tableName, String tableComment, List<GenTableColumn> columns) {
        SchemaGenerateResult result = new SchemaGenerateResult();
        result.setConfigKey(configKey);
        result.setTableName(tableName);
        result.setTableComment(tableComment);

        try {
            List<Object> searchSchema = buildSearchSchema(columns);
            List<Object> columnsSchema = buildColumns(columns);
            List<Object> editSchema = buildEditSchema(columns);
            Map<String, String> apiConfig = buildApiConfig(configKey);

            result.setSearchSchema(objectMapper.writeValueAsString(searchSchema));
            result.setColumnsSchema(objectMapper.writeValueAsString(columnsSchema));
            result.setEditSchema(objectMapper.writeValueAsString(editSchema));
            result.setApiConfig(objectMapper.writeValueAsString(apiConfig));
        } catch (Exception e) {
            log.error("[SchemaGenerator] Schema生成失败, tableName={}", tableName, e);
            throw new RuntimeException("Schema生成失败: " + e.getMessage(), e);
        }

        return result;
    }

    List<Object> buildSearchSchema(List<GenTableColumn> columns) {
        List<Object> schema = new ArrayList<>();
        List<GenTableColumn> searchColumns = columns.stream()
                .filter(c -> c.getIsQuery() != null && c.getIsQuery() == 1)
                .sorted(Comparator.comparingInt(c -> c.getSort() != null ? c.getSort() : 0))
                .collect(Collectors.toList());

        for (GenTableColumn col : searchColumns) {
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("field", col.getJavaField());
            field.put("label", StringUtils.isNotBlank(col.getColumnComment()) ? col.getColumnComment() : col.getJavaField());

            String searchType = resolveSearchType(col);
            field.put("type", searchType);

            if ("select".equals(searchType) || "radio".equals(searchType)) {
                if (StringUtils.isNotBlank(col.getDictType())) {
                    field.put("dictType", col.getDictType());
                }
            }
            if ("daterange".equals(searchType)) {
                field.put("type", "daterange");
            }

            schema.add(field);
        }
        return schema;
    }

    List<Object> buildColumns(List<GenTableColumn> columns) {
        List<Object> schema = new ArrayList<>();
        List<GenTableColumn> listColumns = columns.stream()
                .filter(c -> c.getIsList() != null && c.getIsList() == 1)
                .sorted(Comparator.comparingInt(c -> c.getSort() != null ? c.getSort() : 0))
                .collect(Collectors.toList());

        for (GenTableColumn col : listColumns) {
            Map<String, Object> column = new LinkedHashMap<>();
            column.put("key", col.getJavaField());
            column.put("title", StringUtils.isNotBlank(col.getColumnComment()) ? col.getColumnComment() : col.getJavaField());
            column.put("dataIndex", col.getJavaField());

            if (StringUtils.isNotBlank(col.getDictType())) {
                Map<String, Object> render = new LinkedHashMap<>();
                render.put("type", "dictTag");
                render.put("dictType", col.getDictType());
                column.put("render", render);
            }

            String htmlType = col.getHtmlType();
            if ("DATETIME".equals(htmlType) || "DATE".equals(htmlType)) {
                column.put("width", 180);
            } else if ("TEXTAREA".equals(htmlType)) {
                column.put("width", 200);
                column.put("ellipsis", true);
            }

            schema.add(column);
        }

        Map<String, Object> actionColumn = new LinkedHashMap<>();
        actionColumn.put("key", "actions");
        actionColumn.put("title", "操作");
        actionColumn.put("dataIndex", "actions");
        actionColumn.put("width", 180);
        actionColumn.put("fixed", "right");
        schema.add(actionColumn);

        return schema;
    }

    List<Object> buildEditSchema(List<GenTableColumn> columns) {
        List<Object> schema = new ArrayList<>();
        List<GenTableColumn> editColumns = columns.stream()
                .filter(c -> (c.getIsInsert() != null && c.getIsInsert() == 1)
                        || (c.getIsEdit() != null && c.getIsEdit() == 1))
                .filter(c -> !BASE_FIELDS.contains(c.getJavaField()))
                .filter(c -> c.getIsPk() == null || c.getIsPk() == 0)
                .sorted(Comparator.comparingInt(c -> c.getSort() != null ? c.getSort() : 0))
                .collect(Collectors.toList());

        for (GenTableColumn col : editColumns) {
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("field", col.getJavaField());
            field.put("label", StringUtils.isNotBlank(col.getColumnComment()) ? col.getColumnComment() : col.getJavaField());

            String editType = resolveEditType(col);
            field.put("type", editType);

            if (col.getIsRequired() != null && col.getIsRequired() == 1) {
                field.put("required", true);
            }

            if (StringUtils.isNotBlank(col.getDictType())) {
                field.put("dictType", col.getDictType());
            }

            if ("textarea".equals(editType)) {
                Map<String, Object> props = new LinkedHashMap<>();
                props.put("rows", 3);
                field.put("props", props);
            }

            if (col.getIsEdit() != null && col.getIsEdit() == 0 && col.getIsInsert() != null && col.getIsInsert() == 1) {
                Map<String, Object> props = field.containsKey("props") ? (Map<String, Object>) field.get("props") : new LinkedHashMap<>();
                props.put("disabledOnEdit", true);
                field.put("props", props);
            }

            schema.add(field);
        }
        return schema;
    }

    Map<String, String> buildApiConfig(String configKey) {
        Map<String, String> apiConfig = new LinkedHashMap<>();
        apiConfig.put("list", "get@/ai/crud/" + configKey + "/page");
        apiConfig.put("detail", "get@/ai/crud/" + configKey + "/{id}");
        apiConfig.put("create", "post@/ai/crud/" + configKey);
        apiConfig.put("update", "put@/ai/crud/" + configKey);
        apiConfig.put("delete", "delete@/ai/crud/" + configKey + "/{id}");
        return apiConfig;
    }

    private String resolveSearchType(GenTableColumn col) {
        String queryType = col.getQueryType();
        if (StringUtils.isNotBlank(queryType) && QUERY_TYPE_TO_SEARCH_TYPE.containsKey(queryType)) {
            String type = QUERY_TYPE_TO_SEARCH_TYPE.get(queryType);
            if ("select".equals(type) && StringUtils.isNotBlank(col.getDictType())) {
                return "select";
            }
            if ("select".equals(type) && StringUtils.isBlank(col.getDictType())) {
                return "input";
            }
            return type;
        }
        if (StringUtils.isNotBlank(col.getDictType())) {
            return "select";
        }
        return "input";
    }

    private String resolveEditType(GenTableColumn col) {
        String htmlType = col.getHtmlType();
        if (StringUtils.isNotBlank(htmlType) && HTML_TYPE_TO_EDIT_TYPE.containsKey(htmlType.toUpperCase())) {
            return HTML_TYPE_TO_EDIT_TYPE.get(htmlType.toUpperCase());
        }
        String columnType = col.getColumnType();
        if (StringUtils.isNotBlank(columnType)) {
            if (columnType.startsWith("text") || columnType.startsWith("longtext")) {
                return "textarea";
            }
            if (columnType.startsWith("int") || columnType.startsWith("bigint")
                    || columnType.startsWith("decimal") || columnType.startsWith("double")
                    || columnType.startsWith("float") || columnType.startsWith("tinyint")) {
                return "number";
            }
            if (columnType.startsWith("date") || columnType.startsWith("timestamp")) {
                return "datetime";
            }
        }
        return "input";
    }
}
