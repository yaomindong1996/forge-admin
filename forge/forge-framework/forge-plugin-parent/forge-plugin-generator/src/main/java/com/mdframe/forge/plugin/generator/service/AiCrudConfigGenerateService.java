package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.dto.AiCrudGenerateRequest;
import com.mdframe.forge.plugin.generator.dto.AiCrudGenerateResult;
import com.mdframe.forge.plugin.generator.dto.SchemaGenerateResult;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.mapper.GenTableColumnMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeRuntimeConfigBuilder;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCrudConfigGenerateService {

    private final AiClientAdapter aiClientAdapter;
    private final SchemaGenerator schemaGenerator;
    private final GenTableColumnMapper genTableColumnMapper;
    private final LowcodeRuntimeConfigBuilder lowcodeRuntimeConfigBuilder;
    private final ObjectMapper objectMapper;

    public AiCrudGenerateResult generateFromDescription(AiCrudGenerateRequest request) {
        if (StringUtils.isBlank(request.getDescription())) {
            return AiCrudGenerateResult.fail("描述不能为空");
        }
        if (StringUtils.isBlank(request.getConfigKey())) {
            return AiCrudGenerateResult.fail("configKey不能为空");
        }

        String prompt = buildDescriptionPrompt(request.getDescription(), request.getConfigKey());
        return callAiAndBuildResult(prompt, request.getConfigKey());
    }

    public AiCrudGenerateResult generateFromTable(AiCrudGenerateRequest request) {
        if (StringUtils.isBlank(request.getTableName())) {
            return AiCrudGenerateResult.fail("表名不能为空");
        }
        if (StringUtils.isBlank(request.getConfigKey())) {
            return AiCrudGenerateResult.fail("configKey不能为空");
        }

        List<GenTableColumn> columns = genTableColumnMapper.selectDbTableColumnsByName(request.getTableName());
        if (columns == null || columns.isEmpty()) {
            return AiCrudGenerateResult.fail("未找到表字段信息: " + request.getTableName());
        }

        enrichColumnDefaults(columns);

        String prompt = buildTablePrompt(request.getTableName(), columns, request.getConfigKey());
        AiCrudGenerateResult result = callAiAndBuildResult(prompt, request.getConfigKey());

        if (!result.isSuccess() || result.isFallback()) {
            log.info("[AiCrudConfigGenerateService] AI生成失败或降级，使用规则引擎生成: tableName={}", request.getTableName());
            return fallbackFromTable(request.getTableName(), request.getConfigKey(), columns);
        }

        result.setTableName(request.getTableName());
        return result;
    }

    private AiCrudGenerateResult callAiAndBuildResult(String prompt, String configKey) {
        try {
            Map<String, String> contextVars = new HashMap<>();
            contextVars.put("configKey", configKey);

            AiClientAdapter.AiClientResult aiResult = aiClientAdapter.call("crud_config_builder", prompt, contextVars, 60);

            if (aiResult.isFallback()) {
                log.warn("[AiCrudConfigGenerateService] AI降级: {}", aiResult.getFallbackReason());
                AiCrudGenerateResult result = AiCrudGenerateResult.fail(aiResult.getFallbackReason());
                result.setFallback(true);
                return result;
            }

            return parseAiResponse(aiResult.getContent(), configKey);
        } catch (Exception e) {
            log.error("[AiCrudConfigGenerateService] AI调用异常", e);
            AiCrudGenerateResult result = AiCrudGenerateResult.fail("AI调用异常: " + e.getMessage());
            result.setFallback(true);
            return result;
        }
    }

    private AiCrudGenerateResult parseAiResponse(String content, String configKey) {
        try {
            String jsonStr = content;
            if (content.contains("```json")) {
                jsonStr = content.substring(content.indexOf("```json") + 7);
                jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
            } else if (content.contains("```")) {
                jsonStr = content.substring(content.indexOf("```") + 3);
                jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
            }
            jsonStr = jsonStr.trim();

            JsonNode node = objectMapper.readTree(jsonStr);

            String tableName = getTextOrDefault(node, "tableName", "");
            String tableComment = getTextOrDefault(node, "tableComment", "");
            String searchSchema = getSchemaAsString(node, "searchSchema");
            String columnsSchema = getSchemaAsString(node, "columnsSchema");
            String editSchema = getSchemaAsString(node, "editSchema");
            String apiConfig = getSchemaAsString(node, "apiConfig");
            String dictConfig = getSchemaAsString(node, "dictConfig");
            String desensitizeConfig = getSchemaAsString(node, "desensitizeConfig");
            String encryptConfig = getSchemaAsString(node, "encryptConfig");
            String transConfig = getSchemaAsString(node, "transConfig");
            String modelSchema = getSchemaAsString(node, "modelSchema");
            String pageSchema = getSchemaAsString(node, "pageSchema");
            String options = getSchemaAsString(node, "options");
            String layoutType = getTextOrDefault(node, "layoutType", "");

            if (hasLowcodeProtocol(modelSchema, pageSchema)
                    && (StringUtils.isAnyBlank(searchSchema, columnsSchema, editSchema) || StringUtils.isBlank(apiConfig))) {
                LowcodeRuntimeConfig runtimeConfig = buildRuntimeFromLowcode(configKey, modelSchema, pageSchema);
                tableName = StringUtils.defaultIfBlank(tableName, runtimeConfig.getTableName());
                tableComment = StringUtils.defaultIfBlank(tableComment, runtimeConfig.getTableComment());
                searchSchema = StringUtils.defaultIfBlank(searchSchema, runtimeConfig.getSearchSchema());
                columnsSchema = StringUtils.defaultIfBlank(columnsSchema, runtimeConfig.getColumnsSchema());
                editSchema = StringUtils.defaultIfBlank(editSchema, runtimeConfig.getEditSchema());
                apiConfig = StringUtils.defaultIfBlank(apiConfig, runtimeConfig.getApiConfig());
                options = StringUtils.defaultIfBlank(options, runtimeConfig.getOptions());
                dictConfig = StringUtils.defaultIfBlank(dictConfig, runtimeConfig.getDictConfig());
                desensitizeConfig = StringUtils.defaultIfBlank(desensitizeConfig, runtimeConfig.getDesensitizeConfig());
                encryptConfig = StringUtils.defaultIfBlank(encryptConfig, runtimeConfig.getEncryptConfig());
                transConfig = StringUtils.defaultIfBlank(transConfig, runtimeConfig.getTransConfig());
                layoutType = StringUtils.defaultIfBlank(layoutType, runtimeConfig.getLayoutType());
            }

            if (StringUtils.isBlank(apiConfig)) {
                Map<String, String> defaultApiConfig = schemaGenerator.buildApiConfig(configKey);
                apiConfig = objectMapper.writeValueAsString(defaultApiConfig);
            }

            AiCrudGenerateResult result = AiCrudGenerateResult.ok(configKey, tableName, tableComment,
                    searchSchema, columnsSchema, editSchema, apiConfig,
                    dictConfig, desensitizeConfig, encryptConfig, transConfig);
            result.setModelSchema(modelSchema);
            result.setPageSchema(pageSchema);
            result.setOptions(options);
            result.setLayoutType(layoutType);
            return result;
        } catch (Exception e) {
            log.error("[AiCrudConfigGenerateService] AI响应解析失败", e);
            return AiCrudGenerateResult.fail("AI响应格式解析失败: " + e.getMessage());
        }
    }

    private AiCrudGenerateResult fallbackFromTable(String tableName, String configKey, List<GenTableColumn> columns) {
        try {
            SchemaGenerateResult schemaResult = schemaGenerator.generate(configKey, tableName, "", columns);
            AiCrudGenerateResult result = AiCrudGenerateResult.ok(configKey, tableName, "",
                    schemaResult.getSearchSchema(), schemaResult.getColumnsSchema(),
                    schemaResult.getEditSchema(), schemaResult.getApiConfig(),
                    "", "", "", "");
            LowcodeProtocol protocol = buildLowcodeProtocol(tableName, columns);
            result.setModelSchema(objectMapper.writeValueAsString(protocol.modelSchema()));
            result.setPageSchema(objectMapper.writeValueAsString(protocol.pageSchema()));
            result.setOptions(objectMapper.writeValueAsString(protocol.options()));
            result.setLayoutType("simple-crud");
            result.setFallback(true);
            return result;
        } catch (Exception e) {
            log.error("[AiCrudConfigGenerateService] 规则引擎生成失败", e);
            return AiCrudGenerateResult.fail("规则引擎生成失败: " + e.getMessage());
        }
    }

    private boolean hasLowcodeProtocol(String modelSchema, String pageSchema) {
        return StringUtils.isNotBlank(modelSchema) && StringUtils.isNotBlank(pageSchema);
    }

    private LowcodeRuntimeConfig buildRuntimeFromLowcode(String configKey, String modelSchema, String pageSchema) throws Exception {
        LowcodeModelSchema model = objectMapper.readValue(modelSchema, LowcodeModelSchema.class);
        LowcodePageSchema page = objectMapper.readValue(pageSchema, LowcodePageSchema.class);
        return lowcodeRuntimeConfigBuilder.buildRuntimeConfig(configKey, model, page);
    }

    private String buildDescriptionPrompt(String description, String configKey) {
        return "根据以下描述生成CRUD配置：\n\n" + description +
                "\n\n请生成包含以下字段的JSON配置，优先生成低代码业务协议：\n" +
                "- tableName: 建议的表名（如有）\n" +
                "- tableComment: 表描述\n" +
                "- modelSchema: 业务数据模型协议，包含 appType/tableMode/tableName/businessName/fields\n" +
                "- pageSchema: 页面搭建协议，包含 layoutType/zones\n" +
                "- searchSchema: 搜索表单配置数组\n" +
                "- columnsSchema: 表格列配置数组\n" +
                "- editSchema: 编辑表单配置数组\n" +
                "- apiConfig: API配置对象\n\n" +
                "configKey: " + configKey + "\n\n" +
                "modelSchema.fields 每个字段需包含：field, columnName, label, dataType, length, required, searchable, listVisible, formVisible, componentType, queryType\n" +
                "pageSchema.layoutType 第一版只允许 simple-crud 或 tree-crud；如是树形单表，modelSchema.appType=TREE 且必须有 parentId/pid 父级字段\n" +
                "每个字段项需包含：field(字段名), label(标签), type(控件类型)\n" +
                "搜索类型：input/select/daterange\n" +
                "编辑类型：input/textarea/select/radio/checkbox/switch/date/datetime/number/upload\n" +
                "表格列需包含：key, title, dataIndex\n" +
                "有dictType的select类型字段请加上dictType属性\n" +
                "有手机号/身份证/邮箱/银行卡号字段时请生成desensitizeConfig脱敏配置\n" +
                "有dictType的字段请同时生成transConfig翻译配置\n" +
                "需要接口加解密的场景请生成encryptConfig配置\n" +
                "apiConfig格式：{list: 'get@/ai/crud/" + configKey + "/page', detail: 'get@/ai/crud/" + configKey + "/:id', " +
                "create: 'post@/ai/crud/" + configKey + "', update: 'put@/ai/crud/" + configKey + "', delete: 'delete@/ai/crud/" + configKey + "/:id'}\n" +
                "请仅输出JSON，不要其他内容。";
    }

    private String buildTablePrompt(String tableName, List<GenTableColumn> columns, String configKey) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据以下数据库表结构生成CRUD配置：\n\n");
        sb.append("表名: ").append(tableName).append("\n");
        sb.append("字段信息:\n");
        for (GenTableColumn col : columns) {
            sb.append("- ").append(col.getColumnName())
                    .append(" (").append(col.getColumnType()).append(")")
                    .append(" 备注: ").append(StringUtils.isNotBlank(col.getColumnComment()) ? col.getColumnComment() : "无")
                    .append(" 主键: ").append(col.getIsPk() != null && col.getIsPk() == 1 ? "是" : "否")
                    .append(" 必填: ").append(col.getIsRequired() != null && col.getIsRequired() == 1 ? "是" : "否")
                    .append("\n");
        }
        sb.append("\nconfigKey: ").append(configKey).append("\n\n");
        sb.append("请生成包含以下字段的JSON配置，优先生成低代码业务协议：\n");
        sb.append("- tableName, tableComment, modelSchema, pageSchema, searchSchema, columnsSchema, editSchema, apiConfig\n\n");
        sb.append("modelSchema.fields 每个字段需包含 field, columnName, label, dataType, length, required, searchable, listVisible, formVisible, componentType, queryType\n");
        sb.append("pageSchema.layoutType 第一版只允许 simple-crud 或 tree-crud；如是树形单表，modelSchema.appType=TREE 且必须有 parentId/pid 父级字段\n");
        sb.append("搜索类型：input/select/daterange\n");
        sb.append("编辑类型：input/textarea/select/radio/checkbox/switch/date/datetime/number/upload\n");
        sb.append("表格列需包含：key, title, dataIndex，最后加一列操作列(key=actions)\n");
        sb.append("有dictType的select类型字段请加上dictType属性\n");
        sb.append("有手机号/身份证/邮箱/银行卡号字段时请生成desensitizeConfig脱敏配置\n");
        sb.append("有dictType的字段请同时生成transConfig翻译配置\n");
        sb.append("需要接口加解密的场景请生成encryptConfig配置\n");
        sb.append("基类字段(id/tenant_id/create_by/create_time/update_by/update_time/del_flag)不进editSchema\n");
        sb.append("apiConfig格式：{list: 'get@/ai/crud/").append(configKey).append("/page', ...}\n");
        sb.append("请仅输出JSON，不要其他内容。");
        return sb.toString();
    }

    private void enrichColumnDefaults(List<GenTableColumn> columns) {
        for (GenTableColumn col : columns) {
            if (col.getIsInsert() == null) col.setIsInsert(1);
            if (col.getIsEdit() == null) col.setIsEdit(1);
            if (col.getIsList() == null) col.setIsList(1);
            if (col.getIsQuery() == null) col.setIsQuery(0);
            if (col.getQueryType() == null) col.setQueryType("EQ");
            if (col.getHtmlType() == null) col.setHtmlType("INPUT");
            if (col.getSort() == null) col.setSort(0);
            if (col.getIsRequired() == null) col.setIsRequired(0);
        }
    }

    private String getTextOrDefault(JsonNode node, String field, String defaultValue) {
        JsonNode f = node.get(field);
        return f != null && !f.isNull() ? f.asText() : defaultValue;
    }

    private String getSchemaAsString(JsonNode node, String field) throws Exception {
        JsonNode f = node.get(field);
        if (f == null || f.isNull()) return "";
        return objectMapper.writeValueAsString(f);
    }

    private LowcodeProtocol buildLowcodeProtocol(String tableName, List<GenTableColumn> columns) {
        Map<String, Object> modelSchema = new LinkedHashMap<>();
        List<Map<String, Object>> fields = new ArrayList<>();
        for (GenTableColumn column : columns) {
            if (isBaseColumn(column) || isPrimaryKey(column)) {
                continue;
            }
            fields.add(buildLowcodeField(column));
        }

        modelSchema.put("appType", "SINGLE");
        modelSchema.put("tableMode", "EXISTING");
        modelSchema.put("tableName", tableName);
        modelSchema.put("businessName", tableName);
        modelSchema.put("treeConfig", Map.of(
                "enabled", false,
                "keyField", "id",
                "parentField", "parentId",
                "labelField", inferLabelField(fields),
                "childrenField", "children",
                "treeTitle", tableName,
                "loadMode", "full"
        ));
        modelSchema.put("fields", fields);
        modelSchema.put("children", List.of());

        Map<String, Object> pageSchema = new LinkedHashMap<>();
        pageSchema.put("layoutType", "simple-crud");
        pageSchema.put("zones", buildPageZones(fields));

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("modalType", "modal");
        options.put("modalWidth", "800px");
        options.put("showImport", true);
        options.put("showExport", true);
        options.put("enableCustomQuery", true);
        return new LowcodeProtocol(modelSchema, pageSchema, options);
    }

    private Map<String, Object> buildLowcodeField(GenTableColumn column) {
        Map<String, Object> field = new LinkedHashMap<>();
        String fieldName = resolveJavaField(column);
        String componentType = resolveLowcodeComponentType(column);
        field.put("field", fieldName);
        field.put("columnName", column.getColumnName());
        field.put("label", StringUtils.defaultIfBlank(column.getColumnComment(), fieldName));
        field.put("dataType", resolveLowcodeDataType(column));
        field.put("length", resolveColumnLength(column.getColumnType()));
        field.put("precision", 2);
        field.put("required", column.getIsRequired() != null && column.getIsRequired() == 1);
        field.put("defaultValue", null);
        field.put("searchable", column.getIsQuery() != null && column.getIsQuery() == 1);
        field.put("listVisible", column.getIsList() == null || column.getIsList() == 1);
        field.put("formVisible", (column.getIsInsert() == null || column.getIsInsert() == 1)
                || (column.getIsEdit() == null || column.getIsEdit() == 1));
        field.put("componentType", componentType);
        field.put("queryType", resolveLowcodeQueryType(column));
        field.put("dictType", StringUtils.defaultString(column.getDictType()));
        field.put("sensitiveType", StringUtils.defaultIfBlank(column.getDesensitizeType(), "NONE"));
        field.put("encryptAlgorithm", "");
        field.put("sortable", false);
        field.put("width", 160);
        field.put("remark", StringUtils.defaultString(column.getColumnComment()));
        return field;
    }

    private List<Map<String, Object>> buildPageZones(List<Map<String, Object>> fields) {
        List<Map<String, Object>> zones = new ArrayList<>();
        zones.add(buildPageZone("search", "search-form", true, fields.stream()
                .filter(field -> Boolean.TRUE.equals(field.get("searchable")))
                .map(field -> String.valueOf(field.get("field")))
                .toList(), Map.of()));
        zones.add(buildPageZone("table", "data-table", true, fields.stream()
                .filter(field -> !Boolean.FALSE.equals(field.get("listVisible")))
                .map(field -> String.valueOf(field.get("field")))
                .toList(), Map.of(
                        "showImport", true,
                        "showExport", true,
                        "hideBatchDelete", false,
                        "enableCustomQuery", true
                )));
        zones.add(buildPageZone("edit", "edit-form", true, fields.stream()
                .filter(field -> !Boolean.FALSE.equals(field.get("formVisible")))
                .map(field -> String.valueOf(field.get("field")))
                .toList(), Map.of()));
        return zones;
    }

    private Map<String, Object> buildPageZone(String zoneKey,
                                              String componentKey,
                                              boolean enabled,
                                              List<String> fieldRefs,
                                              Map<String, Object> props) {
        Map<String, Object> zone = new LinkedHashMap<>();
        zone.put("zoneKey", zoneKey);
        zone.put("componentKey", componentKey);
        zone.put("enabled", enabled);
        zone.put("fieldRefs", fieldRefs);
        zone.put("props", props);
        return zone;
    }

    private boolean isBaseColumn(GenTableColumn column) {
        return SetHolder.BASE_COLUMNS.contains(column.getColumnName());
    }

    private boolean isPrimaryKey(GenTableColumn column) {
        return column.getIsPk() != null && column.getIsPk() == 1;
    }

    private String resolveJavaField(GenTableColumn column) {
        if (StringUtils.isNotBlank(column.getJavaField())) {
            return column.getJavaField();
        }
        return DynamicQueryGenerator.snakeToCamel(column.getColumnName());
    }

    private String resolveLowcodeDataType(GenTableColumn column) {
        String columnType = StringUtils.defaultString(column.getColumnType()).toLowerCase();
        if (columnType.startsWith("bigint")) {
            return "bigint";
        }
        if (columnType.startsWith("int")) {
            return "int";
        }
        if (columnType.startsWith("tinyint")) {
            return "tinyint";
        }
        if (columnType.startsWith("decimal") || columnType.startsWith("double") || columnType.startsWith("float")) {
            return "decimal";
        }
        if (columnType.startsWith("date") && !columnType.startsWith("datetime")) {
            return "date";
        }
        if (columnType.startsWith("datetime") || columnType.startsWith("timestamp")) {
            return "datetime";
        }
        if (columnType.startsWith("time")) {
            return "time";
        }
        if (columnType.contains("text")) {
            return "text";
        }
        return "varchar";
    }

    private String resolveLowcodeComponentType(GenTableColumn column) {
        String htmlType = StringUtils.defaultString(column.getHtmlType()).toUpperCase();
        return switch (htmlType) {
            case "TEXTAREA" -> "textarea";
            case "SELECT" -> "select";
            case "RADIO" -> "radio";
            case "CHECKBOX" -> "checkbox";
            case "SWITCH" -> "switch";
            case "DATE" -> "date";
            case "DATETIME" -> "datetime";
            case "NUMBER" -> "number";
            case "IMAGEUPLOAD" -> "imageUpload";
            case "FILEUPLOAD", "UPLOAD" -> "fileUpload";
            case "TREESELECT" -> "treeSelect";
            case "CASCADER" -> "cascader";
            default -> inferComponentFromColumnType(column);
        };
    }

    private String inferComponentFromColumnType(GenTableColumn column) {
        String dataType = resolveLowcodeDataType(column);
        return switch (dataType) {
            case "int", "bigint", "decimal", "tinyint" -> "number";
            case "text", "longtext" -> "textarea";
            case "date" -> "date";
            case "datetime" -> "datetime";
            case "time" -> "time";
            default -> StringUtils.isNotBlank(column.getDictType()) ? "select" : "input";
        };
    }

    private String resolveLowcodeQueryType(GenTableColumn column) {
        String queryType = StringUtils.defaultString(column.getQueryType()).toUpperCase();
        return switch (queryType) {
            case "LIKE" -> "like";
            case "GT" -> "gt";
            case "GTE", "GE" -> "ge";
            case "LT" -> "lt";
            case "LTE", "LE" -> "le";
            case "IN" -> "in";
            case "BETWEEN" -> "between";
            case "NE" -> "ne";
            default -> "eq";
        };
    }

    private Integer resolveColumnLength(String columnType) {
        if (StringUtils.isBlank(columnType) || !columnType.contains("(") || !columnType.contains(")")) {
            return 128;
        }
        String lengthText = columnType.substring(columnType.indexOf('(') + 1, columnType.indexOf(')')).split(",")[0];
        try {
            return Integer.valueOf(lengthText.trim());
        } catch (NumberFormatException e) {
            return 128;
        }
    }

    private String inferLabelField(List<Map<String, Object>> fields) {
        return fields.stream()
                .map(field -> String.valueOf(field.get("field")))
                .filter(field -> "name".equals(field) || "title".equals(field) || "label".equals(field))
                .findFirst()
                .orElseGet(() -> fields.isEmpty() ? "name" : String.valueOf(fields.get(0).get("field")));
    }

    private record LowcodeProtocol(Map<String, Object> modelSchema,
                                   Map<String, Object> pageSchema,
                                   Map<String, Object> options) {
    }

    private static class SetHolder {
        private static final java.util.Set<String> BASE_COLUMNS = java.util.Set.of(
                "id", "tenant_id", "create_by", "create_time", "create_dept", "update_by", "update_time", "del_flag"
        );
    }
}
