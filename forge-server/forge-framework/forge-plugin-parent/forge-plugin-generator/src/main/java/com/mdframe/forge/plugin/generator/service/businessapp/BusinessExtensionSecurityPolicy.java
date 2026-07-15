package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 扩展版本内容的通用大小、JSON 和敏感信息防护。
 */
final class BusinessExtensionSecurityPolicy {

    static final int MAX_CONTENT_LENGTH = 128 * 1024;
    static final int MAX_CONFIG_LENGTH = 32 * 1024;

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "token", "accesstoken", "access_token", "password", "secret", "clientsecret",
            "client_secret", "webhooksecret", "webhook_secret", "apikey", "api_key", "ak", "sk",
            "authorization", "cookie"
    );

    private BusinessExtensionSecurityPolicy() {
    }

    static String normalizeContent(String content) {
        String value = StringUtils.defaultString(content);
        if (value.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("扩展内容不能超过128KB");
        }
        return value;
    }

    static String normalizeProcessedContent(String content) {
        String value = StringUtils.trimToNull(content);
        if (value != null && value.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("扩展处理后内容不能超过128KB");
        }
        return value;
    }

    static String normalizeConfig(ObjectMapper objectMapper, String configJson) {
        String value = StringUtils.trimToNull(configJson);
        if (value == null) {
            return "{}";
        }
        if (value.length() > MAX_CONFIG_LENGTH) {
            throw new BusinessException("扩展配置不能超过32KB");
        }
        try {
            JsonNode node = objectMapper.readTree(value);
            if (node == null || !node.isObject()) {
                throw new BusinessException("扩展配置必须是合法 JSON 对象");
            }
            assertNoSensitiveKeys(node);
            return objectMapper.writeValueAsString(node);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("扩展配置必须是合法 JSON 对象");
        }
    }

    static String contentHash(String content, String processedContent, String configJson) {
        return DigestUtils.sha256Hex(StringUtils.defaultString(content) + '\n'
                + StringUtils.defaultString(processedContent) + '\n' + StringUtils.defaultString(configJson));
    }

    private static void assertNoSensitiveKeys(JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String normalized = field.getKey().replace("-", "_").toLowerCase(Locale.ROOT);
                String compact = normalized.replace("_", "");
                if (SENSITIVE_KEYS.contains(normalized) || SENSITIVE_KEYS.contains(compact)
                        || compact.endsWith("token") || compact.endsWith("secret")
                        || compact.endsWith("password") || compact.endsWith("apikey")) {
                    throw new BusinessException("扩展配置不能保存明文密钥、Token、Cookie 或密码等敏感信息");
                }
                assertNoSensitiveKeys(field.getValue());
            }
        } else if (node.isArray()) {
            node.forEach(BusinessExtensionSecurityPolicy::assertNoSensitiveKeys);
        }
    }
}
