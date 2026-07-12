package com.mdframe.forge.plugin.capability.flowaction.service;

import com.mdframe.forge.plugin.capability.flowaction.source.FlowActionSourceService;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;
import com.mdframe.forge.plugin.capability.secureaction.spi.GovernedCapabilityExecutionAdapter;
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
import java.util.Map;

@RequiredArgsConstructor
public class FlowActionExecutionAdapter implements GovernedCapabilityExecutionAdapter {

    private final FlowActionSourceService sourceService;
    private final BusinessFlowService flowService;
    private final FlowActionExecutionLogService executionLogService;

    @Override
    public boolean supports(SecureActionDescriptor descriptor) {
        return descriptor != null
                && "FLOW_ACTION".equals(descriptor.sourceType())
                && "FLOW".equals(descriptor.behavior());
    }

    @Override
    public void validate(SecureActionDescriptor descriptor, Map<String, Object> input) {
        ExecutionIdentity identity = requireIdentity();
        requireSource(identity, descriptor);
        Long recordId = requireRecordId(input);
        String operation = descriptor.actionCode();
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
        return executionLogService.execute(descriptor, identity, input, requestId, () -> {
            if (!replayOrRecovery) {
                validate(descriptor, input);
            }
            BusinessFlowRuntimeVO runtime = "START".equals(descriptor.actionCode())
                    ? start(descriptor, input)
                    : complete(descriptor, identity, input, replayOrRecovery);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("executeStatus", "SUCCESS");
            result.put("message", StringUtils.defaultIfBlank(runtime.getMessage(), "流程动作执行成功"));
            result.put("correlationId", requestId);
            result.put("idempotentHit", false);
            return result;
        });
    }

    private BusinessFlowRuntimeVO start(SecureActionDescriptor descriptor, Map<String, Object> input) {
        BusinessFlowStartDTO command = new BusinessFlowStartDTO();
        command.setObjectCode(descriptor.objectCode());
        command.setRecordId(requireRecordId(input));
        command.setVariables(new LinkedHashMap<>());
        return flowService.startDocumentFlowForCapability(command);
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

    private void requireSource(ExecutionIdentity identity, SecureActionDescriptor descriptor) {
        long bindingId = descriptor.policySnapshot().path("bindingId").asLong(0L);
        String flowModelKey = descriptor.policySnapshot().path("flowModelKey").asText();
        if (bindingId <= 0 || StringUtils.isBlank(flowModelKey)
                || !descriptor.actionCode().equals(
                        descriptor.policySnapshot().path("operation").asText())) {
            throw new BusinessException(409, "POLICY_MISMATCH");
        }
        sourceService.requireMatching(
                identity.loginUser().getTenantId(), descriptor.suiteCode(), descriptor.objectCode(),
                descriptor.publishedObjectVersion(), bindingId, flowModelKey);
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
        Object value = input.get("arguments");
        if (!(value instanceof Map<?, ?> raw)) {
            throw new BusinessException("arguments 必须是对象");
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
}
