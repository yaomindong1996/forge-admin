package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.dto.FlowApprovalPointResultDTO;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowErrorLog;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.enums.FlowBusinessStatus;
import com.mdframe.forge.starter.flow.enums.FlowTaskStatus;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowErrorLogService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Flowable 任务动作命令协调器。
 *
 * <p>使用 Command Coordinator + Template Method 固化授权、节点策略、Flowable 副作用、
 * 本地镜像回写和错误审计顺序。事务边界仍由 Facade 的公开方法持有。</p>
 */
@Slf4j
final class FlowTaskActionCoordinator {

    private static final String ACTION_APPROVE = "approve";
    private static final String ACTION_REJECT = "reject";
    private static final String ACTION_REJECT_TO_START = "rejectToStart";
    private static final String ACTION_DELEGATE = "delegate";
    private static final String ACTION_TERMINATE = "terminate";
    private static final String AUTO_APPROVAL_FIRST_ONLY = "firstOnly";
    private static final String AUTO_APPROVAL_CONSECUTIVE = "consecutive";
    private static final String RETURN_SOURCE_ACTIVITY_ID = "FLOW_RETURN_SOURCE_ACTIVITY_ID";
    private static final String RETURN_TARGET_ACTIVITY_ID = "FLOW_RETURN_TARGET_ACTIVITY_ID";
    private static final String RETURN_TO_START_PENDING = "FLOW_RETURN_TO_START_PENDING";
    private static final String DIRECT_SEND_VARIABLE = "directSend";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final FlowTaskMapper flowTaskMapper;
    private final FlowBusinessMapper flowBusinessMapper;
    private final FlowErrorLogService flowErrorLogService;
    private final FlowTaskNodePolicy nodePolicy;
    private final FlowTaskFormConfigurationResolver formResolver;
    private final MutationActorGuard mutationActorGuard;
    private final Consumer<String> taskTenantGuard;
    private final Consumer<String> targetUserValidator;
    private final BiPredicate<Task, String> processStarterPredicate;

    FlowTaskActionCoordinator(RuntimeService runtimeService,
                              TaskService taskService,
                              RepositoryService repositoryService,
                              HistoryService historyService,
                              FlowTaskMapper flowTaskMapper,
                              FlowBusinessMapper flowBusinessMapper,
                              FlowErrorLogService flowErrorLogService,
                              FlowTaskNodePolicy nodePolicy,
                              FlowTaskFormConfigurationResolver formResolver,
                              MutationActorGuard mutationActorGuard,
                              Consumer<String> taskTenantGuard,
                              Consumer<String> targetUserValidator,
                              BiPredicate<Task, String> processStarterPredicate) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.repositoryService = repositoryService;
        this.historyService = historyService;
        this.flowTaskMapper = flowTaskMapper;
        this.flowBusinessMapper = flowBusinessMapper;
        this.flowErrorLogService = flowErrorLogService;
        this.nodePolicy = nodePolicy;
        this.formResolver = formResolver;
        this.mutationActorGuard = mutationActorGuard;
        this.taskTenantGuard = taskTenantGuard;
        this.targetUserValidator = targetUserValidator;
        this.processStarterPredicate = processStarterPredicate;
    }

    void approve(String taskId, String userId, String comment, String signature,
                 Map<String, Object> variables, Long tenantId,
                 String idempotencyKey, String requestDigest,
                 List<FlowApprovalPointResultDTO> approvalPointResults) {
        FlowTask storedTask = authorizeTaskAction(
                taskId, userId, tenantId, "APPROVE", idempotencyKey, requestDigest, FlowTaskStatus.APPROVED);
        if (storedTask == null) {
            return;
        }
        Task task = requireRuntimeTask(taskId);
        validateFlowableAssignee(task, userId);
        BpmnModel actionBpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        FlowNode actionFlowNode = nodePolicy.resolveFlowNode(actionBpmnModel, task.getTaskDefinitionKey());
        nodePolicy.validateTaskAction(task, ACTION_APPROVE, comment, signature, actionFlowNode);
        formResolver.validateDynamicFormArrayVariables(task, actionFlowNode, variables);
        nodePolicy.validateRequiredVariables(variables, actionFlowNode);
        nodePolicy.validateApprovalPoints(approvalPointResults, actionFlowNode);

        try {
            if (comment != null && !comment.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), comment);
            }
            nodePolicy.recordApprovalPointResults(task, approvalPointResults);
            Map<String, Object> completeVariables = mergeActionVariables(variables, true);
            completeTask(task, completeVariables);
            directSendAfterReturn(task, completeVariables, userId);

            FlowTask flowTask = completedTask(FlowTaskStatus.APPROVED, comment, signature);
            flowTask.setActionIdempotencyKey(idempotencyKey);
            flowTask.setActionRequestDigest(requestDigest);
            flowTask.setActionType(idempotencyKey == null ? null : "APPROVE");
            updateTaskActionResultRequired(taskId, flowTask);

            log.info("审批通过：taskId={}, userId={}", taskId, userId);
            autoApproveRepeatedTasks(task.getProcessInstanceId(),
                    actionBpmnModel == null ? null : actionBpmnModel.getMainProcess());
        } catch (Exception e) {
            recordTaskError(task, "TASK_APPROVE", e);
            throw e;
        }
    }

    void reject(String taskId, String userId, String comment, String signature,
                Long tenantId, String idempotencyKey, String requestDigest, boolean rejectToStart) {
        FlowTask storedTask = authorizeTaskAction(
                taskId, userId, tenantId, rejectToStart ? "REJECT_TO_START" : "REJECT",
                idempotencyKey, requestDigest, FlowTaskStatus.REJECTED);
        if (storedTask == null) {
            return;
        }
        Task task = requireRuntimeTask(taskId);
        validateFlowableAssignee(task, userId);
        nodePolicy.validateTaskAction(
                task, rejectToStart ? ACTION_REJECT_TO_START : ACTION_REJECT, comment, signature);

        try {
            if (comment != null && !comment.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), comment);
            }
            Map<String, Object> variables = mergeActionVariables(null, false);
            if (rejectToStart) {
                variables.put("rejectToStart", true);
                runtimeService.setVariable(task.getProcessInstanceId(), RETURN_SOURCE_ACTIVITY_ID,
                        task.getTaskDefinitionKey());
                runtimeService.removeVariable(task.getProcessInstanceId(), RETURN_TARGET_ACTIVITY_ID);
                runtimeService.setVariable(task.getProcessInstanceId(), RETURN_TO_START_PENDING, true);
            }
            completeTask(task, variables);

            FlowTask flowTask = completedTask(FlowTaskStatus.REJECTED, comment, signature);
            flowTask.setActionIdempotencyKey(idempotencyKey);
            flowTask.setActionRequestDigest(requestDigest);
            flowTask.setActionType(idempotencyKey == null ? null
                    : (rejectToStart ? "REJECT_TO_START" : "REJECT"));
            updateTaskActionResultRequired(taskId, flowTask);
            log.info("审批驳回：taskId={}, userId={}", taskId, userId);
        } catch (Exception e) {
            recordTaskError(task, rejectToStart ? "TASK_REJECT_TO_START" : "TASK_REJECT", e);
            throw e;
        }
    }

    void delegate(String taskId, String userId, String targetUserId, String comment, String signature,
                  Long tenantId, String idempotencyKey, String requestDigest) {
        if (isBlank(targetUserId)) {
            throw new RuntimeException("新处理人不能为空");
        }
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_TASK_TENANT_REQUIRED");
        }
        mutationActorGuard.verify(taskId, userId, false);
        targetUserValidator.accept(targetUserId.trim());
        FlowTask storedTask = authorizeTaskAction(taskId, userId, tenantId, "DELEGATE",
                idempotencyKey, requestDigest, FlowTaskStatus.CLAIMED);
        if (storedTask == null) {
            return;
        }
        Task task = requireRuntimeTask(taskId);
        nodePolicy.validateTaskAction(task, ACTION_DELEGATE, comment, signature);

        try {
            String owner = task.getAssignee() != null && !task.getAssignee().isEmpty()
                    ? task.getAssignee()
                    : userId;
            if (owner != null && !owner.isEmpty()) {
                taskService.setOwner(taskId, owner);
            }
            taskService.setAssignee(taskId, targetUserId.trim());

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(FlowTaskStatus.CLAIMED.getCode());
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setAssignee(targetUserId.trim());
            flowTask.setOwner(owner);
            flowTask.setActionIdempotencyKey(idempotencyKey);
            flowTask.setActionRequestDigest(requestDigest);
            flowTask.setActionType(idempotencyKey == null ? null : "DELEGATE");
            updateTaskActionResultRequired(taskId, flowTask);
            log.info("转办任务：taskId={}, from={}, to={}", taskId, userId, targetUserId);
        } catch (Exception e) {
            recordTaskError(task, "TASK_DELEGATE", e);
            throw e;
        }
    }

    void returnTask(String taskId, String userId, String comment, String signature,
                    String requestedTargetActivityId) {
        taskTenantGuard.accept(taskId);
        Task task = requireRuntimeTask(taskId);
        validateFlowableAssignee(task, userId);
        nodePolicy.validateReturnAction(task, comment, signature, requestedTargetActivityId);

        try {
            String targetActivityId = nodePolicy.resolveReturnTarget(task, requestedTargetActivityId);
            if (isBlank(targetActivityId)) {
                throw new BusinessException("当前任务没有可退回的上一审批节点");
            }
            if (comment != null && !comment.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), "退回：" + comment);
            }
            runtimeService.setVariable(task.getProcessInstanceId(), RETURN_SOURCE_ACTIVITY_ID,
                    task.getTaskDefinitionKey());
            runtimeService.setVariable(task.getProcessInstanceId(), RETURN_TARGET_ACTIVITY_ID, targetActivityId);
            runtimeService.removeVariable(task.getProcessInstanceId(), RETURN_TO_START_PENDING);

            List<String> currentActivityIds = runtimeService.getActiveActivityIds(task.getProcessInstanceId());
            if (currentActivityIds == null || currentActivityIds.isEmpty()) {
                currentActivityIds = Collections.singletonList(task.getTaskDefinitionKey());
            }
            if (currentActivityIds.size() != 1
                    || !Objects.equals(currentActivityIds.get(0), task.getTaskDefinitionKey())) {
                throw new RuntimeException("当前流程存在多个活动分支，不能安全退回指定节点");
            }
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(task.getProcessInstanceId())
                    .moveActivityIdTo(task.getTaskDefinitionKey(), targetActivityId)
                    .changeState();

            updateTaskByTenant(taskId, completedTask(FlowTaskStatus.RETURNED, comment, signature));
            log.info("退回任务：taskId={}, userId={}, targetActivityId={}",
                    taskId, userId, targetActivityId);
        } catch (Exception e) {
            recordTaskError(task, "TASK_RETURN", e);
            throw e;
        }
    }

    void reassignByInitiator(String taskId, String userId, String targetUserId, String reason) {
        if (isBlank(targetUserId)) {
            throw new RuntimeException("任务不存在或新处理人不能为空");
        }
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new RuntimeException("FLOW_TASK_TENANT_REQUIRED");
        }
        FlowTask localTask = flowTaskMapper.selectByTaskIdForUpdateAndTenant(taskId, tenantId);
        if (localTask == null || !tenantId.equals(localTask.getTenantId())) {
            throw new RuntimeException("FLOW_TASK_TENANT_MISMATCH");
        }
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null || !Objects.equals(localTask.getProcessInstanceId(), task.getProcessInstanceId())) {
            throw new RuntimeException("FLOW_TASK_NOT_FOUND");
        }
        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceIdAndTenantIdForUpdate(
                task.getProcessInstanceId(), tenantId);
        if (business == null || !Objects.equals(business.getProcessInstanceId(), task.getProcessInstanceId())) {
            throw new RuntimeException("FLOW_TASK_TENANT_MISMATCH");
        }
        boolean allowed = Objects.equals(userId, task.getAssignee())
                || Objects.equals(userId, task.getOwner())
                || Objects.equals(userId, business.getApplyUserId());
        if (!allowed) {
            throw new RuntimeException("仅当前处理人、任务拥有人或流程发起人可以改派");
        }
        targetUserValidator.accept(targetUserId.trim());
        String owner = !isBlank(task.getAssignee()) ? task.getAssignee() : userId;
        taskService.setOwner(taskId, owner);
        taskService.setAssignee(taskId, targetUserId.trim());
        if (!isBlank(reason)) {
            taskService.addComment(taskId, task.getProcessInstanceId(), "改派", reason.trim());
        }
        FlowTask flowTask = new FlowTask();
        flowTask.setAssignee(targetUserId.trim());
        flowTask.setOwner(owner);
        flowTask.setStatus(FlowTaskStatus.CLAIMED.getCode());
        flowTask.setComment(reason);
        if (!updateTaskByTenant(taskId, flowTask)) {
            throw new IllegalStateException("改派任务状态同步失败");
        }
        log.info("流程任务改派：taskId={}, from={}, to={}", taskId, userId, targetUserId);
    }

    void terminateTask(String taskId, String userId, String comment, String signature) {
        mutationActorGuard.verify(taskId, userId, true);
        Long tenantId = requireTenantId();
        Task task = requireRuntimeTask(taskId);
        nodePolicy.validateTaskAction(task, ACTION_TERMINATE, comment, signature);

        try {
            List<String> activeTaskIds = taskService.createTaskQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .list()
                    .stream()
                    .map(Task::getId)
                    .filter(Objects::nonNull)
                    .toList();
            String reason = !isBlank(comment) ? comment : "审批人终结流程";
            taskService.addComment(taskId, task.getProcessInstanceId(), "终结流程：" + reason);
            runtimeService.deleteProcessInstance(task.getProcessInstanceId(), reason);
            if (!activeTaskIds.isEmpty()) {
                flowTaskMapper.updateProcessTaskStatusByTaskIds(activeTaskIds, tenantId,
                        FlowTaskStatus.TERMINATED.getCode(), LocalDateTime.now());
            }

            FlowBusiness business = flowBusinessMapper.selectByProcessInstanceIdAndTenantIdForUpdate(
                    task.getProcessInstanceId(), tenantId);
            if (business != null) {
                business.setStatus(FlowBusinessStatus.TERMINATED.getCode());
                business.setEndTime(LocalDateTime.now());
                business.setUpdateTime(LocalDateTime.now());
                flowBusinessMapper.updateById(business);
            }
            updateTaskByTenant(taskId, completedTask(FlowTaskStatus.TERMINATED, comment, signature));
            log.info("审批人终结流程：taskId={}, processInstanceId={}, userId={}",
                    taskId, task.getProcessInstanceId(), userId);
        } catch (Exception e) {
            recordTaskError(task, "TASK_TERMINATE", e);
            throw e;
        }
    }

    private FlowTask authorizeTaskAction(String taskId, String userId, Long tenantId,
                                         String actionType, String idempotencyKey,
                                         String requestDigest, FlowTaskStatus completedStatus) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_TASK_TENANT_REQUIRED");
        }
        FlowTask storedTask = flowTaskMapper.selectByTaskIdForUpdateAndTenant(taskId, tenantId);
        if (FlowTaskActionAuthorization.authorize(
                storedTask, userId, tenantId, actionType,
                idempotencyKey, requestDigest, completedStatus)) {
            return null;
        }
        return storedTask;
    }

    private Task requireRuntimeTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        return task;
    }

    private void validateFlowableAssignee(Task task, String userId) {
        if (!userId.equals(task.getAssignee())) {
            throw new BusinessException("当前用户不是任务办理人");
        }
    }

    private FlowTask completedTask(FlowTaskStatus status, String comment, String signature) {
        FlowTask flowTask = new FlowTask();
        flowTask.setStatus(status.getCode());
        flowTask.setComment(comment);
        flowTask.setSignature(signature);
        flowTask.setCompleteTime(LocalDateTime.now());
        return flowTask;
    }

    private void updateTaskActionResultRequired(String taskId, FlowTask flowTask) {
        if (!updateTaskByTenant(taskId, flowTask)) {
            throw new IllegalStateException("FLOW_TASK_STATE_UPDATE_FAILED");
        }
    }

    private boolean updateTaskByTenant(String taskId, FlowTask task) {
        Long tenantId = requireTenantId();
        return flowTaskMapper.updateByTaskIdAndTenant(taskId, tenantId, task) > 0;
    }

    private Map<String, Object> mergeActionVariables(Map<String, Object> variables, boolean approved) {
        Map<String, Object> completeVariables = variables != null ? new HashMap<>(variables) : new HashMap<>();
        completeVariables.put("approved", approved);
        completeVariables.put("approvalResult", approved ? "approve" : "reject");
        completeVariables.put("rejectToStart", false);
        return completeVariables;
    }

    private void directSendAfterReturn(Task completedTask, Map<String, Object> actionVariables, String userId) {
        String processInstanceId = completedTask.getProcessInstanceId();
        if (!isProcessRunning(processInstanceId)) {
            return;
        }
        Map<String, Object> returnVariables = runtimeService.getVariables(processInstanceId);
        Object source = returnVariables == null ? null : returnVariables.get(RETURN_SOURCE_ACTIVITY_ID);
        Object target = returnVariables == null ? null : returnVariables.get(RETURN_TARGET_ACTIVITY_ID);
        Object returnToStartPending = returnVariables == null ? null : returnVariables.get(RETURN_TO_START_PENDING);
        boolean returnedToHistoricalNode = target != null
                && Objects.equals(String.valueOf(target), completedTask.getTaskDefinitionKey());
        if (source == null || (!returnedToHistoricalNode && !Boolean.TRUE.equals(readBoolean(returnToStartPending)))) {
            return;
        }
        boolean directSend = Boolean.TRUE.equals(readBoolean(
                actionVariables == null ? null : actionVariables.get(DIRECT_SEND_VARIABLE)));
        if (!directSend) {
            clearDirectSendMarks(processInstanceId);
            return;
        }
        if (Boolean.TRUE.equals(readBoolean(returnToStartPending))
                && !processStarterPredicate.test(completedTask, userId)) {
            throw new RuntimeException("仅流程发起人可以执行驳回后的直送");
        }
        String sourceActivityId = String.valueOf(source);
        List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
        if (activeActivityIds == null || activeActivityIds.isEmpty()) {
            return;
        }
        if (activeActivityIds.contains(sourceActivityId)) {
            clearDirectSendMarks(processInstanceId);
            return;
        }
        if (activeActivityIds.size() != 1) {
            throw new RuntimeException("当前流程存在多个活动分支，不能安全直送");
        }
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo(activeActivityIds.get(0), sourceActivityId)
                .changeState();
        clearDirectSendMarks(processInstanceId);
    }

    private void clearDirectSendMarks(String processInstanceId) {
        runtimeService.removeVariables(processInstanceId, List.of(
                RETURN_SOURCE_ACTIVITY_ID, RETURN_TARGET_ACTIVITY_ID, RETURN_TO_START_PENDING));
    }

    private Boolean readBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if ("true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text) || "0".equals(text) || "no".equalsIgnoreCase(text)) {
            return false;
        }
        return null;
    }

    private void completeTask(Task task, Map<String, Object> variables) {
        String taskId = task.getId();
        if (DelegationState.PENDING.equals(task.getDelegationState())) {
            log.info("任务处于委派待解决状态，先 resolve 再 complete：taskId={}, assignee={}, owner={}",
                    taskId, task.getAssignee(), task.getOwner());
            taskService.resolveTask(taskId);
        }
        try {
            if (variables != null && !variables.isEmpty()) {
                runtimeService.setVariables(task.getProcessInstanceId(), variables);
                taskService.complete(taskId, variables);
            } else {
                taskService.complete(taskId);
            }
        } catch (FlowableObjectNotFoundException e) {
            throw new RuntimeException("任务已处理或流程已结束，请刷新后重试", e);
        }
    }

    private boolean isProcessRunning(String processInstanceId) {
        if (isBlank(processInstanceId)) {
            return false;
        }
        try {
            return runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult() != null;
        } catch (Exception e) {
            log.debug("判断流程是否仍在运行失败: processInstanceId={}", processInstanceId);
            return false;
        }
    }

    private void autoApproveRepeatedTasks(String processInstanceId, Process resolvedProcess) {
        ProcessInstance instance;
        try {
            instance = isBlank(processInstanceId)
                    ? null
                    : runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
        } catch (Exception e) {
            return;
        }
        if (instance == null) {
            return;
        }
        String mode = nodePolicy.resolveAutoApprovalMode(resolvedProcess, instance.getProcessDefinitionId());
        if (!AUTO_APPROVAL_FIRST_ONLY.equals(mode) && !AUTO_APPROVAL_CONSECUTIVE.equals(mode)) {
            return;
        }

        Set<String> completedAutomatically = new HashSet<>();
        int guard = 0;
        while (guard++ < 30) {
            List<Task> activeTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .list();
            Task matchedTask = activeTasks.stream()
                    .filter(task -> !completedAutomatically.contains(task.getId()))
                    .filter(task -> shouldAutoApproveTask(task, mode))
                    .findFirst()
                    .orElse(null);
            if (matchedTask == null) {
                return;
            }
            autoApproveTask(matchedTask, mode);
            completedAutomatically.add(matchedTask.getId());
        }
        log.warn("重复审批自动同意达到保护上限：processInstanceId={}, mode={}", processInstanceId, mode);
    }

    private boolean shouldAutoApproveTask(Task task, String mode) {
        if (task == null || isBlank(task.getAssignee())) {
            return false;
        }
        String assignee = task.getAssignee();
        if (AUTO_APPROVAL_FIRST_ONLY.equals(mode)) {
            return historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .taskAssignee(assignee)
                    .finished()
                    .count() > 0;
        }
        HistoricTaskInstance previousTask = findLastFinishedTask(task.getProcessInstanceId());
        return previousTask != null && Objects.equals(previousTask.getAssignee(), assignee);
    }

    private HistoricTaskInstance findLastFinishedTask(String processInstanceId) {
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .listPage(0, 1);
        return tasks == null || tasks.isEmpty() ? null : tasks.get(0);
    }

    private void autoApproveTask(Task task, String mode) {
        String comment = "系统自动同意（重复审批人）";
        taskService.addComment(task.getId(), task.getProcessInstanceId(), comment);
        completeTask(task, mergeActionVariables(null, true));
        FlowTask flowTask = completedTask(FlowTaskStatus.APPROVED, comment, null);
        updateTaskByTenant(task.getId(), flowTask);
        log.info("重复审批自动同意：taskId={}, processInstanceId={}, assignee={}, mode={}",
                task.getId(), task.getProcessInstanceId(), task.getAssignee(), mode);
    }

    private void recordTaskError(Task task, String errorStage, Throwable error) {
        FlowErrorLog errorLog = new FlowErrorLog();
        errorLog.setProcessInstanceId(task.getProcessInstanceId());
        errorLog.setTaskId(task.getId());
        errorLog.setActivityId(task.getTaskDefinitionKey());
        errorLog.setActivityName(task.getName());
        errorLog.setErrorStage(errorStage);
        flowErrorLogService.recordError(errorLog, error);
    }

    private Long requireTenantId() {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_TASK_TENANT_REQUIRED");
        }
        return tenantId;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @FunctionalInterface
    interface MutationActorGuard {
        void verify(String taskId, String userId, boolean allowInitiator);
    }
}
