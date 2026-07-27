package com.mdframe.forge.plugin.generator.service.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.crypto.CryptoAlgorithm;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 低代码 encrypt_config 解析器。
 */
@Component
@RequiredArgsConstructor
public class LowcodeEncryptConfigParser {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,63}$");

    private final ObjectMapper objectMapper;

    public List<FieldRule> parse(String encryptConfigJson) {
        if (StringUtils.isBlank(encryptConfigJson)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(encryptConfigJson);
            if (!root.isObject()) {
                throw new BusinessException("encryptConfig必须为JSON对象");
            }
            List<FieldRule> rules = new ArrayList<>();
            root.properties().forEach(entry -> rules.add(parseRule(entry.getKey(), entry.getValue())));
            return rules;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("encryptConfig格式不正确");
        }
    }

    private FieldRule parseRule(String fieldName, JsonNode ruleNode) {
        validateIdentifier(fieldName, "加密字段名");
        if (ruleNode == null || !ruleNode.isObject()) {
            throw new BusinessException("encryptConfig字段规则必须为JSON对象: " + fieldName);
        }
        String algorithm = text(ruleNode, "algorithm");
        if (StringUtils.isBlank(algorithm)) {
            throw new BusinessException("加密字段缺少algorithm: " + fieldName);
        }
        try {
            algorithm = CryptoAlgorithm.fromCode(algorithm).getCode();
        } catch (IllegalArgumentException e) {
            throw new BusinessException("不支持的低代码加密算法: " + algorithm);
        }
        String columnName = StringUtils.firstNonBlank(
                text(ruleNode, "columnName"),
                text(ruleNode, "column"),
                text(ruleNode, "dbColumn"),
                DynamicQueryGenerator.camelToSnake(fieldName)
        );
        validateIdentifier(columnName, "加密字段列名");
        return new FieldRule(fieldName, columnName, algorithm);
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private void validateIdentifier(String value, String label) {
        if (StringUtils.isBlank(value) || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new BusinessException(label + "非法: " + value);
        }
    }

    public record FieldRule(String fieldName, String columnName, String algorithm) {
    }
}
