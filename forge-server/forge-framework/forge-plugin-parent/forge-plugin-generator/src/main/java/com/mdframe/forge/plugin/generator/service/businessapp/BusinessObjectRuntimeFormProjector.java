package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.dto.businessapp.FormDesignerSchemaDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeComponentCatalog;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 将表单设计协议投影为运行时编辑区协议。
 *
 * <p>采用 Projector 模式集中字段设置、嵌套布局、动态可见性及弹窗/抽屉配置编译。</p>
 */
final class BusinessObjectRuntimeFormProjector {

    private static final String FORM_DESIGNER_SCHEMA_OPTION_KEY = "formDesignerSchema";
    private static final Set<String> FORM_FIELD_COMPONENT_KEYS = LowcodeComponentCatalog.FIELD_COMPONENT_KEYS;

    private final Predicate<Object> dynamicOptionSource;

    BusinessObjectRuntimeFormProjector(Predicate<Object> dynamicOptionSource) {
        this.dynamicOptionSource = dynamicOptionSource;
    }

    void applyFormDesignerSchemaToEditZone(LowcodePageSchema pageSchema, LowcodeModelSchema modelSchema,
                                                   FormDesignerSchemaDTO formSchema) {
        if (pageSchema == null || modelSchema == null || formSchema == null) {
            return;
        }
        LowcodePageZone editZone = findOrCreateZone(pageSchema, "edit", "edit-form");
        Set<String> modelFields = lowcodeFieldMap(modelSchema).keySet();
        List<String> formFieldRefs = new ArrayList<>();
        Map<String, Object> compiledSettings = new LinkedHashMap<>();
        int gridColumns = clamp(integerValue(mapValue(formSchema.getLayout()).get("gridColumns"), 2), 1, 3);
        int defaultLabelWidth = integerValue(mapValue(formSchema.getLayout()).get("labelWidth"), 100);
        int rowGap = integerValue(mapValue(formSchema.getLayout()).get("rowGap"), 16);
        int columnGap = integerValue(mapValue(formSchema.getLayout()).get("columnGap"), 16);

        collectRuntimeFormFields(formSchema.getComponents(), modelFields, gridColumns, defaultLabelWidth,
                null, formFieldRefs, compiledSettings);

        List<String> retainedRelationRefs = editZone.getFieldRefs() == null
                ? new ArrayList<>()
                : editZone.getFieldRefs().stream()
                .filter(ref -> StringUtils.isNotBlank(ref) && !modelFields.contains(ref))
                .toList();
        LinkedHashSet<String> refs = new LinkedHashSet<>(formFieldRefs);
        refs.addAll(retainedRelationRefs);
        editZone.setFieldRefs(new ArrayList<>(refs));

        Map<String, Object> props = editZone.getProps() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(editZone.getProps());
        replaceModelFieldSettings(props, modelFields, compiledSettings);
        props.put("editGridCols", gridColumns);
        props.put("labelPlacement", StringUtils.defaultIfBlank(text(mapValue(formSchema.getLayout()).get("labelPlacement")), "left"));
        props.put("labelWidth", defaultLabelWidth);
        props.put("labelAlign", normalizeLabelAlign(text(mapValue(formSchema.getLayout()).get("labelAlign"))));
        props.put("size", normalizeRuntimeFormSize(text(mapValue(formSchema.getLayout()).get("size"))));
        props.put("showFeedback", readBoolean(mapValue(formSchema.getLayout()).get("showFeedback"), true));
        props.put("hideRequiredAsterisk", readBoolean(mapValue(formSchema.getLayout()).get("hideRequiredAsterisk"), false));
        props.put("inlineFeedback", readBoolean(mapValue(formSchema.getLayout()).get("inlineFeedback"), false));
        putIfPresent(props, "editFormStyle", mapValue(formSchema.getLayout()).get("formStyle"));
        putIfNotBlank(props, "editFormClass", text(mapValue(formSchema.getLayout()).get("formClass")));
        applyFormLayoutOpenModeAndModalProps(props, mapValue(formSchema.getLayout()));
        props.put("rowGap", rowGap);
        props.put("columnGap", columnGap);
        props.put("formLayout", buildRuntimeFormLayout(formSchema.getComponents(), modelFields, gridColumns));
        props.put("compiledFrom", FORM_DESIGNER_SCHEMA_OPTION_KEY);
        props.put(FORM_DESIGNER_SCHEMA_OPTION_KEY, formSchema);
        props.remove("formCreateRule");
        props.remove("formCreateOptions");
        props.remove("canvas");
        editZone.setProps(props);
    }

    private void collectRuntimeFormFields(List<Map<String, Object>> components, Set<String> modelFields,
                                          int gridColumns, int defaultLabelWidth, Integer inheritedSpan,
                                          List<String> formFieldRefs, Map<String, Object> compiledSettings) {
        if (components == null) {
            return;
        }
        for (Map<String, Object> component : components) {
            if (component == null) {
                continue;
            }
            String componentKey = text(component.get("componentKey"));
            if (FORM_FIELD_COMPONENT_KEYS.contains(componentKey)
                    && !"virtual".equals(StringUtils.defaultIfBlank(text(mapValue(component.get("fieldBinding")).get("mode")), "field"))) {
                Map<String, Object> binding = mapValue(component.get("fieldBinding"));
                String fieldCode = text(binding.get("fieldCode"));
                if (StringUtils.isBlank(fieldCode) || !modelFields.contains(fieldCode)) {
                    continue;
                }
                Map<String, Object> visibility = mapValue(component.get("visibility"));
                if (readBoolean(visibility.get("hidden"), false)
                        && !hasRuntimeVisibilityRules(component)) {
                    continue;
                }
                formFieldRefs.add(fieldCode);
                compiledSettings.put(fieldCode, buildFormFieldSetting(component, componentKey, gridColumns,
                        defaultLabelWidth, inheritedSpan));
                continue;
            }
            Integer nextSpan = inheritedSpan;
            if (isColumnLayoutComponent(componentKey)) {
                int fallbackSpan = inheritedSpan == null ? 1 : inheritedSpan;
                nextSpan = clamp(integerValue(mapValue(component.get("layout")).get("span"),
                        integerValue(mapValue(component.get("props")).get("span"), fallbackSpan)), 1, gridColumns);
            }
            collectRuntimeFormFields(listOfMap(component.get("children")), modelFields, gridColumns,
                    defaultLabelWidth, nextSpan, formFieldRefs, compiledSettings);
        }
    }

    List<Map<String, Object>> buildRuntimeFormLayout(List<Map<String, Object>> components,
                                                             Set<String> modelFields,
                                                             int gridColumns) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        if (components == null) {
            return nodes;
        }
        int index = 0;
        for (Map<String, Object> component : components) {
            Object node = buildRuntimeFormLayoutNode(component, modelFields, gridColumns, index++);
            if (node instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        nodes.add(new LinkedHashMap<>((Map<String, Object>) map));
                    }
                }
            } else if (node instanceof Map<?, ?> map) {
                nodes.add(new LinkedHashMap<>((Map<String, Object>) map));
            }
        }
        return nodes;
    }

    private Object buildRuntimeFormLayoutNode(Map<String, Object> component, Set<String> modelFields,
                                              int gridColumns, int index) {
        if (component == null) {
            return null;
        }
        String componentKey = text(component.get("componentKey"));
        String key = StringUtils.defaultIfBlank(text(component.get("id")), componentKey + "_" + index);
        if (FORM_FIELD_COMPONENT_KEYS.contains(componentKey)
                && !"virtual".equals(StringUtils.defaultIfBlank(text(mapValue(component.get("fieldBinding")).get("mode")), "field"))) {
            Map<String, Object> binding = mapValue(component.get("fieldBinding"));
            String fieldCode = text(binding.get("fieldCode"));
            if (StringUtils.isBlank(fieldCode) || !modelFields.contains(fieldCode)
                    || (readBoolean(mapValue(component.get("visibility")).get("hidden"), false)
                    && !hasRuntimeVisibilityRules(component))) {
                return null;
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("nodeType", "field");
            node.put("key", key);
            node.put("field", fieldCode);
            node.put("span", clamp(integerValue(mapValue(component.get("layout")).get("span"), 1), 1, gridColumns));
            return node;
        }

        List<Map<String, Object>> children = buildRuntimeFormLayout(listOfMap(component.get("children")),
                modelFields, gridColumns);
        Map<String, Object> props = sanitizeRuntimeLayoutProps(mapValue(component.get("props")));
        String label = resolveRuntimeLayoutLabel(component);
        int span = clamp(integerValue(mapValue(component.get("layout")).get("span"),
                integerValue(props.get("span"), gridColumns)), 1, gridColumns);

        if (isRowLayoutComponent(componentKey)) {
            return runtimeLayoutNode("row", key, label, props, children, gridColumns);
        }
        if (isColumnLayoutComponent(componentKey)) {
            return runtimeLayoutNode("col", key, label, props, children, span);
        }
        if (Set.of("elCard", "card").contains(componentKey)) {
            return runtimeLayoutNode("card", key, label, props, children, gridColumns);
        }
        if (Set.of("elTabs", "tabs").contains(componentKey)) {
            return runtimeLayoutNode("tabs", key, label, props, children, gridColumns);
        }
        if (Set.of("elTabPane", "tabPane").contains(componentKey)) {
            return runtimeLayoutNode("tabPane", key, label, props, children, gridColumns);
        }
        if (Set.of("elCollapse", "collapse").contains(componentKey)) {
            return runtimeLayoutNode("collapse", key, label, props, children, gridColumns);
        }
        if (Set.of("elCollapseItem", "collapseItem").contains(componentKey)) {
            return runtimeLayoutNode("collapseItem", key, label, props, children, gridColumns);
        }
        if ("button".equals(componentKey)) {
            Map<String, Object> node = runtimeLayoutNode("button", key, label, props, List.of(), span);
            node.put("align", normalizeAlign(text(mapValue(component.get("layout")).get("align"))));
            return node;
        }
        if (Set.of("table", "tableGrid").contains(componentKey)) {
            return runtimeLayoutNode(componentKey, key, label, props, children,
                    "table".equals(componentKey) ? gridColumns : span);
        }
        if (Set.of("AiCrudPage", "aiCrudPage", "crud", "crudBlock").contains(componentKey)) {
            return runtimeLayoutNode("AiCrudPage", key, label, props, children, gridColumns);
        }
        if (LowcodeComponentCatalog.isPageWidgetComponent(componentKey)) {
            Map<String, Object> node = runtimeLayoutNode("widget", key, label, props, children, span);
            node.put("componentKey", componentKey);
            return node;
        }
        if (Set.of("elDivider", "divider", "AiFormSectionTitle").contains(componentKey)) {
            return runtimeLayoutNode("divider", key, label, props, List.of(), gridColumns);
        }
        if (Set.of("fcTitle", "title", "groupTitle").contains(componentKey)) {
            return runtimeLayoutNode("groupTitle", key, label, props, List.of(), gridColumns);
        }
        return children.isEmpty() ? null : children;
    }

    private Map<String, Object> runtimeLayoutNode(String nodeType, String key, String label,
                                                  Map<String, Object> props,
                                                  List<Map<String, Object>> children,
                                                  int span) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("nodeType", nodeType);
        node.put("key", key);
        if (StringUtils.isNotBlank(label)) {
            node.put("label", label);
        }
        if (props != null && !props.isEmpty()) {
            node.put("props", props);
        }
        if (children != null && !children.isEmpty()) {
            node.put("children", children);
        }
        node.put("span", span);
        return node;
    }

    private Map<String, Object> sanitizeRuntimeLayoutProps(Map<String, Object> source) {
        Map<String, Object> props = new LinkedHashMap<>(source);
        props.remove("__fc");
        props.remove("__fcType");
        props.remove("fieldBinding");
        return props;
    }

    private String resolveRuntimeLayoutLabel(Map<String, Object> component) {
        Map<String, Object> props = mapValue(component.get("props"));
        return StringUtils.defaultIfBlank(text(props.get("header")),
                StringUtils.defaultIfBlank(text(props.get("label")),
                        StringUtils.defaultIfBlank(text(props.get("title")),
                                StringUtils.defaultIfBlank(text(props.get("formCreateChild")), text(component.get("label"))))));
    }

    private boolean isRowLayoutComponent(String componentKey) {
        return Set.of("fcRow", "row").contains(componentKey);
    }

    private boolean isColumnLayoutComponent(String componentKey) {
        return "col".equals(componentKey);
    }

    private Map<String, Object> buildFormFieldSetting(Map<String, Object> component, String componentKey,
                                                      int gridColumns, int defaultLabelWidth) {
        return buildFormFieldSetting(component, componentKey, gridColumns, defaultLabelWidth, null);
    }

    private Map<String, Object> buildFormFieldSetting(Map<String, Object> component, String componentKey,
                                                      int gridColumns, int defaultLabelWidth,
                                                      Integer inheritedSpan) {
        Map<String, Object> setting = new LinkedHashMap<>();
        setting.put("componentType", normalizeRuntimeComponentType(componentKey));
        putIfNotBlank(setting, "label", text(component.get("label")));
        Map<String, Object> layout = mapValue(component.get("layout"));
        setting.put("align", normalizeAlign(text(layout.get("align"))));
        setting.put("span", clamp(inheritedSpan == null ? integerValue(layout.get("span"), 1) : inheritedSpan,
                1, gridColumns));
        setting.put("labelWidth", integerValue(layout.get("labelWidth"), defaultLabelWidth));
        Map<String, Object> rawProps = new LinkedHashMap<>(mapValue(component.get("props")));
        Map<String, Object> formCreateMeta = mapValue(rawProps.get("__fc"));
        Map<String, Object> props = sanitizeRuntimeFieldProps(rawProps);
        copyRuntimeFieldProps(setting, props);
        applyRuntimeFieldMeta(setting, component, props, formCreateMeta);
        // 仅动态 optionSource 清除残留静态 options；STATIC 必须保留 options 供 value→label
        if (props.get("optionSource") instanceof Map<?, ?> os && isDynamicOptionSource(os)) {
            props.remove("options");
            // 动态选项来源：冗余保存显示名称到 <field>Name，回显无需再查源表
            if (StringUtils.isBlank(text(props.get("labelValueField")))) {
                String fieldCode = StringUtils.firstNonBlank(
                        text(mapValue(component.get("fieldBinding")).get("fieldCode")),
                        text(component.get("field")),
                        text(props.get("fieldCode")));
                if (StringUtils.isNotBlank(fieldCode)) {
                    props.put("labelValueField", fieldCode + "Name");
                }
            }
        }
        if (!props.isEmpty()) {
            setting.put("props", props);
        }
        Map<String, Object> validation = mapValue(component.get("validation"));
        if (validation.containsKey("required")) {
            setting.put("required", readBoolean(validation.get("required"), false));
        }
        putIfNotBlank(setting, "requiredMessage", text(validation.get("requiredMessage")));
        List<Map<String, Object>> rules = listOfMap(validation.get("rules"));
        if (!rules.isEmpty()) {
            setting.put("rules", rules);
            rules.stream()
                    .filter(rule -> readBoolean(rule.get("required"), false))
                    .findFirst()
                    .ifPresent(rule -> {
                        putIfPresent(setting, "trigger", rule.get("trigger"));
                        if (StringUtils.isBlank(text(setting.get("requiredMessage")))) {
                            putIfNotBlank(setting, "requiredMessage", text(rule.get("message")));
                        }
                    });
        }
        Map<String, Object> visibility = mapValue(component.get("visibility"));
        if (visibility.containsKey("readonly")) {
            setting.put("readonly", readBoolean(visibility.get("readonly"), false));
        }
        if (visibility.containsKey("hidden")) {
            setting.put("hidden", readBoolean(visibility.get("hidden"), false));
            setting.put("formVisible", !readBoolean(visibility.get("hidden"), false));
        }
        Object runtimeRules = props.get("runtimeRules");
        if (runtimeRules instanceof List<?> runtimeRuleList && !runtimeRuleList.isEmpty()) {
            setting.put("runtimeRules", runtimeRuleList);
        } else if (component.get("runtimeRules") instanceof List<?> directRuleList) {
            setting.put("runtimeRules", directRuleList);
        }
        putIfNotBlank(setting, "dictType", text(props.get("dictType")));
        if (props.containsKey("defaultValue")) {
            setting.put("defaultValue", props.get("defaultValue"));
        }
        return setting;
    }

    private boolean hasRuntimeVisibilityRules(Map<String, Object> component) {
        if (component == null) {
            return false;
        }
        Object directRules = component.get("runtimeRules");
        if (containsVisibilityRule(directRules)) {
            return true;
        }
        return containsVisibilityRule(mapValue(component.get("props")).get("runtimeRules"));
    }

    private boolean containsVisibilityRule(Object value) {
        if (!(value instanceof List<?> rules)) {
            return false;
        }
        return rules.stream().filter(item -> item instanceof Map<?, ?>)
                .map(item -> (Map<?, ?>) item)
                .anyMatch(rule -> {
                    Object effectValue = rule.get("effect");
                    if (effectValue instanceof Map<?, ?> effect) {
                        return effect.containsKey("visible") || effect.containsKey("hidden");
                    }
                    return rule.containsKey("visible") || rule.containsKey("hidden");
                });
    }

    private Map<String, Object> sanitizeRuntimeFieldProps(Map<String, Object> source) {
        Map<String, Object> props = new LinkedHashMap<>(source);
        props.remove("__fc");
        props.remove("__fcType");
        props.remove("fieldBinding");
        return props;
    }

    private void copyRuntimeFieldProps(Map<String, Object> setting, Map<String, Object> props) {
        List.of("placeholder", "clearable", "filterable", "multiple", "size", "maxlength", "showCount",
                        "rows", "autosize", "min", "max", "step", "precision", "showButton",
                        "checkedValue", "uncheckedValue", "checkedText", "uncheckedText", "format",
                        "valueFormat", "startPlaceholder", "endPlaceholder", "showFeedback", "showLabel")
                .forEach(key -> putIfPresent(setting, key, props.get(key)));
    }

    private void applyRuntimeFieldMeta(Map<String, Object> setting,
                                       Map<String, Object> component,
                                       Map<String, Object> props,
                                       Map<String, Object> formCreateMeta) {
        putIfPresent(setting, "componentStyle", firstPresent(formCreateMeta.get("style"), props.get("style")));
        putIfPresent(setting, "componentClass", firstPresent(props.get("className"), props.get("class")));
        putIfPresent(setting, "formItemClass", firstPresent(formCreateMeta.get("className"), formCreateMeta.get("class")));
        Map<String, Object> wrap = mapValue(formCreateMeta.get("wrap"));
        putIfPresent(setting, "formItemStyle", firstPresent(mapValue(component.get("layout")).get("formItemStyle"), wrap.get("style")));
        if (wrap.containsKey("labelWidth")) {
            setting.put("labelWidth", integerValue(wrap.get("labelWidth"), integerValue(setting.get("labelWidth"), 100)));
        }
        if (wrap.containsKey("show") && !readBoolean(wrap.get("show"), true)) {
            setting.put("showLabel", false);
        }
    }

    private Object firstPresent(Object primary, Object fallback) {
        return primary != null ? primary : fallback;
    }

    private boolean isDynamicOptionSource(Object optionSource) {
        return dynamicOptionSource != null && dynamicOptionSource.test(optionSource);
    }

    private LowcodePageZone findOrCreateZone(LowcodePageSchema pageSchema, String zoneKey, String componentKey) {
        if (pageSchema.getZones() == null) {
            pageSchema.setZones(new ArrayList<>());
        }
        LowcodePageZone zone = pageSchema.getZones().stream()
                .filter(item -> item != null && zoneKey.equals(item.getZoneKey()))
                .findFirst()
                .orElse(null);
        if (zone != null) {
            if (StringUtils.isBlank(zone.getComponentKey())) {
                zone.setComponentKey(componentKey);
            }
            if (zone.getProps() == null) {
                zone.setProps(new LinkedHashMap<>());
            }
            return zone;
        }
        zone = new LowcodePageZone();
        zone.setZoneKey(zoneKey);
        zone.setComponentKey(componentKey);
        zone.setEnabled(true);
        zone.setFieldRefs(new ArrayList<>());
        zone.setProps(new LinkedHashMap<>());
        pageSchema.getZones().add(zone);
        return zone;
    }

    private Map<String, LowcodeFieldSchema> lowcodeFieldMap(LowcodeModelSchema modelSchema) {
        Map<String, LowcodeFieldSchema> fields = new LinkedHashMap<>();
        if (modelSchema != null && modelSchema.getFields() != null) {
            for (LowcodeFieldSchema field : modelSchema.getFields()) {
                if (field != null && StringUtils.isNotBlank(field.getField())) {
                    fields.put(field.getField(), field);
                }
            }
        }
        return fields;
    }

    private String normalizeRuntimeComponentType(String componentKey) {
        return switch (StringUtils.defaultString(componentKey)) {
            case "inputNumber", "input-number", "inputnumber", "integer", "money" -> "number";
            case "upload" -> "fileUpload";
            case "orgSelect", "departmentSelect", "departmentTreeSelect", "deptSelect", "deptTreeSelect",
                    "elTreeSelect", "orgName", "deptName" -> "orgTreeSelect";
            case "userPicker", "userName" -> "userSelect";
            default -> componentKey;
        };
    }

    private String normalizeAlign(String value) {
        String align = StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
        return Set.of("left", "center", "right").contains(align) ? align : "left";
    }

    private String normalizeLabelAlign(String value) {
        String align = StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
        return Set.of("left", "right").contains(align) ? align : "right";
    }

    private String normalizeRuntimeFormSize(String value) {
        String size = StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
        if ("default".equals(size) || "medium".equals(size)) {
            return "medium";
        }
        return Set.of("small", "large").contains(size) ? size : "medium";
    }

    private String normalizeFormOpenMode(String value) {
        String mode = StringUtils.defaultString(value).trim();
        if ("tabWorkspace".equalsIgnoreCase(mode)) {
            return "tabWorkspace";
        }
        String normalized = mode.toLowerCase(Locale.ROOT);
        return Set.of("modal", "drawer", "flat").contains(normalized) ? normalized : "modal";
    }

    private String normalizeDrawerPlacement(Object value, Object fallback) {
        String placement = StringUtils.defaultString(text(value)).trim().toLowerCase(Locale.ROOT);
        if (Set.of("left", "right", "top", "bottom").contains(placement)) {
            return placement;
        }
        String fallbackPlacement = StringUtils.defaultString(text(fallback)).trim().toLowerCase(Locale.ROOT);
        return Set.of("left", "right", "top", "bottom").contains(fallbackPlacement) ? fallbackPlacement : "right";
    }

    private void applyFormLayoutOpenModeAndModalProps(Map<String, Object> props, Map<String, Object> layout) {
        String formOpenMode = normalizeFormOpenMode(StringUtils.firstNonBlank(
                text(layout.get("formOpenMode")), text(layout.get("modalType"))));
        props.put("formOpenMode", formOpenMode);
        props.put("modalType", "modal".equals(formOpenMode) || "drawer".equals(formOpenMode) ? formOpenMode : "modal");
        putIfNotBlank(props, "modalWidth", StringUtils.trimToNull(text(layout.get("modalWidth"))));
        putIfNotBlank(props, "detailModalWidth", StringUtils.trimToNull(text(layout.get("detailModalWidth"))));
        props.put("drawerPlacement", normalizeDrawerPlacement(layout.get("drawerPlacement"), props.get("drawerPlacement")));
        props.put("enableCollapse", readBoolean(layout.get("enableCollapse"), false));
        int maxVisibleFields = integerValue(layout.get("maxVisibleFields"), 0);
        if (maxVisibleFields > 0) {
            props.put("maxVisibleFields", maxVisibleFields);
        }
    }

    private void replaceModelFieldSettings(Map<String, Object> props,
                                           Set<String> modelFields,
                                           Map<String, Object> compiledSettings) {
        Map<String, Object> existing = new LinkedHashMap<>(mapValue(props.get("fieldSettings")));
        modelFields.forEach(existing::remove);
        existing.putAll(compiledSettings);
        props.put("fieldSettings", existing);
    }

    private List<Map<String, Object>> flattenFormComponents(List<Map<String, Object>> components) {
        List<Map<String, Object>> result = new ArrayList<>();
        collectFormComponents(components, result);
        return result;
    }

    private void collectFormComponents(List<Map<String, Object>> components, List<Map<String, Object>> result) {
        if (components == null) {
            return;
        }
        for (Map<String, Object> component : components) {
            if (component == null) {
                continue;
            }
            result.add(component);
            collectFormComponents(listOfMap(component.get("children")), result);
        }
    }

    private int integerValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                String digits = text.trim().replaceAll("[^0-9-]", "");
                if (StringUtils.isBlank(digits) || "-".equals(digits)) {
                    return defaultValue;
                }
                try {
                    return Integer.parseInt(digits);
                } catch (NumberFormatException ignoredAgain) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return new LinkedHashMap<>();
    }

    private List<Map<String, Object>> listOfMap(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(this::mapValue)
                .toList();
    }

    private boolean readBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = StringUtils.trimToEmpty(String.valueOf(value));
        if (StringUtils.isBlank(text)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}

