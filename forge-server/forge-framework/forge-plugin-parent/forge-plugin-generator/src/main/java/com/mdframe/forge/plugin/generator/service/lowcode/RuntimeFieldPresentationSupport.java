package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerLayoutReader.normalizeAlign;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerLayoutReader.text;

/** 运行时字段展示与组件属性的共享白名单，供搜索、表格和编辑编译复用。 */
final class RuntimeFieldPresentationSupport {

    private RuntimeFieldPresentationSupport() {
    }

    static void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    static void applyAlignment(Map<String, Object> item, Map<String, Object> pageSetting) {
        String align = normalizeAlign(StringUtils.defaultIfBlank(text(pageSetting.get("align")),
                text(pageSetting.get("textAlign"))));
        if (StringUtils.isNotBlank(align)) {
            item.put("align", align);
        }
    }

    static String normalizeFixed(String value) {
        String fixed = StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
        return Set.of("left", "right").contains(fixed) ? fixed : null;
    }

    static Map<String, Object> sanitizeFieldBasicProps(LowcodeFieldSchema field) {
        if (field == null || field.getBasicProps() == null || field.getBasicProps().isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> props = new LinkedHashMap<>();
        copyBasicProp(field.getBasicProps(), props, "placeholder");
        copyBasicProp(field.getBasicProps(), props, "cascade");
        copyBasicProp(field.getBasicProps(), props, "cascadeConfig");
        copyBasicProp(field.getBasicProps(), props, "sourceField");
        copyBasicProp(field.getBasicProps(), props, "sourceDictType");
        copyBasicProp(field.getBasicProps(), props, "linkedDictType");
        copyBasicProp(field.getBasicProps(), props, "linkedDictValue");
        copyBasicProp(field.getBasicProps(), props, "parentDictCode");
        copyBasicProp(field.getBasicProps(), props, "matchMode");
        copyBasicProp(field.getBasicProps(), props, "emptyStrategy");
        copyBasicProp(field.getBasicProps(), props, "clearOnSourceChange");
        copyBasicProp(field.getBasicProps(), props, "clearable");
        copyBasicProp(field.getBasicProps(), props, "filterable");
        copyBasicProp(field.getBasicProps(), props, "multiple");
        copyBasicProp(field.getBasicProps(), props, "optionSource");
        copyBasicProp(field.getBasicProps(), props, "options");
        copyBasicProp(field.getBasicProps(), props, "fieldMappings");
        copyBasicProp(field.getBasicProps(), props, "mappings");
        copyBasicProp(field.getBasicProps(), props, "labelValueField");
        copyBasicProp(field.getBasicProps(), props, "targetField");
        copyBasicProp(field.getBasicProps(), props, "rootCode");
        copyBasicProp(field.getBasicProps(), props, "dataRight");
        copyBasicProp(field.getBasicProps(), props, "virtualDisabled");
        copyBasicProp(field.getBasicProps(), props, "limit");
        copyBasicProp(field.getBasicProps(), props, "fileSize");
        copyBasicProp(field.getBasicProps(), props, "fileType");
        copyBasicProp(field.getBasicProps(), props, "storageType");
        copyBasicProp(field.getBasicProps(), props, "valueType");
        copyBasicProp(field.getBasicProps(), props, "showTip");
        copyBasicProp(field.getBasicProps(), props, "showFileList");
        copyBasicProp(field.getBasicProps(), props, "uploadButtonText");
        copyBasicProp(field.getBasicProps(), props, "businessType");
        copyBasicProp(field.getBasicProps(), props, "businessId");
        copyBasicProp(field.getBasicProps(), props, "referenceObjectCode");
        copyBasicProp(field.getBasicProps(), props, "referenceDisplayField");
        copyBasicProp(field.getBasicProps(), props, "referenceValueField");
        copyBasicProp(field.getBasicProps(), props, "targetObjectCode");
        copyBasicProp(field.getBasicProps(), props, "recordSelector");
        copyBasicProp(field.getBasicProps(), props, "recordSelectorConfig");
        copyBasicProp(field.getBasicProps(), props, "selector");
        copyBasicProp(field.getBasicProps(), props, "selectorConfig");
        copyBasicProp(field.getBasicProps(), props, "relationKey");
        copyBasicProp(field.getBasicProps(), props, "inlineCreateEnabled");
        copyBasicProp(field.getBasicProps(), props, "showInDetail");
        copyBasicProp(field.getBasicProps(), props, "validation");
        copyBasicProp(field.getBasicProps(), props, "min");
        copyBasicProp(field.getBasicProps(), props, "max");
        copyBasicProp(field.getBasicProps(), props, "minimum");
        copyBasicProp(field.getBasicProps(), props, "maximum");
        copyBasicProp(field.getBasicProps(), props, "step");
        copyBasicProp(field.getBasicProps(), props, "precision");
        copyBasicProp(field.getBasicProps(), props, "maxlength");
        copyBasicProp(field.getBasicProps(), props, "maxLength");
        copyBasicProp(field.getBasicProps(), props, "checkedValue");
        copyBasicProp(field.getBasicProps(), props, "uncheckedValue");
        copyBasicProp(field.getBasicProps(), props, "checkedText");
        copyBasicProp(field.getBasicProps(), props, "uncheckedText");
        copyBasicProp(field.getBasicProps(), props, "runtimeRules");
        copyBasicProp(field.getBasicProps(), props, "__events");
        return props;
    }

    private static void copyBasicProp(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    static boolean isSystemField(LowcodeFieldSchema field) {
        return field != null && Boolean.TRUE.equals(field.getSystemField());
    }

}
