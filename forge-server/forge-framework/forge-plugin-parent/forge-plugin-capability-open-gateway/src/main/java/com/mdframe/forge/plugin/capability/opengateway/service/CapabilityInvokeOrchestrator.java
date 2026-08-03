package com.mdframe.forge.plugin.capability.opengateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;
import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityInvocationAuditEvent;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityInvocationAuditService;
import com.mdframe.forge.plugin.capability.identity.security.AuthenticatedCapabilityIdentity;
import com.mdframe.forge.plugin.capability.identity.security.CapabilitySecurityPrincipal;
import com.mdframe.forge.plugin.capability.model.CapabilityResultStatus;
import com.mdframe.forge.plugin.capability.opengateway.catalog.OpenGatewayCapability;
import com.mdframe.forge.plugin.capability.opengateway.catalog.OpenGatewayCapabilityResolver;
import com.mdframe.forge.plugin.capability.opengateway.catalog.OpenGatewayCatalogRow;
import com.mdframe.forge.plugin.capability.opengateway.config.OpenGatewayProperties;
import com.mdframe.forge.plugin.capability.opengateway.dto.OpenGatewayResponse;
import com.mdframe.forge.plugin.capability.opengateway.entity.AiCapabilityOpenapiIdempotency;
import com.mdframe.forge.plugin.capability.opengateway.exception.OpenGatewayException;
import com.mdframe.forge.plugin.capability.opengateway.mapper.AiCapabilityOpenapiIdempotencyMapper;
import com.mdframe.forge.plugin.capability.opengateway.mapper.OpenGatewayCatalogMapper;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidationException;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;
import com.mdframe.forge.plugin.capability.secureaction.exception.SecureActionUnavailableException;
import com.mdframe.forge.plugin.capability.spi.ScopeBasedCapabilityAuthorizationPolicy;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.openapi.security.idempotency.IdempotencyCommand;
import com.mdframe.forge.starter.openapi.security.idempotency.IdempotencyResult;
import com.mdframe.forge.starter.openapi.security.idempotency.OpenApiIdempotencyManager;
import com.mdframe.forge.starter.openapi.security.ratelimit.OpenApiRateLimitManager;
import com.mdframe.forge.starter.openapi.security.ratelimit.RateLimitPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 能力开放网关九步编排：scope → 授权目录 → 主体类型 → 权限 → 限流 →
 * 幂等 → Schema 校验 → 受控执行 → 审计。所有失败路径统一转换为契约错误响应，不向入口层抛异常。
 */
@RequiredArgsConstructor
@Slf4j
public class CapabilityInvokeOrchestrator {

    private final OpenGatewayCatalogMapper catalogMapper;
    private final OpenGatewayCapabilityResolver resolver;
    private final OpenGatewayContextBridge contextBridge;
    private final OpenApiRateLimitManager rateLimitManager;
    private final OpenApiIdempotencyManager idempotencyManager;
    private final AiCapabilityOpenapiIdempotencyMapper idempotencyMapper;
    private final CapabilitySchemaValidator schemaValidator;
    private final CapabilityInvocationAuditService auditService;
    private final ObjectMapper objectMapper;
    private final OpenGatewayProperties properties;

    public OpenGatewayResponse invoke(
            AuthenticatedCapabilityIdentity identity,
            String capabilityCode,
            String idempotencyKey,
            Map<String, Object> payload,
            String requestId) {
        long startedAt = System.nanoTime();
        CapabilitySecurityPrincipal principal = identity.principal();
        OpenGatewayCapability capability = null;
        AtomicBoolean executionCompleted = new AtomicBoolean(false);
        AtomicReference<String> stage = new AtomicReference<>("CONTEXT_INIT");
        log.info("[能力开放网关编排] 调用开始: requestId={}, capabilityCode={}, clientId={}, clientCode={}, actorType={}, actorUserId={}, tenantId={}, activeOrgId={}",
                requestId, capabilityCode, principal.clientId(), principal.clientCode(),
                principal.actorType(), principal.actorUserId(), principal.tenantId(), principal.activeOrgId());
        try (AutoCloseable ignored = contextBridge.open(identity, requestId)) {
            stage.set("SCOPE_AUTHORIZATION");
            requireInvokeScope(principal, capabilityCode);
            OpenGatewayCatalogRow row;
            try {
                stage.set("GRANT_RESOLUTION");
                row = catalogMapper.selectGrantedCapability(
                        principal.tenantId(), principal.clientId(), capabilityCode);
            }
            catch (RuntimeException exception) {
                throw new OpenGatewayException(
                        "INTERNAL_ERROR", 503, "能力授权服务暂时不可用", exception);
            }
            stage.set("CAPABILITY_RESOLUTION");
            capability = resolver.resolve(row);
            stage.set("ACTOR_AUTHORIZATION");
            requireActorTypeAllowed(capability.requiredActorType(), principal.actorType());
            SecureActionDescriptor descriptor = capability.descriptor();
            stage.set("RBAC_AUTHORIZATION");
            requirePermissions(identity.loginUser(), capability, principal, requestId);
            log.info("[能力开放网关授权] 授权通过: requestId={}, capabilityCode={}, version={}, clientId={}, actorType={}, actorUserId={}, tenantId={}, activeOrgId={}",
                    requestId, descriptor.capabilityCode(), descriptor.version(), principal.clientId(),
                    principal.actorType(), principal.actorUserId(), principal.tenantId(), principal.activeOrgId());

            boolean write = !"READ_ONLY".equals(descriptor.behavior());
            stage.set("RATE_LIMIT");
            rateLimitManager.acquire("capability:" + principal.clientId(),
                    write ? "write" : "read",
                    RateLimitPolicy.perMinute(write
                            ? properties.getWritePermitsPerMinute()
                            : properties.getReadPermitsPerMinute()));

            if (!write) {
                return doExecute(capability, principal, payload,
                        idempotencyKey, requestId, startedAt, executionCompleted, stage);
            }
            stage.set("IDEMPOTENCY");
            if (StringUtils.isBlank(idempotencyKey)) {
                throw new OpenGatewayException("SCHEMA_INVALID", 400, "missing_idempotency_key");
            }
            OpenGatewayCapability executableCapability = capability;
            String scopeKey = principal.tenantId() + ":" + principal.clientId()
                    + ":" + descriptor.capabilityId();
            IdempotencyCommand<OpenGatewayResponse> command = new IdempotencyCommand<>(
                    scopeKey, idempotencyKey,
                    keyHash -> loadSnapshot(principal, descriptor.capabilityId(), keyHash),
                    (keyHash, response) -> writeSnapshot(
                            principal, descriptor.capabilityId(), keyHash, requestId, response));
            IdempotencyResult<OpenGatewayResponse> result = idempotencyManager.execute(command,
                    () -> doExecute(executableCapability, principal, payload,
                            idempotencyKey, requestId, startedAt, executionCompleted, stage));
            if (result.idempotentHit()) {
                log.info("[能力开放网关幂等] 命中历史结果: requestId={}, capabilityCode={}, version={}, clientId={}, tenantId={}, durationMs={}",
                        requestId, descriptor.capabilityCode(), descriptor.version(),
                        principal.clientId(), principal.tenantId(), elapsed(startedAt));
                return result.value().asIdempotentHit(requestId);
            }
            return result.value();
        }
        catch (Exception exception) {
            return failure(exception, capability, principal, capabilityCode,
                    requestId, startedAt, executionCompleted.get(), stage.get());
        }
    }

    private OpenGatewayResponse doExecute(
            OpenGatewayCapability capability,
            CapabilitySecurityPrincipal principal,
            Map<String, Object> payload,
            String idempotencyKey,
            String requestId,
            long startedAt,
            AtomicBoolean executionCompleted,
            AtomicReference<String> stage) {
        SecureActionDescriptor descriptor = capability.descriptor();
        stage.set("INPUT_PREPARATION");
        Map<String, Object> targetInput = new LinkedHashMap<>(
                capability.adapter().prepareInput(descriptor, payload));
        JsonNode inputNode = objectMapper.valueToTree(targetInput);
        stage.set("INPUT_SCHEMA_VALIDATION");
        schemaValidator.validateInstance(descriptor.inputSchema(), inputNode);
        log.info("[能力开放网关校验] Schema 通过: requestId={}, capabilityCode={}, version={}, clientId={}, tenantId={}",
                requestId, descriptor.capabilityCode(), descriptor.version(),
                principal.clientId(), principal.tenantId());
        if (StringUtils.isNotBlank(idempotencyKey)) {
            targetInput.put("idempotencyKey", idempotencyKey);
        }
        stage.set("POLICY_VALIDATION");
        capability.adapter().validate(descriptor, targetInput);
        stage.set("AUDIT_RESERVATION");
        audit(descriptor, principal, requestId, CapabilityResultStatus.ERROR,
                "EXECUTION_PENDING", null, null, elapsed(startedAt));
        log.info("[能力开放网关执行] 开始执行: requestId={}, capabilityCode={}, version={}, clientId={}, actorType={}, actorUserId={}, tenantId={}, activeOrgId={}",
                requestId, descriptor.capabilityCode(), descriptor.version(), principal.clientId(),
                principal.actorType(), principal.actorUserId(), principal.tenantId(), principal.activeOrgId());
        stage.set("ADAPTER_EXECUTION");
        Map<String, Object> data = new LinkedHashMap<>(
                capability.adapter().execute(descriptor, targetInput, requestId));
        data.putIfAbsent("idempotentHit", false);
        if (Boolean.TRUE.equals(data.get("idempotentHit"))) {
            log.info("[能力开放网关幂等] 执行适配器复用历史结果: requestId={}, capabilityCode={}, version={}, clientId={}, tenantId={}",
                    requestId, descriptor.capabilityCode(), descriptor.version(),
                    principal.clientId(), principal.tenantId());
        }
        stage.set("OUTPUT_SCHEMA_VALIDATION");
        schemaValidator.validateInstance(descriptor.outputSchema(), objectMapper.valueToTree(data));
        executionCompleted.set(true);
        String executeStatus = text(data.get("executeStatus"));
        String resultCode = "PENDING_APPROVAL".equals(executeStatus)
                ? "PENDING_APPROVAL" : "SUCCESS";
        stage.set("AUDIT_FINALIZATION");
        audit(descriptor, principal, requestId, CapabilityResultStatus.SUCCESS,
                resultCode, null, null, elapsed(startedAt));
        log.info("[能力开放网关执行] 执行成功: requestId={}, capabilityCode={}, version={}, clientId={}, actorType={}, actorUserId={}, tenantId={}, resultCode={}, durationMs={}",
                requestId, descriptor.capabilityCode(), descriptor.version(), principal.clientId(),
                principal.actorType(), principal.actorUserId(), principal.tenantId(),
                resultCode, elapsed(startedAt));
        return OpenGatewayResponse.success(data, requestId);
    }

    private void requireInvokeScope(CapabilitySecurityPrincipal principal, String capabilityCode) {
        Set<String> scopes = principal.scopes();
        boolean allowed = scopes != null
                && (scopes.contains(ScopeBasedCapabilityAuthorizationPolicy.ALL_SCOPE)
                || scopes.contains(ScopeBasedCapabilityAuthorizationPolicy.INVOKE_SCOPE)
                || scopes.contains("capability:invoke:" + capabilityCode));
        if (!allowed) {
            throw new OpenGatewayException("FORBIDDEN", 403, "当前凭据未获得该能力所需 scope");
        }
    }

    private void requireActorTypeAllowed(String requiredActorType, CapabilityActorType actorType) {
        if ("BOTH".equals(requiredActorType) || actorType.name().equals(requiredActorType)) {
            return;
        }
        throw new OpenGatewayException(
                "ACTOR_TYPE_NOT_ALLOWED", 403, "该能力不允许当前调用主体类型调用");
    }

    private void requirePermissions(
            LoginUser loginUser,
            OpenGatewayCapability capability,
            CapabilitySecurityPrincipal principal,
            String requestId) {
        SecureActionDescriptor descriptor = capability.descriptor();
        String platformPermission = capability.adapter().platformPermission(descriptor);
        boolean platformAllowed = hasPermission(loginUser, platformPermission);
        boolean businessAllowed = hasPermission(loginUser, descriptor.permission());
        if (!platformAllowed || !businessAllowed) {
            log.warn("[能力开放网关授权] 权限不足: requestId={}, capabilityCode={}, version={}, clientId={}, actorUserId={}, tenantId={}, platformPermission={}, platformAllowed={}, businessPermission={}, businessAllowed={}",
                    requestId, descriptor.capabilityCode(), descriptor.version(), principal.clientId(),
                    principal.actorUserId(), principal.tenantId(), platformPermission, platformAllowed,
                    descriptor.permission(), businessAllowed);
            throw new OpenGatewayException("FORBIDDEN", 403, "当前调用方无权执行该能力");
        }
    }

    private boolean hasPermission(LoginUser user, String permission) {
        if (StringUtils.isBlank(permission) || user.getPermissions() == null) {
            return false;
        }
        Set<String> permissions = user.getPermissions();
        if (permissions.contains("*:*:*") || permissions.contains(permission)) {
            return true;
        }
        int splitIndex = permission.lastIndexOf(':');
        while (splitIndex > 0) {
            if (permissions.contains(permission.substring(0, splitIndex) + ":*")) {
                return true;
            }
            splitIndex = permission.lastIndexOf(':', splitIndex - 1);
        }
        return false;
    }

    private OpenGatewayResponse loadSnapshot(
            CapabilitySecurityPrincipal principal, Long capabilityId, String keyHash) {
        AiCapabilityOpenapiIdempotency snapshot = idempotencyMapper.selectActiveSnapshot(
                principal.tenantId(), principal.clientId(), capabilityId, keyHash);
        if (snapshot == null) {
            return null;
        }
        try {
            return objectMapper.readValue(snapshot.getResponseSnapshot(), OpenGatewayResponse.class);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("幂等响应快照反序列化失败", exception);
        }
    }

    private void writeSnapshot(
            CapabilitySecurityPrincipal principal, Long capabilityId,
            String keyHash, String requestId, OpenGatewayResponse response) {
        AiCapabilityOpenapiIdempotency snapshot = new AiCapabilityOpenapiIdempotency();
        snapshot.setTenantId(principal.tenantId());
        snapshot.setClientId(principal.clientId());
        snapshot.setCapabilityId(capabilityId);
        snapshot.setIdempotencyKeyHash(keyHash);
        snapshot.setRequestId(requestId);
        try {
            snapshot.setResponseSnapshot(objectMapper.writeValueAsString(response));
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("幂等响应快照序列化失败", exception);
        }
        snapshot.setExpiresAt(LocalDateTime.now().plus(properties.getIdempotencyTtl()));
        snapshot.setDelFlag(0L);
        idempotencyMapper.insert(snapshot);
    }

    private OpenGatewayResponse failure(
            Exception exception,
            OpenGatewayCapability capability,
            CapabilitySecurityPrincipal principal,
            String requestedCapabilityCode,
            String requestId,
            long startedAt,
            boolean executionCompleted,
            String failureStage) {
        ErrorMapping mapping = map(exception);
        if (capability != null && !executionCompleted && !isAuditUnavailable(exception)) {
            try {
                audit(capability.descriptor(), principal, requestId, CapabilityResultStatus.ERROR,
                        "FAILED", mapping.errorCode(), schemaPath(exception), elapsed(startedAt));
            }
            catch (SecureActionUnavailableException auditException) {
                mapping = new ErrorMapping("INTERNAL_ERROR", 503, "能力审计服务暂时不可用");
            }
        }
        SecureActionDescriptor descriptor = capability == null ? null : capability.descriptor();
        String version = descriptor == null ? null : descriptor.version();
        String resolvedCapabilityCode = descriptor == null
                ? requestedCapabilityCode : descriptor.capabilityCode();
        String path = schemaPath(exception);
        if (mapping.httpStatus() >= 500) {
            log.warn("[能力开放网关失败] 调用失败: requestId={}, capabilityCode={}, version={}, clientId={}, clientCode={}, actorType={}, actorUserId={}, tenantId={}, activeOrgId={}, failureStage={}, resultCode={}, httpStatus={}, schemaPath={}, durationMs={}, exceptionType={}",
                    requestId, resolvedCapabilityCode, version, principal.clientId(), principal.clientCode(),
                    principal.actorType(), principal.actorUserId(), principal.tenantId(), principal.activeOrgId(),
                    failureStage, mapping.errorCode(), mapping.httpStatus(), path, elapsed(startedAt),
                    exception.getClass().getSimpleName(), exception);
        }
        else {
            log.warn("[能力开放网关失败] 调用拒绝: requestId={}, capabilityCode={}, version={}, clientId={}, clientCode={}, actorType={}, actorUserId={}, tenantId={}, activeOrgId={}, failureStage={}, resultCode={}, httpStatus={}, schemaPath={}, durationMs={}, exceptionType={}",
                    requestId, resolvedCapabilityCode, version, principal.clientId(), principal.clientCode(),
                    principal.actorType(), principal.actorUserId(), principal.tenantId(), principal.activeOrgId(),
                    failureStage, mapping.errorCode(), mapping.httpStatus(), path, elapsed(startedAt),
                    exception.getClass().getSimpleName());
        }
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("failureStage", failureStage);
        if (StringUtils.isNotBlank(path)) {
            diagnostics.put("schemaPath", path);
        }
        return OpenGatewayResponse.error(
                mapping.errorCode(), mapping.message(), requestId,
                mapping.httpStatus(), diagnostics);
    }

    private ErrorMapping map(Exception exception) {
        if (exception instanceof OpenGatewayException gateway) {
            return new ErrorMapping(gateway.getErrorCode(), gateway.getHttpStatus(), gateway.getMessage());
        }
        if (exception instanceof CapabilitySchemaValidationException validation) {
            return new ErrorMapping("SCHEMA_INVALID", 400,
                    "调用参数不符合能力输入契约：" + validation.getMessage());
        }
        if (exception instanceof SecureActionUnavailableException) {
            return new ErrorMapping("INTERNAL_ERROR", 503, "能力执行依赖服务暂时不可用");
        }
        if (exception instanceof BusinessException business) {
            Integer code = business.getCode();
            if (Integer.valueOf(401).equals(code)) {
                return new ErrorMapping("UNAUTHORIZED", 401, business.getMessage());
            }
            if (Integer.valueOf(403).equals(code)) {
                return new ErrorMapping("FORBIDDEN", 403, business.getMessage());
            }
            if (Integer.valueOf(404).equals(code)) {
                return new ErrorMapping("RESOURCE_NOT_FOUND", 404, business.getMessage());
            }
            if (Integer.valueOf(409).equals(code)) {
                return new ErrorMapping("CONFLICT", 409, business.getMessage());
            }
            if (Integer.valueOf(429).equals(code)) {
                return new ErrorMapping("RATE_LIMITED", 429, business.getMessage());
            }
            if (Integer.valueOf(503).equals(code)) {
                return new ErrorMapping("INTERNAL_ERROR", 503, business.getMessage());
            }
            return new ErrorMapping("SCHEMA_INVALID", 400, business.getMessage());
        }
        return new ErrorMapping("INTERNAL_ERROR", 500, "能力执行失败，请稍后重试");
    }

    private void audit(
            SecureActionDescriptor descriptor,
            CapabilitySecurityPrincipal principal,
            String requestId,
            CapabilityResultStatus status,
            String resultCode,
            String errorCode,
            String schemaPath,
            long durationMs) {
        try {
            auditService.recordOrUpdate(principal.tenantId(), new CapabilityInvocationAuditEvent(
                    requestId, principal.clientId(), principal.clientCode(),
                    descriptor.capabilityId(), descriptor.capabilityCode(), descriptor.version(),
                    principal.actorType(), principal.actorUserId(), principal.serviceUserId(),
                    principal.activeOrgId(), status, resultCode, errorCode, schemaPath,
                    null, durationMs));
        }
        catch (RuntimeException exception) {
            log.warn("[能力开放网关审计] 记录失败, requestId={}, capabilityCode={}, exceptionType={}",
                    requestId, descriptor.capabilityCode(),
                    exception.getClass().getSimpleName(), exception);
            throw new SecureActionUnavailableException("AUDIT_UNAVAILABLE", exception);
        }
    }

    private boolean isAuditUnavailable(Exception exception) {
        return exception instanceof SecureActionUnavailableException unavailable
                && "AUDIT_UNAVAILABLE".equals(unavailable.getErrorCode());
    }

    private String schemaPath(Exception exception) {
        return exception instanceof CapabilitySchemaValidationException validation
                ? validation.getPath() : null;
    }

    private String text(Object value) {
        return value == null ? null : StringUtils.trimToNull(String.valueOf(value));
    }

    private long elapsed(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    private record ErrorMapping(String errorCode, int httpStatus, String message) {
    }
}
