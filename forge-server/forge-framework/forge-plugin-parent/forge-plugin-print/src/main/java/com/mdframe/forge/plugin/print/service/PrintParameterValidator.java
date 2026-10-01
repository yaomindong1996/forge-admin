package com.mdframe.forge.plugin.print.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 校验业务页面传入的受控打印参数。安全身份和版本字段永远不能由调用方覆盖。
 */
@Component
@RequiredArgsConstructor
public class PrintParameterValidator {

    private static final int MAX_BYTES = 64 * 1024;
    private static final Set<String> TYPES = Set.of("string", "integer", "number", "boolean");
    private static final Set<String> RESERVED = Set.of(
            "tenantId", "userId", "actor", "applicationId", "applicationVersionId",
            "businessSourceId", "sourceCode", "sourceRevision", "templateId",
            "templateVersionId", "processRunId");

    private final ObjectMapper mapper;

    public void validateSchema(String schemaJson) {
        JsonNode schema = parseSchema(schemaJson);
        Iterator<Map.Entry<String, JsonNode>> fields = schema.fields();
        while (fields.hasNext()) {
            var field = fields.next();
            validateFieldName(field.getKey());
            validateDefinition(field.getKey(), field.getValue());
        }
    }

    public Map<String, Object> validate(String schemaJson, Map<String, Object> input) {
        JsonNode schema = parseSchema(schemaJson);
        Map<String, Object> params = input == null ? Map.of() : input;
        requireSize(params);
        rejectUnknown(schema, params);
        Map<String, Object> result = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = schema.fields();
        while (fields.hasNext()) {
            var field = fields.next();
            validateFieldName(field.getKey());
            validateDefinition(field.getKey(), field.getValue());
            Object value = params.get(field.getKey());
            if (value == null && field.getValue().has("default")) {
                value = mapper.convertValue(field.getValue().get("default"), Object.class);
            }
            Object normalized = validateValue(field.getKey(), field.getValue(), value);
            if (normalized != null) {
                result.put(field.getKey(), normalized);
            }
        }
        return Map.copyOf(result);
    }

    private JsonNode parseSchema(String schemaJson) {
        try {
            JsonNode schema = schemaJson == null || schemaJson.isBlank()
                    ? mapper.createObjectNode()
                    : mapper.readTree(schemaJson);
            if (schema == null || !schema.isObject()) {
                throw invalid("打印参数协议必须是 JSON 对象");
            }
            return schema;
        } catch (java.io.IOException error) {
            throw invalid("打印参数协议不是合法 JSON");
        }
    }

    private void requireSize(Map<String, Object> params) {
        if (params.size() > 50) {
            throw invalid("打印参数数量超过限制");
        }
        byte[] encoded;
        try {
            encoded = mapper.writeValueAsBytes(params);
        } catch (java.io.IOException error) {
            throw invalid("打印参数无法序列化");
        }
        if (encoded.length > MAX_BYTES) {
            throw invalid("打印参数超过大小限制");
        }
    }

    private void rejectUnknown(JsonNode schema, Map<String, Object> params) {
        for (String key : params.keySet()) {
            validateFieldName(key);
            if (!schema.has(key)) {
                throw invalid("打印参数未在协议中声明：" + key);
            }
        }
    }

    private void validateFieldName(String name) {
        if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_]{0,79}")
                || RESERVED.contains(name)) {
            throw invalid("打印参数名称不允许使用：" + name);
        }
    }

    private Object validateValue(String name, JsonNode definition, Object value) {
        String type = definition.path("type").asText("").toLowerCase(java.util.Locale.ROOT);
        if (value == null) {
            if (definition.path("required").asBoolean(false)) {
                throw invalid("缺少必填打印参数：" + name);
            }
            return null;
        }
        Object normalized = switch (type) {
            case "string" -> stringValue(name, definition, value);
            case "integer" -> integerValue(name, definition, value);
            case "number" -> numberValue(name, definition, value);
            case "boolean" -> booleanValue(name, value);
            default -> throw invalid("打印参数类型不支持：" + name);
        };
        validateEnum(name, definition, normalized);
        return normalized;
    }

    private void validateDefinition(String name, JsonNode definition) {
        if (!definition.isObject()) {
            throw invalid("打印参数定义必须是对象：" + name);
        }
        String type = definition.path("type").asText("").toLowerCase(java.util.Locale.ROOT);
        if (!TYPES.contains(type)) {
            throw invalid("打印参数类型不支持：" + name);
        }
        if (definition.has("maxLength")) {
            int maxLength = definition.path("maxLength").asInt(-1);
            if (!"string".equals(type) || maxLength < 1 || maxLength > 10000) {
                throw invalid("打印参数文本长度定义无效：" + name);
            }
        }
        if (definition.has("min") && !definition.get("min").isNumber()
                || definition.has("max") && !definition.get("max").isNumber()) {
            throw invalid("打印参数数值范围定义无效：" + name);
        }
        JsonNode options = definition.get("enum");
        if (options != null && (!options.isArray() || options.isEmpty())) {
            throw invalid("打印参数枚举定义无效：" + name);
        }
    }

    private String stringValue(String name, JsonNode definition, Object value) {
        if (!(value instanceof String text)) {
            throw invalid("打印参数类型不匹配：" + name);
        }
        int maxLength = definition.path("maxLength").asInt(1000);
        if (maxLength < 1 || maxLength > 10000 || text.length() > maxLength) {
            throw invalid("打印参数文本长度无效：" + name);
        }
        return text;
    }

    private Long integerValue(String name, JsonNode definition, Object value) {
        if (!(value instanceof Number number)) {
            throw invalid("打印参数类型不匹配：" + name);
        }
        try {
            Long result = new BigDecimal(number.toString()).longValueExact();
            validateRange(name, definition, BigDecimal.valueOf(result));
            return result;
        } catch (ArithmeticException | NumberFormatException error) {
            throw invalid("打印参数必须是整数：" + name);
        }
    }

    private BigDecimal numberValue(String name, JsonNode definition, Object value) {
        if (!(value instanceof Number number)) {
            throw invalid("打印参数类型不匹配：" + name);
        }
        BigDecimal decimal;
        try {
            decimal = new BigDecimal(number.toString());
        } catch (NumberFormatException error) {
            throw invalid("打印参数必须是有限数字：" + name);
        }
        validateRange(name, definition, decimal);
        return decimal;
    }

    private void validateRange(String name, JsonNode definition, BigDecimal value) {
        if ((definition.has("min") && value.compareTo(definition.get("min").decimalValue()) < 0)
                || (definition.has("max")
                && value.compareTo(definition.get("max").decimalValue()) > 0)) {
            throw invalid("打印参数数值超出范围：" + name);
        }
    }

    private Boolean booleanValue(String name, Object value) {
        if (!(value instanceof Boolean result)) {
            throw invalid("打印参数类型不匹配：" + name);
        }
        return result;
    }

    private void validateEnum(String name, JsonNode definition, Object value) {
        JsonNode options = definition.get("enum");
        if (options == null) {
            return;
        }
        if (!options.isArray() || options.isEmpty()) {
            throw invalid("打印参数枚举定义无效：" + name);
        }
        JsonNode actual = mapper.valueToTree(value);
        for (JsonNode option : options) {
            if (option.equals(actual)) {
                return;
            }
        }
        throw invalid("打印参数不在允许范围内：" + name);
    }

    private RuntimeException invalid(String message) {
        return PrintFailure.of(400, "PRINT_PARAMETER_INVALID", message);
    }
}
