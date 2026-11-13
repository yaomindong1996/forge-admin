package com.mdframe.forge.plugin.generator.service.businessapp.taskform;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将低代码 formDesignerSchema + 节点权限编译为审批端统一 UI 文档（SAP uiData 风格）。
 * Phase 1：产出 sections + components；不删除旧 fields 协议。
 */
public final class TaskFormUiDocumentCompiler {

    public static final String PROTOCOL_VERSION = "1";

    private TaskFormUiDocumentCompiler() {
    }

    public static Map<String, Object> compile(JSONObject formSchema,
                                              String formKey,
                                              List<Map<String, Object>> resolvedFields,
                                              List<Map<String, Object>> fieldPermissions) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("version", PROTOCOL_VERSION);
        doc.put("uiType", "business-object");
        doc.put("formKey", StringUtils.trimToEmpty(formKey));

        Map<String, Permission> permissionMap = indexPermissions(fieldPermissions);
        List<Map<String, Object>> components = compileComponents(formSchema, permissionMap);
        List<Map<String, Object>> sections = compileSections(formSchema, resolvedFields, components);
        doc.put("sections", sections);
        doc.put("components", components);
        doc.put("actions", List.of());
        return doc;
    }

    private static List<Map<String, Object>> compileSections(JSONObject formSchema,
                                                             List<Map<String, Object>> resolvedFields,
                                                             List<Map<String, Object>> components) {
        JSONArray pageSections = readArray(formSchema == null ? null : formSchema.get("pageSections"));
        if (pageSections != null && !pageSections.isEmpty()) {
            List<Map<String, Object>> sections = new ArrayList<>();
            for (int i = 0; i < pageSections.size(); i++) {
                JSONObject raw = pageSections.getJSONObject(i);
                if (raw == null) {
                    continue;
                }
                Map<String, Object> section = new LinkedHashMap<>();
                section.put("type", "Section");
                section.put("sectionId", firstNonBlank(raw.getString("sectionId"), "section_" + (i + 1)));
                section.put("sectionType", firstNonBlank(raw.getString("sectionType"), "card"));
                section.put("title", StringUtils.defaultString(raw.getString("title")));
                section.put("fields", copyStringList(raw.get("fields")));
                if (raw.get("fieldOverrides") instanceof Map<?, ?> overrides) {
                    section.put("fieldOverrides", new LinkedHashMap<>(castMap(overrides)));
                } else {
                    section.put("fieldOverrides", Map.of());
                }
                section.put("collapsible", Boolean.TRUE.equals(raw.getBoolean("collapsible")));
                section.put("collapsedByDefault", Boolean.TRUE.equals(raw.getBoolean("collapsedByDefault")));
                sections.add(section);
            }
            if (!sections.isEmpty()) {
                appendUnassignedFields(sections, defaultSection(resolvedFields, components));
                return sections;
            }
        }
        return List.of(defaultSection(resolvedFields, components));
    }

    /**
     * 设计态改字段编码后分区引用可能停在旧编码，未被任何分区引用的主表字段补进第一个内容分区，
     * 避免待办表单整块丢字段。
     */
    private static void appendUnassignedFields(List<Map<String, Object>> sections, Map<String, Object> allFields) {
        Set<String> referenced = new LinkedHashSet<>();
        for (Map<String, Object> section : sections) {
            referenced.addAll(copyStringList(section.get("fields")));
        }
        List<String> missing = copyStringList(allFields.get("fields")).stream()
                .filter(code -> !referenced.contains(code))
                .toList();
        if (missing.isEmpty()) {
            return;
        }
        Map<String, Object> target = sections.stream()
                .filter(section -> !"child_table".equals(section.get("sectionType")))
                .findFirst()
                .orElse(null);
        if (target == null) {
            Map<String, Object> fallback = new LinkedHashMap<>(allFields);
            fallback.put("fields", missing);
            sections.add(0, fallback);
            return;
        }
        List<String> merged = copyStringList(target.get("fields"));
        merged.addAll(missing);
        target.put("fields", merged);
    }

    private static Map<String, Object> defaultSection(List<Map<String, Object>> resolvedFields,
                                                      List<Map<String, Object>> components) {
        List<String> fieldCodes = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (resolvedFields != null) {
            for (Map<String, Object> field : resolvedFields) {
                if (field == null) {
                    continue;
                }
                // 子表字段不进主分区
                if (StringUtils.isNotBlank(text(field.get("childKey")))) {
                    continue;
                }
                String code = firstNonBlank(text(field.get("field")), text(field.get("fieldCode")));
                if (code != null && seen.add(code)) {
                    fieldCodes.add(code);
                }
            }
        }
        if (fieldCodes.isEmpty()) {
            for (Map<String, Object> component : components) {
                String code = firstNonBlank(text(component.get("field")), text(component.get("fieldCode")));
                if (code != null && seen.add(code)) {
                    fieldCodes.add(code);
                }
            }
        }
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("type", "Section");
        section.put("sectionId", "main");
        section.put("sectionType", "card");
        section.put("title", "");
        section.put("fields", fieldCodes);
        section.put("fieldOverrides", Map.of());
        section.put("collapsible", false);
        section.put("collapsedByDefault", false);
        return section;
    }

    private static List<Map<String, Object>> compileComponents(JSONObject formSchema,
                                                               Map<String, Permission> permissionMap) {
        List<Map<String, Object>> result = new ArrayList<>();
        JSONArray components = readArray(formSchema == null ? null : formSchema.get("components"));
        if (components == null || components.isEmpty()) {
            JSONObject settings = readObject(formSchema == null ? null : formSchema.get("settings"));
            components = readArray(settings.get("components"));
        }
        walkComponents(components, result, permissionMap);
        return result;
    }

    private static void walkComponents(JSONArray components,
                                       List<Map<String, Object>> out,
                                       Map<String, Permission> permissionMap) {
        if (components == null) {
            return;
        }
        for (int i = 0; i < components.size(); i++) {
            JSONObject raw = components.getJSONObject(i);
            if (raw == null) {
                continue;
            }
            String componentKey = StringUtils.firstNonBlank(
                    raw.getString("componentKey"),
                    raw.getString("type"),
                    raw.getString("componentType"));
            // 子表由 childrenConfig 单独渲染，不进入主表单 AiForm 树
            if ("subTable".equalsIgnoreCase(componentKey) || "childTable".equalsIgnoreCase(componentKey)) {
                continue;
            }
            Map<String, Object> node = compileComponentNode(raw, permissionMap);
            out.add(node);
            JSONArray children = readArray(raw.get("children"));
            if (children != null && !children.isEmpty()) {
                List<Map<String, Object>> childNodes = new ArrayList<>();
                walkComponents(children, childNodes, permissionMap);
                if (!childNodes.isEmpty()) {
                    node.put("children", childNodes);
                }
            }
        }
    }

    private static Map<String, Object> compileComponentNode(JSONObject raw,
                                                            Map<String, Permission> permissionMap) {
        Map<String, Object> node = new LinkedHashMap<>();
        JSONObject binding = readObject(raw.get("fieldBinding"));
        JSONObject props = readObject(raw.get("props"));
        JSONObject validation = readObject(raw.get("validation"));
        String field = firstNonBlank(
                binding.getString("fieldCode"),
                raw.getString("field"),
                props.getString("field"));
        String type = firstNonBlank(
                raw.getString("componentKey"),
                raw.getString("type"),
                raw.getString("componentType"),
                "input");
        node.put("type", type);
        if (StringUtils.isNotBlank(raw.getString("componentKey"))) {
            node.put("componentKey", raw.getString("componentKey"));
        }
        node.put("id", firstNonBlank(raw.getString("id"), field));
        if (field != null) {
            node.put("field", field);
            node.put("fieldCode", field);
            node.put("path", field);
        }
        node.put("label", firstNonBlank(
                raw.getString("label"),
                props.getString("label"),
                props.getString("title"),
                field));
        boolean required = Boolean.TRUE.equals(validation.getBoolean("required"));
        node.put("required", required);

        Permission permission = field == null ? null : permissionMap.get(field);
        boolean visible = permission == null || permission.visible;
        boolean editable = permission != null && permission.writable;
        // 无显式权限时：沿用设计器，不强制可写（审批默认偏只读）
        if (permission == null) {
            editable = false;
            visible = true;
        }
        node.put("visible", visible);
        node.put("editable", editable);
        if (StringUtils.isNotBlank(props.getString("dictType"))) {
            node.put("dictType", props.getString("dictType"));
        }
        if (!props.isEmpty()) {
            node.put("props", new LinkedHashMap<>(props));
        }
        // 轻量布局扩展（可选）：与前端 compileUiDocument 对齐，渲染端不认识可忽略
        JSONObject layout = readObject(raw.get("layout"));
        if (layout.get("span") != null) {
            node.put("span", layout.get("span"));
        }
        if (layout.get("gridStyle") != null) {
            node.put("gridStyle", layout.get("gridStyle"));
        }
        if (StringUtils.isNotBlank(layout.getString("align"))) {
            node.put("align", layout.getString("align"));
        }
        return node;
    }

    private static Map<String, Permission> indexPermissions(List<Map<String, Object>> fieldPermissions) {
        Map<String, Permission> map = new LinkedHashMap<>();
        if (fieldPermissions == null) {
            return map;
        }
        for (Map<String, Object> item : fieldPermissions) {
            if (item == null) {
                continue;
            }
            String field = firstNonBlank(text(item.get("field")), text(item.get("fieldCode")));
            if (field == null) {
                continue;
            }
            boolean readable = !Boolean.FALSE.equals(asBoolean(item.get("readable")))
                    && !Boolean.FALSE.equals(asBoolean(item.get("visible")));
            boolean writable = Boolean.TRUE.equals(asBoolean(item.get("writable")));
            map.put(field, new Permission(readable, writable));
        }
        return map;
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
            return false;
        }
        return null;
    }

    private static List<String> copyStringList(Object raw) {
        List<String> result = new ArrayList<>();
        if (!(raw instanceof List<?> list)) {
            return result;
        }
        for (Object item : list) {
            String value = text(item);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        return result;
    }

    private static JSONObject readObject(Object value) {
        if (value instanceof JSONObject json) {
            return json;
        }
        if (value instanceof Map<?, ?> map) {
            return new JSONObject(castMap(map));
        }
        return new JSONObject();
    }

    private static JSONArray readArray(Object value) {
        if (value instanceof JSONArray array) {
            return array;
        }
        if (value instanceof List<?> list) {
            return JSONArray.from(list);
        }
        return new JSONArray();
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private record Permission(boolean visible, boolean writable) {
    }
}
