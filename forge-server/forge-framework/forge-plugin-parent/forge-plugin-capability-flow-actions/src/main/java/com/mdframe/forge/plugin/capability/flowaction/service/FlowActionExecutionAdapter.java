package com.mdframe.forge.plugin.capability.flowaction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.capability.flowaction.source.FlowActionSourceService;
import com.mdframe.forge.plugin.capability.flowaction.source.FlowActionSourceService.ResolvedFlowActionSource;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;
import com.mdframe.forge.plugin.capability.secureaction.spi.GovernedCapabilitySnapshot;
import com.mdframe.forge.plugin.capability.secureaction.spi.GovernedOpenGatewayAdapter;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowStartDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskActionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowRuntimeVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class FlowActionExecutionAdapter implements GovernedOpenGatewayAdapter {

    public static final String PLATFORM_PERMISSION = "ai:capability:flow-action:invoke";

    private static final Set<String> RECORD_PAYLOAD_FIELDS = Set.of("recordId", "arguments");
    private static final Set<String> SUBMIT_PAYLOAD_FIELDS = Set.of("data");

    private final FlowActionSourceService sourceService;
    private final BusinessFlowService flowService;
    private final FlowActionExecutionLogService executionLogService;
    private final FlowApplicationSubmissionService submissionService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(GovernedCapabilitySnapshot snapshot) {
        return snapshot != null
                && "FLOW_ACTION".equals(snapshot.sourceType())
                && "FLOW".equals(snapshot.behavior());
    }

    @Override
    public SecureActionDescriptor resolve(
            GovernedCapabilitySnapshot snapshot,
            JsonNode grantPolicy) {
        String[] source = StringUtils.defaultString(snapshot.sourceKey()).split("/", -1);
        if (source.length != 3) {
            throw conflict();
        }
        int publishedVersion = positiveVersion(snapshot.sourceVersion());
        if (snapshot.policySnapshot().path("publishedObjectVersion").asInt() != publishedVersion) {
            throw conflict();
        }
        Set<String> operations = fields(snapshot.policySnapshot().path("allowedOperations"));
        operations.retainAll(fields(grantPolicy.path("allowedOperations")));
        if (!operations.contains(source[2])
                || !source[2].equals(snapshot.policySnapshot().path("operation").asText())) {
            throw conflict();
        }
        Set<String> allowedFields = Set.of();
        Set<String> requiredFields = Set.of();
        if ("SUBMIT".equals(source[2])) {
            Set<String> effectiveFields = fields(snapshot.policySnapshot().path("allowedFields"));
            effectiveFields.retainAll(fields(grantPolicy.path("allowedFields")));
            Set<String> versionRequiredFields = fields(
                    snapshot.policySnapshot().path("requiredFields"));
            if (effectiveFields.isEmpty()
                    || !effectiveFields.containsAll(versionRequiredFields)) {
                throw conflict();
            }
            allowedFields = Set.copyOf(effectiveFields);
            requiredFields = Set.copyOf(versionRequiredFields);
        }
        return new SecureActionDescriptor(
                snapshot.capabilityId(), snapshot.capabilityCode(), snapshot.capabilityName(),
                snapshot.description(), snapshot.version(), snapshot.sourceType(), snapshot.sourceKey(),
                snapshot.sourceVersion(), snapshot.behavior(), snapshot.riskLevel(),
                source[0], source[1], source[2], publishedVersion,
                snapshot.policySnapshot().path("permission").asText(),
                Set.copyOf(allowedFields), Set.copyOf(requiredFields),
                snapshot.policySnapshot(), effectiveInputSchema(
                        snapshot.inputSchema(), source[2], allowedFields, requiredFields),
                snapshot.outputSchema());
    }

    @Override
    public String platformPermission(SecureActionDescriptor descriptor) {
        return PLATFORM_PERMISSION;
    }

    @Override
    public Map<String, Object> prepareInput(
            SecureActionDescriptor descriptor,
            Map<String, Object> payload) {
        Map<String, Object> request = payload == null ? Map.of() : payload;
        if ("SUBMIT".equals(descriptor.actionCode())) {
            if (!SUBMIT_PAYLOAD_FIELDS.containsAll(request.keySet())) {
                throw new BusinessException("SUBMIT payload 只允许 data 顶层字段");
            }
            Map<String, Object> data = mapValue(request.get("data"), "data");
            if (!descriptor.allowedFields().containsAll(data.keySet())) {
                throw new BusinessException("data 包含当前客户端授权未开放的业务字段");
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("data", data);
            return result;
        }
        if (!RECORD_PAYLOAD_FIELDS.containsAll(request.keySet())) {
            throw new BusinessException("payload 包含未允许的顶层字段");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        String recordId = StringUtils.trimToNull(text(request.get("recordId")));
        if (recordId != null) {
            result.put("recordId", recordId);
        }
        result.put("arguments", argumentsValue(request.get("arguments")));
        return result;
    }

    @Override
    public boolean supports(SecureActionDescriptor descriptor) {
        return descriptor != null
                && "FLOW_ACTION".equals(descriptor.sourceType())
                && "FLOW".equals(descriptor.behavior());
    }

    @Override
    public void validate(SecureActionDescriptor descriptor, Map<String, Object> input) {
        ExecutionIdentity identity = requireIdentity();
        ResolvedFlowActionSource source = requireSource(identity, descriptor);
        String operation = descriptor.actionCode();
        if ("SUBMIT".equals(operation)) {
            requireSubmissionPolicy(descriptor, source, input);
            return;
        }
        Long recordId = requireRecordId(input);
        if ("START".equals(operation)) {
            requireEmptyArguments(input);
            return;
        }
        if (!"APPROVE".equals(operation) && !"REJECT".equals(operation)) {
            throw new BusinessException(409, "POLICY_MISMATCH");
        }
        Map<String, Object> arguments = arguments(input);
        String taskId = requireText(arguments.get("taskId"), "taskId");
        String comment = StringUtils.trimToNull(text(arguments.get("comment")));
        if ("REJECT".equals(operation) && comment == null) {
            throw new BusinessException("驳回意见不能为空");
        }
        if (comment != null && comment.length() > 500) {
            throw new BusinessException("审批意见不能超过 500 字");
        }
        if (executionLogService.isRecoverableRequest(descriptor, identity, input)) {
            return;
        }
        verifyTaskTarget(descriptor, recordId, taskId);
    }

    @Override
    public Map<String, Object> execute(
            SecureActionDescriptor descriptor,
            Map<String, Object> input,
            String requestId) {
        ExecutionIdentity identity = requireIdentity();
        boolean replayOrRecovery = executionLogService.isRecoverableRequest(descriptor, identity, input);
        return executionLogService.executeWithAttempt(descriptor, identity, input, requestId, attempt -> {
            if (!replayOrRecovery) {
                validate(descriptor, input);
            }
            BusinessFlowRuntimeVO runtime;
            if ("SUBMIT".equals(descriptor.actionCode())) {
                ResolvedFlowActionSource source = requireSource(identity, descriptor);
                requireSubmissionPolicy(descriptor, source, input);
                String recordId = attempt.resolveOrCreateRecord(() -> submissionService.create(
                        source.row(), identity, data(input)));
                runtime = start(descriptor, parseRecordId(recordId));
            }
            else if ("START".equals(descriptor.actionCode())) {
                runtime = start(descriptor, requireRecordId(input));
            }
            else {
                runtime = complete(descriptor, identity, input, replayOrRecovery);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("executeStatus", "SUCCESS");
            result.put("message", StringUtils.defaultIfBlank(runtime.getMessage(),
                    "SUBMIT".equals(descriptor.actionCode()) ? "申请已提交" : "流程动作执行成功"));
            result.put("correlationId", requestId);
            result.put("idempotentHit", false);
            if ("SUBMIT".equals(descriptor.actionCode())) {
                appendSubmissionResult(result, runtime);
            }
            return result;
        });
    }

    private BusinessFlowRuntimeVO start(SecureActionDescriptor descriptor, Long recordId) {
        BusinessFlowStartDTO command = new BusinessFlowStartDTO();
        command.setObjectCode(descriptor.objectCode());
        command.setRecordId(recordId);
        command.setVariables(new LinkedHashMap<>());
        return flowService.startDocumentFlowForCapability(command);
    }

    private void appendSubmissionResult(
            Map<String, Object> result,
            BusinessFlowRuntimeVO runtime) {
        if (runtime == null || runtime.getRecordId() == null
                || StringUtils.isAnyBlank(runtime.getBusinessKey(), runtime.getProcessInstanceId(),
                        runtime.getFlowModelKey(), runtime.getFlowStatus())) {
            throw new BusinessException(503,
                    "流程已发起但返回信息不完整，请使用原 Idempotency-Key 重试");
        }
        result.put("recordId", String.valueOf(runtime.getRecordId()));
        result.put("businessKey", runtime.getBusinessKey());
        result.put("processInstanceId", runtime.getProcessInstanceId());
        result.put("flowModelKey", runtime.getFlowModelKey());
        result.put("flowStatus", runtime.getFlowStatus());
    }

    private BusinessFlowRuntimeVO complete(SecureActionDescriptor descriptor,
                                           ExecutionIdentity identity,
                                           Map<String, Object> input,
                                           boolean recovery) {
        Map<String, Object> arguments = arguments(input);
        BusinessTaskActionDTO command = new BusinessTaskActionDTO();
        command.setAction(descriptor.actionCode().toLowerCase());
        command.setTaskId(requireText(arguments.get("taskId"), "taskId"));
        command.setObjectCode(descriptor.objectCode());
        command.setRecordId(requireRecordId(input));
        command.setComment(StringUtils.trimToNull(text(arguments.get("comment"))));
        command.setUserId(null);
        command.setTenantId(identity.loginUser().getTenantId());
        command.setIdempotencyKey(String.valueOf(input.get("idempotencyKey")));
        command.setRequestDigest(executionLogService.requestDigest(descriptor, input));
        command.setVariables(new LinkedHashMap<>());
        command.setData(new LinkedHashMap<>());
        return recovery
                ? flowService.recoverCapabilityTaskAction(command)
                : flowService.completeBusinessTask(command);
    }

    private void verifyTaskTarget(
            SecureActionDescriptor descriptor,
            Long expectedRecordId,
            String taskId) {
        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId(taskId);
        BusinessTaskFormContextVO context = flowService.getActionableTaskFormContext(query);
        if (context == null
                || !descriptor.objectCode().equals(context.getObjectCode())
                || context.getRecordId() == null
                || !expectedRecordId.equals(context.getRecordId())
                || !sameProcessKey(descriptor.policySnapshot().path("flowModelKey").asText(),
                        context.getProcessDefKey())) {
            throw new BusinessException(403, "FLOW_TASK_MISMATCH");
        }
    }

    private ResolvedFlowActionSource requireSource(
            ExecutionIdentity identity,
            SecureActionDescriptor descriptor) {
        long bindingId = descriptor.policySnapshot().path("bindingId").asLong(0L);
        String flowModelKey = descriptor.policySnapshot().path("flowModelKey").asText();
        if (bindingId <= 0 || StringUtils.isBlank(flowModelKey)
                || !descriptor.actionCode().equals(
                        descriptor.policySnapshot().path("operation").asText())) {
            throw new BusinessException(409, "POLICY_MISMATCH");
        }
        return sourceService.requireMatching(
                identity.loginUser().getTenantId(), descriptor.suiteCode(), descriptor.objectCode(),
                descriptor.publishedObjectVersion(), bindingId, flowModelKey);
    }

    private void requireSubmissionPolicy(
            SecureActionDescriptor descriptor,
            ResolvedFlowActionSource source,
            Map<String, Object> input) {
        Map<String, ?> publishedFields = sourceService.requireSubmissionFields(source);
        if (descriptor.allowedFields().isEmpty()
                || !publishedFields.keySet().containsAll(descriptor.allowedFields())
                || !descriptor.allowedFields().containsAll(descriptor.requiredFields())) {
            throw new BusinessException(409, "POLICY_MISMATCH");
        }
        Map<String, Object> data = data(input);
        if (!descriptor.allowedFields().containsAll(data.keySet())
                || !data.keySet().containsAll(descriptor.requiredFields())) {
            throw new BusinessException("申请数据不符合能力版本字段白名单或必填规则");
        }
    }

    private ExecutionIdentity requireIdentity() {
        ExecutionIdentity identity = ExecutionIdentityContextHolder.current()
                .orElseThrow(() -> new BusinessException(401, "缺少可信 MCP 执行身份"));
        if (!"USER".equals(identity.actorType())
                || identity.actorUserId() == null
                || identity.loginUser().getTenantId() == null
                || identity.loginUser().getActiveOrgId() == null) {
            throw new BusinessException(403, "USER_DELEGATION_REQUIRED");
        }
        return identity;
    }

    private Long requireRecordId(Map<String, Object> input) {
        String value = requireText(input.get("recordId"), "recordId");
        return parseRecordId(value);
    }

    private Long parseRecordId(String value) {
        try {
            long recordId = Long.parseLong(value);
            if (recordId <= 0) {
                throw new NumberFormatException();
            }
            return recordId;
        }
        catch (NumberFormatException exception) {
            throw new BusinessException("recordId 必须是有效的长整型字符串");
        }
    }

    private void requireEmptyArguments(Map<String, Object> input) {
        if (!arguments(input).isEmpty()) {
            throw new BusinessException("START 不接受客户端流程变量");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> arguments(Map<String, Object> input) {
        return argumentsValue(input.get("arguments"));
    }

    private Map<String, Object> argumentsValue(Object value) {
        return mapValue(value, "arguments");
    }

    private Map<String, Object> data(Map<String, Object> input) {
        return mapValue(input.get("data"), "data");
    }

    private Map<String, Object> mapValue(Object value, String field) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new BusinessException(field + " 必须是对象");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private boolean sameProcessKey(String expected, String actual) {
        return StringUtils.equals(processKey(expected), processKey(actual));
    }

    private String processKey(String value) {
        String text = StringUtils.trimToNull(value);
        if (text == null) {
            return null;
        }
        int separator = text.indexOf(':');
        return separator > 0 ? text.substring(0, separator) : text;
    }

    private String requireText(Object value, String field) {
        String text = StringUtils.trimToNull(text(value));
        if (text == null) {
            throw new BusinessException(field + " 不能为空");
        }
        return text;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private JsonNode withoutBodyIdempotency(JsonNode sourceSchema) {
        JsonNode copy = sourceSchema.deepCopy();
        if (!(copy instanceof ObjectNode root)
                || !(root.path("properties") instanceof ObjectNode properties)) {
            throw conflict();
        }
        properties.remove("idempotencyKey");
        if (root.path("required") instanceof ArrayNode required) {
            ArrayNode filtered = objectMapper.createArrayNode();
            required.forEach(item -> {
                if (!"idempotencyKey".equals(item.asText())) {
                    filtered.add(item);
                }
            });
            root.set("required", filtered);
        }
        return root;
    }

    private JsonNode effectiveInputSchema(
            JsonNode sourceSchema,
            String operation,
            Set<String> allowedFields,
            Set<String> requiredFields) {
        JsonNode copy = withoutBodyIdempotency(sourceSchema);
        if (!"SUBMIT".equals(operation)) {
            return copy;
        }
        if (!(copy instanceof ObjectNode root)
                || !(root.path("properties") instanceof ObjectNode rootProperties)
                || !(rootProperties.path("data") instanceof ObjectNode data)
                || !(data.path("properties") instanceof ObjectNode dataProperties)) {
            throw conflict();
        }
        Set<String> schemaFields = new LinkedHashSet<>();
        dataProperties.fieldNames().forEachRemaining(schemaFields::add);
        if (!schemaFields.containsAll(allowedFields)) {
            throw conflict();
        }
        schemaFields.stream()
                .filter(field -> !allowedFields.contains(field))
                .forEach(dataProperties::remove);
        ArrayNode required = objectMapper.createArrayNode();
        requiredFields.forEach(required::add);
        data.set("required", required);
        data.put("additionalProperties", false);
        return root;
    }

    private int positiveVersion(String sourceVersion) {
        try {
            int value = Integer.parseInt(sourceVersion);
            if (value > 0) {
                return value;
            }
        }
        catch (NumberFormatException ignored) {
        }
        throw conflict();
    }

    private Set<String> fields(JsonNode node) {
        Set<String> result = new LinkedHashSet<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                if (item.isTextual() && !item.asText().isBlank()) {
                    result.add(item.asText());
                }
            });
        }
        return result;
    }

    private BusinessException conflict() {
        return new BusinessException(409, "能力发布模型与授权策略不一致");
    }
}
