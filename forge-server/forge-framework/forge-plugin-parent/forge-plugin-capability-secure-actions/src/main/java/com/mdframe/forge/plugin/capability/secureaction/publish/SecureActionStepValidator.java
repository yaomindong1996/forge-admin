package com.mdframe.forge.plugin.capability.secureaction.publish;

import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectActionVO;
import com.mdframe.forge.starter.core.exception.BusinessException;

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
        Map<String, Object> config = action == null ? null : action.getActionConfig();
        Object rawSteps = config == null ? null : config.get("steps");
        if (!(rawSteps instanceof List<?>)) {
            rawSteps = config == null ? null : config.get("stepList");
        }
        if (!(rawSteps instanceof List<?> steps) || steps.isEmpty()) {
            throw new BusinessException("受控业务动作缺少执行步骤");
        }
        validateContainers(config, 0);
    }

    private void validateStepList(List<?> steps, int depth) {
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
            if (!ALLOWED_STEP_TYPES.contains(type)) {
                throw new BusinessException("当前阶段禁止发布动作步骤: " + type);
            }
            validateContainers(step, depth + 1);
        }
    }

    private void validateContainers(Map<?, ?> source, int depth) {
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
                validateStepList(steps, depth);
            }
            else if (!DATA_CONTAINER_KEYS.contains(key) && value instanceof Map<?, ?> nested) {
                validateContainers(nested, depth + 1);
            }
        }
    }

    private String normalize(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            throw new BusinessException("受控业务动作步骤类型不能为空");
        }
        return String.valueOf(value)
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace('-', '_')
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}
