package com.mdframe.forge.starter.config.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.starter.config.entity.SysConfigGroup;
import com.mdframe.forge.starter.core.util.CryptoDeploymentSecretPolicy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * crypto 配置分组响应清洗与写入检测。
 */
@Component
public class CryptoConfigSanitizer {

    private final ObjectMapper objectMapper;

    public CryptoConfigSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SysConfigGroup sanitize(SysConfigGroup group) {
        if (group == null || !CryptoDeploymentSecretPolicy.isCryptoGroup(group.getGroupCode())) {
            return group;
        }
        group.setConfigValue(sanitizeJson(group.getConfigValue()));
        return group;
    }

    public boolean containsDeploymentSecretValue(String groupCode, String configValue) {
        if (!CryptoDeploymentSecretPolicy.isCryptoGroup(groupCode)) {
            return false;
        }
        if (configValue == null || configValue.isBlank()) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(configValue);
            return containsDeploymentSecretField(root);
        } catch (Exception e) {
            return true;
        }
    }

    public String sanitizeJson(String configValue) {
        if (configValue == null || configValue.isBlank()) {
            return configValue;
        }
        try {
            JsonNode root = objectMapper.readTree(configValue);
            removeDeploymentSecretFields(root);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{}";
        }
    }

    private boolean containsDeploymentSecretField(JsonNode node) {
        if (node == null) {
            return false;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (CryptoDeploymentSecretPolicy.isDeploymentSecretJsonField(field.getKey())
                        || containsDeploymentSecretField(field.getValue())) {
                    return true;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                if (containsDeploymentSecretField(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void removeDeploymentSecretFields(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<String> names = objectNode.fieldNames();
            List<String> removeNames = new ArrayList<>();
            while (names.hasNext()) {
                String name = names.next();
                if (CryptoDeploymentSecretPolicy.isDeploymentSecretJsonField(name)) {
                    removeNames.add(name);
                }
            }
            removeNames.forEach(objectNode::remove);
            objectNode.elements().forEachRemaining(this::removeDeploymentSecretFields);
        } else if (node.isArray()) {
            node.elements().forEachRemaining(this::removeDeploymentSecretFields);
        }
    }
}
