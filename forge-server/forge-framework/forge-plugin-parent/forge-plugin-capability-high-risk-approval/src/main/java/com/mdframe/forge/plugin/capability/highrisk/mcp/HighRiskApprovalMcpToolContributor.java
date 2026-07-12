package com.mdframe.forge.plugin.capability.highrisk.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.mcp.adapter.McpToolSchemaProjector;
import com.mdframe.forge.plugin.mcp.security.McpTransportContextKeys;
import com.mdframe.forge.plugin.mcp.spi.McpToolContributor;
import com.mdframe.forge.starter.core.exception.BusinessException;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class HighRiskApprovalMcpToolContributor implements McpToolContributor {

    private final HighRiskApprovalQueryService queryService;
    private final ObjectMapper objectMapper;

    @Override
    public Collection<McpServerFeatures.SyncToolSpecification> contribute(
            McpToolSchemaProjector schemaProjector) {
        String description = "查询当前用户和客户端发起的高风险动作审批状态";
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("capability.approval.get")
                .title(description)
                .description(description)
                .inputSchema(inputSchema())
                .outputSchema(outputSchema())
                .annotations(new McpSchema.ToolAnnotations(
                        description, true, false, true, false, false))
                .build();
        return List.of(McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(this::get)
                .build());
    }

    private McpSchema.CallToolResult get(
            McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        String requestId = requestId(exchange);
        try {
            Object value = request == null || request.arguments() == null
                    ? null : request.arguments().get("approvalRequestId");
            Long approvalId = parseId(value);
            return result(queryService.get(approvalId), false, requestId, null);
        }
        catch (RuntimeException exception) {
            String errorCode = errorCode(exception);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("requestId", requestId);
            error.put("errorCode", errorCode);
            error.put("message", safeMessage(errorCode));
            return result(error, true, requestId, errorCode);
        }
    }

    private Long parseId(Object value) {
        String text = value == null ? null : String.valueOf(value).trim();
        if (text == null || !text.matches("^[1-9][0-9]{0,19}$")) {
            throw new BusinessException("INVALID_ARGUMENT");
        }
        try {
            return Long.valueOf(text);
        }
        catch (NumberFormatException exception) {
            throw new BusinessException("INVALID_ARGUMENT");
        }
    }

    private McpSchema.CallToolResult result(
            Map<String, Object> value, boolean error, String requestId, String errorCode) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("requestId", requestId);
        if (errorCode != null) {
            meta.put("errorCode", errorCode);
        }
        try {
            return McpSchema.CallToolResult.builder()
                    .addTextContent(objectMapper.writeValueAsString(value))
                    .structuredContent(value)
                    .isError(error)
                    .meta(Map.copyOf(meta))
                    .build();
        }
        catch (Exception exception) {
            return McpSchema.CallToolResult.builder()
                    .addTextContent("{\"errorCode\":\"INTERNAL_ERROR\"}")
                    .isError(true)
                    .build();
        }
    }

    private McpSchema.JsonSchema inputSchema() {
        return new McpSchema.JsonSchema(
                "object",
                Map.of("approvalRequestId", Map.of(
                        "type", "string", "description", "高风险审批请求ID")),
                List.of("approvalRequestId"), false, Map.of(), Map.of());
    }

    private Map<String, Object> outputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("approvalRequestId", Map.of("type", "string"));
        properties.put("capabilityCode", Map.of("type", "string"));
        properties.put("version", Map.of("type", "string"));
        properties.put("status", Map.of("type", "string"));
        properties.put("resultCode", Map.of("type", "string"));
        properties.put("submittedAt", Map.of("type", "string"));
        properties.put("expiresAt", Map.of("type", "string"));
        properties.put("completedAt", Map.of("type", "string"));
        properties.put("correlationId", Map.of("type", "string"));
        properties.put("message", Map.of("type", "string"));
        return Map.of(
                "$schema", "https://json-schema.org/draft/2020-12/schema",
                "type", "object",
                "properties", properties,
                "required", List.of("approvalRequestId", "capabilityCode", "version",
                        "status", "resultCode", "message"),
                "additionalProperties", false);
    }

    private String requestId(McpSyncServerExchange exchange) {
        if (exchange != null && exchange.transportContext() != null) {
            Object value = exchange.transportContext().get(McpTransportContextKeys.REQUEST_ID);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return UUID.randomUUID().toString();
    }

    private String errorCode(RuntimeException exception) {
        String message = exception.getMessage();
        if (List.of("UNAUTHENTICATED", "USER_DELEGATION_REQUIRED", "FORBIDDEN",
                "APPROVAL_NOT_FOUND", "INVALID_ARGUMENT").contains(message)) {
            return message;
        }
        if (exception instanceof BusinessException business
                && Integer.valueOf(404).equals(business.getCode())) {
            return "APPROVAL_NOT_FOUND";
        }
        if (exception instanceof BusinessException business
                && Integer.valueOf(403).equals(business.getCode())) {
            return "FORBIDDEN";
        }
        if (exception instanceof BusinessException business
                && Integer.valueOf(401).equals(business.getCode())) {
            return "UNAUTHENTICATED";
        }
        return exception instanceof BusinessException ? "INVALID_ARGUMENT" : "INTERNAL_ERROR";
    }

    private String safeMessage(String errorCode) {
        return switch (errorCode) {
            case "UNAUTHENTICATED" -> "缺少可信 MCP 执行身份";
            case "USER_DELEGATION_REQUIRED" -> "审批状态必须由具体用户委托查询";
            case "FORBIDDEN" -> "当前用户无权查询高风险审批";
            case "APPROVAL_NOT_FOUND" -> "审批请求不存在或不属于当前用户和客户端";
            case "INVALID_ARGUMENT" -> "审批请求ID格式无效";
            default -> "审批状态查询失败，请稍后重试";
        };
    }
}
