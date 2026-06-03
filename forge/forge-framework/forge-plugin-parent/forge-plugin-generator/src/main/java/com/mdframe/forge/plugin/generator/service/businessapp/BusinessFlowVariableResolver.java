package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectQueryDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowVariableCandidateVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowVariableMappingSuggestionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 业务流程变量候选项解析器。
 */
@Service
@RequiredArgsConstructor
public class BusinessFlowVariableResolver {

    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\$\\{\\s*([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern SIMPLE_IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
    private static final Set<String> IGNORED_IDENTIFIERS = Set.of(
            "true", "false", "null", "and", "or", "not", "eq", "ne", "gt", "ge", "lt", "le"
    );

    @Autowired(required = false)
    private FlowClient flowClient;

    private final BusinessObjectService objectService;
    private final AiCrudConfigMapper crudConfigMapper;
    private final ObjectMapper objectMapper;

    public Map<String, Object> resolve(String modelKey, String objectCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        Map<String, BusinessFlowVariableCandidateVO> variables = new LinkedHashMap<>();

        addBuiltInVariables(variables);
        Map<String, Object> model = loadFlowModel(modelKey, warnings);
        parseBpmnVariables(text(model.get("bpmnXml")), variables, warnings);
        parseFormVariables(text(model.get("formJson")), variables, warnings);

        List<Map<String, Object>> fieldCandidates = collectFieldCandidates(objectCode, warnings);
        List<BusinessFlowVariableMappingSuggestionVO> suggestions = buildSuggestions(
                new ArrayList<>(variables.values()), fieldCandidates);

        result.put("modelKey", modelKey);
        result.put("modelName", text(model.get("modelName")));
        result.put("flowVariables", new ArrayList<>(variables.values()));
        result.put("fieldCandidates", fieldCandidates);
        result.put("mappingSuggestions", suggestions);
        result.put("warnings", warnings);
        return result;
    }

    private Map<String, Object> loadFlowModel(String modelKey, List<String> warnings) {
        if (StringUtils.isBlank(modelKey)) {
            warnings.add("流程模型 Key 为空，仅返回内置变量");
            return new LinkedHashMap<>();
        }
        if (flowClient == null) {
            warnings.add("流程服务客户端未配置，仅返回内置变量");
            return new LinkedHashMap<>();
        }
        try {
            FlowResult<Map<String, Object>> response = flowClient.getModelByKey(modelKey);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                warnings.add("流程模型详情获取失败: " + (response == null ? "无返回结果" : response.getMsg()));
                return new LinkedHashMap<>();
            }
            return response.getData();
        } catch (Exception e) {
            warnings.add("流程模型详情获取失败: " + e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private void addBuiltInVariables(Map<String, BusinessFlowVariableCandidateVO> variables) {
        addVariable(variables, "initiator", "发起人", "BUILT_IN", "内置变量", "string", null, "流程发起人", true, false);
        addVariable(variables, "startUserId", "发起人ID", "BUILT_IN", "内置变量", "string", null, "流程发起人用户 ID", true, false);
        addVariable(variables, "businessKey", "业务Key", "BUILT_IN", "内置变量", "string", null, "objectCode:recordId", true, true);
        addVariable(variables, "recordId", "记录ID", "BUILT_IN", "内置变量", "long", null, "低代码业务记录 ID", true, true);
        addVariable(variables, "objectCode", "对象编码", "BUILT_IN", "内置变量", "string", null, "业务对象编码", true, true);
        addVariable(variables, "deptId", "部门ID", "BUILT_IN", "内置变量", "string", null, "发起人部门 ID", true, false);
        addVariable(variables, "deptManager", "部门负责人", "BUILT_IN", "内置变量", "string", null, "常用于审批人表达式", true, false);
    }

    private void parseBpmnVariables(String bpmnXml,
                                    Map<String, BusinessFlowVariableCandidateVO> variables,
                                    List<String> warnings) {
        if (StringUtils.isBlank(bpmnXml)) {
            warnings.add("流程模型缺少 BPMN XML，无法解析条件表达式变量");
            return;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(bpmnXml)));
            traverseBpmnNode(document.getDocumentElement(), variables);
        } catch (Exception e) {
            warnings.add("BPMN XML 解析失败，仅返回内置变量和表单变量: " + e.getMessage());
        }
    }

    private void traverseBpmnNode(Node node, Map<String, BusinessFlowVariableCandidateVO> variables) {
        if (node == null) {
            return;
        }
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                extractTemplateVariables(attr.getNodeValue(), "BPMN", attr.getNodeName(), variables);
                if (isAssigneeAttribute(attr.getNodeName()) && SIMPLE_IDENTIFIER_PATTERN.matcher(attr.getNodeValue()).matches()) {
                    addVariable(variables, attr.getNodeValue(), attr.getNodeValue(), "BPMN", "审批人配置",
                            "string", attr.getNodeName(), "从 BPMN 审批人/候选人配置解析", false, true);
                }
            }
        }
        if (node.getNodeType() == Node.TEXT_NODE) {
            extractTemplateVariables(node.getNodeValue(), "BPMN", "conditionExpression", variables);
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            traverseBpmnNode(child, variables);
        }
    }

    private boolean isAssigneeAttribute(String name) {
        String normalized = StringUtils.defaultString(name).toLowerCase(Locale.ROOT);
        return normalized.endsWith("assignee")
                || normalized.endsWith("candidateusers")
                || normalized.endsWith("candidategroups");
    }

    private void extractTemplateVariables(String text,
                                          String source,
                                          String expression,
                                          Map<String, BusinessFlowVariableCandidateVO> variables) {
        if (StringUtils.isBlank(text)) {
            return;
        }
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(text);
        while (matcher.find()) {
            String variable = matcher.group(1);
            if (StringUtils.isNotBlank(variable) && !IGNORED_IDENTIFIERS.contains(variable.toLowerCase(Locale.ROOT))) {
                addVariable(variables, variable, variable, source, "表达式变量", "string", expression,
                        "从流程表达式解析", false, true);
            }
        }
    }

    private void parseFormVariables(String formJson,
                                    Map<String, BusinessFlowVariableCandidateVO> variables,
                                    List<String> warnings) {
        if (StringUtils.isBlank(formJson)) {
            return;
        }
        try {
            Object form = objectMapper.readValue(formJson, Object.class);
            collectFormVariables(form, variables);
        } catch (Exception e) {
            warnings.add("流程动态表单解析失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void collectFormVariables(Object node, Map<String, BusinessFlowVariableCandidateVO> variables) {
        if (node instanceof Map<?, ?> map) {
            String field = firstText(map, "field", "fieldCode", "prop", "name", "model");
            if (StringUtils.isNotBlank(field) && SIMPLE_IDENTIFIER_PATTERN.matcher(field).matches()) {
                String label = StringUtils.defaultIfBlank(firstText(map, "label", "title", "fieldName"), field);
                addVariable(variables, field, label, "FORM", "流程表单", "string", null, "从流程动态表单解析", false, false);
            }
            map.values().forEach(value -> collectFormVariables(value, variables));
        } else if (node instanceof List<?> list) {
            list.forEach(item -> collectFormVariables(item, variables));
        }
    }

    private List<Map<String, Object>> collectFieldCandidates(String objectCode, List<String> warnings) {
        if (StringUtils.isBlank(objectCode)) {
            return List.of();
        }
        try {
            BusinessObjectQueryDTO query = new BusinessObjectQueryDTO();
            query.setObjectCode(objectCode);
            List<BusinessObjectVO> objects = objectService.list(query);
            if (objects == null || objects.isEmpty()) {
                warnings.add("业务对象不存在或无权限访问: " + objectCode);
                return List.of();
            }
            String configKey = objects.get(0).getConfigKey();
            if (StringUtils.isBlank(configKey)) {
                warnings.add("业务对象缺少运行配置，无法读取字段候选项: " + objectCode);
                return List.of();
            }
            AiCrudConfig config = crudConfigMapper.selectByConfigKey(resolveTenantId(), configKey);
            if (config == null || StringUtils.isBlank(config.getModelSchema())) {
                warnings.add("业务对象模型配置不存在: " + objectCode);
                return List.of();
            }
            LowcodeModelSchema modelSchema = objectMapper.readValue(config.getModelSchema(), LowcodeModelSchema.class);
            List<Map<String, Object>> fields = new ArrayList<>();
            if (modelSchema.getFields() != null) {
                for (LowcodeFieldSchema field : modelSchema.getFields()) {
                    if (field == null || StringUtils.isBlank(field.getField()) || Boolean.TRUE.equals(field.getSystemField())) {
                        continue;
                    }
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("fieldCode", field.getField());
                    item.put("columnName", field.getColumnName());
                    item.put("fieldLabel", StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
                    item.put("dataType", field.getDataType());
                    item.put("dictType", field.getDictType());
                    fields.add(item);
                }
            }
            return fields;
        } catch (Exception e) {
            warnings.add("业务对象字段解析失败: " + e.getMessage());
            return List.of();
        }
    }

    private List<BusinessFlowVariableMappingSuggestionVO> buildSuggestions(
            List<BusinessFlowVariableCandidateVO> variables,
            List<Map<String, Object>> fields) {
        List<BusinessFlowVariableMappingSuggestionVO> suggestions = new ArrayList<>();
        Set<String> used = new LinkedHashSet<>();
        for (Map<String, Object> field : fields) {
            String fieldCode = text(field.get("fieldCode"));
            String fieldLabel = text(field.get("fieldLabel"));
            BusinessFlowVariableCandidateVO best = null;
            int bestScore = 0;
            for (BusinessFlowVariableCandidateVO variable : variables) {
                int score = scoreMapping(fieldCode, fieldLabel, variable.getVariableName(), variable.getDisplayName());
                if (score > bestScore) {
                    bestScore = score;
                    best = variable;
                }
            }
            if (best != null && bestScore >= 70 && used.add(best.getVariableName())) {
                BusinessFlowVariableMappingSuggestionVO suggestion = new BusinessFlowVariableMappingSuggestionVO();
                suggestion.setFormField(fieldCode);
                suggestion.setFieldLabel(fieldLabel);
                suggestion.setFlowVariable(best.getVariableName());
                suggestion.setVariableDisplayName(best.getDisplayName());
                suggestion.setConfidence(bestScore);
                suggestion.setReason(bestScore >= 95 ? "字段编码完全匹配" : "字段编码或名称相似");
                suggestions.add(suggestion);
            }
        }
        return suggestions;
    }

    private int scoreMapping(String fieldCode, String fieldLabel, String variableName, String variableLabel) {
        String fieldKey = normalizeKey(fieldCode);
        String fieldName = normalizeKey(fieldLabel);
        String variableKey = normalizeKey(variableName);
        String variableDisplay = normalizeKey(variableLabel);
        if (StringUtils.isBlank(fieldKey) || StringUtils.isBlank(variableKey)) {
            return 0;
        }
        if (fieldKey.equals(variableKey)) {
            return 100;
        }
        if (normalizeKey(camelToSnake(fieldCode)).equals(variableKey) || normalizeKey(snakeToCamel(fieldCode)).equals(variableKey)) {
            return 96;
        }
        if (fieldKey.contains(variableKey) || variableKey.contains(fieldKey)) {
            return 84;
        }
        if (StringUtils.isNotBlank(fieldName) && (fieldName.equals(variableDisplay) || fieldName.contains(variableDisplay))) {
            return 78;
        }
        if ("deptmanager".equals(variableKey) && (fieldKey.contains("owner") || fieldKey.contains("manager"))) {
            return 76;
        }
        return 0;
    }

    private void addVariable(Map<String, BusinessFlowVariableCandidateVO> variables,
                             String variableName,
                             String displayName,
                             String source,
                             String sourceLabel,
                             String dataType,
                             String expression,
                             String description,
                             Boolean builtIn,
                             Boolean required) {
        if (StringUtils.isBlank(variableName) || variables.containsKey(variableName)) {
            return;
        }
        BusinessFlowVariableCandidateVO vo = new BusinessFlowVariableCandidateVO();
        vo.setVariableName(variableName);
        vo.setDisplayName(StringUtils.defaultIfBlank(displayName, variableName));
        vo.setSource(source);
        vo.setSourceLabel(sourceLabel);
        vo.setDataType(StringUtils.defaultIfBlank(dataType, "string"));
        vo.setExpression(expression);
        vo.setDescription(description);
        vo.setBuiltIn(Boolean.TRUE.equals(builtIn));
        vo.setRequired(Boolean.TRUE.equals(required));
        variables.put(variableName, vo);
    }

    private String firstText(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private String normalizeKey(String value) {
        return StringUtils.defaultString(value).replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private String snakeToCamel(String value) {
        if (StringUtils.isBlank(value) || !value.contains("_")) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            result.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return result.toString();
    }

    private String camelToSnake(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        for (char ch : value.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                result.append('_').append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }
}
