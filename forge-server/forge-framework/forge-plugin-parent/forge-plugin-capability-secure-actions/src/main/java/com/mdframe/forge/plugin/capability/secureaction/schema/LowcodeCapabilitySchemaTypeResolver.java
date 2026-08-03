package com.mdframe.forge.plugin.capability.secureaction.schema;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * 将低代码发布快照中的字段类型稳定映射为能力 JSON Schema 类型。
 */
public final class LowcodeCapabilitySchemaTypeResolver {

    private static final Set<String> INTEGER_TYPES = Set.of(
            "int", "integer", "smallint", "mediumint", "tinyint", "bigint", "long", "serial");
    private static final Set<String> NUMBER_TYPES = Set.of(
            "decimal", "numeric", "double", "float", "real", "number");
    private static final Set<String> BOOLEAN_TYPES = Set.of("bool", "boolean", "bit");
    private static final Set<String> OBJECT_TYPES = Set.of("json", "jsonb", "object");
    private static final Set<String> ARRAY_TYPES = Set.of("array");
    private static final Set<String> INTEGER_BUSINESS_TYPES = Set.of(
            "USER", "DEPT", "ORG", "REFERENCE", "RECORD_SELECTOR");
    private static final Set<String> NUMERIC_COMPONENT_TYPES = Set.of(
            "number", "inputnumber", "input-number", "integer");

    private LowcodeCapabilitySchemaTypeResolver() {
    }

    public static String resolve(LowcodeFieldSchema field) {
        if (field == null) {
            return "string";
        }
        String rawDataType = StringUtils.trimToEmpty(field.getDataType()).toLowerCase(Locale.ROOT);
        String dataType = normalizeDataType(rawDataType);
        String businessType = StringUtils.trimToEmpty(field.getBusinessFieldType()).toUpperCase(Locale.ROOT);
        String componentType = StringUtils.trimToEmpty(field.getComponentType()).toLowerCase(Locale.ROOT);

        if (BOOLEAN_TYPES.contains(dataType)
                || rawDataType.matches("^tinyint\\s*\\(\\s*1\\s*\\).*$")
                || "SWITCH".equals(businessType)
                || "switch".equals(componentType)) {
            return "boolean";
        }
        if ("MONEY".equals(businessType) || "money".equals(dataType)) {
            return "integer";
        }
        if (INTEGER_TYPES.contains(dataType) || INTEGER_BUSINESS_TYPES.contains(businessType)) {
            return "integer";
        }
        if (NUMBER_TYPES.contains(dataType)) {
            return "number";
        }
        if ("NUMBER".equals(businessType) || NUMERIC_COMPONENT_TYPES.contains(componentType)) {
            return "integer";
        }
        if (ARRAY_TYPES.contains(dataType)) {
            return "array";
        }
        if (OBJECT_TYPES.contains(dataType)) {
            return "object";
        }
        return "string";
    }

    private static String normalizeDataType(String dataType) {
        if (StringUtils.isBlank(dataType)) {
            return "";
        }
        String normalized = dataType
                .replace("`", "")
                .replaceAll("\\([^)]*\\)", " ")
                .replaceAll("\\s+", " ")
                .trim();
        int separator = normalized.indexOf(' ');
        return separator < 0 ? normalized : normalized.substring(0, separator);
    }
}
