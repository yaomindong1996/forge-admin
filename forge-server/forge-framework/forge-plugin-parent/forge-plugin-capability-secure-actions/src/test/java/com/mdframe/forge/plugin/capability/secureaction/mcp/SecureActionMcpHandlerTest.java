package com.mdframe.forge.plugin.capability.secureaction.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityInvocationAuditService;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionCatalogService;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionSearchResult;
import com.mdframe.forge.plugin.capability.secureaction.exception.SecureActionUnavailableException;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionStepValidator;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionPublishedModelPolicy;
import com.mdframe.forge.plugin.capability.secureaction.spi.GovernedCapabilityExecutionAdapter;
import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityInvocationAuditEvent;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectDesignVersion;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessActionExecutionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectActionService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessActionExecuteResultVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectActionVO;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.session.LoginUser;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecureActionMcpHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureActionCatalogService catalogService = mock(SecureActionCatalogService.class);
    private final BusinessObjectActionService actionService = mock(BusinessObjectActionService.class);
    private final BusinessActionExecutionService executionService = mock(BusinessActionExecutionService.class);
    private final CapabilityInvocationAuditService auditService = mock(CapabilityInvocationAuditService.class);
    private final SecureActionMcpHandler handler = new SecureActionMcpHandler(
            catalogService, actionService, executionService, new SecureActionStepValidator(),
            new SecureActionPublishedModelPolicy(objectMapper),
            new CapabilitySchemaValidator(), auditService, objectMapper);

    @BeforeEach
    void setUp() {
        when(catalogService.requireAuthorized("business.order.confirm")).thenReturn(descriptor());
        when(catalogService.requireIdentity()).thenReturn(identity("USER"));
        when(actionService.resolvePublishedAction("purchase", "order", "confirm", 3))
                .thenReturn(publishedAction());
        BusinessActionExecuteResultVO result = new BusinessActionExecuteResultVO();
        result.setExecuteStatus("SUCCESS");
        result.setMessage("完成");
        result.setCorrelationId("corr-1");
        result.setIdempotentHit(false);
        when(executionService.executePublished(any(), eq(3), eq("req-1"))).thenReturn(result);
    }

    @Test
    void shouldExecuteOnlyAfterElicitationAccept() {
        McpSyncServerExchange exchange = exchangeWithElicitation();
        when(exchange.createElicitation(any())).thenReturn(new McpSchema.ElicitResult(
                McpSchema.ElicitResult.Action.ACCEPT, Map.of("confirm", true)));

        McpSchema.CallToolResult result = handler.invoke(exchange, request());

        assertThat(result.isError()).isFalse();
        verify(executionService).executePublished(any(), eq(3), eq("req-1"));
        verify(auditService, times(2)).recordOrUpdate(eq(1L), any());
    }

    @Test
    void shouldRouteHighRiskActionToApprovalAdapterAndAuditPending() {
        GovernedCapabilityExecutionAdapter adapter = mock(GovernedCapabilityExecutionAdapter.class);
        SecureActionDescriptor high = highRiskDescriptor();
        when(catalogService.requireAuthorized("business.order.confirm")).thenReturn(high);
        when(adapter.supports(high)).thenReturn(true);
        when(adapter.execute(eq(high), any(), eq("req-1"))).thenReturn(Map.of(
                "executeStatus", "PENDING_APPROVAL",
                "message", "高风险动作已提交人工审批",
                "correlationId", "req-1",
                "idempotentHit", false,
                "approvalRequestId", "99"));
        SecureActionMcpHandler highRiskHandler = new SecureActionMcpHandler(
                catalogService, actionService, executionService, new SecureActionStepValidator(),
                new SecureActionPublishedModelPolicy(objectMapper), new CapabilitySchemaValidator(),
                auditService, objectMapper, List.of(adapter));
        McpSyncServerExchange exchange = exchangeWithElicitation();
        when(exchange.createElicitation(any())).thenReturn(new McpSchema.ElicitResult(
                McpSchema.ElicitResult.Action.ACCEPT, Map.of("confirm", true)));

        McpSchema.CallToolResult result = highRiskHandler.invoke(exchange, request());

        assertThat(result.isError()).isFalse();
        assertThat(result.structuredContent()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> structured = (Map<String, Object>) result.structuredContent();
        assertThat(structured)
                .containsEntry("executeStatus", "PENDING_APPROVAL")
                .containsEntry("approvalRequestId", "99");
        verify(executionService, never()).executePublished(any(), any(), any());
        ArgumentCaptor<CapabilityInvocationAuditEvent> audit =
                ArgumentCaptor.forClass(CapabilityInvocationAuditEvent.class);
        verify(auditService, times(2)).recordOrUpdate(eq(1L), audit.capture());
        assertThat(audit.getAllValues().get(1).resultCode()).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void shouldExposeSourceBehaviorAndOperationInCatalogResults() {
        when(catalogService.search(null, null))
                .thenReturn(new SecureActionSearchResult(List.of(descriptor()), false));
        when(catalogService.requireAuthorized("business.order.confirm")).thenReturn(descriptor());

        McpSchema.CallToolResult search = handler.search(exchangeWithElicitation(),
                new McpSchema.CallToolRequest("capability.search", Map.of()));
        McpSchema.CallToolResult describe = handler.describe(exchangeWithElicitation(),
                new McpSchema.CallToolRequest("capability.describe",
                        Map.of("capabilityCode", "business.order.confirm")));

        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) ((List<?>) ((Map<?, ?>)
                search.structuredContent()).get("items")).get(0);
        assertThat(first).containsEntry("sourceType", "BUSINESS_ACTION")
                .containsEntry("behavior", "ACTION")
                .containsEntry("operation", "confirm");
        @SuppressWarnings("unchecked")
        Map<String, Object> described = (Map<String, Object>) describe.structuredContent();
        assertThat(described)
                .containsEntry("sourceType", "BUSINESS_ACTION")
                .containsEntry("behavior", "ACTION")
                .containsEntry("operation", "confirm");
    }

    @Test
    void shouldNotExecuteWhenConfirmationIsDeclined() {
        McpSyncServerExchange exchange = exchangeWithElicitation();
        when(exchange.createElicitation(any())).thenReturn(new McpSchema.ElicitResult(
                McpSchema.ElicitResult.Action.DECLINE, Map.of()));

        McpSchema.CallToolResult result = handler.invoke(exchange, request());

        assertThat(result.isError()).isTrue();
        assertThat(((Map<?, ?>) result.structuredContent()).get("errorCode"))
                .isEqualTo("CONFIRMATION_DECLINED");
        verify(executionService, never()).executePublished(any(), any(), any());
    }

    @Test
    void shouldRequireUserDelegationBeforeElicitation() {
        when(catalogService.requireIdentity()).thenReturn(identity("SERVICE"));
        McpSyncServerExchange exchange = exchangeWithElicitation();

        McpSchema.CallToolResult result = handler.invoke(exchange, request());

        assertThat(result.isError()).isTrue();
        assertThat(((Map<?, ?>) result.structuredContent()).get("errorCode"))
                .isEqualTo("USER_DELEGATION_REQUIRED");
        verify(exchange, never()).createElicitation(any());
        verify(executionService, never()).executePublished(any(), any(), any());
    }

    @Test
    void shouldRejectWhenPublishedModelNoLongerContainsAuthorizedField() {
        var published = publishedAction();
        published.version().setModelSnapshot(
                "{\"fields\":[{\"field\":\"other\",\"dataType\":\"string\"}]}");
        when(actionService.resolvePublishedAction("purchase", "order", "confirm", 3))
                .thenReturn(published);
        McpSyncServerExchange exchange = exchangeWithElicitation();

        McpSchema.CallToolResult result = handler.invoke(exchange, request());

        assertThat(result.isError()).isTrue();
        assertThat(((Map<?, ?>) result.structuredContent()).get("errorCode"))
                .isEqualTo("POLICY_MISMATCH");
        verify(exchange, never()).createElicitation(any());
        verify(executionService, never()).executePublished(any(), any(), any());
    }

    @Test
    void shouldRejectUnknownFieldBeforeElicitation() {
        McpSyncServerExchange exchange = exchangeWithElicitation();
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "capability.invoke", Map.of(
                "capabilityCode", "business.order.confirm",
                "recordId", "1001",
                "idempotencyKey", "order-confirm-1001",
                "arguments", Map.of("secret", "value")));

        McpSchema.CallToolResult result = handler.invoke(exchange, request);

        assertThat(result.isError()).isTrue();
        assertThat(((Map<?, ?>) result.structuredContent()).get("errorCode"))
                .isEqualTo("INVALID_ARGUMENT");
        verify(exchange, never()).createElicitation(any());
        verify(executionService, never()).executePublished(any(), any(), any());
    }

    @Test
    void shouldRejectForgedTopLevelIdentityAndFlowControlFieldsBeforeLookup() {
        for (String field : List.of("userId", "tenantId", "activeOrgId", "flowModelKey",
                "variables", "processInstanceId", "unknown")) {
            Map<String, Object> arguments = new LinkedHashMap<>(request().arguments());
            arguments.put(field, "forged");
            McpSchema.CallToolResult result = handler.invoke(
                    exchangeWithElicitation(),
                    new McpSchema.CallToolRequest("capability.invoke", arguments));

            assertThat(result.isError()).as(field).isTrue();
            assertThat(((Map<?, ?>) result.structuredContent()).get("errorCode"))
                    .as(field).isEqualTo("INVALID_ARGUMENT");
        }
        verify(catalogService, never()).requireAuthorized(any());
        verify(executionService, never()).executePublished(any(), any(), any());
    }

    @Test
    void shouldRequireElicitationCapability() {
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        io.modelcontextprotocol.common.McpTransportContext context =
                io.modelcontextprotocol.common.McpTransportContext.create(Map.of(
                        com.mdframe.forge.plugin.mcp.security.McpTransportContextKeys.REQUEST_ID, "req-1"));
        when(exchange.transportContext()).thenReturn(context);
        when(exchange.getClientCapabilities()).thenReturn(
                McpSchema.ClientCapabilities.builder().build());

        McpSchema.CallToolResult result = handler.invoke(exchange, request());

        assertThat(result.isError()).isTrue();
        assertThat(((Map<?, ?>) result.structuredContent()).get("errorCode"))
                .isEqualTo("CONFIRMATION_REQUIRED");
        verify(executionService, never()).executePublished(any(), any(), any());
    }

    @Test
    void shouldNotExecuteWhenConfirmationIsCancelled() {
        McpSyncServerExchange exchange = exchangeWithElicitation();
        when(exchange.createElicitation(any())).thenReturn(new McpSchema.ElicitResult(
                McpSchema.ElicitResult.Action.CANCEL, Map.of()));

        McpSchema.CallToolResult result = handler.invoke(exchange, request());

        assertThat(result.isError()).isTrue();
        assertThat(((Map<?, ?>) result.structuredContent()).get("errorCode"))
                .isEqualTo("CONFIRMATION_DECLINED");
        verify(executionService, never()).executePublished(any(), any(), any());
    }

    @Test
    void shouldReturnStableIdempotencyConflict() {
        McpSyncServerExchange exchange = exchangeWithElicitation();
        when(exchange.createElicitation(any())).thenReturn(new McpSchema.ElicitResult(
                McpSchema.ElicitResult.Action.ACCEPT, Map.of("confirm", true)));
        when(executionService.executePublished(any(), eq(3), eq("req-1")))
                .thenThrow(new com.mdframe.forge.starter.core.exception.BusinessException(
                        "幂等键已被不同业务动作参数使用，请更换幂等键"));

        McpSchema.CallToolResult result = handler.invoke(exchange, request());

        assertThat(result.isError()).isTrue();
        assertThat(((Map<?, ?>) result.structuredContent()).get("errorCode"))
                .isEqualTo("IDEMPOTENCY_CONFLICT");
    }

    @Test
    void shouldAllowCreateActionWithoutRecordId() {
        McpSyncServerExchange exchange = exchangeWithElicitation();
        when(exchange.createElicitation(any())).thenReturn(new McpSchema.ElicitResult(
                McpSchema.ElicitResult.Action.ACCEPT, Map.of("confirm", true)));
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "capability.invoke", Map.of(
                "capabilityCode", "business.order.confirm",
                "idempotencyKey", "order-create-1001",
                "arguments", Map.of("status", "DRAFT")));

        McpSchema.CallToolResult result = handler.invoke(exchange, request);

        assertThat(result.isError()).isFalse();
        verify(executionService).executePublished(
                org.mockito.ArgumentMatchers.argThat(command -> command.getRecordId() == null),
                eq(3), eq("req-1"));
    }

    @Test
    void shouldRejectInvokeWhenTokenScopeIsInsufficient() {
        ExecutionIdentity identity = identity("USER", Set.of("capability:discover"));
        when(catalogService.requireIdentity()).thenReturn(identity);
        org.mockito.Mockito.doThrow(new com.mdframe.forge.starter.core.exception.BusinessException(
                        403, "INSUFFICIENT_SCOPE"))
                .when(catalogService).requireInvocationScope(
                        identity, "business.order.confirm");

        McpSchema.CallToolResult result = handler.invoke(exchangeWithElicitation(), request());

        assertThat(result.isError()).isTrue();
        assertThat(((Map<?, ?>) result.structuredContent()).get("errorCode"))
                .isEqualTo("INSUFFICIENT_SCOPE");
        verify(actionService, never()).resolvePublishedAction(any(), any(), any(), any());
        verify(executionService, never()).executePublished(any(), any(), any());
    }

    @Test
    void shouldNotExecuteWhenAuditReservationIsUnavailable() {
        McpSyncServerExchange exchange = exchangeWithElicitation();
        when(exchange.createElicitation(any())).thenReturn(new McpSchema.ElicitResult(
                McpSchema.ElicitResult.Action.ACCEPT, Map.of("confirm", true)));
        doThrow(new IllegalStateException("database unavailable"))
                .when(auditService).recordOrUpdate(eq(1L), any());

        McpSchema.CallToolResult result = handler.invoke(exchange, request());

        assertThat(result.isError()).isTrue();
        assertThat(((Map<?, ?>) result.structuredContent()).get("errorCode"))
                .isEqualTo("AUDIT_UNAVAILABLE");
        verify(executionService, never()).executePublished(any(), any(), any());
    }

    @Test
    void shouldReturnStableAuthorizationUnavailableError() {
        when(catalogService.requireAuthorized("business.order.confirm"))
                .thenThrow(new SecureActionUnavailableException(
                        "AUTHORIZATION_UNAVAILABLE", new IllegalStateException("database unavailable")));

        McpSchema.CallToolResult result = handler.invoke(exchangeWithElicitation(), request());

        assertThat(result.isError()).isTrue();
        assertThat(((Map<?, ?>) result.structuredContent()).get("errorCode"))
                .isEqualTo("AUTHORIZATION_UNAVAILABLE");
        verify(executionService, never()).executePublished(any(), any(), any());
    }

    @Test
    void shouldBindRequestFingerprintToCanonicalRequestContent() {
        Map<String, Object> firstArguments = new LinkedHashMap<>();
        firstArguments.put("status", "CONFIRMED");
        firstArguments.put("remark", "ok");
        Map<String, Object> reorderedArguments = new LinkedHashMap<>();
        reorderedArguments.put("remark", "ok");
        reorderedArguments.put("status", "CONFIRMED");
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("recordId", "1001");
        first.put("idempotencyKey", "order-confirm-1001");
        first.put("arguments", firstArguments);
        Map<String, Object> reordered = new LinkedHashMap<>();
        reordered.put("arguments", reorderedArguments);
        reordered.put("idempotencyKey", "order-confirm-1001");
        reordered.put("recordId", "1001");
        Map<String, Object> changed = new LinkedHashMap<>(first);
        changed.put("recordId", "1002");

        assertThat(handler.requestFingerprint(descriptor(), first))
                .isEqualTo(handler.requestFingerprint(descriptor(), reordered))
                .isNotEqualTo(handler.requestFingerprint(descriptor(), changed));
    }

    @Test
    void shouldRejectUnsafeNestedStepBeforeAuditAndExecution() {
        var published = publishedAction();
        published.action().setActionConfig(Map.of("steps", List.of(Map.of(
                "stepType", "UPDATE_FIELD",
                "stepConfig", Map.of("childSteps", List.of(Map.of("stepType", "START_FLOW")))))));
        when(actionService.resolvePublishedAction("purchase", "order", "confirm", 3))
                .thenReturn(published);

        McpSchema.CallToolResult result = handler.invoke(exchangeWithElicitation(), request());

        assertThat(result.isError()).isTrue();
        verify(executionService, never()).executePublished(any(), any(), any());
    }

    private McpSchema.CallToolRequest request() {
        return new McpSchema.CallToolRequest("capability.invoke", Map.of(
                "capabilityCode", "business.order.confirm",
                "recordId", "1001",
                "idempotencyKey", "order-confirm-1001",
                "arguments", Map.of("status", "CONFIRMED")));
    }

    private McpSyncServerExchange exchangeWithElicitation() {
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        when(exchange.getClientCapabilities()).thenReturn(
                McpSchema.ClientCapabilities.builder().elicitation().build());
        io.modelcontextprotocol.common.McpTransportContext context =
                io.modelcontextprotocol.common.McpTransportContext.create(Map.of(
                        com.mdframe.forge.plugin.mcp.security.McpTransportContextKeys.REQUEST_ID, "req-1"));
        when(exchange.transportContext()).thenReturn(context);
        return exchange;
    }

    private SecureActionDescriptor descriptor() {
        String input = """
                {"$schema":"https://json-schema.org/draft/2020-12/schema",
                 "type":"object","additionalProperties":false,
                 "properties":{"recordId":{"type":"string"},
                   "idempotencyKey":{"type":"string"},
                   "arguments":{"type":"object","additionalProperties":false,
                     "properties":{"status":{"type":"string"}},"required":["status"]}},
                 "required":["idempotencyKey","arguments"]}
                """;
        String output = """
                {"type":"object","additionalProperties":true}
                """;
        try {
            return new SecureActionDescriptor(10L, "business.order.confirm", "确认订单",
                    "确认订单", "1.0.0", "purchase", "order", "confirm", 3,
                    "purchase:order:confirm", Set.of("status"), Set.of("status"),
                    objectMapper.readTree(input), objectMapper.readTree(output));
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private SecureActionDescriptor highRiskDescriptor() {
        SecureActionDescriptor medium = descriptor();
        return new SecureActionDescriptor(
                medium.capabilityId(), medium.capabilityCode(), medium.capabilityName(),
                medium.description(), medium.version(), medium.sourceType(), medium.sourceKey(),
                medium.sourceVersion(), medium.behavior(), "HIGH", medium.suiteCode(),
                medium.objectCode(), medium.actionCode(), medium.publishedObjectVersion(),
                medium.permission(), medium.allowedFields(), medium.requiredFields(),
                medium.policySnapshot(), medium.inputSchema(), medium.outputSchema());
    }

    private ExecutionIdentity identity(String actorType) {
        return identity(actorType, Set.of("capability:invoke", "capability:discover"));
    }

    private ExecutionIdentity identity(String actorType, Set<String> scopes) {
        LoginUser user = new LoginUser();
        user.setUserId("USER".equals(actorType) ? 101L : 999L);
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        user.setUserStatus(1);
        user.setPermissions(Set.of("ai:capability:business-action:invoke", "purchase:order:confirm"));
        return new ExecutionIdentity(user, actorType, user.getUserId(), 999L,
                301L, "agent_client", "token-1", scopes);
    }

    private BusinessObjectActionService.ResolvedPublishedBusinessAction publishedAction() {
        AiBusinessObject object = new AiBusinessObject();
        object.setObjectCode("order");
        BusinessObjectActionVO action = new BusinessObjectActionVO();
        action.setActionCode("confirm");
        action.setActionConfig(Map.of("steps", List.of(Map.of("stepType", "UPDATE_FIELD"))));
        AiBusinessObjectDesignVersion version = new AiBusinessObjectDesignVersion();
        version.setPublishVersion(3);
        version.setModelSnapshot("{\"fields\":[{\"field\":\"status\",\"dataType\":\"string\"}]}");
        return new BusinessObjectActionService.ResolvedPublishedBusinessAction(object, action, version);
    }
}
