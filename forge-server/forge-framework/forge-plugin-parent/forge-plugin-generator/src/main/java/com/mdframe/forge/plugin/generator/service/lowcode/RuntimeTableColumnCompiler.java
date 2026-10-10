package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerLayoutReader.text;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldPresentationSupport.applyAlignment;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldPresentationSupport.normalizeFixed;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldPresentationSupport.putIfNotBlank;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFormRuleSettingResolver.firstPresent;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFormRuleSettingResolver.integerValue;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimePageRefFieldFactory.safeKey;

/** 列表字段到运行时列协议的编译器，复用字段元数据的动态选项源判定。 */
final class RuntimeTableColumnCompiler {

    private final RuntimeFieldMetadataCompiler fieldMetadataCompiler;

    RuntimeTableColumnCompiler(RuntimeFieldMetadataCompiler fieldMetadataCompiler) {
        this.fieldMetadataCompiler = fieldMetadataCompiler;
    }

    Map<String, Object> buildTableColumn(LowcodeFieldSchema field) {
        return buildTableColumn(field, Map.of());
    }

    Map<String, Object> buildTableColumn(LowcodeFieldSchema field, Map<String, Object> pageSetting) {
        return buildTableColumn(field, pageSetting, null, null);
    }

    Map<String, Object> buildTableColumn(LowcodeFieldSchema field,
                                                 Map<String, Object> pageSetting,
                                                 LowcodeModelSchema modelSchema,
                                                 LowcodePageSchema pageSchema) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", field.getField());
        item.put("title", resolveTableColumnTitle(field, pageSetting, pageSchema));
        item.put("dataIndex", field.getField());
        applyAlignment(item, pageSetting);
        Integer settingWidth = integerValue(pageSetting.get("width"));
        if (settingWidth != null && settingWidth > 0) {
            item.put("width", settingWidth);
        } else if (field.getWidth() != null && field.getWidth() > 0) {
            item.put("width", field.getWidth());
        }
        String fixed = normalizeFixed(text(pageSetting.get("fixed")));
        if (StringUtils.isNotBlank(fixed)) {
            item.put("fixed", fixed);
        }
        Object sortable = pageSetting.get("sortable");
        if (Boolean.TRUE.equals(sortable) || Boolean.TRUE.equals(field.getSortable())) {
            item.put("sorter", true);
        }
        if (field.getAdvancedProps() != null && !field.getAdvancedProps().isEmpty()) {
            item.put("advancedProps", new LinkedHashMap<>(field.getAdvancedProps()));
        }
        if (StringUtils.isNotBlank(field.getFieldStatus())) {
            item.put("fieldStatus", field.getFieldStatus());
        }
        item.put("listVisible", true);
        String componentType = StringUtils.defaultIfBlank(text(pageSetting.get("componentType")), field.getComponentType());
        componentType = StringUtils.defaultIfBlank(componentType, "input");
        String renderType = StringUtils.defaultIfBlank(text(pageSetting.get("renderType")),
                resolveDefaultRenderType(field, componentType));
        String targetField = StringUtils.defaultIfBlank(text(pageSetting.get("targetField")), field.getField() + "Name");
        RuntimeRelationLookupCompiler.RelationLookupMeta lookupMeta = RuntimeRelationLookupCompiler.resolve(
                modelSchema, pageSchema, field.getField());
        if (lookupMeta != null) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "relationName");
            render.put("targetField", RuntimeRelationLookupCompiler.displayAlias(field.getField()));
            render.put("relationModelCode", lookupMeta.modelCode());
            render.put("displayField", lookupMeta.displayField());
            item.put("render", render);
        } else if (field.isReferenceField()) {
            // 字段级引用：选中记录时显示名称冗余写入伴随列，relationName 渲染优先读伴随列，存量空值退化显示 ID。
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "relationName");
            render.put("targetField", field.referenceDisplayFieldName());
            item.put("render", render);
        } else if ("dictTag".equals(renderType) || (StringUtils.isBlank(renderType) && StringUtils.isNotBlank(field.getDictType()))) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "dictTag");
            render.put("dictType", field.getDictType());
            item.put("render", render);
        } else if ("orgName".equals(renderType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "orgName");
            render.put("targetField", targetField);
            item.put("render", render);
        } else if ("userName".equals(renderType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "userName");
            render.put("targetField", targetField);
            item.put("render", render);
        } else if ("regionName".equals(renderType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "regionName");
            render.put("targetField", targetField);
            item.put("render", render);
        } else if ("fileUpload".equals(renderType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "fileUpload");
            render.put("targetField", targetField);
            item.put("render", render);
        } else if ("imageUpload".equals(renderType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "imageUpload");
            render.put("targetField", targetField);
            item.put("render", render);
        } else if (fieldMetadataCompiler.hasDynamicOptionSource(field, componentType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "relationName");
            render.put("targetField", field.getField() + "Name");
            item.put("render", render);
        } else if (isStaticOptionComponent(componentType)) {
            // 静态下拉/单选：把 options 打进列协议，列表直接 value→label，不依赖 xxxName
            List<Map<String, Object>> staticOptions = resolveStaticOptions(field, pageSetting);
            if (!staticOptions.isEmpty()) {
                Map<String, Object> render = new LinkedHashMap<>();
                render.put("type", "staticOptions");
                render.put("options", staticOptions);
                item.put("render", render);
            }
        } else if ("switch".equals(renderType) || "switch".equals(componentType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "switch");
            Map<String, Object> basicProps = field.getBasicProps() == null ? Map.of() : field.getBasicProps();
            Object checkedValue = firstPresent(pageSetting.get("checkedValue"), basicProps.get("checkedValue"), 1);
            Object uncheckedValue = firstPresent(pageSetting.get("uncheckedValue"), basicProps.get("uncheckedValue"), 0);
            render.put("checkedValue", checkedValue);
            render.put("uncheckedValue", uncheckedValue);
            Object checkedText = firstPresent(pageSetting.get("checkedText"), basicProps.get("checkedText"));
            Object uncheckedText = firstPresent(pageSetting.get("uncheckedText"), basicProps.get("uncheckedText"));
            if (checkedText != null) {
                render.put("checkedText", checkedText);
            }
            if (uncheckedText != null) {
                render.put("uncheckedText", uncheckedText);
            }
            item.put("render", render);
            if (!item.containsKey("width") && integerValue(pageSetting.get("width")) == null) {
                item.put("width", 100);
            }
            if (!item.containsKey("align")) {
                item.put("align", "center");
            }
        }
        copyTableColumnDesignerSettings(item, pageSetting);
        return item;
    }

    private void copyTableColumnDesignerSettings(Map<String, Object> item, Map<String, Object> pageSetting) {
        putIfNotBlank(item, "renderType", text(pageSetting.get("renderType")));
        putIfNotBlank(item, "targetField", text(pageSetting.get("targetField")));
        putIfNotBlank(item, "textColor", text(pageSetting.get("textColor")));
        String clickAction = StringUtils.defaultIfBlank(text(pageSetting.get("clickAction")), "none");
        if (!"none".equals(clickAction)) {
            item.put("clickAction", clickAction);
            putIfNotBlank(item, "targetPageKey", text(pageSetting.get("targetPageKey")));
            putIfNotBlank(item, "targetFormKey", text(pageSetting.get("targetFormKey")));
            putIfNotBlank(item, "targetParamName", text(pageSetting.get("targetParamName")));
            putIfNotBlank(item, "targetParamField", text(pageSetting.get("targetParamField")));
        }
    }

    private String resolveDefaultRenderType(LowcodeFieldSchema field, String componentType) {
        if (field != null && StringUtils.isNotBlank(field.getDictType())) {
            return "dictTag";
        }
        return switch (StringUtils.defaultString(componentType)) {
            case "orgTreeSelect" -> "orgName";
            case "userSelect" -> "userName";
            case "regionTreeSelect" -> "regionName";
            case "fileUpload", "imageUpload", "switch" -> componentType;
            default -> "";
        };
    }

    private boolean isStaticOptionComponent(String componentType) {
        return Set.of("select", "radio", "radioButton", "checkbox", "cascader", "treeSelect", "transfer")
                .contains(StringUtils.defaultString(componentType));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveStaticOptions(LowcodeFieldSchema field,
                                                           Map<String, Object> pageSetting) {
        Object fromPage = null;
        Object designerProps = pageSetting == null ? null : pageSetting.get("props");
        if (designerProps instanceof Map<?, ?> propsMap) {
            fromPage = propsMap.get("options");
        }
        if (fromPage == null && pageSetting != null) {
            fromPage = pageSetting.get("options");
        }
        Object fromField = field != null && field.getBasicProps() != null
                ? field.getBasicProps().get("options")
                : null;
        Object source = fromPage != null ? fromPage : fromField;
        if (!(source instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add(new LinkedHashMap<>((Map<String, Object>) map));
            }
        }
        return result;
    }

    private String resolveTableColumnTitle(LowcodeFieldSchema field,
                                           Map<String, Object> pageSetting,
                                           LowcodePageSchema pageSchema) {
        String title = StringUtils.firstNonBlank(
                text(pageSetting.get("title")),
                text(pageSetting.get("label")),
                field.getLabel(),
                field.getField());
        return RuntimePageRefFieldFactory.stripChildModelNamePrefix(
                title, resolveChildModelName(pageSchema, field.getField()));
    }

    private String resolveChildModelName(LowcodePageSchema pageSchema, String fieldName) {
        if (pageSchema == null || pageSchema.getModelRefs() == null || StringUtils.isBlank(fieldName)) {
            return null;
        }
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || ref.getFields() == null) {
                continue;
            }
            for (Map<String, Object> source : ref.getFields()) {
                String sourceField = StringUtils.defaultIfBlank(text(source.get("sourceField")), text(source.get("field")));
                String fieldRef = StringUtils.defaultIfBlank(text(source.get("fieldRef")),
                        safeKey(ref.getModelCode()) + "__" + sourceField);
                if (fieldName.equals(fieldRef)) {
                    return ref.getModelName();
                }
            }
        }
        return null;
    }

}
