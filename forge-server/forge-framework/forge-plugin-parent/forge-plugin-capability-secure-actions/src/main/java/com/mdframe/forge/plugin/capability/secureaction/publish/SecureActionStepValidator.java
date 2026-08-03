package com.mdframe.forge.plugin.capability.secureaction.publish;

import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectActionVO;
import com.mdframe.forge.starter.core.exception.BusinessException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SecureActionStepValidator {

    public static final Set<String> ALLOWED_STEP_TYPES = Set.of("UPDATE_FIELD", "CREATE_RECORD");

    private static final Set<String> STEP_CONTAINER_KEYS = Set.of("steps", "stepList", "childSteps");
    private static final Set<String> DATA_CONTAINER_KEYS = Set.of(
            "fieldMapping", "fieldMappings", "fields", "params", "staticValues", "values", "data");
    private static final int MAX_NESTING_DEPTH = 16;

    public void validate(BusinessObjectActionVO action) {
        ValidationResult result = inspect(action);
        if (!result.publishable()) {
            throw new BusinessException(result.unavailableReason());
        }
    }

    public ValidationResult inspect(BusinessObjectActionVO action) {
        Set<String> stepTypes = new LinkedHashSet<>();
        try {
            validateInternal(action, stepTypes);
            return new ValidationResult(true, null, sortedStepTypes(stepTypes));
        }
        catch (BusinessException exception) {
            return new ValidationResult(
                    false,
                    unavailableReason(action, exception.getMessage()),
                    sortedStepTypes(stepTypes));
        }
    }

    private void validateInternal(BusinessObjectActionVO action, Set<String> stepTypes) {
        Map<String, Object> config = action == null ? null : action.getActionConfig();
        Object rawSteps = config == null ? null : config.get("steps");
        if (!(rawSteps instanceof List<?>)) {
            rawSteps = config == null ? null : config.get("stepList");
        }
        if (!(rawSteps instanceof List<?> steps) || steps.isEmpty()) {
            throw new BusinessException(missingStepsReason(action));
        }
        validateContainers(config, 0, stepTypes);
    }

    private String missingStepsReason(BusinessObjectActionVO action) {
        String actionType = action == null ? null : normalizeText(action.getActionType());
        if ("OPEN_PAGE".equals(actionType)) {
            return "属于页面操作（OPEN_PAGE），只负责打开表单或页面，不包含服务端可执行步骤";
        }
        if ("START_FLOW".equals(actionType) || "START_APPROVAL".equals(actionType)) {
            return "属于流程发起入口（" + actionType + "），应通过流程动作能力开放";
        }
        return "没有配置执行步骤";
    }

    private void validateStepList(List<?> steps, int depth, Set<String> stepTypes) {
        if (depth > 0) {
            throw new BusinessException("当前阶段禁止嵌套受控业务动作步骤");
        }
        if (steps.isEmpty()) {
            throw new BusinessException("受控业务动作包含空步骤容器");
        }
        for (Object rawStep : steps) {
            if (!(rawStep instanceof Map<?, ?> step)) {
                throw new BusinessException("受控业务动作包含无效步骤");
            }
            String type = normalize(step.get("stepType"));
            stepTypes.add(type);
            if (!ALLOWED_STEP_TYPES.contains(type)) {
                throw new BusinessException("当前阶段禁止发布动作步骤: " + type);
            }
            validateContainers(step, depth + 1, stepTypes);
        }
    }

    private void validateContainers(Map<?, ?> source, int depth, Set<String> stepTypes) {
        if (source == null) {
            return;
        }
        if (depth > MAX_NESTING_DEPTH) {
            throw new BusinessException("受控业务动作步骤嵌套过深");
        }
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (STEP_CONTAINER_KEYS.contains(key)) {
                if (!(value instanceof List<?> steps)) {
                    throw new BusinessException("受控业务动作步骤容器格式无效: " + key);
                }
                validateStepList(steps, depth, stepTypes);
            }
            else if (!DATA_CONTAINER_KEYS.contains(key) && value instanceof Map<?, ?> nested) {
                validateContainers(nested, depth + 1, stepTypes);
            }
        }
    }

    private String normalize(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            throw new BusinessException("受控业务动作步骤类型不能为空");
        }
        return normalizeText(value);
    }

    private String normalizeText(Object value) {
        return String.valueOf(value)
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace('-', '_')
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private List<String> sortedStepTypes(Set<String> stepTypes) {
        return stepTypes.stream().sorted().toList();
    }

    private String unavailableReason(BusinessObjectActionVO action, String detail) {
        String actionName = action == null ? null : action.getActionName();
        String actionCode = action == null ? null : action.getActionCode();
        String displayName = actionName == null || actionName.isBlank() ? actionCode : actionName;
        if (displayName == null || displayName.isBlank()) {
            displayName = "未命名动作";
        }
        String reason = detail == null || detail.isBlank() ? "执行步骤配置无效" : detail;
        String actionType = action == null ? null : normalizeText(action.getActionType());
        if ("OPEN_PAGE".equals(actionType) && reason.contains("页面操作")) {
            return "业务动作「" + displayName + "」" + reason
                    + "。如果要让外围系统创建申请并立即发起流程，请在能力注册时选择“流程动作 → 提交业务申请”；"
                    + "如果只需开放无流程的服务端处理，请新建业务自动化并配置受控执行步骤";
        }
        if (("START_FLOW".equals(actionType) || "START_APPROVAL".equals(actionType))
                && reason.contains("流程发起入口")) {
            return "业务动作「" + displayName + "」" + reason
                    + "。创建记录并发起流程请选择“提交业务申请”，已有记录发起流程请选择“发起已有记录流程”";
        }
        return "业务动作「" + displayName + "」" + reason
                + "。请在业务对象设计器的“自动化动作”中修正配置并重新发布业务对象；"
                + "开放平台当前仅支持更新字段（UPDATE_FIELD）和创建记录（CREATE_RECORD）步骤";
    }

    public record ValidationResult(
            boolean publishable,
            String unavailableReason,
            List<String> stepTypes) {
    }
}
