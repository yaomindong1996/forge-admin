package com.mdframe.forge.plugin.capability.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.mdframe.forge.plugin.capability.exception.CapabilityDefinitionException;
import com.mdframe.forge.plugin.capability.model.CapabilityErrorCode;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class CapabilitySchemaValidator {

    public static final String DRAFT_2020_12 = "https://json-schema.org/draft/2020-12/schema";

    private static final Set<String> SUPPORTED_KEYWORDS = Set.of(
            "$schema", "type", "description", "properties", "required",
            "additionalProperties", "enum", "items", "minimum", "maximum",
            "minLength", "maxLength", "minItems", "maxItems");
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "object", "array", "string", "integer", "number", "boolean", "null");
    private static final Set<String> UNIVERSAL_KEYWORDS = Set.of(
            "$schema", "type", "description", "enum");

    public ValidatedCapabilitySchema validateDefinition(JsonNode schema) {
        if (schema == null || !schema.isObject()) {
            throw unsupported("$", "Schema 必须是 object");
        }
        if (!DRAFT_2020_12.equals(schema.path("$schema").asText())) {
            throw unsupported("$.$schema", "必须显式使用 JSON Schema Draft 2020-12");
        }
        validateSchemaNode(schema, "$", true);
        return new ValidatedCapabilitySchema(schema);
    }

    public void validateInstance(JsonNode schema, JsonNode instance) {
        validateDefinition(schema);
        validateValue(schema, instance, "$");
    }

    private void validateSchemaNode(JsonNode schema, String path, boolean root) {
        Iterator<String> names = schema.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!SUPPORTED_KEYWORDS.contains(name)) {
                throw unsupported(path + "." + name, "不支持的 Schema 关键字 " + name);
            }
            if (!root && "$schema".equals(name)) {
                throw unsupported(path + ".$schema", "$schema 只能出现在根节点");
            }
        }

        Set<String> types = parseTypes(schema.path("type"), path + ".type");
        if (types.isEmpty()) {
            throw unsupported(path + ".type", "每个 Schema 节点都必须声明 type");
        }
        if (types.size() > 2 || (types.size() == 2 && !types.contains("null"))) {
            throw unsupported(path + ".type", "类型联合只支持一种基础类型与 null");
        }
        String baseType = types.stream().filter(type -> !"null".equals(type)).findFirst().orElse("null");
        validateKeywordApplicability(schema, path, baseType);

        JsonNode description = schema.get("description");
        if (description != null && !description.isTextual()) {
            throw unsupported(path + ".description", "description 必须是字符串");
        }

        validateNonNegativeInteger(schema, "minLength", path);
        validateNonNegativeInteger(schema, "maxLength", path);
        validateNonNegativeInteger(schema, "minItems", path);
        validateNonNegativeInteger(schema, "maxItems", path);
        validateNumber(schema, "minimum", path);
        validateNumber(schema, "maximum", path);
        validateOrderedBounds(schema, "minLength", "maxLength", path);
        validateOrderedBounds(schema, "minItems", "maxItems", path);
        validateOrderedNumbers(schema, "minimum", "maximum", path);

        JsonNode enumNode = schema.get("enum");
        if (enumNode != null && (!enumNode.isArray() || enumNode.isEmpty())) {
            throw unsupported(path + ".enum", "enum 必须是非空数组");
        }
        if (enumNode != null) {
            Set<JsonNode> values = new HashSet<>();
            for (JsonNode value : enumNode) {
                if (!values.add(value)) {
                    throw unsupported(path + ".enum", "enum 不能包含重复值");
                }
                if (!matchesAnyType(types, value)) {
                    throw unsupported(path + ".enum", "enum 值类型必须符合 type");
                }
            }
        }

        if (types.contains("object")) {
            validateObjectSchema(schema, path);
        }
        if (types.contains("array")) {
            JsonNode items = schema.get("items");
            if (items == null || !items.isObject()) {
                throw unsupported(path + ".items", "array 必须声明 object 类型的 items");
            }
            validateSchemaNode(items, path + ".items", false);
        }
    }

    private void validateKeywordApplicability(JsonNode schema, String path, String baseType) {
        Set<String> allowed = new HashSet<>(UNIVERSAL_KEYWORDS);
        switch (baseType) {
            case "object" -> allowed.addAll(Set.of("properties", "required", "additionalProperties"));
            case "array" -> allowed.addAll(Set.of("items", "minItems", "maxItems"));
            case "string" -> allowed.addAll(Set.of("minLength", "maxLength"));
            case "integer", "number" -> allowed.addAll(Set.of("minimum", "maximum"));
            default -> {
                // boolean 与 null 在阶段 0 没有额外关键字。
            }
        }
        schema.fieldNames().forEachRemaining(keyword -> {
            if (!allowed.contains(keyword)) {
                throw unsupported(path + "." + keyword,
                        "关键字 " + keyword + " 不适用于类型 " + baseType);
            }
        });
    }

    private void validateObjectSchema(JsonNode schema, String path) {
        JsonNode properties = schema.get("properties");
        if (properties != null && !properties.isObject()) {
            throw unsupported(path + ".properties", "properties 必须是 object");
        }
        JsonNode additionalProperties = schema.get("additionalProperties");
        if (additionalProperties != null && !additionalProperties.isBoolean()) {
            throw unsupported(path + ".additionalProperties", "additionalProperties 只支持 boolean");
        }
        Set<String> propertyNames = new HashSet<>();
        if (properties != null) {
            properties.properties().forEach(entry -> {
                propertyNames.add(entry.getKey());
                if (!entry.getValue().isObject()) {
                    throw unsupported(path + ".properties." + entry.getKey(), "属性 Schema 必须是 object");
                }
                validateSchemaNode(entry.getValue(), path + ".properties." + entry.getKey(), false);
            });
        }
        JsonNode required = schema.get("required");
        if (required != null) {
            if (!required.isArray()) {
                throw unsupported(path + ".required", "required 必须是数组");
            }
            Set<String> requiredNames = new HashSet<>();
            for (JsonNode item : required) {
                if (!item.isTextual() || !requiredNames.add(item.asText())) {
                    throw unsupported(path + ".required", "required 只能包含不重复的字段名");
                }
                if (!propertyNames.contains(item.asText())) {
                    throw unsupported(path + ".required", "required 字段必须存在于 properties");
                }
            }
        }
    }

    private void validateValue(JsonNode schema, JsonNode value, String path) {
        Set<String> types = parseTypes(schema.path("type"), path + ".type");
        if (!matchesAnyType(types, value)) {
            throw invalid(path, "值类型不符合 Schema");
        }
        if (value == null || value.isNull()) {
            validateEnum(schema, value, path);
            return;
        }
        validateEnum(schema, value, path);

        if (value.isObject()) {
            validateObjectValue(schema, value, path);
        }
        else if (value.isArray()) {
            validateArrayValue(schema, value, path);
        }
        else if (value.isTextual()) {
            validateStringValue(schema, value, path);
        }
        else if (value.isNumber()) {
            validateNumberValue(schema, value, path);
        }
    }

    private void validateObjectValue(JsonNode schema, JsonNode value, String path) {
        JsonNode properties = schema.path("properties");
        JsonNode required = schema.get("required");
        if (required != null) {
            for (JsonNode item : required) {
                if (!value.has(item.asText())) {
                    throw invalid(path + "." + item.asText(), "缺少必填字段");
                }
            }
        }
        boolean allowAdditional = !schema.has("additionalProperties")
                || schema.path("additionalProperties").asBoolean();
        Iterator<String> fields = value.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            JsonNode propertySchema = properties.get(field);
            if (propertySchema == null) {
                if (!allowAdditional) {
                    throw invalid(path + "." + field, "不允许额外字段");
                }
                continue;
            }
            validateValue(propertySchema, value.get(field), path + "." + field);
        }
    }

    private void validateArrayValue(JsonNode schema, JsonNode value, String path) {
        int size = value.size();
        if (schema.has("minItems") && size < schema.path("minItems").asInt()) {
            throw invalid(path, "数组元素数量小于 minItems");
        }
        if (schema.has("maxItems") && size > schema.path("maxItems").asInt()) {
            throw invalid(path, "数组元素数量大于 maxItems");
        }
        JsonNode itemSchema = schema.path("items");
        for (int index = 0; index < size; index++) {
            validateValue(itemSchema, value.get(index), path + "[" + index + "]");
        }
    }

    private void validateStringValue(JsonNode schema, JsonNode value, String path) {
        int length = value.asText().length();
        if (schema.has("minLength") && length < schema.path("minLength").asInt()) {
            throw invalid(path, "字符串长度小于 minLength");
        }
        if (schema.has("maxLength") && length > schema.path("maxLength").asInt()) {
            throw invalid(path, "字符串长度大于 maxLength");
        }
    }

    private void validateNumberValue(JsonNode schema, JsonNode value, String path) {
        BigDecimal number = value.decimalValue();
        if (schema.has("minimum") && number.compareTo(schema.path("minimum").decimalValue()) < 0) {
            throw invalid(path, "数值小于 minimum");
        }
        if (schema.has("maximum") && number.compareTo(schema.path("maximum").decimalValue()) > 0) {
            throw invalid(path, "数值大于 maximum");
        }
    }

    private void validateEnum(JsonNode schema, JsonNode value, String path) {
        JsonNode enumNode = schema.get("enum");
        if (enumNode == null) {
            return;
        }
        for (JsonNode allowed : enumNode) {
            if (allowed.equals(value)) {
                return;
            }
        }
        throw invalid(path, "值不在 enum 范围内");
    }

    private boolean matchesAnyType(Set<String> types, JsonNode value) {
        if (value == null || value.isNull()) {
            return types.contains("null");
        }
        return (types.contains("object") && value.isObject())
                || (types.contains("array") && value.isArray())
                || (types.contains("string") && value.isTextual())
                || (types.contains("integer") && value.isIntegralNumber())
                || (types.contains("number") && value.isNumber())
                || (types.contains("boolean") && value.isBoolean());
    }

    private Set<String> parseTypes(JsonNode typeNode, String path) {
        Set<String> types = new HashSet<>();
        if (typeNode.isTextual()) {
            types.add(typeNode.asText());
        }
        else if (typeNode.isArray()) {
            for (JsonNode item : typeNode) {
                if (!item.isTextual() || !types.add(item.asText())) {
                    throw unsupported(path, "type 数组只能包含不重复的字符串");
                }
            }
        }
        else {
            throw unsupported(path, "type 必须是字符串或字符串数组");
        }
        if (!SUPPORTED_TYPES.containsAll(types)) {
            throw unsupported(path, "包含不支持的类型");
        }
        return types;
    }

    private void validateNonNegativeInteger(JsonNode schema, String keyword, String path) {
        JsonNode value = schema.get(keyword);
        if (value != null && (!value.isIntegralNumber() || value.asLong() < 0)) {
            throw unsupported(path + "." + keyword, keyword + " 必须是非负整数");
        }
    }

    private void validateNumber(JsonNode schema, String keyword, String path) {
        JsonNode value = schema.get(keyword);
        if (value != null && !value.isNumber()) {
            throw unsupported(path + "." + keyword, keyword + " 必须是数值");
        }
    }

    private void validateOrderedBounds(
            JsonNode schema,
            String minimumKeyword,
            String maximumKeyword,
            String path) {
        if (schema.has(minimumKeyword)
                && schema.has(maximumKeyword)
                && schema.path(minimumKeyword).asLong() > schema.path(maximumKeyword).asLong()) {
            throw unsupported(path, minimumKeyword + " 不能大于 " + maximumKeyword);
        }
    }

    private void validateOrderedNumbers(
            JsonNode schema,
            String minimumKeyword,
            String maximumKeyword,
            String path) {
        if (schema.has(minimumKeyword)
                && schema.has(maximumKeyword)
                && schema.path(minimumKeyword).decimalValue()
                .compareTo(schema.path(maximumKeyword).decimalValue()) > 0) {
            throw unsupported(path, minimumKeyword + " 不能大于 " + maximumKeyword);
        }
    }

    private CapabilityDefinitionException unsupported(String path, String message) {
        return new CapabilityDefinitionException(CapabilityErrorCode.SCHEMA_UNSUPPORTED,
                message + "，路径：" + path);
    }

    private CapabilitySchemaValidationException invalid(String path, String message) {
        return new CapabilitySchemaValidationException(path, message);
    }
}
