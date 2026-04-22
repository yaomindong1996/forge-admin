package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.dto.AiCrudGenerateRequest;
import com.mdframe.forge.plugin.generator.dto.AiCrudGenerateResult;
import com.mdframe.forge.plugin.generator.dto.SchemaGenerateResult;
import com.mdframe.forge.plugin.generator.mapper.GenTableColumnMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCrudConfigGenerateService {

    private final AiClientAdapter aiClientAdapter;
    private final SchemaGenerator schemaGenerator;
    private final GenTableColumnMapper genTableColumnMapper;
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

            if (StringUtils.isBlank(apiConfig)) {
                Map<String, String> defaultApiConfig = schemaGenerator.buildApiConfig(configKey);
                apiConfig = objectMapper.writeValueAsString(defaultApiConfig);
            }

            return AiCrudGenerateResult.ok(configKey, tableName, tableComment,
                    searchSchema, columnsSchema, editSchema, apiConfig,
                    dictConfig, desensitizeConfig, encryptConfig, transConfig);
        } catch (Exception e) {
            log.error("[AiCrudConfigGenerateService] AI响应解析失败", e);
            return AiCrudGenerateResult.fail("AI响应格式解析失败: " + e.getMessage());
        }
    }

    private AiCrudGenerateResult fallbackFromTable(String tableName, String configKey, List<GenTableColumn> columns) {
        try {
            SchemaGenerateResult schemaResult = schemaGenerator.generate(configKey, tableName, "", columns);
            return AiCrudGenerateResult.ok(configKey, tableName, "",
                    schemaResult.getSearchSchema(), schemaResult.getColumnsSchema(),
                    schemaResult.getEditSchema(), schemaResult.getApiConfig(),
                    "", "", "", "");
        } catch (Exception e) {
            log.error("[AiCrudConfigGenerateService] 规则引擎生成失败", e);
            return AiCrudGenerateResult.fail("规则引擎生成失败: " + e.getMessage());
        }
    }

    private String buildDescriptionPrompt(String description, String configKey) {
        return "根据以下描述生成CRUD配置：\n\n" + description +
                "\n\n请生成包含以下字段的JSON配置：\n" +
                "- tableName: 建议的表名（如有）\n" +
                "- tableComment: 表描述\n" +
                "- searchSchema: 搜索表单配置数组\n" +
                "- columnsSchema: 表格列配置数组\n" +
                "- editSchema: 编辑表单配置数组\n" +
                "- apiConfig: API配置对象\n\n" +
                "configKey: " + configKey + "\n\n" +
                "每个字段项需包含：field(字段名), label(标签), type(控件类型)\n" +
                "搜索类型：input/select/daterange\n" +
                "编辑类型：input/textarea/select/radio/checkbox/switch/date/datetime/number/upload\n" +
                "表格列需包含：key, title, dataIndex\n" +
                "有dictType的select类型字段请加上dictType属性\n" +
                "有手机号/身份证/邮箱/银行卡号字段时请生成desensitizeConfig脱敏配置\n" +
                "有dictType的字段请同时生成transConfig翻译配置\n" +
                "需要接口加解密的场景请生成encryptConfig配置\n" +
                "apiConfig格式：{list: 'get@/ai/crud/" + configKey + "/page', detail: 'get@/ai/crud/" + configKey + "/{id}', " +
                "create: 'post@/ai/crud/" + configKey + "', update: 'put@/ai/crud/" + configKey + "', delete: 'delete@/ai/crud/" + configKey + "/{id}'}\n" +
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
        sb.append("请生成包含以下字段的JSON配置：\n");
        sb.append("- tableName, tableComment, searchSchema, columnsSchema, editSchema, apiConfig\n\n");
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
}
