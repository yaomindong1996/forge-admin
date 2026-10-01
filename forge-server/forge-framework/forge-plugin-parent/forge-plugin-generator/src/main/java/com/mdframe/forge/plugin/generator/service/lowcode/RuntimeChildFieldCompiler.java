package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Selects and orders editable child fields without changing the shared edit-field renderer. */
final class RuntimeChildFieldCompiler {

    private RuntimeChildFieldCompiler() {
    }

    static List<Map<String, Object>> compile(LowcodePageModelRef ref,
                                             List<String> selectedEditRefs,
                                             String childFkField,
                                             Function<LowcodeFieldSchema, Map<String, Object>> editFieldRenderer) {
        if (ref.getFields() == null) {
            return List.of();
        }
        // Explicit child-panel selection wins over the edit zone's field refs.
        List<String> configuredFieldCodes = readConfiguredChildFieldCodes(ref);
        if (!configuredFieldCodes.isEmpty()) {
            Map<String, Integer> configuredOrder = new LinkedHashMap<>();
            for (int i = 0; i < configuredFieldCodes.size(); i++) {
                configuredOrder.putIfAbsent(configuredFieldCodes.get(i), i);
            }
            List<Map<String, Object>> configuredFields = new ArrayList<>();
            for (Map<String, Object> source : ref.getFields()) {
                LowcodeFieldSchema field = buildChildField(ref, source);
                if (field == null || !configuredOrder.containsKey(field.getField())
                        || !isChildEditFieldAllowed(field, childFkField)) {
                    continue;
                }
                Map<String, Object> item = editFieldRenderer.apply(field);
                item.put("sourceField", field.getField());
                item.put("fieldRef", RuntimePageRefFieldFactory.safeKey(ref.getModelCode()) + "__" + field.getField());
                item.put("columnName", field.getColumnName());
                item.put("modelCode", ref.getModelCode());
                item.put("modelName", ref.getModelName());
                configuredFields.add(item);
            }
            configuredFields.sort(Comparator.comparingInt(item ->
                    configuredOrder.getOrDefault(text(item.get("sourceField")), Integer.MAX_VALUE)));
            return configuredFields;
        }
        Set<String> selectedEditRefSet = new LinkedHashSet<>(selectedEditRefs);
        boolean hasSelectedChildRefs = ref.getFields().stream()
                .map(source -> StringUtils.defaultIfBlank(text(source.get("fieldRef")),
                        RuntimePageRefFieldFactory.safeKey(ref.getModelCode()) + "__"
                                + StringUtils.defaultIfBlank(text(source.get("sourceField")), text(source.get("field")))))
                .anyMatch(selectedEditRefSet::contains);
        Map<String, Integer> selectedOrder = new LinkedHashMap<>();
        for (int i = 0; i < selectedEditRefs.size(); i++) {
            selectedOrder.putIfAbsent(selectedEditRefs.get(i), i);
        }
        List<Map<String, Object>> fields = new ArrayList<>();
        for (Map<String, Object> source : ref.getFields()) {
            LowcodeFieldSchema field = buildChildField(ref, source);
            if (field == null) {
                continue;
            }
            String fieldRef = StringUtils.defaultIfBlank(text(source.get("fieldRef")),
                    RuntimePageRefFieldFactory.safeKey(ref.getModelCode()) + "__" + field.getField());
            if (hasSelectedChildRefs && !selectedEditRefSet.contains(fieldRef)) {
                continue;
            }
            if (!isChildEditFieldAllowed(field, childFkField)) {
                continue;
            }
            Map<String, Object> item = editFieldRenderer.apply(field);
            item.put("sourceField", field.getField());
            item.put("fieldRef", fieldRef);
            item.put("columnName", field.getColumnName());
            item.put("modelCode", ref.getModelCode());
            item.put("modelName", ref.getModelName());
            fields.add(item);
        }
        if (!selectedOrder.isEmpty()) {
            fields.sort(Comparator.comparingInt(item ->
                    selectedOrder.getOrDefault(text(item.get("fieldRef")), Integer.MAX_VALUE)));
        }
        return fields;
    }

    private static List<String> readConfiguredChildFieldCodes(LowcodePageModelRef ref) {
        if (ref == null || ref.getProps() == null) {
            return List.of();
        }
        Object value = ref.getProps().get("childFieldCodes");
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(item -> StringUtils.trimToNull(text(item)))
                .filter(item -> item != null)
                .toList();
    }

    private static LowcodeFieldSchema buildChildField(LowcodePageModelRef ref, Map<String, Object> source) {
        LowcodeFieldSchema field = RuntimePageRefFieldFactory.build(ref, source);
        if (field == null) {
            return null;
        }
        String sourceField = StringUtils.defaultIfBlank(text(source.get("sourceField")), text(source.get("field")));
        field.setField(sourceField);
        field.setLabel(StringUtils.defaultIfBlank(text(source.get("rawLabel")),
                StringUtils.defaultIfBlank(text(source.get("label")), sourceField)));
        return field;
    }

    private static boolean isChildEditFieldAllowed(LowcodeFieldSchema field, String childFkField) {
        if (field == null) {
            return false;
        }
        String fieldName = field.getField();
        String columnName = field.getColumnName();
        if (StringUtils.equals(fieldName, childFkField) || StringUtils.equals(columnName, childFkField)
                || StringUtils.equals(columnName, camelToSnake(childFkField))) {
            return false;
        }
        // 只读字段必须保留：与子表独立填表一致，由 RuntimeEditFieldCompiler 标 readonly/disabled，
        // 前端 ChildTableEditor 按只读单元格渲染；过滤掉会导致主子表里整列消失（含公式字段）。
        String status = StringUtils.defaultString(field.getFieldStatus());
        return !Boolean.TRUE.equals(field.getSystemField())
                && !"DISABLED".equalsIgnoreCase(status) && !"HIDDEN".equalsIgnoreCase(status)
                && !RuntimeSystemFields.FIELD_NAMES.contains(fieldName)
                && !RuntimeSystemFields.COLUMN_NAMES.contains(columnName)
                && !Boolean.TRUE.equals(field.getPrimaryKey())
                && (field.getFormVisible() == null || Boolean.TRUE.equals(field.getFormVisible()));
    }

    private static String camelToSnake(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
