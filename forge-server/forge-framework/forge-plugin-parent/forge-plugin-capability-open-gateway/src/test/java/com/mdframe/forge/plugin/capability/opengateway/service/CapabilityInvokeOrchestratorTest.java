package com.mdframe.forge.plugin.capability.opengateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityInvocationAuditService;
import com.mdframe.forge.plugin.capability.identity.security.AuthenticatedCapabilityIdentity;
import com.mdframe.forge.plugin.capability.identity.security.CapabilitySecurityPrincipal;
import com.mdframe.forge.plugin.capability.opengateway.catalog.OpenGatewayCapability;
import com.mdframe.forge.plugin.capability.opengateway.catalog.OpenGatewayCapabilityResolver;
import com.mdframe.forge.plugin.capability.opengateway.catalog.OpenGatewayCatalogRow;
import com.mdframe.forge.plugin.capability.opengateway.config.OpenGatewayProperties;
import com.mdframe.forge.plugin.capability.opengateway.dto.OpenGatewayResponse;
import com.mdframe.forge.plugin.capability.opengateway.mapper.AiCapabilityOpenapiIdempotencyMapper;
import com.mdframe.forge.plugin.capability.opengateway.mapper.OpenGatewayCatalogMapper;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;
import com.mdframe.forge.plugin.capability.secureaction.spi.GovernedOpenGatewayAdapter;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.openapi.security.idempotency.IdempotencyResult;
import com.mdframe.forge.starter.openapi.security.idempotency.OpenApiIdempotencyManager;
import com.mdframe.forge.starter.openapi.security.ratelimit.OpenApiRateLimitManager;
import com.mdframe.forge.starter.openapi.security.ratelimit.RateLimitPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 九步编排单测：scope/主体类型/权限/限流/幂等/payload 校验各失败分支
 * 与 READ_ONLY、写成功路径，全部断言对外契约错误码与 HTTP 状态。
 */
class CapabilityInvokeOrchestratorTest {

    private static final Long TENANT_ID = 1L;
    private static final Long CLIENT_ID = 10L;
    private static final String CAPABILITY_CODE = "order.sync";
    private static final String REQUEST_ID = "req-20260731-0001";
    private static final String IDEMPOTENCY_KEY = "idem-20260731-0001";

    private OpenGatewayCatalogMapper catalogMapper;
    private OpenGatewayCapabilityResolver resolver;
    private OpenGatewayContextBridge contextBridge;
    private OpenApiRateLimitManager rateLimitManager;
    private OpenApiIdempotencyManager idempotencyManager;
    private AiCapabilityOpenapiIdempotencyMapper idempotencyMapper;
    private CapabilitySchemaValidator schemaValidator;
    private CapabilityInvocationAuditService auditService;
    private GovernedOpenGatewayAdapter executionAdapter;
    private CapabilityInvokeOrchestrator orchestrator;

    @BeforeEach
    void setUp() throws Exception {
        catalogMapper = mock(OpenGatewayCatalogMapper.class);
        resolver = mock(OpenGatewayCapabilityResolver.class);
        contextBridge = mock(OpenGatewayContextBridge.class);
        rateLimitManager = mock(OpenApiRateLimitManager.class);
        idempotencyManager = mock(OpenApiIdempotencyManager.class);
        idempotencyMapper = mock(AiCapabilityOpenapiIdempotencyMapper.class);
        schemaValidator = mock(CapabilitySchemaValidator.class);
        auditService = mock(CapabilityInvocationAuditService.class);
        executionAdapter = mock(GovernedOpenGatewayAdapter.class);
        when(contextBridge.open(any(), any())).thenReturn(() -> {
        });
        orchestrator = new CapabilityInvokeOrchestrator(
                catalogMapper, resolver, contextBridge, rateLimitManager,
                idempotencyManager, idempotencyMapper, schemaValidator, auditService,
                new ObjectMapper(), new OpenGatewayProperties());
    }

    private AuthenticatedCapabilityIdentity identity(Set<String> scopes, Set<String> permissions) {
        CapabilitySecurityPrincipal principal = new CapabilitySecurityPrincipal(
                CLIENT_ID, "erp-sync", CapabilityActorType.SERVICE, 100L, 100L,
                TENANT_ID, 1L, 1, "token-1", "openapi", scopes);
        LoginUser loginUser = new LoginUser();
        loginUser.setPermissions(permissions);
        return new AuthenticatedCapabilityIdentity(principal, loginUser);
    }

    private AuthenticatedCapabilityIdentity fullAccessIdentity() {
        return identity(Set.of("capability:invoke"), Set.of("*:*:*"));
    }

    private SecureActionDescriptor descriptor(String sourceType, String behavior, String riskLevel) {
        return new SecureActionDescriptor(
                100L, CAPABILITY_CODE, "订单同步", "desc", "1.0.0",
                sourceType, "srcKey", "1", behavior, riskLevel,
                "suite", "obj", "act", 1, "ai:capability:order:invoke",
                Set.of("amount"), Set.of(), null, null, null);
    }

    private void stubGranted(SecureActionDescriptor descriptor, String requiredActorType) {
        OpenGatewayCatalogRow row = new OpenGatewayCatalogRow();
        when(catalogMapper.selectGrantedCapability(TENANT_ID, CLIENT_ID, CAPABILITY_CODE))
                .thenReturn(row);
        when(resolver.resolve(row)).thenReturn(
                new OpenGatewayCapability(descriptor, requiredActorType, executionAdapter));
        when(executionAdapter.platformPermission(descriptor))
                .thenReturn("ai:capability:flow-action:invoke");
        when(executionAdapter.prepareInput(eq(descriptor), any())).thenAnswer(
                invocation -> invocation.getArgument(1));
    }

    @Test
    void shouldRejectMissingInvokeScopeWithForbidden() {
        OpenGatewayResponse response = orchestrator.invoke(
                identity(Set.of("capability:discover"), Set.of("*:*:*")),
                CAPABILITY_CODE, null, Map.of(), REQUEST_ID);
        assertEquals("FORBIDDEN", response.code());
        assertEquals(403, response.status());
        verify(catalogMapper, never()).selectGrantedCapability(anyLong(), anyLong(), anyString());
    }

    @Test
    void shouldMapCatalogFailureToInternalError503() {
        when(catalogMapper.selectGrantedCapability(TENANT_ID, CLIENT_ID, CAPABILITY_CODE))
                .thenThrow(new IllegalStateException("db down"));
        OpenGatewayResponse response = orchestrator.invoke(
                fullAccessIdentity(), CAPABILITY_CODE, null, Map.of(), REQUEST_ID);
        assertEquals("INTERNAL_ERROR", response.code());
        assertEquals(503, response.status());
        assertEquals("能力授权服务暂时不可用", response.message());
    }

    @Test
    void shouldRejectActorTypeMismatch() {
        stubGranted(descriptor("FLOW_ACTION", "ACTION", "LOW"), "USER");
        OpenGatewayResponse response = orchestrator.invoke(
                fullAccessIdentity(), CAPABILITY_CODE, IDEMPOTENCY_KEY, Map.of(), REQUEST_ID);
        assertEquals("ACTOR_TYPE_NOT_ALLOWED", response.code());
        assertEquals(403, response.status());
    }

    @Test
    void shouldAllowBothActorTypeAndRejectMissingPermission() {
        stubGranted(descriptor("BUSINESS_ACTION", "ACTION", "MEDIUM"), "BOTH");
        OpenGatewayResponse response = orchestrator.invoke(
                identity(Set.of("capability:*"), Set.of("ai:capability:business-action:invoke")),
                CAPABILITY_CODE, IDEMPOTENCY_KEY, Map.of(), REQUEST_ID);
        assertEquals("FORBIDDEN", response.code());
        assertEquals(403, response.status());
        assertEquals("当前调用方无权执行该能力", response.message());
    }

    @Test
    void shouldMapRateLimitTo429() {
        stubGranted(descriptor("FLOW_ACTION", "ACTION", "LOW"), "SERVICE");
        doThrow(new BusinessException(429, "请求过于频繁，请稍后再试"))
                .when(rateLimitManager).acquire(anyString(), eq("write"), any(RateLimitPolicy.class));
        OpenGatewayResponse response = orchestrator.invoke(
                fullAccessIdentity(), CAPABILITY_CODE, IDEMPOTENCY_KEY, Map.of(), REQUEST_ID);
        assertEquals("RATE_LIMITED", response.code());
        assertEquals(429, response.status());
    }

    @Test
    void shouldRejectWriteWithoutIdempotencyKey() {
        stubGranted(descriptor("FLOW_ACTION", "ACTION", "LOW"), "SERVICE");
        OpenGatewayResponse response = orchestrator.invoke(
                fullAccessIdentity(), CAPABILITY_CODE, " ", Map.of(), REQUEST_ID);
        assertEquals("SCHEMA_INVALID", response.code());
        assertEquals(400, response.status());
        assertEquals("missing_idempotency_key", response.message());
        verify(idempotencyManager, never()).execute(any(), any());
    }

    @Test
    void shouldReturnIdempotentHitSnapshotWithCurrentRequestId() {
        stubGranted(descriptor("FLOW_ACTION", "ACTION", "LOW"), "SERVICE");
        OpenGatewayResponse snapshot = OpenGatewayResponse.success(
                Map.of("orderId", "9"), "req-old");
        when(idempotencyManager.execute(any(), any()))
                .thenReturn(IdempotencyResult.hit(snapshot));
        OpenGatewayResponse response = orchestrator.invoke(
                fullAccessIdentity(), CAPABILITY_CODE, IDEMPOTENCY_KEY,
                Map.of("arguments", Map.of()), REQUEST_ID);
        assertEquals("SUCCESS", response.code());
        assertEquals(REQUEST_ID, response.requestId());
        assertEquals(Boolean.TRUE, response.data().get("idempotentHit"));
        assertEquals("9", response.data().get("orderId"));
    }

    @Test
    void shouldRejectUnknownTopLevelPayloadField() {
        SecureActionDescriptor descriptor = descriptor("FLOW_ACTION", "READ_ONLY", "LOW");
        stubGranted(descriptor, "SERVICE");
        when(executionAdapter.prepareInput(eq(descriptor), any()))
                .thenThrow(new BusinessException("payload 包含未允许的顶层字段"));
        OpenGatewayResponse response = orchestrator.invoke(
                fullAccessIdentity(), CAPABILITY_CODE, null,
                Map.of("foo", 1, "arguments", Map.of()), REQUEST_ID);
        assertEquals("SCHEMA_INVALID", response.code());
        assertEquals(400, response.status());
        assertEquals("payload 包含未允许的顶层字段", response.message());
    }

    @Test
    void shouldMapUnavailableBusinessRecordToResourceNotFound() {
        SecureActionDescriptor descriptor = descriptor("FLOW_ACTION", "READ_ONLY", "LOW");
        stubGranted(descriptor, "SERVICE");
        doThrow(new BusinessException(
                404,
                "记录不存在或无权限访问，请使用当前委托用户可见的已保存业务记录 ID"))
                .when(executionAdapter).validate(eq(descriptor), any());

        OpenGatewayResponse response = orchestrator.invoke(
                fullAccessIdentity(), CAPABILITY_CODE, null,
                Map.of("recordId", "121212", "arguments", Map.of()), REQUEST_ID);

        assertEquals("RESOURCE_NOT_FOUND", response.code());
        assertEquals(404, response.status());
        assertTrue(response.message().contains("已保存业务记录 ID"));
    }

    @Test
    void shouldSkipIdempotencyForReadOnlyAndSucceed() {
        stubGranted(descriptor("FLOW_ACTION", "READ_ONLY", "LOW"), "SERVICE");
        when(executionAdapter.execute(any(), any(), eq(REQUEST_ID)))
                .thenReturn(Map.of("result", "ok"));
        OpenGatewayResponse response = orchestrator.invoke(
                fullAccessIdentity(), CAPABILITY_CODE, null,
                Map.of("arguments", Map.of()), REQUEST_ID);
        assertEquals("SUCCESS", response.code());
        assertEquals(200, response.status());
        assertEquals("ok", response.data().get("result"));
        assertEquals(Boolean.FALSE, response.data().get("idempotentHit"));
        verify(idempotencyManager, never()).execute(any(), any());
        verify(rateLimitManager).acquire(
                eq("capability:" + CLIENT_ID), eq("read"), any(RateLimitPolicy.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldExecuteWriteCapabilityFreshlyThroughIdempotencyManager() {
        stubGranted(descriptor("FLOW_ACTION", "ACTION", "LOW"), "SERVICE");
        when(executionAdapter.execute(any(), any(), eq(REQUEST_ID)))
                .thenReturn(Map.of("executeStatus", "DONE"));
        when(idempotencyManager.execute(any(), any())).thenAnswer(invocation -> {
            Supplier<OpenGatewayResponse> action = invocation.getArgument(1);
            return IdempotencyResult.fresh(action.get());
        });
        OpenGatewayResponse response = orchestrator.invoke(
                fullAccessIdentity(), CAPABILITY_CODE, IDEMPOTENCY_KEY,
                Map.of("arguments", Map.of()), REQUEST_ID);
        assertEquals("SUCCESS", response.code());
        assertEquals(200, response.status());
        assertEquals("DONE", response.data().get("executeStatus"));
        assertEquals(Boolean.FALSE, response.data().get("idempotentHit"));
        verify(rateLimitManager).acquire(
                eq("capability:" + CLIENT_ID), eq("write"), any(RateLimitPolicy.class));
        assertTrue(response.timestamp() > 0);
    }
}
