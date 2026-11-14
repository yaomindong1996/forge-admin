package com.mdframe.forge.plugin.generator.service.businessprocess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.businessprocess.schema.BusinessProcessNode;
import com.mdframe.forge.plugin.generator.businessprocess.schema.BusinessProcessSchema;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 将已发布手动开始节点编译为页面 START_PROCESS 动作。
 */
@Component
@RequiredArgsConstructor
public class BusinessProcessRuntimeActionCompiler {

    public static final String ACTION_TYPE = "START_PROCESS";
    public static final String DEFAULT_PERMISSION = "ai:businessProcess:start";
    /** 新建业务流程时开始节点的默认名称，不代表业务含义，不能作为按钮文案。 */
    public static final String DEFAULT_START_NODE_NAME = "手动开始";

    private static final Set<String> START_TYPES = Set.of("START_MANUAL");
    private static final List<String> DEFAULT_POSITIONS = List.of("ROW", "DETAIL");

    private final ObjectMapper objectMapper;

    /**
     * @param processNames 流程编码 → 流程名称；缺失时以流程编码兜底
     */
    public List<Map<String, Object>> compileSnapshots(
            Collection<BusinessProcessSnapshot> snapshots,
            String applicationCode,
            Map<String, String> processNames) {
        if (snapshots == null || snapshots.isEmpty() || StringUtils.isBlank(applicationCode)) {
            return List.of();
        }
        Map<String, String> names = processNames == null ? Map.of() : processNames;
        List<Map<String, Object>> actions = new ArrayList<>();
        for (BusinessProcessSnapshot snapshot : snapshots) {
            if (snapshot == null) {
                continue;
            }
            String processName = firstText(names.get(snapshot.processCode()), snapshot.processCode());
            actions.addAll(compileActions(snapshot, applicationCode, processName));
        }
        return List.copyOf(actions);
    }

    public List<Map<String, Object>> compileActions(
            BusinessProcessSnapshot snapshot,
            String applicationCode,
            String processName) {
        if (snapshot == null) {
            return List.of();
        }
        return compileSchema(
                toSchema(snapshot.businessProcessJson()),
                applicationCode,
                snapshot.processCode(),
                processName);
    }

    public List<Map<String, Object>> compileSchema(
            BusinessProcessSchema schema,
            String applicationCode,
            String processCode,
            String processName) {
        if (schema == null || StringUtils.isAnyBlank(applicationCode, processCode)) {
            return List.of();
        }
        BusinessProcessNode start = findManualStart(schema);
        if (start == null) {
            return List.of();
        }
        Map<String, Object> config = start.getConfig() == null ? Map.of() : start.getConfig();
        String objectCode = schema.getSubject() == null ? null : StringUtils.trimToNull(schema.getSubject().getObjectCode());
        // 手动开始节点的流程名称用于流程画布/时间线；运行时按钮文案单独配置，避免改按钮时污染节点语义。
        // 兼容历史草稿：未配置 buttonLabel 时继续使用原来的节点名称回退链路。
        String label = firstText(text(config.get("buttonLabel")), meaningfulStartName(start), processName, processCode, "启动流程");
        String permission = firstText(text(config.get("permission")), DEFAULT_PERMISSION);
        String confirmText = text(config.get("confirmText"));
        String visibleCondition = compileDisplayCondition(config.get("visibleCondition"));
        Set<String> positions = normalizePositions(config.get("positions"));
        List<Map<String, Object>> actions = new ArrayList<>();
        if (positions.contains("ROW")) {
            actions.add(action(
                    processCode,
                    applicationCode,
                    objectCode,
                    label,
                    "row",
                    permission,
                    confirmText,
                    visibleCondition));
        }
        if (positions.contains("DETAIL")) {
            actions.add(action(
                    processCode + ":detail",
                    applicationCode,
                    objectCode,
                    label,
                    "detail",
                    permission,
                    confirmText,
                    visibleCondition));
        }
        if (positions.contains("FORM")) {
            actions.add(action(
                    processCode + ":form",
                    applicationCode,
                    objectCode,
                    label,
                    "form",
                    permission,
                    confirmText,
                    visibleCondition));
        }
        if (positions.contains("TOOLBAR")) {
            actions.add(action(
                    processCode + ":toolbar",
                    applicationCode,
                    objectCode,
                    label,
                    "toolbar",
                    permission,
                    confirmText,
                    visibleCondition));
        }
        return List.copyOf(actions);
    }

    private Map<String, Object> action(
            String actionKey,
            String applicationCode,
            String objectCode,
            String label,
            String position,
            String permission,
            String confirmText,
            String visibleCondition) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("key", "startProcess:" + actionKey);
        action.put("label", label);
        action.put("type", "success");
        action.put("position", position);
        action.put("actionType", ACTION_TYPE);
        action.put("applicationCode", applicationCode);
        action.put("processCode", StringUtils.substringBefore(actionKey, ":"));
        if (StringUtils.isNotBlank(objectCode)) {
            action.put("objectCode", objectCode);
        }
        action.put("permission", permission);
        action.put("permissionCode", permission);
        if (StringUtils.isNotBlank(confirmText)) {
            action.put("confirm", true);
            action.put("confirmText", confirmText);
        }
        if (StringUtils.isNotBlank(visibleCondition)) {
            action.put("displayCondition", visibleCondition);
            action.put("visibleCondition", visibleCondition);
        }
        action.put("successBehavior", "refreshList");
        return action;
    }

    public String compileDisplayCondition(Object raw) {
        if (raw == null) {
            return "";
        }
        if (raw instanceof String text) {
            return text.trim();
        }
        if (!(raw instanceof Map<?, ?> map)) {
            return "";
        }
        Object rules = map.get("rules");
        if (!(rules instanceof List<?> list) || list.isEmpty()) {
            return "";
        }
        List<String> expressions = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> rule)) {
                continue;
            }
            String field = text(rule.get("field"));
            if (StringUtils.isBlank(field)) {
                continue;
            }
            String operator = upper(text(rule.get("operator")));
            String value = text(rule.get("value"));
            expressions.add(compileRuleExpression(field, operator, value));
        }
        if (expressions.isEmpty()) {
            return "";
        }
        String joiner = "OR".equalsIgnoreCase(text(map.get("operator")))
                || "ANY".equalsIgnoreCase(text(map.get("operator"))) ? " OR " : " AND ";
        return expressions.size() == 1 ? expressions.get(0) : String.join(joiner, expressions);
    }

    private String compileRuleExpression(String field, String operator, String value) {
        return switch (operator) {
            case "NE" -> field + " != " + value;
            case "IN" -> field + " in " + value;
            case "NOT_EMPTY" -> field + " != ";
            case "EMPTY" -> field + " = ";
            case "GT" -> field + " > " + value;
            case "GTE", "GE" -> field + " >= " + value;
            case "LT" -> field + " < " + value;
            case "LTE", "LE" -> field + " <= " + value;
            default -> field + " = " + value;
        };
    }

    public BusinessProcessSchema schemaFromJson(Map<String, Object> json) {
        return toSchema(json);
    }

    private BusinessProcessNode findManualStart(BusinessProcessSchema schema) {
        if (schema.getNodes() == null) {
            return null;
        }
        return schema.getNodes().stream()
                .filter(node -> node != null && START_TYPES.contains(upper(node.getType())))
                .findFirst()
                .orElse(null);
    }

    private String meaningfulStartName(BusinessProcessNode start) {
        String name = text(start.getName());
        return DEFAULT_START_NODE_NAME.equals(name) ? "" : name;
    }

    private Set<String> normalizePositions(Object raw) {
        Set<String> positions = new LinkedHashSet<>();
        if (raw instanceof Collection<?> collection) {
            for (Object item : collection) {
                String value = upper(text(item));
                if (StringUtils.isNotBlank(value)) {
                    positions.add(value);
                }
            }
        }
        else if (raw instanceof String text && StringUtils.isNotBlank(text)) {
            for (String item : text.split(",")) {
                String value = upper(item.trim());
                if (StringUtils.isNotBlank(value)) {
                    positions.add(value);
                }
            }
        }
        if (positions.isEmpty()) {
            positions.addAll(DEFAULT_POSITIONS);
        }
        return positions;
    }

    private BusinessProcessSchema toSchema(Map<String, Object> json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return objectMapper.convertValue(json, BusinessProcessSchema.class);
    }

    private String firstText(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String text(Object value) {
        return value == null ? "" : StringUtils.trimToEmpty(String.valueOf(value));
    }

    private String upper(String value) {
        return StringUtils.isBlank(value) ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
