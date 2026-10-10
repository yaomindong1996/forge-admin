package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerLayoutReader.text;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponentResolver.applySelectionLabelProps;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponentResolver.buildPlaceholder;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponentResolver.ensureDynamicOptionSourceLabelValueField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponentResolver.isTextComponent;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponentResolver.resolveEditComponentType;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldPresentationSupport.applyAlignment;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldPresentationSupport.isSystemField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldPresentationSupport.sanitizeFieldBasicProps;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFormRuleSettingResolver.booleanWithDefault;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFormRuleSettingResolver.firstPresent;

/** 将字段元数据与设计器设置编译为运行时编辑字段协议。 */
final class RuntimeEditFieldCompiler {

    private RuntimeEditFieldCompiler() {
    }

    static Map<String, Object> buildEditField(LowcodeFieldSchema field) {
        return buildEditField(field, Map.of());
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> buildEditField(LowcodeFieldSchema field, Map<String, Object> pageSetting) {
        return buildEditField(field, pageSetting, null, null);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> buildEditField(LowcodeFieldSchema field,
                                               Map<String, Object> pageSetting,
                                               LowcodeModelSchema modelSchema,
                                               LowcodePageSchema pageSchema) {
        Map<String, Object> item = new LinkedHashMap<>();
        String label = StringUtils.defaultIfBlank(text(pageSetting.get("label")),
                StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        RuntimeRelationLookupCompiler.RelationLookupMeta lookupMeta = RuntimeRelationLookupCompiler.resolve(
                modelSchema, pageSchema, field.getField());
        String componentType = resolveEditComponentType(field, pageSetting);
        if (lookupMeta != null) {
            componentType = "select";
        }
        Map<String, Object> formulaConfig = field.getFormulaConfig();
        boolean formulaField = formulaConfig != null && !formulaConfig.isEmpty();
        item.put("field", field.getField());
        item.put("label", label);
        item.put("type", componentType);
        applyAlignment(item, pageSetting);
        boolean required = pageSetting.containsKey("required")
                ? booleanWithDefault(pageSetting.get("required"), false)
                : Boolean.TRUE.equals(field.getRequired());
        List<Map<String, Object>> validationRules = resolveRuntimeValidationRules(pageSetting);
        if (!pageSetting.containsKey("required")
                && validationRules.stream().anyMatch(rule -> booleanWithDefault(rule.get("required"), false))) {
            required = true;
        }
        boolean readonly = pageSetting.containsKey("readonly")
                ? booleanWithDefault(pageSetting.get("readonly"), false)
                : Boolean.TRUE.equals(field.getReadonly());
        if (formulaField) {
            readonly = true;
            required = false;
            validationRules.removeIf(rule -> booleanWithDefault(rule.get("required"), false));
        }
        if (!required) {
            validationRules.removeIf(rule -> booleanWithDefault(rule.get("required"), false));
        }
        item.put("required", !isSystemField(field) && required);
        String requiredMessage = StringUtils.defaultIfBlank(text(pageSetting.get("requiredMessage")),
                resolveRequiredRuleMessage(validationRules));
        if (StringUtils.isNotBlank(requiredMessage)) {
            item.put("requiredMessage", requiredMessage);
        }
        Object trigger = StringUtils.isNotBlank(text(pageSetting.get("trigger")))
                ? pageSetting.get("trigger")
                : resolveRequiredRuleTrigger(validationRules);
        if (trigger != null) {
            item.put("trigger", trigger);
        }
        if (isSystemField(field) || readonly) {
            item.put("disabled", true);
            item.put("readonly", true);
        }
        copyRuntimeSetting(item, pageSetting, "hidden");
        copyRuntimeSetting(item, pageSetting, "formVisible");
        copyRuntimeSetting(item, pageSetting, "runtimeRules");
        if (formulaField) {
            item.put("formulaConfig", new LinkedHashMap<>(formulaConfig));
        }
        if (field.getAdvancedProps() != null && !field.getAdvancedProps().isEmpty()) {
            item.put("advancedProps", new LinkedHashMap<>(field.getAdvancedProps()));
        }
        String dictType = StringUtils.defaultIfBlank(text(pageSetting.get("dictType")), field.getDictType());
        if (StringUtils.isNotBlank(dictType)) {
            item.put("dictType", dictType);
        }
        if (pageSetting.containsKey("defaultValue")) {
            item.put("defaultValue", pageSetting.get("defaultValue"));
        } else if (field.getDefaultValue() != null) {
            item.put("defaultValue", field.getDefaultValue());
        }
        Object span = pageSetting.get("span");
        if (span != null) {
            item.put("span", span);
        }
        Object formItemStyle = pageSetting.get("formItemStyle");
        if (formItemStyle != null) {
            item.put("formItemStyle", formItemStyle);
        }
        Object gridStyle = pageSetting.get("gridStyle");
        if (gridStyle != null) {
            item.put("gridStyle", gridStyle);
        }
        Object labelWidth = pageSetting.get("labelWidth");
        if (labelWidth != null) {
            item.put("labelWidth", labelWidth);
        }
        copyRuntimeSetting(item, pageSetting, "componentStyle");
        copyRuntimeSetting(item, pageSetting, "componentClass");
        copyRuntimeSetting(item, pageSetting, "formItemClass");
        copyRuntimeSetting(item, pageSetting, "showFeedback");
        copyRuntimeSetting(item, pageSetting, "showLabel");

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("placeholder", buildPlaceholder(componentType, label));
        if (isSystemField(field) || readonly) {
            props.put("disabled", true);
            props.put("readonly", true);
        }
        if (field.getLength() != null && field.getLength() > 0 && isTextComponent(componentType)) {
            props.put("maxlength", field.getLength());
        }
        if (field.getPrecision() != null && field.getPrecision() >= 0 && "number".equals(componentType)) {
            props.put("precision", field.getPrecision());
        }
        props.putAll(sanitizeFieldBasicProps(field));
        Object designerProps = pageSetting.get("props");
        if (designerProps instanceof Map<?, ?> designerPropsMap) {
            Map<String, Object> sanitizedDesignerProps = new LinkedHashMap<>((Map<String, Object>) designerPropsMap);
            Map<String, Object> formCreateMeta = mapValue(sanitizedDesignerProps.get("__fc"));
            sanitizedDesignerProps.remove("__fc");
            sanitizedDesignerProps.remove("__fcType");
            sanitizedDesignerProps.remove("fieldBinding");
            props.putAll(sanitizedDesignerProps);
            applyFormCreateMeta(item, formCreateMeta, props);
        }
        LowcodeFieldConstraintSupport.applyRuntimeConstraints(field, componentType, props);
        // 仅动态 optionSource 时清除残留静态 options，避免抢在 remoteOptionSource 之前返回；
        // STATIC（含仅声明 type=STATIC）必须保留 options，供编辑/详情/列表 value→label。
        if (props.get("optionSource") instanceof Map<?, ?> os && isDynamicOptionSourceType(os.get("type"))) {
            props.remove("options");
        }
        copyRuntimePropsToField(item, props);
        applySelectionLabelProps(props, field.getField(), componentType);
        if (isSystemField(field) || readonly) {
            props.put("disabled", true);
            props.put("readonly", true);
        }
        item.put("props", props);
        if (lookupMeta != null) {
            item.put("relationLookup", RuntimeRelationLookupCompiler.buildConfig(lookupMeta));
            RuntimeRelationLookupCompiler.applyProps(item, lookupMeta, label);
        } else if (field.isSelectionLabelField()) {
            // 引用/人员/部门/动态选项下拉：选中时同步提交显示名称到伴随列（<field>Name），
            // 编辑回显与列表渲染使用冗余字段，无需再查源表。
            props.putIfAbsent("labelValueField", field.referenceDisplayFieldName());
        } else {
            // 页面 props 已带动态 optionSource、但模型字段尚未回写 basicProps 时，仍补齐伴随字段绑定
            ensureDynamicOptionSourceLabelValueField(props, field.getField(), componentType);
        }

        if (required) {
            String message = StringUtils.defaultIfBlank(requiredMessage, buildPlaceholder(componentType, label));
            if (validationRules.stream().noneMatch(rule -> booleanWithDefault(rule.get("required"), false))) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("required", true);
                rule.put("message", message);
                rule.put("trigger", trigger == null ? List.of("blur", "change") : trigger);
                validationRules.add(0, rule);
            } else {
                validationRules.forEach(rule -> {
                    if (booleanWithDefault(rule.get("required"), false) && StringUtils.isBlank(text(rule.get("message")))) {
                        rule.put("message", message);
                    }
                });
            }
            item.put("requiredMessage", message);
        }
        if (!validationRules.isEmpty()) {
            item.put("rules", validationRules);
        }
        return item;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> resolveRuntimeValidationRules(Map<String, Object> pageSetting) {
        Object source = pageSetting.get("rules");
        if (!(source instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> rules = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                rules.add(new LinkedHashMap<>((Map<String, Object>) map));
            }
        }
        return rules;
    }

    private static String resolveRequiredRuleMessage(List<Map<String, Object>> validationRules) {
        return validationRules.stream()
                .filter(rule -> booleanWithDefault(rule.get("required"), false))
                .map(rule -> text(rule.get("message")))
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse("");
    }

    private static Object resolveRequiredRuleTrigger(List<Map<String, Object>> validationRules) {
        return validationRules.stream()
                .filter(rule -> booleanWithDefault(rule.get("required"), false))
                .map(rule -> rule.get("trigger"))
                .filter(value -> value != null && StringUtils.isNotBlank(String.valueOf(value)))
                .findFirst()
                .orElse(null);
    }

    private static void copyRuntimeSetting(Map<String, Object> item, Map<String, Object> pageSetting, String key) {
        if (pageSetting.containsKey(key)) {
            item.put(key, pageSetting.get(key));
        }
    }

    private static void copyRuntimePropsToField(Map<String, Object> item, Map<String, Object> props) {
        List.of("placeholder", "clearable", "filterable", "multiple", "size", "maxlength", "showCount",
                        "rows", "autosize", "min", "max", "step", "precision", "showButton",
                        "checkedValue", "uncheckedValue", "checkedText", "uncheckedText", "format",
                        "valueFormat", "startPlaceholder", "endPlaceholder", "showFeedback", "showLabel")
                .forEach(key -> {
                    if (props.containsKey(key)) {
                        item.put(key, props.get(key));
                    }
                });
    }

    private static void applyFormCreateMeta(Map<String, Object> item, Map<String, Object> formCreateMeta, Map<String, Object> props) {
        if (formCreateMeta == null || formCreateMeta.isEmpty()) {
            return;
        }
        Object style = firstPresent(formCreateMeta.get("style"), props.get("style"));
        if (style != null) {
            item.put("componentStyle", style);
        }
        Object componentClass = firstPresent(props.get("className"), props.get("class"));
        if (componentClass != null) {
            item.put("componentClass", componentClass);
        }
        Object formItemClass = firstPresent(formCreateMeta.get("className"), formCreateMeta.get("class"));
        if (formItemClass != null) {
            item.put("formItemClass", formItemClass);
        }
        Map<String, Object> wrap = mapValue(formCreateMeta.get("wrap"));
        if (wrap.get("style") != null) {
            item.put("formItemStyle", wrap.get("style"));
        }
        if (wrap.containsKey("labelWidth")) {
            item.put("labelWidth", wrap.get("labelWidth"));
        }
        if (wrap.containsKey("show") && !booleanWithDefault(wrap.get("show"), true)) {
            item.put("showLabel", false);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return new LinkedHashMap<>();
    }

    /** 非 STATIC（含 QUERY_SOURCE/REMOTE/DICT 等）才视为动态选项源。 */
    private static boolean isDynamicOptionSourceType(Object rawType) {
        if (rawType == null) {
            return false;
        }
        String type = String.valueOf(rawType).trim();
        if (type.isEmpty()) {
            return false;
        }
        return !"STATIC".equalsIgnoreCase(type.replace('-', '_'));
    }

}
