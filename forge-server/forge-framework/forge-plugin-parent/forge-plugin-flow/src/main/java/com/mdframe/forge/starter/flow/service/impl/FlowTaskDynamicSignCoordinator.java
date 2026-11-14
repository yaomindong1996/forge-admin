package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.entity.FlowTaskCandidate;
import com.mdframe.forge.starter.flow.enums.FlowTaskCandidateStatus;
import com.mdframe.forge.starter.flow.enums.FlowTaskSignMode;
import com.mdframe.forge.starter.flow.mapper.FlowTaskCandidateMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.security.FlowAccessGuard;
import com.mdframe.forge.starter.flow.vo.FlowTaskSignRelationVO;
import com.mdframe.forge.starter.orm.support.DuplicateKeyActions;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.api.Task;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 动态加签/减签命令协调器。
 *
 * <p>使用 Command + Strategy 区分普通任务候选人策略和 Flowable 多实例执行策略，
 * 并统一租户、审计、幂等和关系镜像。</p>
 */
@Slf4j
final class FlowTaskDynamicSignCoordinator {

    private static final int MAX_DYNAMIC_SIGNERS = 50;

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final FlowTaskMapper flowTaskMapper;
    private final FlowTaskCandidateMapper flowTaskCandidateMapper;
    private final FlowAccessGuard flowAccessGuard;
    private final BiConsumer<String, String> mutationActorGuard;
    private final Consumer<String> targetUserValidator;

    FlowTaskDynamicSignCoordinator(RuntimeService runtimeService,
                                   TaskService taskService,
                                   RepositoryService repositoryService,
                                   FlowTaskMapper flowTaskMapper,
                                   FlowTaskCandidateMapper flowTaskCandidateMapper,
                                   FlowAccessGuard flowAccessGuard,
                                   BiConsumer<String, String> mutationActorGuard,
                                   Consumer<String> targetUserValidator) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.repositoryService = repositoryService;
        this.flowTaskMapper = flowTaskMapper;
        this.flowTaskCandidateMapper = flowTaskCandidateMapper;
        this.flowAccessGuard = flowAccessGuard;
        this.mutationActorGuard = mutationActorGuard;
        this.targetUserValidator = targetUserValidator;
    }

    void mutate(String taskId, String userId, String targetUserId,
                String reason, String signMode, Long tenantId,
                String idempotencyKey, String requestDigest, boolean add) {
        if (isBlank(targetUserId)) {
            throw new RuntimeException("目标用户不能为空");
        }
        String normalizedSignMode = FlowTaskSignMode.fromCode(signMode).getCode();
        if (!FlowTaskSignMode.PARALLEL.getCode().equals(normalizedSignMode)) {
            throw new IllegalStateException("FLOW_TASK_SIGN_MODE_UNSUPPORTED");
        }
        mutationActorGuard.accept(taskId, userId);
        targetUserValidator.accept(targetUserId.trim());
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_TASK_TENANT_REQUIRED");
        }
        if ((idempotencyKey == null) != (requestDigest == null)) {
            throw new IllegalStateException("FLOW_TASK_IDEMPOTENCY_INVALID");
        }
        if (idempotencyKey != null && flowTaskCandidateMapper == null) {
            throw new IllegalStateException("FLOW_TASK_IDEMPOTENCY_UNAVAILABLE");
        }
        FlowTask localTask = flowTaskMapper.selectByTaskIdAndTenant(taskId, tenantId);
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null || localTask == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        localTask = flowTaskMapper.selectByTaskIdForUpdateAndTenant(taskId, tenantId);
        if (localTask == null || !tenantId.equals(localTask.getTenantId())) {
            throw new RuntimeException("任务不存在或已处理");
        }
        if (Objects.equals(String.valueOf(userId), targetUserId.trim())
                || Objects.equals(String.valueOf(localTask.getAssignee()), targetUserId.trim())) {
            throw new RuntimeException("不能将当前任务办理人再次加入加签名单");
        }
        if (flowTaskCandidateMapper != null && add
                && flowTaskCandidateMapper.countActiveByTaskAndValue(
                tenantId, taskId, FlowTaskCandidate.TYPE_USER, targetUserId.trim()) > 0) {
            throw new RuntimeException("目标用户已经在加签名单中");
        }

        if (idempotencyKey != null) {
            FlowTaskCandidate previous = flowTaskCandidateMapper.selectByIdempotency(
                    tenantId, taskId, FlowTaskCandidate.TYPE_USER, idempotencyKey);
            if (previous != null) {
                if (!Objects.equals(requestDigest, previous.getRequestDigest())
                        || !Objects.equals(targetUserId.trim(), previous.getCandidateValue())
                        || !Objects.equals(add, FlowTaskCandidateStatus.ACTIVE.matches(previous.getStatus()))) {
                    throw new IllegalStateException("FLOW_TASK_IDEMPOTENCY_CONFLICT");
                }
                return;
            }
        }

        if (isMultiInstanceTask(task)) {
            mutateFlowableMultiInstanceSign(task, localTask, userId, targetUserId.trim(), reason,
                    normalizedSignMode, idempotencyKey, requestDigest, add);
            return;
        }
        mutateCandidateSet(task, localTask, userId, targetUserId.trim(), reason,
                normalizedSignMode, idempotencyKey, requestDigest, add);
    }

    List<FlowTaskSignRelationVO> getSignRelations(String taskId, String userId) {
        if (isBlank(taskId) || isBlank(userId)) {
            throw new IllegalArgumentException("FLOW_TASK_SIGN_RELATION_REQUIRED");
        }
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_TASK_TENANT_REQUIRED");
        }
        FlowTask task = flowAccessGuard.requireTaskVisible(taskId);
        if (!tenantId.equals(task.getTenantId())) {
            throw new RuntimeException("FLOW_RESOURCE_NOT_FOUND");
        }
        if (flowTaskCandidateMapper == null) {
            return List.of();
        }
        return flowTaskCandidateMapper.selectDynamicSignRelations(tenantId, task.getTaskId());
    }

    private void mutateCandidateSet(Task task, FlowTask localTask, String userId, String targetUserId,
                                    String reason, String signMode, String idempotencyKey,
                                    String requestDigest, boolean add) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>(splitIds(localTask.getCandidateUsers()));
        boolean changed;
        if (add) {
            if (candidates.size() >= MAX_DYNAMIC_SIGNERS) {
                throw new RuntimeException("单个任务最多允许加签 " + MAX_DYNAMIC_SIGNERS + " 人");
            }
            changed = candidates.add(targetUserId);
            if (changed) {
                taskService.addCandidateUser(task.getId(), targetUserId);
                syncCandidateRelation(localTask, targetUserId, userId, reason, signMode,
                        idempotencyKey, requestDigest, true, null, null);
            }
        } else {
            changed = candidates.remove(targetUserId);
            if (changed) {
                taskService.deleteCandidateUser(task.getId(), targetUserId);
                syncCandidateRelation(localTask, targetUserId, userId, reason, signMode,
                        idempotencyKey, requestDigest, false, null, null);
            }
        }
        if (!changed) {
            throw new RuntimeException(add ? "目标用户已经在加签名单中" : "目标用户不在加签名单中");
        }

        FlowTask update = new FlowTask();
        update.setCandidateUsers(String.join(",", candidates));
        update.setComment(reason);
        if (!updateTaskByTenant(task.getId(), update)) {
            throw new IllegalStateException("加签状态同步失败");
        }
        String action = add ? "加签" : "减签";
        taskService.addComment(task.getId(), task.getProcessInstanceId(), action,
                isBlank(reason) ? action : reason.trim());
        log.info("流程任务{}：taskId={}, actor={}, target={}", action, task.getId(), userId, targetUserId);
    }

    private void mutateFlowableMultiInstanceSign(Task task, FlowTask parentTask, String operatorId,
                                                  String targetUserId, String reason, String signMode,
                                                  String idempotencyKey, String requestDigest, boolean add) {
        UserTask userTask = resolveMultiInstanceUserTask(task);
        if (flowTaskCandidateMapper == null || userTask == null || userTask.getLoopCharacteristics() == null) {
            throw new IllegalStateException("FLOW_TASK_SIGN_MULTI_INSTANCE_UNAVAILABLE");
        }
        MultiInstanceLoopCharacteristics loop = userTask.getLoopCharacteristics();
        if (add) {
            String elementVariable = isBlank(loop.getElementVariable()) ? "assignee" : loop.getElementVariable();
            Map<String, Object> variables = new HashMap<>();
            variables.put(elementVariable, targetUserId);
            Execution execution = runtimeService.addMultiInstanceExecution(
                    userTask.getId(), task.getProcessInstanceId(), variables);
            if (execution == null || isBlank(execution.getId())) {
                throw new IllegalStateException("FLOW_TASK_SIGN_CHILD_EXECUTION_CREATE_FAILED");
            }
            Task childTask = taskService.createTaskQuery().executionId(execution.getId()).singleResult();
            if (childTask == null && !loop.isSequential()) {
                throw new IllegalStateException("FLOW_TASK_SIGN_CHILD_TASK_CREATE_FAILED");
            }
            if (childTask != null && !Objects.equals(targetUserId, childTask.getAssignee())) {
                taskService.setAssignee(childTask.getId(), targetUserId);
            }
            syncCandidateRelation(parentTask, targetUserId, operatorId, reason, signMode,
                    idempotencyKey, requestDigest, true,
                    childTask == null ? null : childTask.getId(), execution.getId());
            taskService.addComment(task.getId(), task.getProcessInstanceId(), "加签",
                    isBlank(reason) ? "加签" : reason.trim());
            return;
        }

        FlowTaskCandidate relation = flowTaskCandidateMapper.selectActiveDynamicSignRelation(
                parentTask.getTenantId(), parentTask.getTaskId(), targetUserId);
        if (relation == null || isBlank(relation.getChildExecutionId())) {
            throw new RuntimeException("目标用户不存在可撤销的多实例加签");
        }
        runtimeService.deleteMultiInstanceExecution(relation.getChildExecutionId(), false);
        syncCandidateRelation(parentTask, targetUserId, operatorId, reason, signMode,
                idempotencyKey, requestDigest, false,
                relation.getChildTaskId(), relation.getChildExecutionId());
        taskService.addComment(task.getId(), task.getProcessInstanceId(), "减签",
                isBlank(reason) ? "减签" : reason.trim());
    }

    private boolean isMultiInstanceTask(Task task) {
        return resolveMultiInstanceUserTask(task) != null;
    }

    private UserTask resolveMultiInstanceUserTask(Task task) {
        if (task == null || isBlank(task.getProcessDefinitionId())
                || isBlank(task.getTaskDefinitionKey()) || repositoryService == null) {
            return null;
        }
        try {
            BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
            if (model == null) {
                return null;
            }
            FlowElement element = model.getFlowElement(task.getTaskDefinitionKey());
            if (element instanceof UserTask userTask && userTask.hasMultiInstanceLoopCharacteristics()) {
                return userTask;
            }
        } catch (Exception e) {
            log.warn("解析多实例任务配置失败: taskId={}, error={}", task.getId(), e.getMessage());
        }
        return null;
    }

    private void syncCandidateRelation(FlowTask task, String candidateUserId, String operatorId,
                                       String reason, String signMode, String idempotencyKey,
                                       String requestDigest, boolean active,
                                       String childTaskId, String childExecutionId) {
        if (flowTaskCandidateMapper == null || task == null || task.getTenantId() == null
                || task.getTaskId() == null || isBlank(candidateUserId)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (active) {
            FlowTaskCandidate relation = new FlowTaskCandidate();
            relation.setTenantId(task.getTenantId());
            relation.setTaskId(task.getTaskId());
            relation.setParentTaskId(task.getTaskId());
            relation.setChildTaskId(childTaskId);
            relation.setChildExecutionId(childExecutionId);
            relation.setProcessInstanceId(task.getProcessInstanceId());
            relation.setCandidateType(FlowTaskCandidate.TYPE_USER);
            relation.setCandidateValue(candidateUserId);
            relation.setSource(FlowTaskCandidate.SOURCE_DYNAMIC_SIGN);
            relation.setSignMode(signMode);
            relation.setOperatorId(operatorId);
            relation.setReason(reason);
            relation.setIdempotencyKey(idempotencyKey);
            relation.setRequestDigest(requestDigest);
            relation.setStatus(FlowTaskCandidateStatus.ACTIVE.getCode());
            relation.setCreateTime(now);
            relation.setUpdateTime(now);
            DuplicateKeyActions.upsert(
                    () -> flowTaskCandidateMapper.updateUpsertByUniqueKey(relation),
                    () -> flowTaskCandidateMapper.insertPlain(relation));
        } else {
            flowTaskCandidateMapper.deactivateWithAudit(task.getTenantId(), task.getTaskId(),
                    FlowTaskCandidate.TYPE_USER, candidateUserId, operatorId, reason,
                    idempotencyKey, requestDigest, now);
        }
    }

    private boolean updateTaskByTenant(String taskId, FlowTask task) {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_TASK_TENANT_REQUIRED");
        }
        return flowTaskMapper.updateByTaskIdAndTenant(taskId, tenantId, task) > 0;
    }

    private List<String> splitIds(String value) {
        if (isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
