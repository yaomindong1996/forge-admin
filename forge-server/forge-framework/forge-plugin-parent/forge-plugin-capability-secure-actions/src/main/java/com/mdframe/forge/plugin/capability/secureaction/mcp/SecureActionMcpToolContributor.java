package com.mdframe.forge.plugin.capability.secureaction.mcp;

import com.mdframe.forge.plugin.mcp.adapter.McpToolSchemaProjector;
import com.mdframe.forge.plugin.mcp.spi.McpToolContributor;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@RequiredArgsConstructor
public class SecureActionMcpToolContributor implements McpToolContributor {

    private final SecureActionMcpHandler handler;

    @Override
    public Collection<McpServerFeatures.SyncToolSpecification> contribute(
            McpToolSchemaProjector schemaProjector) {
        return List.of(
                tool("capability.search", "搜索当前调用方可用的已发布受控能力",
                        searchSchema(), searchOutputSchema(), true, false, true, handler::search),
                tool("capability.describe", "查看一个已授权受控能力的输入输出规范",
                        describeSchema(), describeOutputSchema(), true, false, true, handler::describe),
                tool("capability.invoke", "经幂等校验和人工确认后执行一个受控能力",
                        invokeSchema(), invokeOutputSchema(), false, true, true, handler::invoke));
    }

    private McpServerFeatures.SyncToolSpecification tool(
            String name,
            String description,
            McpSchema.JsonSchema inputSchema,
            Map<String, Object> outputSchema,
            boolean readOnly,
            boolean destructive,
            boolean idempotent,
            BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest,
                    McpSchema.CallToolResult> handlerFunction) {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name(name)
                .title(description)
                .description(description)
                .inputSchema(inputSchema)
                .outputSchema(outputSchema)
                .annotations(new McpSchema.ToolAnnotations(
                        description, readOnly, destructive, idempotent, false, false))
                .build();
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(handlerFunction)
                .build();
    }

    private McpSchema.JsonSchema searchSchema() {
        return schema(Map.of(
                "query", Map.of("type", "string", "description", "能力名称或编码关键字"),
                "limit", Map.of("type", "integer", "minimum", 1, "maximum", 50)), List.of());
    }

    private McpSchema.JsonSchema describeSchema() {
        return schema(Map.of(
                "capabilityCode", Map.of("type", "string", "description", "能力编码")),
                List.of("capabilityCode"));
    }

    private McpSchema.JsonSchema invokeSchema() {
        return schema(Map.of(
                "capabilityCode", Map.of("type", "string", "description", "能力编码"),
                "version", Map.of("type", "string", "description", "可选的已授权版本"),
                "recordId", Map.of("type", "string", "description", "目标记录 ID；创建动作可省略"),
                "idempotencyKey", Map.of("type", "string", "description", "16-128 位幂等键"),
                "arguments", Map.of("type", "object", "description", "业务字段参数")),
                List.of("capabilityCode", "idempotencyKey", "arguments"));
    }

    private Map<String, Object> searchOutputSchema() {
        Map<String, Object> item = objectSchema(Map.of(
                "capabilityCode", Map.of("type", "string"),
                "name", Map.of("type", "string"),
                "description", Map.of("type", "string"),
                "version", Map.of("type", "string"),
                "sourceType", Map.of("type", "string"),
                "behavior", Map.of("type", "string"),
                "operation", Map.of("type", "string"),
                "riskLevel", Map.of("type", "string"),
                "confirmationRequired", Map.of("type", "boolean")),
                List.of("capabilityCode", "name", "description", "version",
                        "sourceType", "behavior", "operation", "riskLevel", "confirmationRequired"));
        return objectSchema(Map.of(
                "items", Map.of("type", "array", "items", item),
                "count", Map.of("type", "integer"),
                "hasMore", Map.of("type", "boolean"),
                "requestId", Map.of("type", "string")),
                List.of("items", "count", "hasMore"));
    }

    private Map<String, Object> describeOutputSchema() {
        return objectSchema(Map.ofEntries(
                Map.entry("capabilityCode", Map.of("type", "string")),
                Map.entry("name", Map.of("type", "string")),
                Map.entry("description", Map.of("type", "string")),
                Map.entry("version", Map.of("type", "string")),
                Map.entry("sourceType", Map.of("type", "string")),
                Map.entry("behavior", Map.of("type", "string")),
                Map.entry("operation", Map.of("type", "string")),
                Map.entry("riskLevel", Map.of("type", "string")),
                Map.entry("confirmationRequired", Map.of("type", "boolean")),
                Map.entry("allowedFields", Map.of("type", "array", "items", Map.of("type", "string"))),
                Map.entry("requiredFields", Map.of("type", "array", "items", Map.of("type", "string"))),
                Map.entry("inputSchema", Map.of("type", "object")),
                Map.entry("outputSchema", Map.of("type", "object"))),
                List.of("capabilityCode", "name", "description", "version", "riskLevel",
                        "sourceType", "behavior", "operation", "confirmationRequired", "allowedFields", "requiredFields",
                        "inputSchema", "outputSchema"));
    }

    private Map<String, Object> invokeOutputSchema() {
        return objectSchema(Map.of(
                "executeStatus", Map.of("type", "string"),
                "message", Map.of("type", "string"),
                "correlationId", Map.of("type", "string"),
                "idempotentHit", Map.of("type", "boolean"),
                "approvalRequestId", Map.of("type", "string")),
                List.of("executeStatus", "message", "correlationId", "idempotentHit"));
    }

    private Map<String, Object> objectSchema(
            Map<String, Object> properties,
            List<String> required) {
        return Map.of(
                "$schema", "https://json-schema.org/draft/2020-12/schema",
                "type", "object",
                "properties", properties,
                "required", required,
                "additionalProperties", false);
    }

    private McpSchema.JsonSchema schema(Map<String, Object> properties, List<String> required) {
        return new McpSchema.JsonSchema(
                "object", properties, required, false, Map.of(), Map.of());
    }
}
