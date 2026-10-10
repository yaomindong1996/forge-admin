package com.mdframe.forge.starter.flow.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.flow.dto.FlowApprovalPointDTO;
import com.mdframe.forge.starter.flow.dto.FlowApprovalPointResultDTO;
import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import com.mdframe.forge.starter.flow.helper.FlowNodePolicyParser;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import com.mdframe.forge.starter.flow.service.FlowNodeConfigService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.Task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Flowable 任务节点动作策略。
 *
 * <p>使用 Policy + Specification 集中 BPMN 扩展属性、节点配置覆盖、必填变量、
 * 审批要点和退回目标规则。该策略只做解析与校验，不执行任务完成或流程状态迁移。</p>
 */
@Slf4j
final class FlowTaskNodePolicy {

    private static final String FLOWABLE_NS = "http://flowable.org/bpmn";
    private static final String ACTION_APPROVE = "approve";
    private static final String ACTION_REJECT = "reject";
    private static final String ACTION_REJECT_TO_START = "rejectToStart";
    private static final String ACTION_DELEGATE = "delegate";
    private static final String ACTION_RETURN = "return";
    private static final String ACTION_TERMINATE = "terminate";
    private static final String ACTION_ADD_SIGN = "addSign";
    private static final String AUTO_APPROVAL_FIRST_ONLY = "firstOnly";
    private static final String AUTO_APPROVAL_CONSECUTIVE = "consecutive";
    private static final String AUTO_APPROVAL_NONE = "none";
    private static final String COMMENT_TYPE_APPROVAL_POINTS = "approvalPoints";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final TaskService taskService;
    private final FlowModelService flowModelService;
    private final FlowNodeConfigService flowNodeConfigService;
    private final Function<String, String> processDefinitionKeyResolver;

    FlowTaskNodePolicy(RepositoryService repositoryService,
                       HistoryService historyService,
                       TaskService taskService,
                       FlowModelService flowModelService,
                       FlowNodeConfigService flowNodeConfigService,
                       Function<String, String> processDefinitionKeyResolver) {
        this.repositoryService = repositoryService;
        this.historyService = historyService;
        this.taskService = taskService;
        this.flowModelService = flowModelService;
        this.flowNodeConfigService = flowNodeConfigService;
        this.processDefinitionKeyResolver = processDefinitionKeyResolver;
    }

    void validateTaskAction(Task task, String action, String comment, String signature) {
        validateTaskAction(task, action, comment, signature, resolveFlowNode(task));
    }

    void validateTaskAction(Task task, String action, String comment, String signature, FlowNode flowNode) {
        TaskApprovalPolicy policy = resolveApprovalPolicy(task, null, flowNode);
        if (!policy.isAllowed(action)) {
            throw new RuntimeException("当前节点不允许执行该审批操作");
        }
        validateCommentAndSignature(policy, comment, signature);
    }

    /** 减签不受该策略限制，保证已加入的人员始终可以移除。 */
    void validateAddSign(Task task) {
        if (!resolveApprovalPolicy(task, null, null).isAllowed(ACTION_ADD_SIGN)) {
            throw new RuntimeException("当前节点不允许加签");
        }
    }

    /**
     * 指定节点驳回走“驳回”语义，不能再要求节点单独开启 allowReturn。
     * 未指定目标时仍按退回上一节点校验 allowReturn。
     */
    void validateReturnAction(Task task, String comment, String signature, String requestedTargetActivityId) {
        TaskApprovalPolicy policy = resolveApprovalPolicy(task, null, null);
        boolean specifiedNode = !isBlank(requestedTargetActivityId);
        boolean allowed = specifiedNode
                ? (policy.allowReject || policy.allowReturn || policy.allowMultiReturn)
                : (policy.allowReturn || policy.allowMultiReturn);
        if (!allowed) {
            throw new RuntimeException(specifiedNode ? "当前节点不允许驳回" : "当前节点不允许退回");
        }
        validateCommentAndSignature(policy, comment, signature);
    }

    void applyApprovalPolicy(TaskFormInfo formInfo, Task task, FlowModel flowModel, FlowNode flowNode) {
        TaskApprovalPolicy policy = resolveApprovalPolicy(task, flowModel, flowNode);
        formInfo.setAllowApprove(policy.allowApprove);
        formInfo.setAllowReject(policy.allowReject);
        formInfo.setAllowDelegate(policy.allowDelegate);
        formInfo.setAllowReturn(policy.allowReturn);
        formInfo.setAllowMultiReturn(policy.allowMultiReturn);
        formInfo.setAllowTerminate(policy.allowTerminate);
        formInfo.setAllowAddSign(policy.allowAddSign);
        formInfo.setRequireSignature(policy.requireSignature);
        formInfo.setRequireComment(policy.requireComment);
        formInfo.setAllowRejectToStart(policy.allowRejectToStart);
    }

    void applyNodePolicy(TaskFormInfo formInfo, FlowNode flowNode) {
        List<FlowApprovalPointDTO> approvalPoints = FlowNodePolicyParser.resolveApprovalPoints(flowNode);
        String approvalPoint = approvalPoints.stream()
                .map(FlowApprovalPointDTO::getContent)
                .collect(Collectors.joining("\n"));
        formInfo.setApprovalPoints(approvalPoints);
        formInfo.setApprovalPoint(isBlank(approvalPoint) ? null : approvalPoint);
        formInfo.setResponsibilityDescription(FlowNodePolicyParser.resolveResponsibilityDescription(flowNode));
        formInfo.setPrintTemplatePolicy(flowNode == null
                ? null : flowNode.getAttributeValue(FLOWABLE_NS, "printTemplatePolicy"));
        formInfo.setPrintTemplateIds(flowNode == null
                ? null : flowNode.getAttributeValue(FLOWABLE_NS, "printTemplateIds"));
    }

    void validateApprovalPoints(List<FlowApprovalPointResultDTO> approvalPointResults, FlowNode flowNode) {
        List<FlowApprovalPointDTO> required = FlowNodePolicyParser.resolveApprovalPoints(flowNode).stream()
                .filter(point -> Boolean.TRUE.equals(point.getRequired()))
                .toList();
        if (required.isEmpty()) {
            return;
        }
        Map<String, Boolean> checked = new HashMap<>();
        if (approvalPointResults != null) {
            for (FlowApprovalPointResultDTO result : approvalPointResults) {
                if (result != null && !isBlank(result.getId())) {
                    checked.put(result.getId(), Boolean.TRUE.equals(result.getChecked()));
                }
            }
        }
        boolean incomplete = required.stream().anyMatch(point -> !Boolean.TRUE.equals(checked.get(point.getId())));
        if (incomplete) {
            throw new RuntimeException("请完成全部必审要点");
        }
    }

    void recordApprovalPointResults(Task task, List<FlowApprovalPointResultDTO> approvalPointResults) {
        if (task == null || approvalPointResults == null || approvalPointResults.isEmpty()) {
            return;
        }
        try {
            String json = OBJECT_MAPPER.writeValueAsString(approvalPointResults);
            taskService.addComment(task.getId(), task.getProcessInstanceId(), COMMENT_TYPE_APPROVAL_POINTS, json);
        } catch (Exception e) {
            log.warn("保存审批要点结果失败: taskId={}", task.getId(), e);
        }
    }

    List<Map<String, Object>> readApprovalPointResults(String taskId) {
        if (isBlank(taskId)) {
            return Collections.emptyList();
        }
        try {
            List<Comment> comments = taskService.getTaskComments(taskId, COMMENT_TYPE_APPROVAL_POINTS);
            if (comments == null || comments.isEmpty()) {
                return Collections.emptyList();
            }
            String message = comments.get(0).getFullMessage();
            if (isBlank(message)) {
                return Collections.emptyList();
            }
            return OBJECT_MAPPER.readValue(message, new TypeReference<>() { });
        } catch (Exception e) {
            log.debug("读取审批要点结果失败: taskId={}", taskId);
            return Collections.emptyList();
        }
    }

    void validateRequiredVariables(Map<String, Object> variables, FlowNode flowNode) {
        if (flowNode == null) {
            return;
        }
        String requiredVariables = readStringFlowableAttribute(flowNode, "requiredVariables");
        if (isBlank(requiredVariables)) {
            return;
        }

        List<String> missing = new ArrayList<>();
        for (String variable : requiredVariables.split("[,;，；]")) {
            String key = variable == null ? "" : variable.trim();
            if (key.isEmpty()) {
                continue;
            }
            Object value = variables == null ? null : variables.get(key);
            if (isEmptyVariableValue(value)) {
                missing.add(key);
            }
        }
        if (missing.isEmpty()) {
            return;
        }

        String message = readStringFlowableAttribute(flowNode, "requiredMessage");
        if (isBlank(message)) {
            message = "请补充必填流程表单信息：" + String.join("、", missing);
        }
        throw new RuntimeException(message);
    }

    String resolveAutoApprovalMode(Process resolvedProcess, String processDefinitionId) {
        Process process = resolvedProcess != null ? resolvedProcess : getBpmnProcess(processDefinitionId);
        return readProcessStringAttribute(process, "autoApprovalMode");
    }

    Boolean readBooleanProcessAttribute(String processDefinitionId, String name) {
        return parseBooleanValue(readProcessStringAttribute(getBpmnProcess(processDefinitionId), name));
    }

    FlowNode resolveFlowNode(BpmnModel bpmnModel, String taskDefinitionKey) {
        if (bpmnModel == null) {
            return null;
        }
        Process process = bpmnModel.getMainProcess();
        if (process == null) {
            return null;
        }
        FlowElement element = process.getFlowElement(taskDefinitionKey);
        return element instanceof FlowNode ? (FlowNode) element : null;
    }

    String resolveReturnTarget(Task task, String requestedTargetActivityId) {
        String previous = findPreviousUserTaskActivityId(task);
        if (isBlank(requestedTargetActivityId)) {
            return previous;
        }
        String target = requestedTargetActivityId.trim();
        if (Objects.equals(target, task.getTaskDefinitionKey())) {
            throw new RuntimeException("不能退回当前任务节点");
        }
        FlowModel model = flowModelService.getModelByKey(
                processDefinitionKeyResolver.apply(task.getProcessDefinitionId()));
        if (model == null || !Boolean.TRUE.equals(model.getAllowMultiReturn())) {
            throw new RuntimeException("当前流程未开启多级退回");
        }
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType("userTask")
                .finished()
                .list();
        boolean found = activities.stream().anyMatch(activity -> target.equals(activity.getActivityId()));
        if (!found) {
            throw new RuntimeException("目标节点不是当前流程已完成的用户任务");
        }
        return target;
    }

    private TaskApprovalPolicy resolveApprovalPolicy(Task task, FlowModel flowModel, FlowNode flowNode) {
        TaskApprovalPolicy policy = TaskApprovalPolicy.defaultPolicy();
        FlowNode effectiveFlowNode = flowNode != null ? flowNode : resolveFlowNode(task);
        if (effectiveFlowNode != null) {
            applyBpmnPolicy(policy, effectiveFlowNode);
        }

        FlowModel effectiveFlowModel = flowModel != null
                ? flowModel
                : flowModelService.getModelByKey(processDefinitionKeyResolver.apply(task.getProcessDefinitionId()));
        if (effectiveFlowModel != null) {
            policy.allowMultiReturn = Boolean.TRUE.equals(effectiveFlowModel.getAllowMultiReturn());
            FlowNodeConfig nodeConfig = flowNodeConfigService.getByModelAndNode(
                    effectiveFlowModel.getId(), task.getTaskDefinitionKey());
            if (nodeConfig != null) {
                applyNodeConfigPolicy(policy, nodeConfig);
            }
        }
        return policy;
    }

    private FlowNode resolveFlowNode(Task task) {
        return resolveFlowNode(
                isBlank(task.getProcessDefinitionId()) ? null
                        : repositoryService.getBpmnModel(task.getProcessDefinitionId()),
                task.getTaskDefinitionKey());
    }

    private void validateCommentAndSignature(TaskApprovalPolicy policy, String comment, String signature) {
        if (policy.requireComment && isBlank(comment)) {
            throw new RuntimeException("请输入审批意见");
        }
        if (policy.requireSignature && isBlank(signature)) {
            throw new RuntimeException("请完成审批签名");
        }
    }

    private void applyBpmnPolicy(TaskApprovalPolicy policy, FlowNode flowNode) {
        Boolean allowApprove = readBooleanFlowableAttribute(flowNode, "allowApprove");
        if (allowApprove != null) policy.allowApprove = allowApprove;
        Boolean allowReject = readBooleanFlowableAttribute(flowNode, "allowReject");
        if (allowReject != null) policy.allowReject = allowReject;
        Boolean allowRejectToStart = readBooleanFlowableAttribute(flowNode, "allowRejectToStart");
        if (allowRejectToStart != null) policy.allowRejectToStart = allowRejectToStart;
        Boolean allowDelegate = readBooleanFlowableAttribute(flowNode, "allowDelegate");
        if (allowDelegate != null) policy.allowDelegate = allowDelegate;
        Boolean allowReturn = readBooleanFlowableAttribute(flowNode, "allowReturn");
        if (allowReturn != null) policy.allowReturn = allowReturn;
        Boolean allowTerminate = readBooleanFlowableAttribute(flowNode, "allowTerminate");
        if (allowTerminate != null) policy.allowTerminate = allowTerminate;
        Boolean allowAddSign = readBooleanFlowableAttribute(flowNode, "allowAddSign");
        if (allowAddSign != null) policy.allowAddSign = allowAddSign;
        Boolean requireSignature = readBooleanFlowableAttribute(flowNode, "requireSignature");
        if (requireSignature != null) policy.requireSignature = requireSignature;
        Boolean requireComment = readBooleanFlowableAttribute(flowNode, "requireComment");
        if (requireComment != null) policy.requireComment = requireComment;
    }

    private void applyNodeConfigPolicy(TaskApprovalPolicy policy, FlowNodeConfig nodeConfig) {
        if (nodeConfig.getAllowApprove() != null) policy.allowApprove = nodeConfig.getAllowApprove();
        if (nodeConfig.getAllowReject() != null) policy.allowReject = nodeConfig.getAllowReject();
        if (nodeConfig.getAllowRejectToStart() != null) {
            policy.allowRejectToStart = nodeConfig.getAllowRejectToStart();
        }
        if (nodeConfig.getAllowDelegate() != null) policy.allowDelegate = nodeConfig.getAllowDelegate();
        if (nodeConfig.getAllowReturn() != null) policy.allowReturn = nodeConfig.getAllowReturn();
        // allow_add_sign 列默认 0 且设计器从未写入，无法区分"未配置"和"禁止"，故加签只认 BPMN 属性。
        if (nodeConfig.getAllowTerminate() != null) policy.allowTerminate = nodeConfig.getAllowTerminate();
        if (nodeConfig.getRequireSignature() != null) policy.requireSignature = nodeConfig.getRequireSignature();
        if (nodeConfig.getRequireComment() != null) policy.requireComment = nodeConfig.getRequireComment();
    }

    private Boolean readBooleanFlowableAttribute(FlowNode flowNode, String name) {
        return parseBooleanValue(readStringFlowableAttribute(flowNode, name));
    }

    private String readStringFlowableAttribute(FlowNode flowNode, String name) {
        String value = flowNode.getAttributeValue(FLOWABLE_NS, name);
        if (isBlank(value)) {
            Map<String, List<ExtensionElement>> extensions = flowNode.getExtensionElements();
            List<ExtensionElement> elements = extensions != null ? extensions.get(name) : null;
            if (elements != null && !elements.isEmpty()) {
                value = elements.get(0).getElementText();
            }
        }
        return value;
    }

    private String readProcessStringAttribute(Process process, String name) {
        if (process == null) {
            return null;
        }
        String value = process.getAttributeValue(FLOWABLE_NS, name);
        if (isBlank(value)) {
            Map<String, List<ExtensionElement>> extensions = process.getExtensionElements();
            List<ExtensionElement> elements = extensions != null ? extensions.get(name) : null;
            if (elements != null && !elements.isEmpty()) {
                value = elements.get(0).getElementText();
            }
        }
        if ("autoApprovalMode".equals(name)
                && !AUTO_APPROVAL_FIRST_ONLY.equals(value)
                && !AUTO_APPROVAL_CONSECUTIVE.equals(value)) {
            return AUTO_APPROVAL_NONE;
        }
        return value;
    }

    private Process getBpmnProcess(String processDefinitionId) {
        if (isBlank(processDefinitionId)) {
            return null;
        }
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        return bpmnModel == null ? null : bpmnModel.getMainProcess();
    }

    private Boolean parseBooleanValue(String value) {
        if (isBlank(value)) {
            return null;
        }
        String normalized = value.trim();
        if ("true".equalsIgnoreCase(normalized) || "1".equals(normalized)
                || "Y".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized) || "0".equals(normalized)
                || "N".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
            return false;
        }
        return null;
    }

    private String findPreviousUserTaskActivityId(Task task) {
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType("userTask")
                .finished()
                .orderByHistoricActivityInstanceEndTime()
                .desc()
                .list();
        if (activities == null || activities.isEmpty()) {
            return null;
        }
        for (HistoricActivityInstance activity : activities) {
            if (!Objects.equals(activity.getActivityId(), task.getTaskDefinitionKey())) {
                return activity.getActivityId();
            }
        }
        return null;
    }

    private boolean isEmptyVariableValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        return value instanceof Collection<?> && ((Collection<?>) value).isEmpty();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class TaskApprovalPolicy {
        private boolean allowApprove;
        private boolean allowReject;
        private boolean allowRejectToStart;
        private boolean allowDelegate;
        private boolean allowReturn;
        private boolean allowMultiReturn;
        private boolean allowTerminate;
        private boolean allowAddSign;
        private boolean requireSignature;
        private boolean requireComment;

        private static TaskApprovalPolicy defaultPolicy() {
            TaskApprovalPolicy policy = new TaskApprovalPolicy();
            policy.allowApprove = true;
            policy.allowReject = true;
            policy.allowRejectToStart = false;
            policy.allowDelegate = true;
            policy.allowReturn = false;
            policy.allowTerminate = false;
            policy.allowAddSign = true;
            policy.requireSignature = false;
            policy.requireComment = true;
            return policy;
        }

        private boolean isAllowed(String action) {
            return switch (action) {
                case ACTION_APPROVE -> allowApprove;
                case ACTION_REJECT -> allowReject;
                case ACTION_REJECT_TO_START -> allowRejectToStart;
                case ACTION_DELEGATE -> allowDelegate;
                case ACTION_RETURN -> allowReturn || allowMultiReturn;
                case ACTION_TERMINATE -> allowTerminate;
                case ACTION_ADD_SIGN -> allowAddSign;
                default -> false;
            };
        }
    }
}
