package com.mdframe.forge.plugin.mcp.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.model.CapabilityResult;
import com.mdframe.forge.plugin.capability.model.CapabilityResultStatus;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.LinkedHashMap;
import java.util.Map;

public final class McpCapabilityResultMapper {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public McpCapabilityResultMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public McpSchema.CallToolResult toMcpResult(CapabilityResult result) {
        boolean error = result.status() == CapabilityResultStatus.ERROR;
        Map<String, Object> structured = new LinkedHashMap<>();
        if (error) {
            structured.put("requestId", result.requestId());
            structured.put("errorCode", result.errorCode());
            structured.put("message", result.message());
        }
        else if (result.data() != null) {
            structured.putAll(objectMapper.convertValue(result.data(), MAP_TYPE));
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("requestId", result.requestId());
        meta.put("durationMs", result.durationMs());
        if (result.errorCode() != null) {
            meta.put("errorCode", result.errorCode());
        }
        return McpSchema.CallToolResult.builder()
                .addTextContent(writeJson(structured))
                .structuredContent(Map.copyOf(structured))
                .isError(error)
                .meta(Map.copyOf(meta))
                .build();
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException exception) {
            return "{\"errorCode\":\"INTERNAL_ERROR\",\"message\":\"结果序列化失败\"}";
        }
    }
}
