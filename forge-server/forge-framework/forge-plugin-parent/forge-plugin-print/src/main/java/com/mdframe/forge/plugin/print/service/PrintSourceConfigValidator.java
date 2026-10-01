package com.mdframe.forge.plugin.print.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.print.enums.PrintBusinessSourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PrintSourceConfigValidator {

    private static final int MAX_JSON_BYTES = 64 * 1024;

    private final ObjectMapper objectMapper;

    private final PrintParameterValidator parameterValidator;

    public void validate(PrintBusinessSourceType type,
                         String providerCode,
                         Long datasetId,
                         String parameterSchemaJson,
                         String mappingJson) {
        if (type == null) {
            throw invalid("打印来源类型不能为空");
        }
        validateType(type, providerCode, datasetId);
        requireObject(parameterSchemaJson, "参数协议");
        parameterValidator.validateSchema(parameterSchemaJson);
        requireObject(mappingJson, "数据映射");
        if (type == PrintBusinessSourceType.DATASET) {
            validateDatasetMapping(mappingJson);
        }
    }

    private void validateType(PrintBusinessSourceType type, String providerCode, Long datasetId) {
        switch (type) {
            case SERVICE -> {
                if (providerCode == null || providerCode.isBlank() || datasetId != null) {
                    throw invalid("服务来源必须选择受管 Provider");
                }
            }
            case DATASET -> {
                if (datasetId == null || providerCode != null) {
                    throw invalid("数据集来源必须选择已发布数据集");
                }
            }
            case API -> throw invalid("受管接口来源尚未开放");
            default -> throw invalid("不支持的打印来源类型");
        }
    }

    private void requireObject(String json, String label) {
        if (json == null || json.isBlank()) {
            return;
        }
        if (json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_JSON_BYTES) {
            throw invalid(label + "超过大小限制");
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node == null || !node.isObject()) {
                throw invalid(label + "必须是 JSON 对象");
            }
        } catch (JsonProcessingException error) {
            throw invalid(label + "不是合法 JSON");
        }
    }

    private void validateDatasetMapping(String json) {
        try {
            JsonNode node = json == null || json.isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(json);
            String recordIdParam = node.path("recordIdParam").asText("");
            String childrenKey = node.path("childrenKey").asText("");
            int maxRows = node.path("maxRows").asInt(1000);
            if (!recordIdParam.matches("[A-Za-z][A-Za-z0-9_]{0,79}")
                    || !childrenKey.isEmpty()
                    && !childrenKey.matches("[A-Za-z][A-Za-z0-9_]{0,79}")
                    || maxRows < 1 || maxRows > 10000) {
                throw invalid("数据集来源必须配置有效的记录参数和行数限制");
            }
        } catch (JsonProcessingException error) {
            throw invalid("数据映射不是合法 JSON");
        }
    }

    private RuntimeException invalid(String message) {
        return PrintFailure.of(400, "PRINT_SOURCE_CONFIG_INVALID", message);
    }
}
