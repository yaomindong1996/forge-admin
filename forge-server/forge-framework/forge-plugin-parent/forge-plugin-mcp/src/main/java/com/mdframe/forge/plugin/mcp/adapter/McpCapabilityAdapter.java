package com.mdframe.forge.plugin.mcp.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.builtin.PingCapabilitySource;
import com.mdframe.forge.plugin.capability.model.CapabilityBehavior;
import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.capability.model.CapabilityDefinition;
import com.mdframe.forge.plugin.capability.model.CapabilityErrorCode;
import com.mdframe.forge.plugin.capability.model.CapabilityInvocation;
import com.mdframe.forge.plugin.capability.model.CapabilityResult;
import com.mdframe.forge.plugin.capability.registry.CapabilityRegistry;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.mcp.security.McpTransportContextKeys;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;
import java.util.UUID;

public final class McpCapabilityAdapter {

    private final CapabilityRegistry capabilityRegistry;
    private final ObjectMapper objectMapper;
    private final McpCapabilityResultMapper resultMapper;
    private final CapabilitySchemaValidator schemaValidator;

    public McpCapabilityAdapter(
            CapabilityRegistry capabilityRegistry,
            ObjectMapper objectMapper,
            McpCapabilityResultMapper resultMapper,
            CapabilitySchemaValidator schemaValidator) {
        this.capabilityRegistry = capabilityRegistry;
        this.objectMapper = objectMapper;
        this.resultMapper = resultMapper;
        this.schemaValidator = schemaValidator;
    }

    public McpServerFeatures.SyncToolSpecification pingToolSpecification(
            McpToolSchemaProjector schemaProjector) {
        CapabilityDefinition definition = capabilityRegistry.requireActive(
                PingCapabilitySource.CAPABILITY_CODE, PingCapabilitySource.VERSION);
        McpSchema.ToolAnnotations annotations = new McpSchema.ToolAnnotations(
                "Forge AI 能力服务健康检查",
                definition.behavior() == CapabilityBehavior.READ_ONLY,
                false,
                true,
                false,
                false);
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name(definition.protocolToolName())
                .title("Forge AI 能力服务健康检查")
                .description(definition.description())
                .inputSchema(schemaProjector.toInputSchema(
                        schemaValidator.validateDefinition(definition.inputSchema())))
                .outputSchema(schemaProjector.toOutputSchema(
                        schemaValidator.validateDefinition(definition.outputSchema())))
                .annotations(annotations)
                .build();
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(this::call)
                .build();
    }

    public McpSchema.CallToolResult call(
            McpSyncServerExchange exchange,
            McpSchema.CallToolRequest request) {
        String requestId = resolveRequestId(exchange);
        if (request == null || !PingCapabilitySource.CAPABILITY_CODE.equals(request.name())) {
            String requestedName = request == null || request.name() == null || request.name().isBlank()
                    ? "unknown"
                    : request.name();
            return resultMapper.toMcpResult(CapabilityResult.error(
                    requestId,
                    requestedName,
                    CapabilityErrorCode.CAPABILITY_NOT_FOUND,
                    "能力不存在或未启用",
                    0L));
        }
        CapabilityCallerContext caller = resolveCaller(exchange);
        if (caller == null) {
            return resultMapper.toMcpResult(CapabilityResult.error(
                    requestId,
                    request.name(),
                    CapabilityErrorCode.UNAUTHENTICATED,
                    "MCP 调用方身份未通过验证",
                    0L));
        }
        JsonNode arguments = objectMapper.valueToTree(
                request.arguments() == null ? Map.of() : request.arguments());
        CapabilityInvocation invocation = new CapabilityInvocation(
                requestId,
                PingCapabilitySource.CAPABILITY_CODE,
                PingCapabilitySource.VERSION,
                caller,
                arguments);
        return resultMapper.toMcpResult(capabilityRegistry.invoke(invocation));
    }

    private CapabilityCallerContext resolveCaller(McpSyncServerExchange exchange) {
        if (exchange == null || exchange.transportContext() == null) {
            return null;
        }
        Object caller = exchange.transportContext().get(McpTransportContextKeys.CALLER_CONTEXT);
        return caller instanceof CapabilityCallerContext context ? context : null;
    }

    private String resolveRequestId(McpSyncServerExchange exchange) {
        if (exchange != null && exchange.transportContext() != null) {
            Object requestId = exchange.transportContext().get(McpTransportContextKeys.REQUEST_ID);
            if (requestId instanceof String value && !value.isBlank()) {
                return value;
            }
        }
        return UUID.randomUUID().toString();
    }
}
