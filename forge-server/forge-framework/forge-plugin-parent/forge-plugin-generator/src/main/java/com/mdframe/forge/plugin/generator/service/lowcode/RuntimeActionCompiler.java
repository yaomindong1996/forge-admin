package com.mdframe.forge.plugin.generator.service.lowcode;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Compiles table, detail and form actions from the merged table settings. */
final class RuntimeActionCompiler {

    private RuntimeActionCompiler() {
    }

    static List<Map<String, Object>> rowActions(Map<String, Object> tableProps, boolean treeRuntime) {
        List<Map<String, Object>> actions = new ArrayList<>(builtinRowActions(tableProps, treeRuntime));
        Set<String> existingKeys = actions.stream()
                .map(action -> text(action.get("key")))
                .collect(Collectors.toSet());
        customActions(tableProps, "row").stream()
                .filter(action -> !isBuiltinRowActionKey(text(action.get("key"))))
                .filter(action -> !existingKeys.contains(text(action.get("key"))))
                .forEach(actions::add);
        return actions;
    }

    /**
     * 读取设计器保存的 builtinRowActions（可改名/隐藏）；未配置时回退默认三项。
     */
    static List<Map<String, Object>> builtinRowActions(Map<String, Object> tableProps, boolean treeRuntime) {
        Map<String, Map<String, Object>> overrides = indexBuiltinRowActionOverrides(tableProps);
        List<Map<String, Object>> defaults = new ArrayList<>();
        defaults.add(defaultAction("edit", "编辑", "primary"));
        defaults.add(defaultAction("detail", "查看详情", "info"));
        if (treeRuntime) {
            defaults.add(defaultAction("addChild", "添加下级", "success"));
        }
        defaults.add(defaultAction("delete", "删除", "error"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : defaults) {
            String key = text(item.get("key"));
            Map<String, Object> override = overrides.get(StringUtils.lowerCase(key));
            if (override != null && Boolean.FALSE.equals(override.get("visible"))) {
                continue;
            }
            Map<String, Object> action = new LinkedHashMap<>(item);
            if (override != null) {
                String label = text(override.get("label"));
                if (StringUtils.isNotBlank(label)) {
                    action.put("label", label);
                }
                String type = text(override.get("type"));
                if (StringUtils.isNotBlank(type)) {
                    action.put("type", type);
                }
            }
            result.add(action);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> indexBuiltinRowActionOverrides(Map<String, Object> tableProps) {
        Map<String, Map<String, Object>> overrides = new LinkedHashMap<>();
        if (tableProps == null) {
            return overrides;
        }
        Object value = tableProps.get("builtinRowActions");
        if (!(value instanceof List<?> list)) {
            return overrides;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> source)) {
                continue;
            }
            String key = text(source.get("key"));
            if (!isBuiltinRowActionKey(key)) {
                continue;
            }
            overrides.put(StringUtils.lowerCase(key), new LinkedHashMap<>((Map<String, Object>) source));
        }
        return overrides;
    }

    private static boolean isBuiltinRowActionKey(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        String normalized = key.trim();
        return "edit".equalsIgnoreCase(normalized)
                || "detail".equalsIgnoreCase(normalized)
                || "delete".equalsIgnoreCase(normalized)
                || "addChild".equalsIgnoreCase(normalized);
    }

    static List<Map<String, Object>> customActions(Map<String, Object> tableProps, String position) {
        Object value = tableProps.get("customActions");
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> source)) {
                continue;
            }
            // 与设计器默认一致：缺省视为行操作，避免自定义按钮被误分到工具栏后列表看不到
            String actionPosition = StringUtils.defaultIfBlank(text(source.get("position")), "row");
            if (!position.equals(actionPosition)) {
                continue;
            }
            if (Boolean.FALSE.equals(source.get("visible"))) {
                continue;
            }
            String key = StringUtils.defaultIfBlank(text(source.get("key")), "custom_" + result.size());
            String label = StringUtils.defaultIfBlank(text(source.get("label")), "自定义按钮");
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("key", key);
            action.put("label", label);
            action.put("type", StringUtils.defaultIfBlank(text(source.get("type")), "default"));
            action.put("position", position);
            action.put("actionType", StringUtils.defaultIfBlank(text(source.get("actionType")), "route"));
            action.put("visible", true);
            putIfNotBlank(action, "actionCode", text(source.get("actionCode")));
            putIfNotBlank(action, "suiteCode", text(source.get("suiteCode")));
            putIfNotBlank(action, "objectCode", text(source.get("objectCode")));
            putIfNotBlank(action, "businessObjectCode", text(source.get("businessObjectCode")));
            putIfNotBlank(action, "targetObjectCode", text(source.get("targetObjectCode")));
            putIfNotBlank(action, "targetEntityCode", text(source.get("targetEntityCode")));
            putIfNotBlank(action, "referenceObjectCode", text(source.get("referenceObjectCode")));
            Map<String, Object> actionConfig = mapValue(source.get("actionConfig"));
            if (!actionConfig.isEmpty()) {
                action.put("actionConfig", actionConfig);
            }
            putIfNotBlank(action, "routePath", text(source.get("routePath")));
            putIfNotBlank(action, "targetPageKey", firstNonBlank(
                    text(source.get("targetPageKey")), text(actionConfig.get("targetPageKey"))));
            putIfNotBlank(action, "targetFormKey", text(source.get("targetFormKey")));
            putIfNotBlank(action, "openTarget", StringUtils.defaultIfBlank(text(source.get("openTarget")), "_self"));
            putIfNotBlank(action, "permissionCode", text(source.get("permissionCode")));
            putIfNotBlank(action, "permissionKey", StringUtils.firstNonBlank(
                    text(source.get("permissionKey")), text(source.get("permissionCode"))));
            putIfNotBlank(action, "permissionStrategy", text(source.get("permissionStrategy")));
            putIfNotBlank(action, "confirmText", text(source.get("confirmText")));
            putIfNotBlank(action, "displayCondition", text(source.get("displayCondition")));
            putIfNotBlank(action, "successMessage", text(source.get("successMessage")));
            putIfNotBlank(action, "failureMessage", text(source.get("failureMessage")));
            putIfNotBlank(action, "successBehavior", text(source.get("successBehavior")));
            List<Map<String, Object>> params = actionParams(source.get("params"));
            if (!params.isEmpty()) {
                action.put("params", params);
            }
            result.add(action);
        }
        return result;
    }

    private static Map<String, Object> defaultAction(String key, String label, String type) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("key", key);
        action.put("label", label);
        action.put("type", type);
        action.put("position", "row");
        return action;
    }

    private static List<Map<String, Object>> actionParams(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> source)) {
                continue;
            }
            String name = text(source.get("name"));
            if (StringUtils.isBlank(name)) {
                continue;
            }
            Map<String, Object> param = new LinkedHashMap<>();
            param.put("name", name);
            putIfNotBlank(param, "target", text(source.get("target")));
            putIfNotBlank(param, "sourceType", text(source.get("sourceType")));
            putIfNotBlank(param, "sourceField", text(source.get("sourceField")));
            param.put("value", StringUtils.defaultString(text(source.get("value"))));
            result.add(param);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return new LinkedHashMap<>();
    }

    private static void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    private static String firstNonBlank(String... values) {
        return StringUtils.firstNonBlank(values);
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
