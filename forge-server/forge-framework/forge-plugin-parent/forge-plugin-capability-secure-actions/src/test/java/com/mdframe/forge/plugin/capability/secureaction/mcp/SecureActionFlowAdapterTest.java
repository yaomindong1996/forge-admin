package com.mdframe.forge.plugin.capability.secureaction.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityInvocationAuditService;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionCatalogService;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionPublishedModelPolicy;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionStepValidator;
import com.mdframe.forge.plugin.capability.secureaction.spi.GovernedCapabilityExecutionAdapter;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessActionExecutionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectActionService;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.session.LoginUser;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecureActionFlowAdapterTest {

    @Test
    void shouldDispatchFlowCapabilityThroughGovernedAdapterAfterConfirmation() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SecureActionCatalogService catalogService = mock(SecureActionCatalogService.class);
        BusinessObjectActionService actionService = mock(BusinessObjectActionService.class);
        BusinessActionExecutionService businessExecution = mock(BusinessActionExecutionService.class);
        CapabilityInvocationAuditService auditService = mock(CapabilityInvocationAuditService.class);
        GovernedCapabilityExecutionAdapter adapter = mock(GovernedCapabilityExecutionAdapter.class);
        SecureActionDescriptor descriptor = descriptor(objectMapper);
        when(catalogService.requireIdentity()).thenReturn(identity());
        when(catalogService.requireAuthorized("purchase.order.flow.approve")).thenReturn(descriptor);
        when(adapter.supports(descriptor)).thenReturn(true);
        when(adapter.execute(eq(descriptor), anyMap(), eq("req-flow-1"))).thenReturn(Map.of(
                "executeStatus", "SUCCESS", "message", "已同意",
                "correlationId", "req-flow-1", "idempotentHit", false));
        SecureActionMcpHandler handler = new SecureActionMcpHandler(
                catalogService, actionService, businessExecution, new SecureActionStepValidator(),
                new SecureActionPublishedModelPolicy(objectMapper), new CapabilitySchemaValidator(),
                auditService, objectMapper, List.of(adapter));
        McpSyncServerExchange exchange = exchange();

        McpSchema.CallToolResult result = handler.invoke(exchange, new McpSchema.CallToolRequest(
                "capability.invoke", Map.of(
                "capabilityCode", "purchase.order.flow.approve",
                "recordId", "1001",
                "idempotencyKey", "flow-action-key-1001",
                "arguments", Map.of("taskId", "task-1", "comment", "同意"))));

        assertThat(result.isError()).isFalse();
        String elicitation = handler.elicitationMessage(descriptor, Map.of(
                "recordId", "1001",
                "idempotencyKey", "flow-action-key-1001",
                "arguments", Map.of("taskId", "sensitive-task-4321", "comment", "sensitive-comment")));
        assertThat(elicitation)
                .contains("操作=APPROVE", "任务=***4321")
                .doesNotContain("sensitive-task-4321", "sensitive-comment");
        verify(adapter).validate(eq(descriptor), anyMap());
        verify(adapter).execute(eq(descriptor), anyMap(), eq("req-flow-1"));
        verify(businessExecution, never()).executePublished(any(), any(), any());
    }

    private SecureActionDescriptor descriptor(ObjectMapper objectMapper) throws Exception {
        var policy = objectMapper.readTree("""
                {"bindingId":71,"flowModelKey":"order_approval","operation":"APPROVE",
                 "publishedObjectVersion":3,"permission":"ai:businessFlow:view",
                 "confirmationMode":"MCP_ELICITATION","allowedOperations":["APPROVE"]}
                """);
        var input = objectMapper.readTree("""
                {"$schema":"https://json-schema.org/draft/2020-12/schema",
                 "type":"object","additionalProperties":false,
                 "properties":{"recordId":{"type":"string"},
                   "idempotencyKey":{"type":"string","minLength":16,"maxLength":128},
                   "arguments":{"type":"object","additionalProperties":false,
                     "properties":{"taskId":{"type":"string"},"comment":{"type":"string"}},
                     "required":["taskId"]}},
                 "required":["recordId","idempotencyKey","arguments"]}
                """);
        return new SecureActionDescriptor(
                10L, "purchase.order.flow.approve", "同意采购单", "同意采购单流程", "1.0.0",
                "FLOW_ACTION", "purchase/order/APPROVE", "3", "FLOW", "MEDIUM",
                "purchase", "order", "APPROVE", 3, "ai:businessFlow:view",
                Set.of(), Set.of(), policy, input,
                objectMapper.readTree("{\"type\":\"object\",\"additionalProperties\":true}"));
    }

    private ExecutionIdentity identity() {
        LoginUser user = new LoginUser();
        user.setUserId(101L);
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        return new ExecutionIdentity(user, "USER", 101L, 999L,
                301L, "agent_client", "token-1", Set.of("capability:invoke"));
    }

    private McpSyncServerExchange exchange() {
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        when(exchange.getClientCapabilities()).thenReturn(
                McpSchema.ClientCapabilities.builder().elicitation().build());
        when(exchange.createElicitation(any())).thenReturn(new McpSchema.ElicitResult(
                McpSchema.ElicitResult.Action.ACCEPT, Map.of("confirm", true)));
        when(exchange.transportContext()).thenReturn(
                io.modelcontextprotocol.common.McpTransportContext.create(Map.of(
                        com.mdframe.forge.plugin.mcp.security.McpTransportContextKeys.REQUEST_ID,
                        "req-flow-1")));
        return exchange;
    }
}
