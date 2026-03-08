package com.mdframe.forge.starter.config.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置转换器
 * 将JSON格式的配置值转换为sys_config表所需的键值对
 */
@Slf4j
@Component
public class ConfigConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将登录配置JSON转换为键值对
     */
    public Map<String, String> convertLoginConfig(String configJson) throws JsonProcessingException {
        Map<String, String> configMap = new HashMap<>();
        JsonNode rootNode = objectMapper.readTree(configJson);

        putIfNotNull(configMap, "forge.auth.captcha.enabled", rootNode, "enableCaptcha");
        putIfNotNull(configMap, "forge.auth.captcha.type", rootNode, "captchaType");
        putIfNotNull(configMap, "forge.auth.max-retry-count", rootNode, "maxRetryCount");
        putIfNotNull(configMap, "forge.auth.lock-time-minutes", rootNode, "lockTimeMinutes");
        putIfNotNull(configMap, "forge.auth.remember-me.enabled", rootNode, "enableRememberMe");
        putIfNotNull(configMap, "forge.auth.remember-me.days", rootNode, "rememberMeDays");

        return configMap;
    }

    /**
     * 将水印配置JSON转换为键值对
     */
    public Map<String, String> convertWatermarkConfig(String configJson) throws JsonProcessingException {
        Map<String, String> configMap = new HashMap<>();
        JsonNode rootNode = objectMapper.readTree(configJson);

        putIfNotNull(configMap, "forge.watermark.enable", rootNode, "enable");
        putIfNotNull(configMap, "forge.watermark.content", rootNode, "content");
        putIfNotNull(configMap, "forge.watermark.opacity", rootNode, "opacity");
        putIfNotNull(configMap, "forge.watermark.font-size", rootNode, "fontSize");
        putIfNotNull(configMap, "forge.watermark.font-color", rootNode, "fontColor");
        putIfNotNull(configMap, "forge.watermark.rotate", rootNode, "rotate");
        putIfNotNull(configMap, "forge.watermark.gap-x", rootNode, "gapX");
        putIfNotNull(configMap, "forge.watermark.gap-y", rootNode, "gapY");
        putIfNotNull(configMap, "forge.watermark.offset-x", rootNode, "offsetX");
        putIfNotNull(configMap, "forge.watermark.offset-y", rootNode, "offsetY");
        putIfNotNull(configMap, "forge.watermark.z-index", rootNode, "zIndex");
        putIfNotNull(configMap, "forge.watermark.show-timestamp", rootNode, "showTimestamp");
        putIfNotNull(configMap, "forge.watermark.timestamp-format", rootNode, "timestampFormat");

        return configMap;
    }

    /**
     * 将加密配置JSON转换为键值对
     */
    public Map<String, String> convertCryptoConfig(String configJson) throws JsonProcessingException {
        Map<String, String> configMap = new HashMap<>();
        JsonNode rootNode = objectMapper.readTree(configJson);

        putIfNotNull(configMap, "forge.crypto.enabled", rootNode, "enabled");
        putIfNotNull(configMap, "forge.crypto.algorithm", rootNode, "algorithm");
        putIfNotNull(configMap, "forge.crypto.secret-key", rootNode, "secretKey");
        putIfNotNull(configMap, "forge.crypto.enable-dynamic-key", rootNode, "enableDynamicKey");
        putIfNotNull(configMap, "forge.crypto.rsa-public-key", rootNode, "rsaPublicKey");
        putIfNotNull(configMap, "forge.crypto.rsa-private-key", rootNode, "rsaPrivateKey");
        putIfNotNull(configMap, "forge.crypto.session-key-expire", rootNode, "sessionKeyExpire");
        putIfNotNull(configMap, "forge.crypto.enable-api-crypto", rootNode, "enableApiCrypto");
        putIfNotNull(configMap, "forge.crypto.enable-field-crypto", rootNode, "enableFieldCrypto");
        putIfNotNull(configMap, "forge.crypto.enable-replay-protection", rootNode, "enableReplayProtection");
        putIfNotNull(configMap, "forge.crypto.replay-time-window", rootNode, "replayTimeWindow");

        // 处理列表类型字段
        JsonNode excludePathsNode = rootNode.get("excludePaths");
        if (excludePathsNode != null && excludePathsNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < excludePathsNode.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(excludePathsNode.get(i).asText());
            }
            configMap.put("forge.crypto.exclude-paths", sb.toString());
        }
        JsonNode replayExcludePathsNode = rootNode.get("replayExcludePaths");
        if (replayExcludePathsNode != null && replayExcludePathsNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < replayExcludePathsNode.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(replayExcludePathsNode.get(i).asText());
            }
            configMap.put("forge.crypto.replay-exclude-paths", sb.toString());
        }

        return configMap;
    }

    /**
     * 将认证配置JSON转换为键值对
     */
    public Map<String, String> convertAuthConfig(String configJson) throws JsonProcessingException {
        Map<String, String> configMap = new HashMap<>();
        JsonNode rootNode = objectMapper.readTree(configJson);

        putIfNotNull(configMap, "forge.auth.enable-api-permission", rootNode, "enableApiPermission");
        putIfNotNull(configMap, "forge.auth.enable-login-lock", rootNode, "enableLoginLock");
        putIfNotNull(configMap, "forge.auth.max-login-attempts", rootNode, "maxLoginAttempts");
        putIfNotNull(configMap, "forge.auth.lock-duration", rootNode, "lockDuration");
        putIfNotNull(configMap, "forge.auth.fail-record-expire", rootNode, "failRecordExpire");
        putIfNotNull(configMap, "forge.auth.same-account-login-strategy", rootNode, "sameAccountLoginStrategy");
        putIfNotNull(configMap, "forge.auth.enable-online-user-management", rootNode, "enableOnlineUserManagement");

        // 处理数组类型字段
        JsonNode apiPermissionExcludePathsNode = rootNode.get("apiPermissionExcludePaths");
        if (apiPermissionExcludePathsNode != null && apiPermissionExcludePathsNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < apiPermissionExcludePathsNode.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(apiPermissionExcludePathsNode.get(i).asText());
            }
            configMap.put("forge.auth.api-permission-exclude-paths", sb.toString());
        }

        return configMap;
    }

    /**
     * 将日志配置JSON转换为键值对
     */
    public Map<String, String> convertLogConfig(String configJson) throws JsonProcessingException {
        Map<String, String> configMap = new HashMap<>();
        JsonNode rootNode = objectMapper.readTree(configJson);

        putIfNotNull(configMap, "forge.log.enable-operation-log", rootNode, "enableOperationLog");
        putIfNotNull(configMap, "forge.log.enable-login-log", rootNode, "enableLoginLog");
        putIfNotNull(configMap, "forge.log.request-params-max-length", rootNode, "requestParamsMaxLength");
        putIfNotNull(configMap, "forge.log.response-result-max-length", rootNode, "responseResultMaxLength");
        putIfNotNull(configMap, "forge.log.print-operation-log", rootNode, "printOperationLog");
        putIfNotNull(configMap, "forge.log.print-login-log", rootNode, "printLoginLog");
        putIfNotNull(configMap, "forge.log.thread-pool-core-size", rootNode, "threadPoolCoreSize");
        putIfNotNull(configMap, "forge.log.thread-pool-max-size", rootNode, "threadPoolMaxSize");
        putIfNotNull(configMap, "forge.log.thread-pool-queue-capacity", rootNode, "threadPoolQueueCapacity");

        // 处理数组类型字段
        JsonNode excludePathsNode = rootNode.get("excludePaths");
        if (excludePathsNode != null && excludePathsNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < excludePathsNode.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(excludePathsNode.get(i).asText());
            }
            configMap.put("forge.log.exclude-paths", sb.toString());
        }

        return configMap;
    }

    /**
     * 辅助方法：如果JSON节点存在，则将其值放入map中
     */
    private void putIfNotNull(Map<String, String> map, String key, JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode != null) {
            String value = fieldNode.asText();
            if (fieldNode.isBoolean()) {
                value = String.valueOf(fieldNode.asBoolean());
            } else if (fieldNode.isNumber()) {
                value = String.valueOf(fieldNode.asLong());
            }
            map.put(key, value);
        }
    }
}
