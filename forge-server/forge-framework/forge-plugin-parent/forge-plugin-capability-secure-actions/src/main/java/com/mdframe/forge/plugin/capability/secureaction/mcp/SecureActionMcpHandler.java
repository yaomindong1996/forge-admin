package com.mdframe.forge.plugin.capability.secureaction.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;
import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityInvocationAuditEvent;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityInvocationAuditService;
import com.mdframe.forge.plugin.capability.model.CapabilityResultStatus;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidationException;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionCatalogService;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;
import com.mdframe.forge.plugin.capability.secureaction.exception.SecureActionUnavailableException;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionStepValidator;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionPublishedModelPolicy;
import com.mdframe.forge.plugin.capability.secureaction.spi.GovernedCapabilityExecutionAdapter;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessActionExecuteDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessActionExecutionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectActionService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessActionExecuteResultVO;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.exception.BusinessException;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class SecureActionMcpHandler {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };
    private static final Set<String> INVOKE_TOP_LEVEL_FIELDS = Set.of(
            "capabilityCode", "version", "recordId", "idempotencyKey", "arguments");

    private final SecureActionCatalogService catalogService;
    private final BusinessObjectActionService actionService;
    private final BusinessActionExecutionService executionService;
    private final SecureActionStepValidator stepValidator;
    private final SecureActionPublishedModelPolicy publishedModelPolicy;
    private final CapabilitySchemaValidator schemaValidator;
    private final CapabilityInvocationAuditService auditService;
    private final ObjectMapper objectMapper;
    private final List<GovernedCapabilityExecutionAdapter> executionAdapters;

    public SecureActionMcpHandler(
            SecureActionCatalogService catalogService,
            BusinessObjectActionService actionService,
            BusinessActionExecutionService executionService,
            SecureActionStepValidator stepValidator,
            SecureActionPublishedModelPolicy publishedModelPolicy,
            CapabilitySchemaValidator schemaValidator,
            CapabilityInvocationAuditService auditService,
            ObjectMapper objectMapper) {
        this(catalogService, actionService, executionService, stepValidator, publishedModelPolicy,
                schemaValidator, auditService, objectMapper, List.of());
    }

    public McpSchema.CallToolResult search(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        try {
            Map<String, Object> args = arguments(request);
            String query = text(args.get("query"));
            Integer limit = integer(args.get("limit"));
            var result = catalogService.search(query, limit);
            List<Map<String, Object>> items = result.items().stream().map(item -> Map.<String, Object>of(
                    "capabilityCode", item.capabilityCode(),
                    "name", item.capabilityName(),
                    "description", item.description(),
                    "version", item.version(),
                    "sourceType", item.sourceType(),
                    "behavior", item.behavior(),
                    "operation", item.actionCode(),
                    "riskLevel", item.riskLevel(),
                    "confirmationRequired", true)).toList();
            return success(Map.of("items", items, "count", items.size(), "hasMore", result.hasMore()), requestId(exchange));
        }
        catch (RuntimeException exception) {
            return failure(exception, requestId(exchange));
        }
    }

    public McpSchema.CallToolResult describe(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        try {
            String capabilityCode = requireText(
                    arguments(request).get("capabilityCode"), "capabilityCode");
            ExecutionIdentity identity = catalogService.requireIdentity();
            catalogService.requireDiscoveryScope(identity, capabilityCode);
            SecureActionDescriptor descriptor = catalogService.requireAuthorized(capabilityCode);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("capabilityCode", descriptor.capabilityCode());
            data.put("name", descriptor.capabilityName());
            data.put("description", descriptor.description());
            data.put("version", descriptor.version());
            data.put("sourceType", descriptor.sourceType());
            data.put("behavior", descriptor.behavior());
            data.put("operation", descriptor.actionCode());
            data.put("riskLevel", descriptor.riskLevel());
            data.put("confirmationRequired", true);
            data.put("allowedFields", descriptor.allowedFields().stream().sorted().toList());
            data.put("requiredFields", descriptor.requiredFields().stream().sorted().toList());
            data.put("inputSchema", objectMapper.convertValue(descriptor.inputSchema(), MAP_TYPE));
            data.put("outputSchema", objectMapper.convertValue(descriptor.outputSchema(), MAP_TYPE));
            return success(data, requestId(exchange));
        }
        catch (RuntimeException exception) {
            return failure(exception, requestId(exchange));
        }
    }

    public McpSchema.CallToolResult invoke(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        String requestId = requestId(exchange);
        long startedAt = System.nanoTime();
        SecureActionDescriptor descriptor = null;
        ExecutionIdentity identity = null;
        boolean executionCompleted = false;
        try {
            Map<String, Object> args = arguments(request);
            requireInvokeTopLevelFields(args);
            String capabilityCode = requireText(args.get("capabilityCode"), "capabilityCode");
            identity = catalogService.requireIdentity();
            catalogService.requireInvocationScope(identity, capabilityCode);
            descriptor = catalogService.requireAuthorized(capabilityCode);
            if (!"USER".equals(identity.actorType())) {
                throw new BusinessException(403, "USER_DELEGATION_REQUIRED");
            }
            String requestedVersion = text(args.get("version"));
            if (requestedVersion != null && !descriptor.version().equals(requestedVersion)) {
                throw new BusinessException("请求版本未获授权");
            }
            Map<String, Object> targetInput = new LinkedHashMap<>();
            String recordId = text(args.get("recordId"));
            if (recordId != null) {
                targetInput.put("recordId", recordId);
            }
            targetInput.put("idempotencyKey", requireIdempotencyKey(args.get("idempotencyKey")));
            Map<String, Object> invocationArguments = requireArgumentsMap(args.get("arguments"));
            if ("BUSINESS_ACTION".equals(descriptor.sourceType())
                    && !descriptor.allowedFields().containsAll(invocationArguments.keySet())) {
                throw new BusinessException("arguments 包含未授权字段");
            }
            targetInput.put("arguments", invocationArguments);
            JsonNode inputNode = objectMapper.valueToTree(targetInput);
            schemaValidator.validateInstance(descriptor.inputSchema(), inputNode);

            GovernedCapabilityExecutionAdapter adapter = null;
            boolean synchronousBusinessAction = "BUSINESS_ACTION".equals(descriptor.sourceType())
                    && "MEDIUM".equals(descriptor.riskLevel());
            if (synchronousBusinessAction) {
                var published = actionService.resolvePublishedAction(
                        descriptor.suiteCode(), descriptor.objectCode(), descriptor.actionCode(),
                        descriptor.publishedObjectVersion());
                stepValidator.validate(published.action());
                if (!publishedModelPolicy.writableFields(published.version()).keySet()
                        .containsAll(descriptor.allowedFields())) {
                    throw new BusinessException(409, "POLICY_MISMATCH");
                }
            }
            else {
                adapter = requireAdapter(descriptor);
                adapter.validate(descriptor, targetInput);
            }
            requireElicitationAccept(exchange, descriptor, targetInput, requestId);
            auditRequired(descriptor, identity, requestId, CapabilityResultStatus.ERROR,
                    "EXECUTION_PENDING", null, null, elapsed(startedAt));

            Map<String, Object> data;
            if (synchronousBusinessAction) {
                BusinessActionExecuteDTO command = new BusinessActionExecuteDTO();
                command.setSuiteCode(descriptor.suiteCode());
                command.setObjectCode(descriptor.objectCode());
                command.setActionCode(descriptor.actionCode());
                command.setRecordId(recordId);
                command.setIdempotencyKey(String.valueOf(targetInput.get("idempotencyKey")));
                command.setFormData(new LinkedHashMap<>(requireArgumentsMap(targetInput.get("arguments"))));
                BusinessActionExecuteResultVO executed = executionService.executePublished(
                        command, descriptor.publishedObjectVersion(), requestId);
                data = new LinkedHashMap<>();
                data.put("executeStatus", executed.getExecuteStatus());
                data.put("message", executed.getMessage());
                data.put("correlationId", executed.getCorrelationId());
                data.put("idempotentHit", Boolean.TRUE.equals(executed.getIdempotentHit()));
            }
            else {
                data = new LinkedHashMap<>(adapter.execute(descriptor, targetInput, requestId));
            }
            executionCompleted = true;
            String executeStatus = text(data.get("executeStatus"));
            String resultCode = "PENDING_APPROVAL".equals(executeStatus)
                    ? "PENDING_APPROVAL" : "SUCCESS";
            auditRequired(descriptor, identity, requestId, CapabilityResultStatus.SUCCESS,
                    resultCode, null, null, elapsed(startedAt));
            return success(data, requestId);
        }
        catch (RuntimeException exception) {
            RuntimeException responseException = exception;
            if (!executionCompleted && descriptor != null && identity != null
                    && !isAuditUnavailable(exception)) {
                try {
                    auditRequired(descriptor, identity, requestId, CapabilityResultStatus.ERROR,
                            "FAILED", errorCode(exception), schemaPath(exception), elapsed(startedAt));
                }
                catch (SecureActionUnavailableException auditException) {
                    responseException = auditException;
                }
            }
            return failure(responseException, requestId);
        }
    }

    private void requireInvokeTopLevelFields(Map<String, Object> arguments) {
        if (!INVOKE_TOP_LEVEL_FIELDS.containsAll(arguments.keySet())) {
            throw new BusinessException("调用参数包含未允许的顶层字段");
        }
    }

    private void requireElicitationAccept(
            McpSyncServerExchange exchange,
            SecureActionDescriptor descriptor,
            Map<String, Object> input,
            String requestId) {
        if (exchange == null || exchange.getClientCapabilities() == null
                || exchange.getClientCapabilities().elicitation() == null) {
            throw new BusinessException(409, "CONFIRMATION_REQUIRED");
        }
        String message = elicitationMessage(descriptor, input);
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("confirm", Map.of(
                        "type", "boolean", "title", "确认执行受控业务动作")),
                "required", List.of("confirm"),
                "additionalProperties", false);
        McpSchema.ElicitResult result = exchange.createElicitation(
                McpSchema.ElicitRequest.builder().message(message).requestedSchema(schema).build());
        if (result == null || result.action() != McpSchema.ElicitResult.Action.ACCEPT
                || result.content() == null
                || !Boolean.TRUE.equals(result.content().get("confirm"))) {
            throw new BusinessException(409, "CONFIRMATION_DECLINED");
        }
    }

    @SuppressWarnings("unchecked")
    String elicitationMessage(SecureActionDescriptor descriptor, Map<String, Object> input) {
        Map<String, Object> arguments = (Map<String, Object>) input.get("arguments");
        String prefix = "确认执行「" + descriptor.capabilityName() + "」；对象="
                + descriptor.objectCode() + "，记录="
                + StringUtils.defaultIfBlank(text(input.get("recordId")), "新记录");
        if ("FLOW_ACTION".equals(descriptor.sourceType())) {
            return prefix + "，操作=" + descriptor.actionCode()
                    + "，任务=" + safeTaskTail(arguments.get("taskId"))
                    + "，请求指纹=" + requestFingerprint(descriptor, input);
        }
        Set<String> fields = arguments.keySet();
        return prefix + "，字段=" + fields.stream().sorted().toList()
                + "，请求指纹=" + requestFingerprint(descriptor, input);
    }

    private String safeTaskTail(Object taskId) {
        String value = text(taskId);
        if (value == null) {
            return "无";
        }
        int visible = Math.min(4, value.length());
        return "***" + value.substring(value.length() - visible);
    }

    private void auditRequired(
            SecureActionDescriptor descriptor,
            ExecutionIdentity identity,
            String requestId,
            CapabilityResultStatus status,
            String resultCode,
            String errorCode,
            String schemaPath,
            long durationMs) {
        try {
            auditService.recordOrUpdate(identity.loginUser().getTenantId(), new CapabilityInvocationAuditEvent(
                    requestId, identity.clientId(), identity.clientCode(), descriptor.capabilityId(),
                    descriptor.capabilityCode(), descriptor.version(),
                    CapabilityActorType.valueOf(identity.actorType()), identity.actorUserId(),
                    identity.serviceUserId(), identity.loginUser().getActiveOrgId(), status,
                    resultCode, errorCode, schemaPath, null, durationMs));
        }
        catch (RuntimeException exception) {
            log.warn("[受控业务动作审计] 记录失败, requestId={}, capabilityCode={}, exceptionType={}",
                    requestId, descriptor.capabilityCode(), exception.getClass().getSimpleName());
            throw new SecureActionUnavailableException("AUDIT_UNAVAILABLE", exception);
        }
    }

    private McpSchema.CallToolResult success(Map<String, Object> value, String requestId) {
        return result(value, false, requestId, null);
    }

    private McpSchema.CallToolResult failure(RuntimeException exception, String requestId) {
        String code = errorCode(exception);
        if (exception instanceof SecureActionUnavailableException) {
            log.warn("[受控业务动作] 基础设施不可用, requestId={}, errorCode={}, exceptionType={}",
                    requestId, code, rootExceptionType(exception));
        }
        return result(Map.of("requestId", requestId, "errorCode", code,
                "message", safeMessage(code)), true, requestId, code);
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
                    .structuredContent(Map.copyOf(value))
                    .isError(error)
                    .meta(Map.copyOf(meta))
                    .build();
        }
        catch (Exception exception) {
            return McpSchema.CallToolResult.builder()
                    .addTextContent("{\"errorCode\":\"INTERNAL_ERROR\"}")
                    .isError(true).build();
        }
    }

    private Map<String, Object> arguments(McpSchema.CallToolRequest request) {
        return request == null || request.arguments() == null
                ? Map.of() : new LinkedHashMap<>(request.arguments());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requireArgumentsMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new BusinessException("arguments 必须是对象");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private String requireIdempotencyKey(Object value) {
        String key = requireText(value, "idempotencyKey");
        if (key.length() < 16 || key.length() > 128
                || !key.matches("^[A-Za-z0-9._:-]+$")) {
            throw new BusinessException("idempotencyKey 必须是 16-128 位安全字符");
        }
        return key;
    }

    private String requireText(Object value, String field) {
        String text = text(value);
        if (text == null) {
            throw new BusinessException(field + " 不能为空");
        }
        return text;
    }

    private String text(Object value) {
        return value == null ? null : StringUtils.trimToNull(String.valueOf(value));
    }

    private Integer integer(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        }
        catch (NumberFormatException exception) {
            throw new BusinessException("limit 必须是整数");
        }
    }

    private String requestId(McpSyncServerExchange exchange) {
        if (exchange != null && exchange.transportContext() != null) {
            Object value = exchange.transportContext().get(
                    com.mdframe.forge.plugin.mcp.security.McpTransportContextKeys.REQUEST_ID);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return UUID.randomUUID().toString();
    }

    private String errorCode(RuntimeException exception) {
        if (exception instanceof SecureActionUnavailableException unavailable) {
            return unavailable.getErrorCode();
        }
        String message = exception.getMessage();
        if ("USER_DELEGATION_REQUIRED".equals(message)
                || "CONFIRMATION_REQUIRED".equals(message)
                || "CONFIRMATION_DECLINED".equals(message)
                || "POLICY_MISMATCH".equals(message)
                || "FLOW_BINDING_MISMATCH".equals(message)
                || "FLOW_TASK_MISMATCH".equals(message)
                || "IDEMPOTENCY_CONFLICT".equals(message)
                || "INSUFFICIENT_SCOPE".equals(message)) {
            return message;
        }
        if (exception instanceof BusinessException business
                && Integer.valueOf(403).equals(business.getCode())) {
            return "FORBIDDEN";
        }
        if (exception instanceof BusinessException business
                && Integer.valueOf(401).equals(business.getCode())) {
            return "UNAUTHENTICATED";
        }
        if (exception instanceof CapabilitySchemaValidationException) {
            return "INVALID_ARGUMENT";
        }
        if (exception instanceof BusinessException
                && message != null && message.contains("幂等")) {
            return "IDEMPOTENCY_CONFLICT";
        }
        if (exception instanceof BusinessException) {
            return "INVALID_ARGUMENT";
        }
        return "EXECUTION_FAILED";
    }

    private String safeMessage(String errorCode) {
        return switch (errorCode) {
            case "USER_DELEGATION_REQUIRED" -> "该业务动作必须由具体用户授权后执行";
            case "CONFIRMATION_REQUIRED" -> "客户端必须支持 MCP elicitation 才能执行该动作";
            case "CONFIRMATION_DECLINED" -> "用户未确认执行该业务动作";
            case "POLICY_MISMATCH" -> "业务动作发布模型与授权字段策略不一致";
            case "FLOW_BINDING_MISMATCH" -> "流程能力版本与当前业务流程绑定不一致";
            case "FLOW_TASK_MISMATCH" -> "当前任务不属于已授权的业务对象或流程";
            case "INSUFFICIENT_SCOPE" -> "当前令牌未获得该业务动作所需 scope";
            case "FORBIDDEN" -> "当前调用方无权执行该业务动作";
            case "UNAUTHENTICATED" -> "缺少可信 MCP 执行身份";
            case "INVALID_ARGUMENT" -> "业务动作参数不符合要求";
            case "IDEMPOTENCY_CONFLICT" -> "幂等键已被其他请求占用或动作仍在执行";
            case "CATALOG_UNAVAILABLE" -> "业务动作目录暂时不可用，请稍后重试";
            case "AUTHORIZATION_UNAVAILABLE" -> "业务动作授权服务暂时不可用，请稍后重试";
            case "AUDIT_UNAVAILABLE" -> "业务动作审计服务暂时不可用，请稍后重试";
            case "FLOW_CATALOG_UNAVAILABLE" -> "流程能力目录暂时不可用，请稍后重试";
            case "FLOW_AUDIT_UNAVAILABLE" -> "流程动作审计暂时不可用，请稍后重试";
            default -> "业务动作执行失败，请稍后重试";
        };
    }

    private String schemaPath(RuntimeException exception) {
        return exception instanceof CapabilitySchemaValidationException validation
                ? validation.getPath() : null;
    }

    private long elapsed(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    String requestFingerprint(SecureActionDescriptor descriptor, Map<String, Object> input) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("capabilityCode", descriptor.capabilityCode());
        request.put("version", descriptor.version());
        request.put("recordId", input.get("recordId"));
        request.put("idempotencyKey", input.get("idempotencyKey"));
        request.put("arguments", input.get("arguments"));
        try {
            byte[] canonical = objectMapper.writeValueAsBytes(canonicalize(request));
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(canonical);
            return java.util.HexFormat.of().formatHex(hash, 0, 8);
        }
        catch (Exception exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }

    private Object canonicalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((key, item) -> sorted.put(String.valueOf(key), canonicalize(item)));
            return sorted;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> items = new java.util.ArrayList<>();
            iterable.forEach(item -> items.add(canonicalize(item)));
            return items;
        }
        return value;
    }

    private String rootExceptionType(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getClass().getSimpleName();
    }

    private boolean isAuditUnavailable(RuntimeException exception) {
        return exception instanceof SecureActionUnavailableException unavailable
                && ("AUDIT_UNAVAILABLE".equals(unavailable.getErrorCode())
                || "FLOW_AUDIT_UNAVAILABLE".equals(unavailable.getErrorCode()));
    }

    private GovernedCapabilityExecutionAdapter requireAdapter(SecureActionDescriptor descriptor) {
        return executionAdapters.stream()
                .filter(adapter -> adapter.supports(descriptor))
                .findFirst()
                .orElseThrow(() -> new BusinessException(409, "POLICY_MISMATCH"));
    }
}
