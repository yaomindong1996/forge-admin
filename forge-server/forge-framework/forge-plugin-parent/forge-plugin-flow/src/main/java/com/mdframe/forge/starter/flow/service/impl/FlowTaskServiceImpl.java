package com.mdframe.forge.starter.flow.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayAdapter;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayItem;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.starter.flow.dto.FlowApprovalPointDTO;
import com.mdframe.forge.starter.flow.dto.FlowApprovalPointResultDTO;
import com.mdframe.forge.starter.flow.dto.ProcessDiagramInfo;
import com.mdframe.forge.starter.flow.dto.ProcessNodeInfo;
import com.mdframe.forge.starter.flow.dto.ProcessSequenceFlowInfo;
import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.helper.FlowNodePolicyParser;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowErrorLog;
import com.mdframe.forge.starter.flow.entity.FlowForm;
import com.mdframe.forge.starter.flow.entity.FlowFormInstance;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.enums.FlowBusinessStatus;
import com.mdframe.forge.starter.flow.enums.FlowDiagramStatus;
import com.mdframe.forge.starter.flow.enums.FlowTaskStatus;
import com.mdframe.forge.starter.flow.enums.FlowTaskSignMode;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowFormInstanceMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskCandidateMapper;
import com.mdframe.forge.starter.flow.entity.FlowTaskCandidate;
import com.mdframe.forge.starter.flow.enums.FlowTaskCandidateStatus;
import com.mdframe.forge.starter.flow.service.FlowErrorLogService;
import com.mdframe.forge.starter.flow.service.FlowFormService;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import com.mdframe.forge.starter.flow.service.FlowNodeConfigService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import com.mdframe.forge.starter.flow.service.FlowTaskService;
import com.mdframe.forge.starter.flow.security.FlowAccessGuard;
import com.mdframe.forge.starter.flow.vo.FlowHistoryItemVO;
import com.mdframe.forge.starter.flow.vo.FlowHistoryPageVO;
import com.mdframe.forge.starter.flow.vo.FlowTaskSignRelationVO;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.task.Comment;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程任务服务实现
 */
@Slf4j
@Service
public class FlowTaskServiceImpl extends ServiceImpl<FlowTaskMapper, FlowTask> implements FlowTaskService {

    private static final int MAX_DYNAMIC_SIGNERS = 50;

    private static final String FLOWABLE_NS = "http://flowable.org/bpmn";
    private static final String ACTION_APPROVE = "approve";
    private static final String ACTION_REJECT = "reject";
    private static final String ACTION_REJECT_TO_START = "rejectToStart";
    private static final String ACTION_DELEGATE = "delegate";
    private static final String ACTION_RETURN = "return";
    private static final String ACTION_TERMINATE = "terminate";
    private static final String AUTO_APPROVAL_FIRST_ONLY = "firstOnly";
    private static final String AUTO_APPROVAL_CONSECUTIVE = "consecutive";
    private static final String AUTO_APPROVAL_NONE = "none";
    private static final String FORM_TYPE_BUSINESS = "business";
    private static final String RETURN_SOURCE_ACTIVITY_ID = "FLOW_RETURN_SOURCE_ACTIVITY_ID";
    private static final int MAX_DETAIL_HISTORY_ITEMS = 1000;
    private static final String RETURN_TARGET_ACTIVITY_ID = "FLOW_RETURN_TARGET_ACTIVITY_ID";
    private static final String RETURN_TO_START_PENDING = "FLOW_RETURN_TO_START_PENDING";
    private static final String DIRECT_SEND_VARIABLE = "directSend";
    private static final String COMMENT_TYPE_APPROVAL_POINTS = "approvalPoints";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    /**
     * 消息服务（可选注入）
     */
    @Autowired(required = false)
    private MessageService messageService;
    
    /**
     * 组织架构集成服务（可选注入）
     */
    @Autowired(required = false)
    private FlowOrgIntegrationService flowOrgIntegrationService;

    /**
     * 流程模型服务
     */
    @Autowired
    private FlowModelService flowModelService;

    /**
     * 流程节点配置服务
     */
    @Autowired
    private FlowNodeConfigService flowNodeConfigService;

    /**
     * 流程业务Mapper
     */
    @Autowired
    private FlowBusinessMapper flowBusinessMapper;

    @Autowired
    private FlowAccessGuard flowAccessGuard;
    
    @Autowired
    private FlowErrorLogService flowErrorLogService;

    @Autowired(required = false)
    private FlowFormService flowFormService;

    @Autowired(required = false)
    private FlowFormInstanceMapper flowFormInstanceMapper;

    @Autowired(required = false)
    private FlowBusinessListDisplayAdapter flowBusinessListDisplayAdapter;

    @Autowired(required = false)
    private FlowTaskCandidateMapper flowTaskCandidateMapper;

    @Override
    public IPage<FlowTask> todoTasks(Page<FlowTask> page, String userId, String title, String category, Integer status) {
        return enrichTaskPage(this.getBaseMapper().selectTodoTasks(page, userId, title, category, status,
                SessionHelper.getTenantId(), SessionHelper.getActiveOrgId()));
    }

    @Override
    public IPage<FlowTask> doneTasks(Page<FlowTask> page, String userId, String title, String category, Integer status) {
        return enrichTaskPage(this.getBaseMapper().selectDoneTasks(page, userId, title, category, status,
                SessionHelper.getTenantId(), SessionHelper.getActiveOrgId()));
    }

    @Override
    public IPage<FlowTask> startedTasks(Page<FlowTask> page, String userId, String title, String category, Integer status) {
        return enrichTaskPage(this.getBaseMapper().selectStartedTasks(page, userId, title, category, status,
                SessionHelper.getTenantId()));
    }

    @Override
    public IPage<FlowTask> candidateTasks(Page<FlowTask> page, String userId, String groupId, String title) {
        if ((userId == null || userId.isEmpty()) && (groupId == null || groupId.isEmpty())) {
            return page;
        }
        return enrichTaskPage(this.getBaseMapper().selectCandidateTasks(
                page, userId, groupId, title, SessionHelper.getTenantId()));
    }

    private IPage<FlowTask> enrichTaskPage(IPage<FlowTask> page) {
        if (flowBusinessListDisplayAdapter == null || page == null || page.getRecords() == null
                || page.getRecords().isEmpty()) {
            return page;
        }
        List<FlowBusinessListDisplayItem> items = page.getRecords().stream()
                .map(this::toDisplayItem)
                .collect(Collectors.toList());
        try {
            flowBusinessListDisplayAdapter.enrich(items);
            for (int i = 0; i < page.getRecords().size(); i++) {
                applyDisplayItem(page.getRecords().get(i), items.get(i));
            }
        } catch (Exception e) {
            log.warn("补齐流程任务业务摘要失败，继续返回流程基础信息: {}", e.getMessage());
        }
        return page;
    }

    private FlowBusinessListDisplayItem toDisplayItem(FlowTask task) {
        FlowBusinessListDisplayItem item = new FlowBusinessListDisplayItem();
        item.setBusinessKey(task.getBusinessKey());
        item.setProcessInstanceId(task.getProcessInstanceId());
        item.setProcessDefKey(task.getProcessDefKey());
        item.setProcessName(task.getProcessName());
        item.setProcessDefinitionName(task.getProcessDefinitionName());
        item.setTaskId(task.getTaskId());
        item.setTaskName(task.getTaskName());
        item.setTitle(task.getTitle());
        item.setObjectCode(task.getObjectCode());
        item.setRecordId(task.getRecordId());
        item.setBusinessObjectName(task.getBusinessObjectName());
        item.setBusinessSummary(task.getBusinessSummary());
        item.setBusinessType(task.getBusinessType());
        item.setBusinessParams(task.getBusinessParams());
        item.setDisplayExtensions(task.getDisplayExtensions());
        return item;
    }

    private void applyDisplayItem(FlowTask task, FlowBusinessListDisplayItem item) {
        if (item == null) {
            return;
        }
        task.setObjectCode(firstNonBlank(item.getObjectCode(), task.getObjectCode()));
        task.setRecordId(item.getRecordId() != null ? item.getRecordId() : task.getRecordId());
        task.setBusinessObjectName(firstNonBlank(item.getBusinessObjectName(), task.getBusinessObjectName()));
        task.setBusinessSummary(firstNonBlank(item.getBusinessSummary(), task.getBusinessSummary()));
        task.setBusinessType(firstNonBlank(item.getBusinessType(), task.getBusinessType()));
        task.setBusinessParams(item.getBusinessParams() != null ? item.getBusinessParams() : task.getBusinessParams());
        task.setDisplayExtensions(item.getDisplayExtensions() != null ? item.getDisplayExtensions() : task.getDisplayExtensions());
        task.setProcessName(firstNonBlank(task.getProcessName(), item.getProcessName()));
        task.setProcessDefinitionName(firstNonBlank(
                task.getProcessDefinitionName(),
                item.getProcessDefinitionName(),
                task.getProcessName(),
                task.getProcessDefKey()));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimTask(String taskId, String userId) {
        if (isBlank(taskId) || isBlank(userId)) {
            throw new IllegalArgumentException("FLOW_TASK_CLAIM_CONTEXT_INVALID");
        }
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_TASK_TENANT_REQUIRED");
        }
        FlowTask localTask = baseMapper.selectByTaskIdForUpdateAndTenant(taskId, tenantId);
        Task runtimeTask = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (localTask == null || runtimeTask == null || !tenantId.equals(localTask.getTenantId())
                || !FlowTaskStatus.PENDING.matches(localTask.getStatus())
                || !isClaimCandidate(localTask, runtimeTask, userId.trim())) {
            throw new IllegalStateException("FLOW_TASK_CLAIM_NOT_ALLOWED");
        }
        taskService.claim(taskId, userId);
        
        FlowTask task = new FlowTask();
        task.setTaskId(taskId);
        task.setAssignee(userId);
        task.setStatus(FlowTaskStatus.CLAIMED.getCode());
        task.setClaimTime(LocalDateTime.now());
        
        updateTaskByTenant(taskId, task);
        log.info("签收任务：taskId={}, userId={}", taskId, userId);
    }

    private boolean isClaimCandidate(FlowTask localTask, Task runtimeTask, String userId) {
        if (containsCsv(localTask.getCandidateUsers(), userId)) {
            return true;
        }
        if (taskService.createTaskQuery().taskId(runtimeTask.getId()).taskCandidateUser(userId).singleResult() != null) {
            return true;
        }
        Set<String> groups = new HashSet<>();
        if (SessionHelper.getRoleIds() != null) {
            SessionHelper.getRoleIds().forEach(id -> groups.add(String.valueOf(id)));
        }
        if (SessionHelper.getRoleKeys() != null) {
            groups.addAll(SessionHelper.getRoleKeys());
        }
        if (SessionHelper.getOrgIds() != null) {
            SessionHelper.getOrgIds().forEach(id -> groups.add(String.valueOf(id)));
        }
        for (String group : splitIds(localTask.getCandidateGroups())) {
            if (groups.contains(group)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsCsv(String csv, String value) {
        return splitIds(csv).contains(value);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(String taskId, String userId, String comment, String signature, Map<String, Object> variables) {
        approve(taskId, userId, comment, signature, variables, SessionHelper.getTenantId(), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(String taskId, String userId, String comment, String signature,
                        Map<String, Object> variables, Long tenantId,
                        String idempotencyKey, String requestDigest,
                        List<FlowApprovalPointResultDTO> approvalPointResults) {
        FlowTask storedTask = authorizeTaskAction(
                taskId, userId, tenantId, "APPROVE", idempotencyKey, requestDigest, FlowTaskStatus.APPROVED);
        if (storedTask == null) {
            return;
        }
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateFlowableAssignee(task, userId);
        validateTaskAction(task, ACTION_APPROVE, comment, signature);
        validateRequiredVariables(task, variables);
        validateApprovalPoints(task, approvalPointResults);

        try {
            if (comment != null && !comment.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), comment);
            }
            recordApprovalPointResults(task, approvalPointResults);

            Map<String, Object> completeVariables = mergeActionVariables(variables, true);
            completeTask(task, completeVariables);
            directSendAfterReturn(task, completeVariables, userId);

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(FlowTaskStatus.APPROVED.getCode());
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setCompleteTime(LocalDateTime.now());
            flowTask.setActionIdempotencyKey(idempotencyKey);
            flowTask.setActionRequestDigest(requestDigest);
            flowTask.setActionType(idempotencyKey == null ? null : "APPROVE");
            updateTaskActionResultRequired(taskId, flowTask);

            log.info("审批通过：taskId={}, userId={}", taskId, userId);
            autoApproveRepeatedTasks(task.getProcessInstanceId());
        } catch (Exception e) {
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), "TASK_APPROVE", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(String taskId, String userId, String comment, String signature) {
        reject(taskId, userId, comment, signature, SessionHelper.getTenantId(), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(String taskId, String userId, String comment, String signature,
                       Long tenantId, String idempotencyKey, String requestDigest) {
        rejectInternal(taskId, userId, comment, signature, tenantId, idempotencyKey, requestDigest, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectToStart(String taskId, String userId, String comment, String signature,
                              Long tenantId, String idempotencyKey, String requestDigest) {
        rejectInternal(taskId, userId, comment, signature, tenantId, idempotencyKey, requestDigest, true);
    }

    private void rejectInternal(String taskId, String userId, String comment, String signature,
                                Long tenantId, String idempotencyKey, String requestDigest,
                                boolean rejectToStart) {
        FlowTask storedTask = authorizeTaskAction(
                taskId, userId, tenantId, rejectToStart ? "REJECT_TO_START" : "REJECT",
                idempotencyKey, requestDigest, FlowTaskStatus.REJECTED);
        if (storedTask == null) {
            return;
        }
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateFlowableAssignee(task, userId);
        validateTaskAction(task, rejectToStart ? ACTION_REJECT_TO_START : ACTION_REJECT, comment, signature);

        try {
            if (comment != null && !comment.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), comment);
            }

            Map<String, Object> variables = mergeActionVariables(null, false);
            if (rejectToStart) {
                variables.put("rejectToStart", true);
                // 保留原驳回节点，供发起人修改后选择直送；具体修改节点仍由业务 BPMN 回路决定。
                runtimeService.setVariable(task.getProcessInstanceId(), RETURN_SOURCE_ACTIVITY_ID,
                        task.getTaskDefinitionKey());
                runtimeService.removeVariable(task.getProcessInstanceId(), RETURN_TARGET_ACTIVITY_ID);
                runtimeService.setVariable(task.getProcessInstanceId(), RETURN_TO_START_PENDING, true);
            }
            completeTask(task, variables);

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(FlowTaskStatus.REJECTED.getCode());
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setCompleteTime(LocalDateTime.now());
            flowTask.setActionIdempotencyKey(idempotencyKey);
            flowTask.setActionRequestDigest(requestDigest);
            flowTask.setActionType(idempotencyKey == null ? null
                    : (rejectToStart ? "REJECT_TO_START" : "REJECT"));
            updateTaskActionResultRequired(taskId, flowTask);

            log.info("审批驳回：taskId={}, userId={}", taskId, userId);
        } catch (Exception e) {
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), rejectToStart ? "TASK_REJECT_TO_START" : "TASK_REJECT", e);
            throw e;
        }
    }

    /**
     * 在 Flow 服务最终副作用边界重新校验租户、签收人与任务状态。
     * 返回 null 表示命中已成功的同请求幂等结果。
     */
    private FlowTask authorizeTaskAction(String taskId, String userId, Long tenantId,
                                         String actionType, String idempotencyKey,
                                         String requestDigest, FlowTaskStatus completedStatus) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_TASK_TENANT_REQUIRED");
        }
        FlowTask storedTask = baseMapper.selectByTaskIdForUpdateAndTenant(taskId, tenantId);
        if (FlowTaskActionAuthorization.authorize(
                storedTask, userId, tenantId, actionType,
                idempotencyKey, requestDigest, completedStatus)) {
            return null;
        }
        return storedTask;
    }

    private void validateFlowableAssignee(Task task, String userId) {
        if (!userId.equals(task.getAssignee())) {
            throw new RuntimeException("FLOW_TASK_ASSIGNEE_MISMATCH");
        }
    }

    private void updateTaskActionResultRequired(String taskId, FlowTask flowTask) {
        if (!updateTaskByTenant(taskId, flowTask)) {
            throw new IllegalStateException("FLOW_TASK_STATE_UPDATE_FAILED");
        }
    }

    private boolean updateTaskByTenant(String taskId, FlowTask task) {
        Long tenantId = requireTenantId();
        return baseMapper.updateByTaskIdAndTenant(taskId, tenantId, task) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegate(String taskId, String userId, String targetUserId, String comment, String signature) {
        delegate(taskId, userId, targetUserId, comment, signature,
                SessionHelper.getTenantId(), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegate(String taskId, String userId, String targetUserId, String comment, String signature,
                         Long tenantId, String idempotencyKey, String requestDigest) {
        if (isBlank(targetUserId)) {
            throw new RuntimeException("新处理人不能为空");
        }
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_TASK_TENANT_REQUIRED");
        }
        assertTaskMutationActor(taskId, userId, false);
        validateReassignTarget(targetUserId.trim());
        FlowTask storedTask = authorizeTaskAction(taskId, userId, tenantId, "DELEGATE",
                idempotencyKey, requestDigest, FlowTaskStatus.CLAIMED);
        if (storedTask == null) {
            return;
        }
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateTaskAction(task, ACTION_DELEGATE, comment, signature);

        try {
            String owner = task.getAssignee() != null && !task.getAssignee().isEmpty()
                    ? task.getAssignee()
                    : userId;
            if (owner != null && !owner.isEmpty()) {
                taskService.setOwner(taskId, owner);
            }
            taskService.setAssignee(taskId, targetUserId.trim());

            FlowTask flowTask = new FlowTask();
            // Flowable 已经设置了 assignee，镜像状态必须是已签收；写成待办会导致
            // 目标用户再次签收失败但列表仍显示“待办”的状态不一致。
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
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), "TASK_DELEGATE", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnTask(String taskId, String userId, String comment, String signature) {
        returnTask(taskId, userId, comment, signature, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnTask(String taskId, String userId, String comment, String signature,
                           String requestedTargetActivityId) {
        assertTaskTenantForAction(taskId);
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateFlowableAssignee(task, userId);
        validateReturnAction(task, comment, signature, requestedTargetActivityId);

        try {
            String targetActivityId = resolveReturnTarget(task, requestedTargetActivityId);
            if (targetActivityId == null || targetActivityId.isEmpty()) {
                throw new RuntimeException("当前任务没有可退回的上一审批节点");
            }

            if (comment != null && !comment.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), "退回：" + comment);
            }

            // 保存“谁发起退回、退回到哪里”，供修正节点选择直送时使用。
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

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(FlowTaskStatus.RETURNED.getCode());
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setCompleteTime(LocalDateTime.now());
            updateTaskByTenant(taskId, flowTask);

            log.info("退回任务：taskId={}, userId={}, targetActivityId={}", taskId, userId, targetActivityId);
        } catch (Exception e) {
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), "TASK_RETURN", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reassignByInitiator(String taskId, String userId, String targetUserId, String reason) {
        if (isBlank(targetUserId)) {
            throw new RuntimeException("任务不存在或新处理人不能为空");
        }
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new RuntimeException("FLOW_TASK_TENANT_REQUIRED");
        }
        FlowTask localTask = baseMapper.selectByTaskIdForUpdateAndTenant(taskId, tenantId);
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
                || (business != null && Objects.equals(userId, business.getApplyUserId()));
        if (!allowed) {
            throw new RuntimeException("仅当前处理人、任务拥有人或流程发起人可以改派");
        }
        validateReassignTarget(targetUserId.trim());
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateTask(String taskId, String userId, String comment, String signature) {
        assertTaskMutationActor(taskId, userId, true);
        Long tenantId = requireTenantId();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateTaskAction(task, ACTION_TERMINATE, comment, signature);

        try {
            List<String> activeTaskIds = taskService.createTaskQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .list()
                    .stream()
                    .map(Task::getId)
                    .filter(Objects::nonNull)
                    .toList();
            String reason = comment != null && !comment.isBlank() ? comment : "审批人终结流程";
            taskService.addComment(taskId, task.getProcessInstanceId(), "终结流程：" + reason);
            runtimeService.deleteProcessInstance(task.getProcessInstanceId(), reason);

            if (!activeTaskIds.isEmpty()) {
                baseMapper.updateProcessTaskStatusByTaskIds(activeTaskIds, tenantId,
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

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(FlowTaskStatus.TERMINATED.getCode());
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setCompleteTime(LocalDateTime.now());
            updateTaskByTenant(taskId, flowTask);

            log.info("审批人终结流程：taskId={}, processInstanceId={}, userId={}",
                    taskId, task.getProcessInstanceId(), userId);
        } catch (Exception e) {
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), "TASK_TERMINATE", e);
            throw e;
        }
    }

    private Map<String, Object> mergeActionVariables(Map<String, Object> variables, boolean approved) {
        Map<String, Object> completeVariables = variables != null ? new HashMap<>(variables) : new HashMap<>();
        completeVariables.put("approved", approved);
        completeVariables.put("approvalResult", approved ? "approve" : "reject");
        return completeVariables;
    }

    /**
     * 退回节点修正后，按用户选择将新任务直接送回原驳回节点，跳过中间节点。
     * Flowable complete 后才会创建后继任务，因此这里基于完成后的活动列表做一次状态迁移。
     */
    private void directSendAfterReturn(Task completedTask, Map<String, Object> actionVariables, String userId) {
        String processInstanceId = completedTask.getProcessInstanceId();
        if (!isProcessRunning(processInstanceId)) {
            return;
        }
        Object source = runtimeService.getVariable(processInstanceId, RETURN_SOURCE_ACTIVITY_ID);
        Object target = runtimeService.getVariable(processInstanceId, RETURN_TARGET_ACTIVITY_ID);
        Object returnToStartPending = runtimeService.getVariable(processInstanceId, RETURN_TO_START_PENDING);
        boolean returnedToHistoricalNode = target != null
                && Objects.equals(String.valueOf(target), completedTask.getTaskDefinitionKey());
        if (source == null || (!returnedToHistoricalNode && !Boolean.TRUE.equals(readBoolean(returnToStartPending)))) {
            return;
        }
        boolean directSend = Boolean.TRUE.equals(readBoolean(
                actionVariables == null ? null : actionVariables.get(DIRECT_SEND_VARIABLE)));
        if (!directSend) {
            runtimeService.removeVariable(processInstanceId, RETURN_SOURCE_ACTIVITY_ID);
            runtimeService.removeVariable(processInstanceId, RETURN_TARGET_ACTIVITY_ID);
            runtimeService.removeVariable(processInstanceId, RETURN_TO_START_PENDING);
            return;
        }
        if (Boolean.TRUE.equals(readBoolean(returnToStartPending))
                && !isProcessStarterTask(completedTask, userId)) {
            throw new RuntimeException("仅流程发起人可以执行驳回后的直送");
        }
        String sourceActivityId = String.valueOf(source);
        List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
        if (activeActivityIds == null || activeActivityIds.isEmpty()) {
            return;
        }
        if (activeActivityIds.contains(sourceActivityId)) {
            runtimeService.removeVariable(processInstanceId, RETURN_SOURCE_ACTIVITY_ID);
            runtimeService.removeVariable(processInstanceId, RETURN_TARGET_ACTIVITY_ID);
            runtimeService.removeVariable(processInstanceId, RETURN_TO_START_PENDING);
            return;
        }
        if (activeActivityIds.size() != 1) {
            throw new RuntimeException("当前流程存在多个活动分支，不能安全直送");
        }
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo(activeActivityIds.get(0), sourceActivityId)
                .changeState();
        runtimeService.removeVariable(processInstanceId, RETURN_SOURCE_ACTIVITY_ID);
        runtimeService.removeVariable(processInstanceId, RETURN_TARGET_ACTIVITY_ID);
        runtimeService.removeVariable(processInstanceId, RETURN_TO_START_PENDING);
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

    /**
     * Flowable 委派态任务不能直接 complete，需要先 resolve。
     */
    private void completeTask(Task task, Map<String, Object> variables) {
        String taskId = task.getId();
        if (DelegationState.PENDING.equals(task.getDelegationState())) {
            log.info("任务处于委派待解决状态，先 resolve 再 complete：taskId={}, assignee={}, owner={}",
                    taskId, task.getAssignee(), task.getOwner());
            taskService.resolveTask(taskId);
        }

        try {
            if (variables != null && !variables.isEmpty()) {
                if (isProcessRunning(task.getProcessInstanceId())) {
                    runtimeService.setVariables(task.getProcessInstanceId(), variables);
                }
                taskService.complete(taskId, variables);
            } else {
                taskService.complete(taskId);
            }
        } catch (org.flowable.common.engine.api.FlowableObjectNotFoundException e) {
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

    private void autoApproveRepeatedTasks(String processInstanceId) {
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

        String mode = readProcessStringAttribute(instance.getProcessDefinitionId(), "autoApprovalMode");
        if (!AUTO_APPROVAL_FIRST_ONLY.equals(mode) && !AUTO_APPROVAL_CONSECUTIVE.equals(mode)) {
            return;
        }

        Set<String> completedAutomatically = new HashSet<>();
        int guard = 0;
        while (guard++ < 30) {
            List<Task> activeTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .list();
            Task matchedTask = null;
            for (Task activeTask : activeTasks) {
                if (completedAutomatically.contains(activeTask.getId())) {
                    continue;
                }
                if (shouldAutoApproveTask(activeTask, mode)) {
                    matchedTask = activeTask;
                    break;
                }
            }
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
            return hasFinishedTaskByAssignee(task.getProcessInstanceId(), assignee);
        }
        HistoricTaskInstance previousTask = findLastFinishedTask(task.getProcessInstanceId());
        return previousTask != null && Objects.equals(previousTask.getAssignee(), assignee);
    }

    private boolean hasFinishedTaskByAssignee(String processInstanceId, String assignee) {
        long count = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(assignee)
                .finished()
                .count();
        return count > 0;
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

        FlowTask flowTask = new FlowTask();
        flowTask.setStatus(FlowTaskStatus.APPROVED.getCode());
        flowTask.setComment(comment);
        flowTask.setCompleteTime(LocalDateTime.now());
        updateTaskByTenant(task.getId(), flowTask);

        log.info("重复审批自动同意：taskId={}, processInstanceId={}, assignee={}, mode={}",
                task.getId(), task.getProcessInstanceId(), task.getAssignee(), mode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegateTask(String taskId, String userId, String delegateUserId, String comment) {
        delegate(taskId, userId, delegateUserId, comment, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSign(String taskId, String userId, String targetUserId, String reason) {
        addSign(taskId, userId, targetUserId, reason, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSign(String taskId, String userId, String targetUserId, String reason, String signMode) {
        mutateCandidateSign(taskId, userId, targetUserId, reason, signMode,
                SessionHelper.getTenantId(), null, null, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSign(String taskId, String userId, String targetUserId, String reason, String signMode,
                        Long tenantId, String idempotencyKey, String requestDigest) {
        mutateCandidateSign(taskId, userId, targetUserId, reason, signMode,
                tenantId, idempotencyKey, requestDigest, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reduceSign(String taskId, String userId, String targetUserId, String reason) {
        reduceSign(taskId, userId, targetUserId, reason, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reduceSign(String taskId, String userId, String targetUserId, String reason, String signMode) {
        mutateCandidateSign(taskId, userId, targetUserId, reason, signMode,
                SessionHelper.getTenantId(), null, null, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reduceSign(String taskId, String userId, String targetUserId, String reason, String signMode,
                           Long tenantId, String idempotencyKey, String requestDigest) {
        mutateCandidateSign(taskId, userId, targetUserId, reason, signMode,
                tenantId, idempotencyKey, requestDigest, false);
    }

    private void mutateCandidateSign(String taskId, String userId, String targetUserId,
                                     String reason, String signMode, Long tenantId,
                                     String idempotencyKey, String requestDigest, boolean add) {
        if (isBlank(targetUserId)) {
            throw new RuntimeException("目标用户不能为空");
        }
        String normalizedSignMode = FlowTaskSignMode.fromCode(signMode).getCode();
        if (!FlowTaskSignMode.PARALLEL.getCode().equals(normalizedSignMode)) {
            throw new IllegalStateException("FLOW_TASK_SIGN_MODE_UNSUPPORTED");
        }
        assertTaskMutationActor(taskId, userId, false);
        validateReassignTarget(targetUserId.trim());
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_TASK_TENANT_REQUIRED");
        }
        if ((idempotencyKey == null) != (requestDigest == null)) {
            throw new IllegalStateException("FLOW_TASK_IDEMPOTENCY_INVALID");
        }
        if (idempotencyKey != null && flowTaskCandidateMapper == null) {
            throw new IllegalStateException("FLOW_TASK_IDEMPOTENCY_UNAVAILABLE");
        }
        FlowTask localTask = baseMapper.selectByTaskIdAndTenant(taskId, tenantId);
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null || localTask == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        localTask = baseMapper.selectByTaskIdForUpdateAndTenant(taskId, tenantId);
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

        LinkedHashSet<String> candidates = new LinkedHashSet<>(splitIds(localTask.getCandidateUsers()));
        boolean changed;
        if (add) {
            if (candidates.size() >= MAX_DYNAMIC_SIGNERS) {
                throw new RuntimeException("单个任务最多允许加签 " + MAX_DYNAMIC_SIGNERS + " 人");
            }
            changed = candidates.add(targetUserId.trim());
            if (changed) {
                taskService.addCandidateUser(taskId, targetUserId.trim());
                syncCandidateRelation(localTask, targetUserId.trim(), userId, reason, normalizedSignMode,
                        idempotencyKey, requestDigest, true, null, null);
            }
        } else {
            changed = candidates.remove(targetUserId.trim());
            if (changed) {
                taskService.deleteCandidateUser(taskId, targetUserId.trim());
                syncCandidateRelation(localTask, targetUserId.trim(), userId, reason, normalizedSignMode,
                        idempotencyKey, requestDigest, false, null, null);
            }
        }
        if (!changed) {
            throw new RuntimeException(add ? "目标用户已经在加签名单中" : "目标用户不在加签名单中");
        }

        FlowTask update = new FlowTask();
        update.setCandidateUsers(String.join(",", candidates));
        update.setComment(reason);
        if (!updateTaskByTenant(taskId, update)) {
            throw new IllegalStateException("加签状态同步失败");
        }
        String action = add ? "加签" : "减签";
        taskService.addComment(taskId, task.getProcessInstanceId(), action,
                isBlank(reason) ? action : reason.trim());
        log.info("流程任务{}：taskId={}, actor={}, target={}", action, taskId, userId, targetUserId);
    }

    /**
     * 对已经由 BPMN 配置为多实例的用户任务，使用 Flowable 原生多实例执行 API 创建/删除子执行。
     * 普通用户任务继续使用候选关系兼容路径，避免把一个普通任务伪装成流程子任务。
     */
    private void mutateFlowableMultiInstanceSign(Task task, FlowTask parentTask, String operatorId,
                                                  String targetUserId, String reason, String signMode,
                                                  String idempotencyKey, String requestDigest, boolean add) {
        UserTask userTask = resolveMultiInstanceUserTask(task);
        if (flowTaskCandidateMapper == null || userTask == null || userTask.getLoopCharacteristics() == null) {
            throw new IllegalStateException("FLOW_TASK_SIGN_MULTI_INSTANCE_UNAVAILABLE");
        }
        MultiInstanceLoopCharacteristics loop = userTask.getLoopCharacteristics();
        if (add) {
            String elementVariable = loop.getElementVariable();
            if (isBlank(elementVariable)) {
                elementVariable = "assignee";
            }
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
            flowTaskCandidateMapper.insertIgnore(relation);
        } else {
            flowTaskCandidateMapper.deactivateWithAudit(task.getTenantId(), task.getTaskId(),
                    FlowTaskCandidate.TYPE_USER, candidateUserId, operatorId, reason,
                    idempotencyKey, requestDigest, now);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlowTaskSignRelationVO> getSignRelations(String taskId, String userId) {
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

    private List<String> splitIds(String value) {
        if (isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String processInstanceId, String userId) {
        try {
            assertSubmitterWithdrawAllowed(processInstanceId, userId);
            List<String> activeTaskIds = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .list()
                    .stream()
                    .map(Task::getId)
                    .filter(Objects::nonNull)
                    .toList();
            runtimeService.deleteProcessInstance(processInstanceId, "用户撤回");

            Long tenantId = SessionHelper.getTenantId();
            if (tenantId == null || tenantId <= 0) {
                throw new IllegalStateException("FLOW_TASK_TENANT_REQUIRED");
            }
            if (!activeTaskIds.isEmpty()) {
                baseMapper.updateProcessTaskStatusByTaskIds(activeTaskIds, tenantId,
                        FlowTaskStatus.WITHDRAWN.getCode(), LocalDateTime.now());
            }

            log.info("撤回流程：processInstanceId={}, userId={}", processInstanceId, userId);
        } catch (Exception e) {
            FlowErrorLog errorLog = new FlowErrorLog();
            errorLog.setProcessInstanceId(processInstanceId);
            errorLog.setErrorStage("TASK_WITHDRAW");
            flowErrorLogService.recordError(errorLog, e);
            throw e;
        }
    }

    private void assertSubmitterWithdrawAllowed(String processInstanceId, String userId) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (instance == null) {
            throw new RuntimeException("流程实例不存在或已结束");
        }

        Boolean allowed = readBooleanProcessAttribute(instance.getProcessDefinitionId(), "allowSubmitterWithdraw");
        if (Boolean.FALSE.equals(allowed)) {
            throw new RuntimeException("当前流程不允许提交人撤回审批中的申请");
        }

        if (!isProcessSubmitter(processInstanceId, userId)) {
            throw new RuntimeException("只有提交人可以撤回该申请");
        }
    }

    private boolean isProcessSubmitter(String processInstanceId, String userId) {
        if (isBlank(userId)) {
            return false;
        }
        Long tenantId = SessionHelper.getTenantId();
        FlowBusiness business = tenantId == null
                ? null
                : flowBusinessMapper.selectByProcessInstanceIdAndTenantId(processInstanceId, tenantId);
        if (business != null && !isBlank(business.getApplyUserId())) {
            return Objects.equals(String.valueOf(business.getApplyUserId()), String.valueOf(userId));
        }
        Object initiator = runtimeService.getVariable(processInstanceId, "initiator");
        if (initiator != null && !isBlank(String.valueOf(initiator))) {
            return Objects.equals(String.valueOf(initiator), String.valueOf(userId));
        }
        log.warn("撤回申请未找到可信提交人信息，拒绝操作：processInstanceId={}, userId={}",
                processInstanceId, userId);
        return false;
    }

    @Override
    public FlowTask getTaskDetail(String taskId) {
        FlowTask task = flowAccessGuard.requireTaskVisible(taskId);
        if (task != null) {
            task.setProcessDefKey(resolveProcessDefinitionKey(
                    firstNonBlank(task.getProcessDefId(), task.getProcessDefKey()),
                    task.getProcessDefKey()));
        }
        return task;
    }

    @Override
    public byte[] getProcessDiagram(String processInstanceId) {
        try {
            flowAccessGuard.requireProcessVisible(processInstanceId);
            // 1. 获取流程实例
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            
            if (historicProcessInstance == null) {
                log.warn("流程实例不存在：{}", processInstanceId);
                return null;
            }
            
            String processDefinitionId = historicProcessInstance.getProcessDefinitionId();
            log.info("获取流程图：processInstanceId={}, processDefinitionId={}", processInstanceId, processDefinitionId);
            
            // 2. 获取BPMN模型
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel == null) {
                log.warn("BPMN模型不存在：{}", processDefinitionId);
                return null;
            }
            
            // 打印BPMN模型信息
            log.info("BPMN模型进程数: {}", bpmnModel.getProcesses() != null ? bpmnModel.getProcesses().size() : 0);
            log.info("BPMN模型LocationMap大小: {}", bpmnModel.getLocationMap() != null ? bpmnModel.getLocationMap().size() : 0);
            log.info("BPMN模型FlowLocationMap大小: {}", bpmnModel.getFlowLocationMap() != null ? bpmnModel.getFlowLocationMap().size() : 0);
            
            // 3. 检查BPMN模型是否有图形信息
            if (!hasGraphicInfo(bpmnModel)) {
                log.info("BPMN模型没有图形坐标信息，尝试从部署资源获取原始流程图");
                return getDiagramFromResource(processDefinitionId);
            }
            
            // 4. 获取已完成的历史活动节点
            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .finished()
                    .orderByHistoricActivityInstanceStartTime()
                    .asc()
                    .listPage(0, MAX_DETAIL_HISTORY_ITEMS);
            
            // 已完成的节点ID列表
            List<String> completedActivityIds = historicActivityInstances.stream()
                    .map(HistoricActivityInstance::getActivityId)
                    .distinct()
                    .collect(Collectors.toList());
            
            log.info("已完成节点数量: {}, 节点ID: {}", completedActivityIds.size(), completedActivityIds);
            
            // 5. 获取当前活动节点（运行中）
            List<String> currentActivityIds = new ArrayList<>();
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            
            if (processInstance != null) {
                // 流程还在运行中，获取当前活动节点
                currentActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
                log.info("当前活动节点数量: {}, 节点ID: {}", currentActivityIds.size(), currentActivityIds);
            }
            
            // 6. 使用流程图生成器生成图片
            ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
            
            if (diagramGenerator == null) {
                log.error("ProcessDiagramGenerator 未配置");
                return null;
            }
            
            // 设置字体（使用系统默认字体，避免字体不存在的问题）
            String activityFontName = "SansSerif";
            String labelFontName = "SansSerif";
            String annotationFontName = "SansSerif";
            
            log.info("开始生成流程图，字体: {}", activityFontName);
            
            // 生成流程图输入流（高亮已完成和当前节点）
            InputStream diagramStream = diagramGenerator.generateDiagram(
                    bpmnModel,
                    "png",
                    completedActivityIds,    // 高亮已完成节点（绿色）
                    currentActivityIds,      // 高亮当前节点（红色）
                    activityFontName,
                    labelFontName,
                    annotationFontName,
                    null,
                    1.0,
                    true
            );
            
            // 7. 转换为字节数组
            if (diagramStream != null) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = diagramStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                diagramStream.close();
                log.info("流程图生成成功，大小: {} bytes", output.size());
                return output.toByteArray();
            }
            
            log.warn("流程图生成返回空流");
            return null;
            
        } catch (Exception e) {
            log.error("生成流程图失败：processInstanceId={}, 错误: {}", processInstanceId, e.getMessage(), e);
            throw new RuntimeException("生成流程图失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取流程定义的 BPMN XML
     */
    private String getBpmnXml(ProcessDefinition processDefinition) {
        try {
            // 获取 BpmnModel
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
            if (bpmnModel == null) {
                log.warn("BPMN模型不存在：{}", processDefinition.getId());
                return null;
            }
            
            // 使用 Flowable 的 XML 导出功能
            byte[] bpmnBytes = new org.flowable.bpmn.converter.BpmnXMLConverter()
                    .convertToXML(bpmnModel);
            
            return new String(bpmnBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("获取BPMN XML失败：processDefinitionId={}", processDefinition.getId(), e);
            return null;
        }
    }
    
    /**
     * 检查BPMN模型是否有图形信息
     */
    private boolean hasGraphicInfo(BpmnModel bpmnModel) {
        if (bpmnModel.getLocationMap() == null || bpmnModel.getLocationMap().isEmpty()) {
            return false;
        }
        // 检查是否有有效的坐标信息
        for (org.flowable.bpmn.model.GraphicInfo graphicInfo : bpmnModel.getLocationMap().values()) {
            if (graphicInfo != null && graphicInfo.getX() >= 0 && graphicInfo.getY() >= 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 从资源获取原始流程图（无高亮）
     */
    private byte[] getDiagramFromResource(String processDefinitionId) {
        try {
            // 获取流程定义
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            
            if (processDefinition == null) {
                log.warn("流程定义不存在：{}", processDefinitionId);
                return null;
            }
            
            log.info("流程定义信息：id={}, name={}, deploymentId={}, diagramResourceName={}",
                    processDefinition.getId(),
                    processDefinition.getName(),
                    processDefinition.getDeploymentId(),
                    processDefinition.getDiagramResourceName());
            
            // 获取流程图资源
            String diagramResourceName = processDefinition.getDiagramResourceName();
            if (diagramResourceName != null && !diagramResourceName.isEmpty()) {
                log.info("尝试从部署资源获取流程图：{}", diagramResourceName);
                try {
                    // 从资源流获取流程图
                    InputStream diagramStream = repositoryService.getResourceAsStream(
                            processDefinition.getDeploymentId(), diagramResourceName);
                    
                    if (diagramStream != null) {
                        // 转换为字节数组
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = diagramStream.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                        diagramStream.close();
                        log.info("成功从部署资源获取流程图，大小：{} bytes", output.size());
                        return output.toByteArray();
                    }
                } catch (Exception e) {
                    log.warn("从部署资源获取流程图失败：{}", e.getMessage());
                }
            }
            
            // 如果没有流程图资源，尝试从 BPMN XML 重新生成
            log.info("尝试从 BPMN XML 资源重新生成流程图");
            return generateDiagramFromBpmnXml(processDefinition);
            
        } catch (Exception e) {
            log.error("从资源获取流程图失败：processDefinitionId={}", processDefinitionId, e);
            return null;
        }
    }
    
    /**
     * 从 BPMN XML 资源重新生成流程图
     */
    private byte[] generateDiagramFromBpmnXml(ProcessDefinition processDefinition) {
        try {
            // 获取 BPMN XML 资源名称
            String resourceName = processDefinition.getResourceName();
            log.info("BPMN XML 资源名称：{}", resourceName);
            
            // 获取 BPMN XML 内容
            InputStream bpmnStream = repositoryService.getResourceAsStream(
                    processDefinition.getDeploymentId(), resourceName);
            
            if (bpmnStream == null) {
                log.warn("无法获取 BPMN XML 资源：{}", resourceName);
                return null;
            }
            
            // 读取 BPMN XML 内容
            ByteArrayOutputStream bpmnOutput = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bpmnStream.read(buffer)) != -1) {
                bpmnOutput.write(buffer, 0, bytesRead);
            }
            bpmnStream.close();
            
            String bpmnXml = bpmnOutput.toString("UTF-8");
            log.info("BPMN XML 内容长度：{}", bpmnXml.length());
            log.info("BPMN XML 是否包含 BPMNDiagram：{}", bpmnXml.contains("BPMNDiagram"));
            
            // 使用 BpmnXMLConverter 解析（第三个参数 true 表示解析图形信息）
            BpmnModel bpmnModel = new org.flowable.bpmn.converter.BpmnXMLConverter()
                    .convertToBpmnModel(
                            new org.flowable.common.engine.impl.util.io.BytesStreamSource(
                                    bpmnXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                            false,
                            true);
            
            log.info("重新解析后的 LocationMap 大小：{}",
                    bpmnModel.getLocationMap() != null ? bpmnModel.getLocationMap().size() : 0);
            
            // 检查是否有图形信息
            if (bpmnModel.getLocationMap() == null || bpmnModel.getLocationMap().isEmpty()) {
                log.warn("BPMN XML 中没有图形坐标信息");
                return null;
            }
            
            // 使用流程图生成器生成图片
            ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
            
            // 设置中文字体
            String activityFontName = "宋体";
            String labelFontName = "宋体";
            String annotationFontName = "宋体";
            
            // 生成流程图（无高亮）
            InputStream diagramStream = diagramGenerator.generateDiagram(
                    bpmnModel,
                    "png",
                    java.util.Collections.emptyList(),
                    java.util.Collections.emptyList(),
                    activityFontName,
                    labelFontName,
                    annotationFontName,
                    null,
                    1.0,
                    true
            );
            
            if (diagramStream != null) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                buffer = new byte[4096];
                while ((bytesRead = diagramStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                diagramStream.close();
                log.info("成功从 BPMN XML 生成流程图，大小：{} bytes", output.size());
                return output.toByteArray();
            }
            
            return null;
        } catch (Exception e) {
            log.error("从 BPMN XML 生成流程图失败", e);
            return null;
        }
    }

    @Override
    public ProcessDiagramInfo getProcessDiagramInfo(String processInstanceId) {
        return getProcessDiagramInfo(processInstanceId, false);
    }

    @Override
    public ProcessDiagramInfo getProcessDiagramInfo(String processInstanceId, boolean includeImage) {
        try {
            flowAccessGuard.requireProcessVisible(processInstanceId);
            log.info("开始获取流程图详情，processInstanceId: {}", processInstanceId);
            
            // 1. 获取流程实例
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            
            if (historicProcessInstance == null) {
                log.warn("流程实例不存在：{}", processInstanceId);
                return null;
            }
            
            String processDefinitionId = historicProcessInstance.getProcessDefinitionId();
            log.info("流程定义ID: {}", processDefinitionId);
            
            // 2. 获取BPMN模型
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel == null) {
                log.warn("BPMN模型不存在：{}", processDefinitionId);
                return null;
            }
            
            // 3. 构建返回结果
            ProcessDiagramInfo diagramInfo = new ProcessDiagramInfo();
            diagramInfo.setProcessInstanceId(processInstanceId);
            diagramInfo.setProcessDefinitionId(processDefinitionId);
            diagramInfo.setProcessName(historicProcessInstance.getName());
            diagramInfo.setStartUserId(historicProcessInstance.getStartUserId());
            diagramInfo.setStartTime(historicProcessInstance.getStartTime());
            diagramInfo.setEndTime(historicProcessInstance.getEndTime());
            
            // 获取发起人姓名
            String startUserId = historicProcessInstance.getStartUserId();
            if (startUserId != null && flowOrgIntegrationService != null) {
                try {
                    Map<String, Object> userInfo = flowOrgIntegrationService.getUserInfo(startUserId);
                    diagramInfo.setStartUserName((String)userInfo.get("realName"));
                    log.info("发起人姓名: {}", (String)userInfo.get("realName"));
                } catch (Exception e) {
                    log.warn("获取发起人姓名失败: {}", e.getMessage());
                }
            }
            
            // 判断流程状态
            if (historicProcessInstance.getEndTime() == null) {
                diagramInfo.setStatus(FlowDiagramStatus.RUNNING.getCode());
            } else if (historicProcessInstance.getDeleteReason() != null) {
                diagramInfo.setStatus(FlowDiagramStatus.TERMINATED.getCode());
            } else {
                diagramInfo.setStatus(FlowDiagramStatus.COMPLETED.getCode());
            }
            log.info("流程状态: {}", diagramInfo.getStatus());
            
            // 4. 获取 BPMN XML（用于前端 bpmn-js 渲染）
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            if (processDefinition != null) {
                String bpmnXml = getBpmnXml(processDefinition);
                diagramInfo.setBpmnXml(bpmnXml);
                log.info("BPMN XML 长度: {}", bpmnXml != null ? bpmnXml.length() : 0);
            }
            
            // 5. 生成流程图图片（备用，默认关闭。BPMN XML 已足够前端渲染，避免每次生成 Base64 PNG 拖慢加载）
            if (includeImage) {
                byte[] diagramBytes = getProcessDiagram(processInstanceId);
                if (diagramBytes != null && diagramBytes.length > 0) {
                    String base64 = Base64.getEncoder().encodeToString(diagramBytes);
                    diagramInfo.setDiagramBase64("data:image/png;base64," + base64);
                    log.info("流程图图片大小: {} bytes, base64长度: {}", diagramBytes.length, base64.length());
                } else {
                    log.warn("未能生成流程图图片");
                }
            }
            
            // 6. 获取节点信息列表
            List<ProcessNodeInfo> nodes = buildNodeInfoList(bpmnModel, processInstanceId);
            diagramInfo.setNodes(nodes);
            log.info("节点数量: {}", nodes != null ? nodes.size() : 0);

            SequenceFlowStatus sequenceFlowStatus = buildSequenceFlowInfoList(bpmnModel, processInstanceId);
            diagramInfo.setSequenceFlows(sequenceFlowStatus.flows());
            diagramInfo.setSequenceFlowStatusAvailable(sequenceFlowStatus.available());
            diagramInfo.setSequenceFlowStatusMessage(sequenceFlowStatus.message());
            
            // 打印节点详情
            if (nodes != null && !nodes.isEmpty()) {
                for (ProcessNodeInfo node : nodes) {
                    log.info("节点: id={}, name={}, type={}, status={}, x={}, y={}",
                            node.getNodeId(), node.getNodeName(), node.getNodeType(),
                            node.getStatus(), node.getX(), node.getY());
                }
            }
            
            return diagramInfo;
            
        } catch (Exception e) {
            log.error("获取流程图详情失败：processInstanceId={}", processInstanceId, e);
            return null;
        }
    }

    /**
     * 从 Flowable 历史活动中批量计算连线执行状态。只有历史级别记录过
     * sequenceFlow 活动时才宣称状态可靠，避免把“没有记录”误报为所有连线未执行。
     */
    private SequenceFlowStatus buildSequenceFlowInfoList(BpmnModel bpmnModel, String processInstanceId) {
        if (bpmnModel == null || bpmnModel.getProcesses() == null || bpmnModel.getProcesses().isEmpty()) {
            return new SequenceFlowStatus(List.of(), false, "BPMN 模型缺失，无法计算连线状态");
        }
        Process process = bpmnModel.getProcesses().get(0);
        List<SequenceFlow> definitions = process.findFlowElementsOfType(SequenceFlow.class);
        if (definitions == null || definitions.isEmpty()) {
            return new SequenceFlowStatus(List.of(), true, null);
        }
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .listPage(0, MAX_DETAIL_HISTORY_ITEMS);
        Set<String> executedFlowIds = activities == null ? Set.of() : activities.stream()
                .filter(activity -> activity != null && activity.getEndTime() != null)
                .filter(activity -> "sequenceFlow".equalsIgnoreCase(activity.getActivityType()))
                .map(HistoricActivityInstance::getActivityId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        if (executedFlowIds.isEmpty()) {
            return new SequenceFlowStatus(List.of(), false,
                    "当前 Flowable 历史级别未记录 sequenceFlow 活动，连线状态不可用");
        }
        List<ProcessSequenceFlowInfo> result = definitions.stream().map(flow -> {
            ProcessSequenceFlowInfo item = new ProcessSequenceFlowInfo();
            item.setFlowId(flow.getId());
            item.setSourceRef(flow.getSourceRef());
            item.setTargetRef(flow.getTargetRef());
            item.setStatus(executedFlowIds.contains(flow.getId())
                    ? FlowDiagramStatus.COMPLETED.getCode() : FlowDiagramStatus.PENDING.getCode());
            return item;
        }).toList();
        return new SequenceFlowStatus(result, true, null);
    }

    private record SequenceFlowStatus(List<ProcessSequenceFlowInfo> flows,
                                      boolean available, String message) {
    }
    
    /**
     * 构建节点信息列表
     */
    private List<ProcessNodeInfo> buildNodeInfoList(BpmnModel bpmnModel, String processInstanceId) {
        List<ProcessNodeInfo> nodeList = new ArrayList<>();
        
        // 获取流程中的所有节点
        org.flowable.bpmn.model.Process process = bpmnModel.getProcesses().get(0);
        if (process == null) {
            return nodeList;
        }
        
        // 获取已完成的历史活动
        Map<String, HistoricActivityInstance> completedActivityMap = new HashMap<>();
        List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .listPage(0, MAX_DETAIL_HISTORY_ITEMS);
        
        for (HistoricActivityInstance activity : historicActivities) {
            if (activity.getEndTime() != null) {
                // 已完成的活动
                if (!completedActivityMap.containsKey(activity.getActivityId()) ||
                    completedActivityMap.get(activity.getActivityId()).getStartTime().before(activity.getStartTime())) {
                    completedActivityMap.put(activity.getActivityId(), activity);
                }
            }
        }
        
        // 获取当前活动节点
        Set<String> currentActivityIds = new HashSet<>();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        
        if (processInstance != null) {
            currentActivityIds = new HashSet<>(runtimeService.getActiveActivityIds(processInstanceId));
        }
        
        // 获取历史任务信息（用于获取处理人）
        Map<String, HistoricTaskInstance> taskMap = new HashMap<>();
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .listPage(0, MAX_DETAIL_HISTORY_ITEMS);
        for (HistoricTaskInstance task : historicTasks) {
            if (!taskMap.containsKey(task.getTaskDefinitionKey()) ||
                taskMap.get(task.getTaskDefinitionKey()).getCreateTime().before(task.getCreateTime())) {
                taskMap.put(task.getTaskDefinitionKey(), task);
            }
        }
        
        // 获取当前任务
        Map<String, Task> currentTaskMap = new HashMap<>();
        List<Task> currentTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
        for (Task task : currentTasks) {
            currentTaskMap.put(task.getTaskDefinitionKey(), task);
        }

        // 同一流程图内的处理人和候选人可能跨多个节点重复出现，复用一次批量查询结果，
        // 避免按节点、按用户触发组织服务 N+1 回查。
        Map<String, Map<String, Object>> userInfoCache = new HashMap<>();
        if (flowOrgIntegrationService != null) {
            Set<String> userIdsToLoad = new LinkedHashSet<>();
            currentTaskMap.values().forEach(task -> {
                if (task.getAssignee() != null && !task.getAssignee().isBlank()) {
                    userIdsToLoad.add(task.getAssignee());
                }
                taskService.getIdentityLinksForTask(task.getId()).stream()
                        .filter(link -> link.getUserId() != null && "candidate".equals(link.getType()))
                        .map(org.flowable.identitylink.api.IdentityLink::getUserId)
                        .forEach(userIdsToLoad::add);
            });
            historicTasks.stream()
                    .map(HistoricTaskInstance::getAssignee)
                    .filter(id -> id != null && !id.isBlank())
                    .forEach(userIdsToLoad::add);
            if (!userIdsToLoad.isEmpty()) {
                Map<String, Map<String, Object>> loaded =
                        flowOrgIntegrationService.getUserInfoBatch(new ArrayList<>(userIdsToLoad));
                if (loaded != null) {
                    userInfoCache.putAll(loaded);
                }
                userIdsToLoad.forEach(id -> userInfoCache.putIfAbsent(id, Collections.emptyMap()));
            }
        }
        
        // 遍历所有节点
        for (FlowNode flowNode : process.findFlowElementsOfType(FlowNode.class)) {
            ProcessNodeInfo nodeInfo = new ProcessNodeInfo();
            nodeInfo.setNodeId(flowNode.getId());
            nodeInfo.setNodeName(flowNode.getName());
            nodeInfo.setNodeType(flowNode.getClass().getSimpleName());
            
            // 获取图形信息
            GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
            if (graphicInfo != null) {
                nodeInfo.setX(graphicInfo.getX());
                nodeInfo.setY(graphicInfo.getY());
                nodeInfo.setWidth(graphicInfo.getWidth());
                nodeInfo.setHeight(graphicInfo.getHeight());
            }
            
            // 设置节点状态
            if (currentActivityIds.contains(flowNode.getId())) {
                nodeInfo.setStatus(FlowDiagramStatus.RUNNING.getCode());
            } else if (completedActivityMap.containsKey(flowNode.getId())) {
                nodeInfo.setStatus(FlowDiagramStatus.COMPLETED.getCode());
                HistoricActivityInstance activity = completedActivityMap.get(flowNode.getId());
                nodeInfo.setStartTime(activity.getStartTime());
                nodeInfo.setEndTime(activity.getEndTime());
                if (activity.getDurationInMillis() != null) {
                    nodeInfo.setDuration(activity.getDurationInMillis());
                }
            } else {
                nodeInfo.setStatus(FlowDiagramStatus.PENDING.getCode());
            }
            
            // 设置处理人信息（仅用户任务）
            if ("UserTask".equals(nodeInfo.getNodeType())) {
                // 先检查当前任务
                Task currentTask = currentTaskMap.get(flowNode.getId());
                if (currentTask != null) {
                    nodeInfo.setTaskId(currentTask.getId());
                    if (currentTask.getAssignee() != null) {
                        List<String> assigneeIds = Collections.singletonList(currentTask.getAssignee());
                        nodeInfo.setAssigneeIds(assigneeIds);
                        // 获取用户详情
                        fillUserInfo(nodeInfo, assigneeIds, userInfoCache);
                    }
                    // 获取候选人信息
                    if (currentTask.getAssignee() == null) {
                        // 任务未签收，获取候选人 - 使用 TaskService 查询
                        List<org.flowable.identitylink.api.IdentityLink> identityLinks = taskService.getIdentityLinksForTask(currentTask.getId());
                        if (identityLinks != null && !identityLinks.isEmpty()) {
                            List<String> candidateUsers = identityLinks.stream()
                                .filter(link -> link.getUserId() != null && "candidate".equals(link.getType()))
                                .map(org.flowable.identitylink.api.IdentityLink::getUserId)
                                .distinct()
                                .collect(Collectors.toList());
                            if (!candidateUsers.isEmpty()) {
                                nodeInfo.setCandidateUserIds(candidateUsers);
                                fillUserInfo(nodeInfo, candidateUsers, userInfoCache);
                            }
                        }
                    }
                } else {
                    // 检查历史任务
                    HistoricTaskInstance historicTask = taskMap.get(flowNode.getId());
                    if (historicTask != null) {
                        if (historicTask.getAssignee() != null) {
                            List<String> assigneeIds = Collections.singletonList(historicTask.getAssignee());
                            nodeInfo.setAssigneeIds(assigneeIds);
                            // 获取用户详情
                            fillUserInfo(nodeInfo, assigneeIds, userInfoCache);
                        }
                        nodeInfo.setStartTime(historicTask.getCreateTime());
                        nodeInfo.setEndTime(historicTask.getEndTime());
                        if (historicTask.getDurationInMillis() != null) {
                            nodeInfo.setDuration(historicTask.getDurationInMillis());
                        }
                    }
                }
            }
            
            nodeList.add(nodeInfo);
        }
        
        return nodeList;
    }
    
    /**
     * 填充用户信息（姓名、组织）
     */
    private void fillUserInfo(ProcessNodeInfo nodeInfo, List<String> userIds,
                              Map<String, Map<String, Object>> userInfoCache) {
        if (flowOrgIntegrationService == null || userIds == null || userIds.isEmpty()) {
            return;
        }

        List<String> missingIds = userIds.stream()
                .filter(id -> id != null && !id.isBlank() && !userInfoCache.containsKey(id))
                .distinct()
                .toList();
        if (!missingIds.isEmpty()) {
            Map<String, Map<String, Object>> loaded = flowOrgIntegrationService.getUserInfoBatch(missingIds);
            if (loaded != null) {
                userInfoCache.putAll(loaded);
            }
            missingIds.forEach(id -> userInfoCache.putIfAbsent(id, Collections.emptyMap()));
        }

        List<String> names = new ArrayList<>();
        List<String> orgs = new ArrayList<>();
        List<Map<String, Object>> details = new ArrayList<>();

        for (String userId : userIds) {
            try {
                Map<String, Object> userInfo = userInfoCache.get(userId);
                if (userInfo != null) {
                    // 获取用户名
                    String name = (String) userInfo.get("name");
                    if (name == null) {
                        name = (String) userInfo.get("nickname");
                    }
                    if (name == null) {
                        name = (String) userInfo.get("username");
                    }
                    if (name != null) {
                        names.add(name);
                    } else {
                        names.add(userId); // 如果没有名字，显示ID
                    }
                    
                    // 获取组织名称
                    String orgName = (String) userInfo.get("deptName");
                    if (orgName == null) {
                        orgName = (String) userInfo.get("orgName");
                    }
                    if (orgName != null) {
                        orgs.add(orgName);
                    }
                    
                    // 添加详情
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("userId", userId);
                    detail.put("name", name != null ? name : userId);
                    detail.put("orgName", orgName);
                    details.add(detail);
                } else {
                    names.add(userId);
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败: userId={}", userId, e);
                names.add(userId);
            }
        }
        
        nodeInfo.setAssigneeNames(names);
        nodeInfo.setAssigneeOrgs(orgs);
        nodeInfo.setAssigneeDetails(details);
    }

    @Override
    public void remind(String taskId) {
        flowAccessGuard.requireTaskVisible(taskId);
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            log.warn("催办失败：任务不存在，taskId={}", taskId);
            return;
        }
        
        log.info("催办任务：taskId={}, taskName={}", taskId, task.getName());
        
        // 发送催办消息通知
        if (messageService != null) {
            try {
                // 获取任务处理人
                String assignee = task.getAssignee();
                if (assignee == null || assignee.isEmpty()) {
                    // 如果任务未签收，尝试获取候选人
                    log.info("任务未签收，跳过消息通知：taskId={}", taskId);
                    return;
                }
                
                // 构建消息
                com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO request =
                    new com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO();
                request.setTitle("流程催办提醒");
                request.setContent(String.format(
                    "您有一个待办任务需要处理：%s，请及时处理。",
                    task.getName()
                ));
                request.setType("SYSTEM");
                request.setChannel("WEB");
                request.setSendScope("USERS");
                
                // 设置接收人
                Set<Long> userIds = new HashSet<>();
                try {
                    userIds.add(Long.parseLong(assignee));
                } catch (NumberFormatException e) {
                    log.warn("无法解析处理人ID：{}", assignee);
                    return;
                }
                request.setUserIds(userIds);
                
                // 发送消息
                messageService.send(request);
                log.info("催办消息发送成功：taskId={}, assignee={}", taskId, assignee);
                
            } catch (Exception e) {
                log.error("发送催办消息失败：taskId={}", taskId, e);
            }
        } else {
            log.warn("消息服务未启用，无法发送催办通知");
        }
    }

    private void validateTaskAction(Task task, String action, String comment, String signature) {
        TaskApprovalPolicy policy = getTaskApprovalPolicy(task);
        if (!policy.isAllowed(action)) {
            throw new RuntimeException("当前节点不允许执行该审批操作");
        }
        validateCommentAndSignature(policy, comment, signature);
    }

    /**
     * 指定节点驳回走“驳回”语义，不能再要求节点单独开启 allowReturn。
     * 未指定目标时仍按退回上一节点校验 allowReturn。
     */
    private void validateReturnAction(Task task, String comment, String signature, String requestedTargetActivityId) {
        TaskApprovalPolicy policy = getTaskApprovalPolicy(task);
        boolean specifiedNode = !isBlank(requestedTargetActivityId);
        boolean allowed = specifiedNode
                ? (policy.allowReject || policy.allowReturn || policy.allowMultiReturn)
                : (policy.allowReturn || policy.allowMultiReturn);
        if (!allowed) {
            throw new RuntimeException(specifiedNode ? "当前节点不允许驳回" : "当前节点不允许退回");
        }
        validateCommentAndSignature(policy, comment, signature);
    }

    private void applyNodePolicy(TaskFormInfo formInfo, FlowNode flowNode) {
        List<FlowApprovalPointDTO> approvalPoints = FlowNodePolicyParser.resolveApprovalPoints(flowNode);
        String approvalPoint = approvalPoints.stream()
                .map(FlowApprovalPointDTO::getContent)
                .collect(Collectors.joining("\n"));
        formInfo.setApprovalPoints(approvalPoints);
        formInfo.setApprovalPoint(isBlank(approvalPoint) ? null : approvalPoint);
        formInfo.setResponsibilityDescription(FlowNodePolicyParser.resolveResponsibilityDescription(flowNode));
    }

    private void validateApprovalPoints(Task task, List<FlowApprovalPointResultDTO> approvalPointResults) {
        FlowNode flowNode = getFlowNode(task);
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

    private void recordApprovalPointResults(Task task, List<FlowApprovalPointResultDTO> approvalPointResults) {
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

    private List<Map<String, Object>> readApprovalPointResults(String taskId) {
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
            return OBJECT_MAPPER.readValue(message, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.debug("读取审批要点结果失败: taskId={}", taskId);
            return Collections.emptyList();
        }
    }

    private void validateCommentAndSignature(TaskApprovalPolicy policy, String comment, String signature) {
        if (policy.requireComment && isBlank(comment)) {
            throw new RuntimeException("请输入审批意见");
        }
        if (policy.requireSignature && isBlank(signature)) {
            throw new RuntimeException("请完成审批签名");
        }
    }

    private TaskApprovalPolicy getTaskApprovalPolicy(Task task) {
        return getTaskApprovalPolicy(task, null, null);
    }

    private TaskApprovalPolicy getTaskApprovalPolicy(Task task, FlowModel flowModel, FlowNode flowNode) {
        TaskApprovalPolicy policy = TaskApprovalPolicy.defaultPolicy();
        FlowNode effectiveFlowNode = flowNode != null ? flowNode : getFlowNode(task);
        if (effectiveFlowNode != null) {
            applyBpmnPolicy(policy, effectiveFlowNode);
        }

        FlowModel effectiveFlowModel = flowModel != null
                ? flowModel
                : flowModelService.getModelByKey(resolveProcessDefinitionKey(task.getProcessDefinitionId(), null));
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

    private FlowNode getFlowNode(Task task) {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        if (bpmnModel == null) {
            return null;
        }
        Process process = bpmnModel.getMainProcess();
        if (process == null) {
            return null;
        }
        FlowElement element = process.getFlowElement(task.getTaskDefinitionKey());
        return element instanceof FlowNode ? (FlowNode) element : null;
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

    private void validateRequiredVariables(Task task, Map<String, Object> variables) {
        FlowNode flowNode = getFlowNode(task);
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

    private boolean isEmptyVariableValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        if (value instanceof Collection<?>) {
            return ((Collection<?>) value).isEmpty();
        }
        return false;
    }

    private Boolean readBooleanProcessAttribute(String processDefinitionId, String name) {
        return parseBooleanValue(readProcessStringAttribute(processDefinitionId, name));
    }

    private String readProcessStringAttribute(String processDefinitionId, String name) {
        Process process = getBpmnProcess(processDefinitionId);
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
        if (bpmnModel == null) {
            return null;
        }
        return bpmnModel.getMainProcess();
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

    private String resolveReturnTarget(Task task, String requestedTargetActivityId) {
        String previous = findPreviousUserTaskActivityId(task);
        if (isBlank(requestedTargetActivityId)) {
            return previous;
        }
        String target = requestedTargetActivityId.trim();
        if (Objects.equals(target, task.getTaskDefinitionKey())) {
            throw new RuntimeException("不能退回当前任务节点");
        }
        FlowModel model = flowModelService.getModelByKey(
                resolveProcessDefinitionKey(task.getProcessDefinitionId(), null));
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

    private boolean isProcessStarterTask(Task task, String userId) {
        if (task == null || isBlank(userId)) {
            return false;
        }
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            return false;
        }
        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceIdAndTenantId(
                task.getProcessInstanceId(), tenantId);
        return business != null
                && !isBlank(business.getApplyUserId())
                && Objects.equals(business.getApplyUserId(), userId.trim());
    }

    private void validateReassignTarget(String targetUserId) {
        if (flowOrgIntegrationService == null
                || !flowOrgIntegrationService.isUserAvailableForTenant(targetUserId, SessionHelper.getTenantId())) {
            throw new RuntimeException("新处理人不存在、已停用或不属于当前租户");
        }
    }

    /**
     * return 接口位于 @IgnoreTenant 的 Flow 服务边界，必须在本地表和流程实例
     * 两侧再次锁定并校验租户，不能只依赖调用方传入的 taskId。
     */
    private void assertTaskTenantForAction(String taskId) {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new RuntimeException("FLOW_TASK_TENANT_REQUIRED");
        }
        FlowTask localTask = baseMapper.selectByTaskIdForUpdateAndTenant(taskId, tenantId);
        Task flowableTask = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (localTask == null || (localTask.getTenantId() != null
                && !tenantId.equals(localTask.getTenantId()))
                || flowableTask == null) {
            throw new RuntimeException("FLOW_TASK_TENANT_MISMATCH");
        }
        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceIdAndTenantIdForUpdate(
                flowableTask.getProcessInstanceId(), tenantId);
        if (business == null || !Objects.equals(business.getProcessInstanceId(), flowableTask.getProcessInstanceId())) {
            throw new RuntimeException("FLOW_TASK_TENANT_MISMATCH");
        }
    }

    /**
     * 委派和任务终结属于高影响写操作，不能只依赖 Flowable taskId 存在性。
     * 先验证租户/业务归属，再验证操作者是当前处理人、拥有者或流程发起人。
     */
    private void assertTaskMutationActor(String taskId, String userId, boolean allowInitiator) {
        if (isBlank(userId)) {
            throw new RuntimeException("FLOW_TASK_ACTOR_REQUIRED");
        }
        assertTaskTenantForAction(taskId);
        Long tenantId = SessionHelper.getTenantId();
        FlowTask localTask = baseMapper.selectByTaskIdForUpdateAndTenant(taskId, tenantId);
        boolean participant = Objects.equals(userId, localTask.getAssignee())
                || Objects.equals(userId, localTask.getOwner())
                || (allowInitiator && Objects.equals(userId, localTask.getStartUserId()));
        if (!participant) {
            throw new RuntimeException("FLOW_TASK_ACTOR_MISMATCH");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Long requireTenantId() {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_TASK_TENANT_REQUIRED");
        }
        return tenantId;
    }

    /**
     * 详情/审批历史仍需兼容历史业务数据中的账号或姓名；列表页已由 Mapper SQL 直接关联用户，
     * 不会走这里的逐条组织服务查询。
     */
    private String resolveUserDisplayName(String userId, String fallback,
                                          Map<String, Map<String, Object>> userInfoCache) {
        if (!isBlank(userId) && userInfoCache != null && userInfoCache.containsKey(userId.trim())) {
            Map<String, Object> userInfo = userInfoCache.get(userId.trim());
            if (userInfo != null) {
                String name = firstNonBlank(
                        textValue(userInfo.get("realName")),
                        textValue(userInfo.get("name")),
                        textValue(userInfo.get("nickname")));
                if (!isBlank(name)) {
                    return name;
                }
            }
        }
        return isBlank(fallback) ? userId : fallback.trim();
    }

    private String resolveUserDisplayName(String userId, String fallback) {
        if (!isBlank(userId) && flowOrgIntegrationService != null) {
            try {
                Map<String, Object> userInfo = flowOrgIntegrationService.getUserInfo(userId.trim());
                if (userInfo != null) {
                    String name = firstNonBlank(
                            textValue(userInfo.get("realName")),
                            textValue(userInfo.get("name")),
                            textValue(userInfo.get("nickname")));
                    if (!isBlank(name)) {
                        return name;
                    }
                }
            } catch (Exception e) {
                log.debug("反查任务用户姓名失败: userId={}", userId, e);
            }
        }
        return isBlank(fallback) ? userId : fallback.trim();
    }

    @Override
    public TaskFormInfo getTaskFormInfo(String taskId) {
        FlowTask visibleTask = flowAccessGuard.requireTaskVisible(taskId);
        // 1. 获取任务信息
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在：" + taskId);
        }

        TaskFormInfo formInfo = new TaskFormInfo();
        formInfo.setTaskId(taskId);
        formInfo.setTaskName(task.getName());
        formInfo.setTaskDefKey(task.getTaskDefinitionKey());
        formInfo.setProcessInstanceId(task.getProcessInstanceId());

        // 2. 获取流程定义Key
        String processDefKey = resolveProcessDefinitionKey(task.getProcessDefinitionId(), null);
        formInfo.setProcessDefKey(processDefKey);

        // 3. 获取流程变量
        Map<String, Object> variables = taskService.getVariables(taskId);
        formInfo.setVariables(variables);

        // 4. 获取业务信息
        Long taskTenantId = visibleTask != null && visibleTask.getTenantId() != null
                ? visibleTask.getTenantId() : SessionHelper.getTenantId();
        FlowBusiness business = taskTenantId == null
                ? null
                : flowBusinessMapper.selectByProcessInstanceIdAndTenantId(
                        task.getProcessInstanceId(), taskTenantId);
        if (business != null) {
            formInfo.setBusinessKey(business.getBusinessKey());
            formInfo.setTitle(business.getTitle());
            formInfo.setStartUserId(business.getApplyUserId());
            formInfo.setStartUserName(resolveUserDisplayName(
                    business.getApplyUserId(), business.getApplyUserName()));
            formInfo.setStartDeptId(business.getApplyDeptId());
            formInfo.setStartDeptName(business.getApplyDeptName());
        }

        // 5. 读取流程模型和 BPMN 节点表单配置，同一次请求内复用给审批策略解析。
        FlowModel flowModel = !isBlank(processDefKey) ? flowModelService.getModelByKey(processDefKey) : null;
        FlowNode flowNode = resolveFormFlowNode(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
        applyFormConfiguration(formInfo, flowModel, flowNode);
        hydrateFormInstanceSnapshotIfNecessary(formInfo, task.getProcessInstanceId(), taskTenantId);

        // 6. 获取节点办理配置（BPMN扩展属性 + 节点配置表，配置表优先）
        TaskApprovalPolicy policy = getTaskApprovalPolicy(task, flowModel, flowNode);
        formInfo.setAllowApprove(policy.allowApprove);
        formInfo.setAllowReject(policy.allowReject);
        formInfo.setAllowDelegate(policy.allowDelegate);
        formInfo.setAllowReturn(policy.allowReturn);
        formInfo.setAllowMultiReturn(flowModel != null && Boolean.TRUE.equals(flowModel.getAllowMultiReturn()));
        formInfo.setReturnTargets(buildReturnTargets(task, formInfo.getAllowMultiReturn()));
        populateDirectSendInfo(formInfo, task);
        formInfo.setAllowTerminate(policy.allowTerminate);
        formInfo.setRequireSignature(policy.requireSignature);
        formInfo.setRequireComment(policy.requireComment);
        formInfo.setAllowRejectToStart(policy.allowRejectToStart);
        applyNodePolicy(formInfo, flowNode);

        log.info("获取任务表单信息：taskId={}, formType={}, formKey={}",
                taskId, formInfo.getFormType(), formInfo.getFormKey());

        return formInfo;
    }

    private List<TaskFormInfo.ReturnTarget> buildReturnTargets(Task task, Boolean allowMultiReturn) {
        if (!Boolean.TRUE.equals(allowMultiReturn)) {
            return Collections.emptyList();
        }
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType("userTask")
                .finished()
                .orderByHistoricActivityInstanceEndTime()
                .desc()
                .list()
                .stream()
                .filter(activity -> !Objects.equals(activity.getActivityId(), task.getTaskDefinitionKey()))
                .collect(Collectors.toMap(HistoricActivityInstance::getActivityId,
                        activity -> {
                            TaskFormInfo.ReturnTarget target = new TaskFormInfo.ReturnTarget();
                            target.setActivityId(activity.getActivityId());
                            target.setActivityName(activity.getActivityName());
                            target.setEndTime(activity.getEndTime());
                            return target;
                        }, (first, ignored) -> first, LinkedHashMap::new))
                .values().stream().toList();
    }

    private void populateDirectSendInfo(TaskFormInfo formInfo, Task task) {
        Object source = runtimeService.getVariable(task.getProcessInstanceId(), RETURN_SOURCE_ACTIVITY_ID);
        Object target = runtimeService.getVariable(task.getProcessInstanceId(), RETURN_TARGET_ACTIVITY_ID);
        Object returnToStartPending = runtimeService.getVariable(task.getProcessInstanceId(), RETURN_TO_START_PENDING);
        boolean returnedToHistoricalNode = target != null
                && Objects.equals(String.valueOf(target), task.getTaskDefinitionKey());
        boolean returnedToStart = Boolean.TRUE.equals(readBoolean(returnToStartPending));
        if (source == null || (!returnedToHistoricalNode && !returnedToStart)) {
            formInfo.setAllowDirectSend(false);
            return;
        }
        String sourceId = String.valueOf(source);
        FlowElement element = resolveFormFlowNode(task.getProcessDefinitionId(), sourceId);
        if (!(element instanceof UserTask)) {
            formInfo.setAllowDirectSend(false);
            return;
        }
        if (returnedToStart && !isProcessStarterTask(task, task.getAssignee())) {
            formInfo.setAllowDirectSend(false);
            return;
        }
        formInfo.setAllowDirectSend(true);
        formInfo.setReturnSourceActivityId(sourceId);
        formInfo.setReturnSourceActivityName(element.getName());
    }

    @Override
    public TaskFormInfo getProcessFormInfo(String processInstanceId, String businessKey, String processDefKey,
                                           String taskId, String taskDefKey) {
        if (!isBlank(taskId)) {
            flowAccessGuard.requireTaskVisible(taskId);
        } else if (!isBlank(processInstanceId)) {
            flowAccessGuard.requireProcessVisible(processInstanceId);
        }
        if (!isBlank(taskId)) {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task != null) {
                return getTaskFormInfo(taskId);
            }
        }

        Long tenantId = requireTenantId();
        FlowTask sourceTask = null;
        if (!isBlank(taskId)) {
            sourceTask = getBaseMapper().selectByIdOrTaskIdAndTenant(taskId, tenantId);
        }

        FlowBusiness business = resolveFlowBusiness(processInstanceId, businessKey);
        String effectiveProcessInstanceId = firstNonBlank(processInstanceId,
                business != null ? business.getProcessInstanceId() : null,
                sourceTask != null ? sourceTask.getProcessInstanceId() : null);
        String effectiveBusinessKey = firstNonBlank(businessKey,
                business != null ? business.getBusinessKey() : null,
                sourceTask != null ? sourceTask.getBusinessKey() : null);
        String rawProcessDefKey = firstNonBlank(processDefKey,
                business != null ? business.getProcessDefKey() : null,
                sourceTask != null ? sourceTask.getProcessDefKey() : null);
        String processDefinitionId = firstNonBlank(
                sourceTask != null ? sourceTask.getProcessDefId() : null,
                business != null ? business.getProcessDefId() : null,
                resolveProcessDefinitionId(effectiveProcessInstanceId, rawProcessDefKey));
        String effectiveProcessDefKey = resolveProcessDefinitionKey(processDefinitionId, rawProcessDefKey);
        String effectiveTaskDefKey = firstNonBlank(
                taskDefKey,
                sourceTask != null ? sourceTask.getTaskDefKey() : null,
                findActiveTaskDefinitionKey(effectiveProcessInstanceId),
                findFirstHistoricTaskDefinitionKey(effectiveProcessInstanceId));

        TaskFormInfo formInfo = new TaskFormInfo();
        formInfo.setTaskId(taskId);
        formInfo.setTaskName(sourceTask != null ? sourceTask.getTaskName() : null);
        formInfo.setTaskDefKey(effectiveTaskDefKey);
        formInfo.setProcessInstanceId(effectiveProcessInstanceId);
        formInfo.setProcessDefKey(effectiveProcessDefKey);
        formInfo.setBusinessKey(effectiveBusinessKey);
        formInfo.setTitle(business != null ? business.getTitle() : sourceTask != null ? sourceTask.getTitle() : null);
        if (business != null) {
            formInfo.setStartUserId(business.getApplyUserId());
            formInfo.setStartUserName(resolveUserDisplayName(
                    business.getApplyUserId(), business.getApplyUserName()));
            formInfo.setStartDeptId(business.getApplyDeptId());
            formInfo.setStartDeptName(business.getApplyDeptName());
        }

        Map<String, Object> variables = readProcessVariablesForForm(effectiveProcessInstanceId);
        if (!isBlank(effectiveBusinessKey)) {
            variables.putIfAbsent("businessKey", effectiveBusinessKey);
        }
        formInfo.setVariables(variables);
        applyFormConfiguration(formInfo, processDefinitionId, effectiveProcessDefKey, effectiveTaskDefKey);
        hydrateFormInstanceSnapshotIfNecessary(formInfo, effectiveProcessInstanceId, tenantId);
        formInfo.setAllowApprove(false);
        formInfo.setAllowReject(false);
        formInfo.setAllowDelegate(false);
        formInfo.setAllowReturn(false);
        formInfo.setAllowTerminate(false);
        formInfo.setRequireComment(false);
        formInfo.setRequireSignature(false);
        formInfo.setAllowRejectToStart(false);
        return formInfo;
    }

    private String findActiveTaskDefinitionKey(String processInstanceId) {
        if (isBlank(processInstanceId)) {
            return null;
        }
        try {
            Task task = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .orderByTaskCreateTime()
                    .asc()
                    .list()
                    .stream()
                    .findFirst()
                    .orElse(null);
            return task == null ? null : task.getTaskDefinitionKey();
        } catch (Exception e) {
            log.debug("读取运行中任务定义Key失败: processInstanceId={}", processInstanceId);
            return null;
        }
    }

    private FlowBusiness resolveFlowBusiness(String processInstanceId, String businessKey) {
        Long tenantId = requireTenantId();
        FlowBusiness business = null;
        if (!isBlank(processInstanceId)) {
            business = flowBusinessMapper.selectByProcessInstanceIdAndTenantId(processInstanceId, tenantId);
        }
        if (business == null && !isBlank(businessKey)) {
            business = flowBusinessMapper.selectByBusinessKeyAndTenantId(tenantId, businessKey);
        }
        return business;
    }

    private String resolveProcessDefinitionId(String processInstanceId, String processDefKey) {
        if (!isBlank(processInstanceId)) {
            try {
                ProcessInstance runtimeInstance = runtimeService.createProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();
                if (runtimeInstance != null) {
                    return runtimeInstance.getProcessDefinitionId();
                }
            } catch (Exception e) {
                log.debug("从运行实例解析流程定义失败: processInstanceId={}", processInstanceId);
            }
            try {
                HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();
                if (historicInstance != null) {
                    return historicInstance.getProcessDefinitionId();
                }
            } catch (Exception e) {
                log.debug("从历史实例解析流程定义失败: processInstanceId={}", processInstanceId);
            }
        }
        if (!isBlank(processDefKey)) {
            try {
                ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionKey(processDefKey)
                        .latestVersion()
                        .singleResult();
                return definition != null ? definition.getId() : null;
            } catch (Exception e) {
                log.debug("从流程定义Key解析最新流程定义失败: processDefKey={}", processDefKey);
            }
        }
        return null;
    }

    private String resolveProcessDefinitionKey(String processDefinitionId, String fallbackProcessDefKey) {
        String key = null;
        if (!isBlank(processDefinitionId)) {
            if (processDefinitionId.contains(":")) {
                key = extractProcessKey(processDefinitionId);
            }
            if (isBlank(key) || Objects.equals(key, processDefinitionId)) {
                try {
                    ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                            .processDefinitionId(processDefinitionId)
                            .singleResult();
                    if (definition != null) {
                        key = definition.getKey();
                    }
                } catch (Exception e) {
                    log.debug("从流程定义ID解析流程定义Key失败: processDefinitionId={}", processDefinitionId);
                }
            }
            if (isBlank(key) || Objects.equals(key, processDefinitionId)) {
                try {
                    BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
                    if (bpmnModel != null && bpmnModel.getMainProcess() != null) {
                        key = bpmnModel.getMainProcess().getId();
                    }
                } catch (Exception e) {
                    log.debug("从BPMN模型解析流程定义Key失败: processDefinitionId={}", processDefinitionId);
                }
            }
        }
        if (!isBlank(key) && !Objects.equals(key, processDefinitionId)) {
            return key;
        }
        if (!isBlank(fallbackProcessDefKey) && fallbackProcessDefKey.contains(":")) {
            return extractProcessKey(fallbackProcessDefKey);
        }
        return fallbackProcessDefKey;
    }

    private Map<String, Object> readProcessVariablesForForm(String processInstanceId) {
        Map<String, Object> variables = new HashMap<>();
        if (isBlank(processInstanceId)) {
            return variables;
        }
        try {
            Map<String, Object> runtimeVariables = runtimeService.getVariables(processInstanceId);
            if (runtimeVariables != null) {
                variables.putAll(runtimeVariables);
            }
        } catch (Exception e) {
            log.debug("读取运行流程变量失败，继续读取历史变量: processInstanceId={}", processInstanceId);
        }
        try {
            List<HistoricVariableInstance> historicVariables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .list();
            if (historicVariables != null) {
                for (HistoricVariableInstance variable : historicVariables) {
                    if (variable != null && variable.getVariableName() != null) {
                        variables.putIfAbsent(variable.getVariableName(), variable.getValue());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("读取历史流程变量失败: processInstanceId={}", processInstanceId, e);
        }
        return variables;
    }

    private String findFirstHistoricTaskDefinitionKey(String processInstanceId) {
        if (isBlank(processInstanceId)) {
            return null;
        }
        try {
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByHistoricTaskInstanceStartTime()
                    .asc()
                    .list();
            if (historicTasks != null && !historicTasks.isEmpty()) {
                return historicTasks.get(0).getTaskDefinitionKey();
            }
        } catch (Exception e) {
            log.debug("读取历史任务定义Key失败: processInstanceId={}", processInstanceId);
        }
        return null;
    }

    private void applyFormConfiguration(TaskFormInfo formInfo, String processDefinitionId, String processDefKey,
                                        String taskDefKey) {
        FlowModel flowModel = !isBlank(processDefKey) ? flowModelService.getModelByKey(processDefKey) : null;
        FlowNode flowNode = resolveFormFlowNode(processDefinitionId, taskDefKey);
        applyFormConfiguration(formInfo, flowModel, flowNode);
    }

    private void applyFormConfiguration(TaskFormInfo formInfo, FlowModel flowModel, FlowNode flowNode) {
        applyNodePolicy(formInfo, flowNode);
        NodeFormConfig nodeForm = readNodeFormConfig(flowNode);
        formInfo.setFormFieldPermissions(nodeForm.formFieldPermissions);

        if (isBusinessNodeForm(flowModel, nodeForm)) {
            applyBusinessNodeFormConfiguration(formInfo, flowModel, nodeForm);
            hydrateResolvedFormSchema(formInfo, nodeForm);
            if (shouldExposeAsDynamicForm(formInfo, flowModel, nodeForm)) {
                formInfo.setFormType("dynamic");
            }
        } else if (!isBlank(nodeForm.formUrl)) {
            formInfo.setFormType("external");
            formInfo.setFormUrl(nodeForm.formUrl);
            formInfo.setFormTarget(!isBlank(nodeForm.formTarget) ? nodeForm.formTarget : "modal");
            applyNodeFormReference(formInfo, nodeForm, Map.of());
        } else if (!isBlank(nodeForm.formKey) || !isBlank(nodeForm.formJson)) {
            formInfo.setFormType("dynamic");
            formInfo.setFormKey(nodeForm.formKey);
            formInfo.setFormJson(resolveFormJson(nodeForm.formKey, nodeForm.formJson));
            applyNodeFormReference(formInfo, nodeForm, Map.of());
        } else if (flowModel != null) {
            applyModelFormConfiguration(formInfo, flowModel);
            hydrateResolvedFormSchema(formInfo, nodeForm);
        } else if (!isBlank(formInfo.getFormJson()) || !isBlank(formInfo.getFormKey())) {
            formInfo.setFormType("dynamic");
            formInfo.setFormJson(resolveFormJson(formInfo.getFormKey(), formInfo.getFormJson()));
        } else {
            formInfo.setFormType("none");
        }
    }

    private void applyModelFormConfiguration(TaskFormInfo formInfo, FlowModel flowModel) {
        if (flowModel == null) {
            formInfo.setFormType("none");
            return;
        }
        String formType = flowModel.getFormType();
        formInfo.setFormType(formType);
        if ("dynamic".equals(formType)) {
            formInfo.setFormKey(flowModel.getFormId());
            formInfo.setFormJson(resolveModelFormJson(flowModel.getFormId(), flowModel.getFormJson()));
            return;
        }
        if ("external".equals(formType)) {
            formInfo.setFormUrl(flowModel.getFormId());
            formInfo.setFormTarget("modal");
            return;
        }
        if (FORM_TYPE_BUSINESS.equals(formType)) {
            Map<String, Object> formRef = readBusinessGlobalFormRef(flowModel.getFormJson());
            formInfo.setFormType(FORM_TYPE_BUSINESS);
            formInfo.setObjectCode(textValue(formRef.get("objectCode")));
            formInfo.setFormKey(firstNonBlank(textValue(formRef.get("formKey")), flowModel.getFormId()));
            formInfo.setFormMode(firstNonBlank(
                    textValue(formRef.get("formMode")),
                    textValue(formRef.get("type"))));
            formInfo.setFormName(textValue(formRef.get("formName")));
            formInfo.setProviderKey(textValue(formRef.get("providerKey")));
            formInfo.setFormUrl(textValue(formRef.get("formUrl")));
            formInfo.setViewKey(firstNonBlank(textValue(formRef.get("viewKey")), "default"));
            formInfo.setFormRef(new LinkedHashMap<>(formRef));
            formInfo.setFormTarget("modal");
            formInfo.setFormJson(flowModel.getFormJson());
        }
    }

    private void applyBusinessNodeFormConfiguration(TaskFormInfo formInfo, FlowModel flowModel,
                                                    NodeFormConfig nodeForm) {
        formInfo.setFormType(FORM_TYPE_BUSINESS);
        Map<String, Object> formRef = flowModel == null ? Map.of() : readBusinessGlobalFormRef(flowModel.getFormJson());
        Map<String, Object> mergedFormRef = mergeFormRef(formRef, nodeForm.formRef);
        formInfo.setFormKey(firstNonBlank(
                nodeForm.formKey,
                textValue(mergedFormRef.get("formKey")),
                flowModel == null ? null : flowModel.getFormId()));
        formInfo.setFormMode(firstNonBlank(
                nodeForm.formMode,
                textValue(mergedFormRef.get("formMode")),
                textValue(mergedFormRef.get("type"))));
        formInfo.setFormName(firstNonBlank(nodeForm.formName, textValue(mergedFormRef.get("formName"))));
        formInfo.setProviderKey(firstNonBlank(nodeForm.providerKey, textValue(mergedFormRef.get("providerKey"))));
        formInfo.setFormUrl(firstNonBlank(nodeForm.formUrl, textValue(mergedFormRef.get("formUrl"))));
        formInfo.setViewKey(firstNonBlank(nodeForm.viewKey, textValue(mergedFormRef.get("viewKey")), "default"));
        putIfPresent(mergedFormRef, "formKey", formInfo.getFormKey());
        putIfPresent(mergedFormRef, "formMode", formInfo.getFormMode());
        putIfPresent(mergedFormRef, "type", formInfo.getFormMode());
        putIfPresent(mergedFormRef, "formName", formInfo.getFormName());
        putIfPresent(mergedFormRef, "providerKey", formInfo.getProviderKey());
        putIfPresent(mergedFormRef, "formUrl", formInfo.getFormUrl());
        putIfPresent(mergedFormRef, "viewKey", formInfo.getViewKey());
        formInfo.setFormRef(mergedFormRef);
        formInfo.setFormTarget("modal");
        formInfo.setFormJson(flowModel == null ? null : flowModel.getFormJson());
        if (isBlank(formInfo.getObjectCode())) {
            formInfo.setObjectCode(firstNonBlank(
                    textValue(mergedFormRef.get("objectCode")),
                    flowModel == null ? null : textValue(readBusinessGlobalFormRef(flowModel.getFormJson()).get("objectCode"))));
        }
    }

    private boolean isBusinessNodeForm(FlowModel flowModel, NodeFormConfig nodeForm) {
        String normalizedMode = normalizeFormMode(nodeForm == null ? null : nodeForm.formMode);
        if ("BUSINESS_CODE_FORM".equals(normalizedMode)) {
            return true;
        }
        if ("EXTERNAL".equals(normalizedMode)) {
            return false;
        }
        if ("BUSINESS_OBJECT_FORM".equals(normalizedMode)) {
            return !isBlank(nodeForm.providerKey)
                    || (flowModel != null && FORM_TYPE_BUSINESS.equalsIgnoreCase(flowModel.getFormType()));
        }
        return flowModel != null && FORM_TYPE_BUSINESS.equalsIgnoreCase(flowModel.getFormType());
    }

    private void hydrateResolvedFormSchema(TaskFormInfo formInfo, NodeFormConfig nodeForm) {
        if (formInfo == null) {
            return;
        }
        if (looksLikeFormSchema(formInfo.getFormJson())) {
            return;
        }
        String schema = resolveFormJson(
                formInfo.getFormKey(),
                nodeForm == null ? formInfo.getFormJson() : firstNonBlank(nodeForm.formJson, formInfo.getFormJson()));
        if (looksLikeFormSchema(schema)) {
            formInfo.setFormJson(schema);
        }
    }

    private boolean shouldExposeAsDynamicForm(TaskFormInfo formInfo, FlowModel flowModel, NodeFormConfig nodeForm) {
        if (!looksLikeFormSchema(formInfo.getFormJson())) {
            return false;
        }
        if (!isBlank(formInfo.getProviderKey()) || !isBlank(nodeForm == null ? null : nodeForm.providerKey)) {
            return false;
        }
        if (flowModel != null && FORM_TYPE_BUSINESS.equalsIgnoreCase(flowModel.getFormType())) {
            return false;
        }
        return true;
    }

    private boolean looksLikeFormSchema(String formJson) {
        if (isBlank(formJson)) {
            return false;
        }
        String text = formJson.trim();
        if (text.startsWith("[")) {
            return true;
        }
        if (!text.startsWith("{")) {
            return false;
        }
        return text.contains("\"field\"") || text.contains("\"rule\"") || text.contains("\"rules\"")
                || text.contains("\"schema\"") || text.contains("\"children\"");
    }

    private String normalizeFormMode(String value) {
        String mode = textValue(value);
        return mode == null ? null : mode.toUpperCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readBusinessGlobalFormRef(String formJson) {
        if (isBlank(formJson)) {
            return Map.of();
        }
        try {
            Map<String, Object> root = OBJECT_MAPPER.readValue(formJson, Map.class);
            Object nested = root.get("formRef");
            if (nested instanceof Map<?, ?> nestedMap) {
                Map<String, Object> merged = new LinkedHashMap<>((Map<String, Object>) nestedMap);
                root.forEach(merged::putIfAbsent);
                return merged;
            }
            return root;
        } catch (Exception e) {
            log.warn("解析流程全局业务表单引用失败: {}", e.getMessage());
            return Map.of();
        }
    }

    private void applyNodeFormReference(TaskFormInfo formInfo, NodeFormConfig nodeForm, Map<String, Object> fallbackRef) {
        Map<String, Object> mergedFormRef = mergeFormRef(fallbackRef, nodeForm.formRef);
        formInfo.setFormMode(firstNonBlank(nodeForm.formMode, textValue(mergedFormRef.get("formMode")), textValue(mergedFormRef.get("type"))));
        formInfo.setFormName(firstNonBlank(nodeForm.formName, textValue(mergedFormRef.get("formName"))));
        formInfo.setProviderKey(firstNonBlank(nodeForm.providerKey, textValue(mergedFormRef.get("providerKey"))));
        formInfo.setViewKey(firstNonBlank(nodeForm.viewKey, textValue(mergedFormRef.get("viewKey")), "default"));
        if (!mergedFormRef.isEmpty()) {
            putIfPresent(mergedFormRef, "formKey", formInfo.getFormKey());
            putIfPresent(mergedFormRef, "formMode", formInfo.getFormMode());
            putIfPresent(mergedFormRef, "type", formInfo.getFormMode());
            putIfPresent(mergedFormRef, "formName", formInfo.getFormName());
            putIfPresent(mergedFormRef, "providerKey", formInfo.getProviderKey());
            putIfPresent(mergedFormRef, "formUrl", formInfo.getFormUrl());
            putIfPresent(mergedFormRef, "viewKey", formInfo.getViewKey());
            formInfo.setFormRef(mergedFormRef);
        }
    }

    private Map<String, Object> mergeFormRef(Map<String, Object> base, Map<String, Object> overrides) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (base != null) {
            merged.putAll(base);
        }
        if (overrides != null) {
            overrides.forEach((key, value) -> {
                if (value != null && !isBlank(String.valueOf(value))) {
                    merged.put(key, value);
                }
            });
        }
        return merged;
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (target == null || value == null || isBlank(String.valueOf(value))) {
            return;
        }
        target.put(key, value);
    }

    private String textValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private FlowNode resolveFormFlowNode(String processDefinitionId, String taskDefKey) {
        if (isBlank(processDefinitionId)) {
            return null;
        }
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        if (bpmnModel == null || bpmnModel.getMainProcess() == null) {
            return null;
        }
        Process process = bpmnModel.getMainProcess();
        if (!isBlank(taskDefKey)) {
            FlowElement element = process.getFlowElement(taskDefKey);
            if (element instanceof FlowNode) {
                return (FlowNode) element;
            }
        }

        FlowNode firstUserTask = null;
        for (FlowElement element : process.getFlowElements()) {
            if (element instanceof UserTask) {
                FlowNode candidate = (FlowNode) element;
                if (firstUserTask == null) {
                    firstUserTask = candidate;
                }
                if (readNodeFormConfig(candidate).hasForm()) {
                    return candidate;
                }
            }
        }
        return firstUserTask;
    }

    private NodeFormConfig readNodeFormConfig(FlowNode flowNode) {
        NodeFormConfig config = new NodeFormConfig();
        if (flowNode == null) {
            return config;
        }
        if (flowNode instanceof UserTask) {
            config.formKey = ((UserTask) flowNode).getFormKey();
        }

        config.formUrl = flowNode.getAttributeValue(FLOWABLE_NS, "formUrl");
        config.formJson = flowNode.getAttributeValue(FLOWABLE_NS, "formJson");
        config.formTarget = flowNode.getAttributeValue(FLOWABLE_NS, "formTarget");
        config.formName = flowNode.getAttributeValue(FLOWABLE_NS, "formName");
        config.providerKey = flowNode.getAttributeValue(FLOWABLE_NS, "providerKey");
        config.viewKey = flowNode.getAttributeValue(FLOWABLE_NS, "viewKey");
        config.formMode = firstNonBlank(
                flowNode.getAttributeValue(FLOWABLE_NS, "formMode"),
                flowNode.getAttributeValue(FLOWABLE_NS, "formType"),
                flowNode.getAttributeValue(FLOWABLE_NS, "type"));
        config.formRef = readBusinessGlobalFormRef(flowNode.getAttributeValue(FLOWABLE_NS, "formRef"));
        config.formFieldPermissions = flowNode.getAttributeValue(FLOWABLE_NS, "formFieldPermissions");

        Map<String, List<ExtensionElement>> extensions = flowNode.getExtensionElements();
        if (extensions == null) {
            return config;
        }
        List<ExtensionElement> formJsonElements = extensions.get("formJson");
        if (isBlank(config.formJson) && formJsonElements != null && !formJsonElements.isEmpty()) {
            config.formJson = formJsonElements.get(0).getElementText();
        }
        List<ExtensionElement> formUrlElements = extensions.get("formUrl");
        if (isBlank(config.formUrl) && formUrlElements != null && !formUrlElements.isEmpty()) {
            config.formUrl = formUrlElements.get(0).getElementText();
        }
        List<ExtensionElement> formTargetElements = extensions.get("formTarget");
        if (isBlank(config.formTarget) && formTargetElements != null && !formTargetElements.isEmpty()) {
            config.formTarget = formTargetElements.get(0).getElementText();
        }
        List<ExtensionElement> formNameElements = extensions.get("formName");
        if (isBlank(config.formName) && formNameElements != null && !formNameElements.isEmpty()) {
            config.formName = formNameElements.get(0).getElementText();
        }
        List<ExtensionElement> providerKeyElements = extensions.get("providerKey");
        if (isBlank(config.providerKey) && providerKeyElements != null && !providerKeyElements.isEmpty()) {
            config.providerKey = providerKeyElements.get(0).getElementText();
        }
        List<ExtensionElement> viewKeyElements = extensions.get("viewKey");
        if (isBlank(config.viewKey) && viewKeyElements != null && !viewKeyElements.isEmpty()) {
            config.viewKey = viewKeyElements.get(0).getElementText();
        }
        List<ExtensionElement> formModeElements = extensions.get("formMode");
        if (isBlank(config.formMode) && formModeElements != null && !formModeElements.isEmpty()) {
            config.formMode = formModeElements.get(0).getElementText();
        }
        List<ExtensionElement> formTypeElements = extensions.get("formType");
        if (isBlank(config.formMode) && formTypeElements != null && !formTypeElements.isEmpty()) {
            config.formMode = formTypeElements.get(0).getElementText();
        }
        List<ExtensionElement> formRefElements = extensions.get("formRef");
        if ((config.formRef == null || config.formRef.isEmpty()) && formRefElements != null && !formRefElements.isEmpty()) {
            config.formRef = readBusinessGlobalFormRef(formRefElements.get(0).getElementText());
        }
        List<ExtensionElement> formFieldPermissionElements = extensions.get("formFieldPermissions");
        if (isBlank(config.formFieldPermissions) && formFieldPermissionElements != null
                && !formFieldPermissionElements.isEmpty()) {
            config.formFieldPermissions = formFieldPermissionElements.get(0).getElementText();
        }
        return config;
    }

    private void hydrateFormInstanceSnapshotIfNecessary(TaskFormInfo formInfo, String processInstanceId) {
        hydrateFormInstanceSnapshotIfNecessary(formInfo, processInstanceId, requireTenantId());
    }

    private void hydrateFormInstanceSnapshotIfNecessary(TaskFormInfo formInfo, String processInstanceId,
                                                       Long tenantId) {
        if (formInfo == null || FORM_TYPE_BUSINESS.equalsIgnoreCase(formInfo.getFormType())) {
            return;
        }
        if (!"dynamic".equalsIgnoreCase(formInfo.getFormType())
                && isBlank(formInfo.getFormKey())
                && isBlank(formInfo.getFormJson())) {
            return;
        }
        hydrateFormInstanceSnapshot(formInfo, processInstanceId, tenantId);
    }

    private void hydrateFormInstanceSnapshot(TaskFormInfo formInfo, String processInstanceId, Long tenantId) {
        if (flowFormInstanceMapper == null || processInstanceId == null || processInstanceId.isEmpty()
                || tenantId == null || tenantId <= 0) {
            return;
        }
        try {
            FlowFormInstance instance = flowFormInstanceMapper.selectByProcessInstanceIdAndTenantId(
                    processInstanceId, tenantId);
            if (instance == null) {
                return;
            }
            formInfo.setFormInstanceId(instance.getId());
            formInfo.setSchemaSnapshot(instance.getSchemaSnapshot());
            formInfo.setFormData(instance.getFormData());
            formInfo.setDataMode(instance.getDataMode());
            formInfo.setObjectCode(instance.getObjectCode());
            formInfo.setRecordId(instance.getRecordId());
            if (formInfo.getFormJson() == null || formInfo.getFormJson().isEmpty()) {
                formInfo.setFormJson(instance.getSchemaSnapshot());
            }
            if (formInfo.getFormKey() == null || formInfo.getFormKey().isEmpty()) {
                formInfo.setFormKey(instance.getFormKey());
            }
        } catch (Exception e) {
            log.warn("加载流程表单实例快照失败: processInstanceId={}", processInstanceId, e);
        }
    }

    private String resolveFormJson(String formKey, String inlineFormJson) {
        if (inlineFormJson != null && !inlineFormJson.isEmpty()) {
            return inlineFormJson;
        }
        if (formKey == null || formKey.isEmpty() || flowFormService == null) {
            return inlineFormJson;
        }
        try {
            return flowFormService.getFormSchema(formKey);
        } catch (Exception e) {
            log.warn("根据 formKey 获取动态表单失败：formKey={}", formKey, e);
            return inlineFormJson;
        }
    }

    private String resolveModelFormJson(String formId, String inlineFormJson) {
        if (inlineFormJson != null && !inlineFormJson.isEmpty()) {
            return inlineFormJson;
        }
        if (formId == null || formId.isEmpty() || flowFormService == null) {
            return inlineFormJson;
        }
        try {
            FlowForm form = flowFormService.getById(Long.valueOf(formId));
            return form != null ? form.getFormSchema() : inlineFormJson;
        } catch (NumberFormatException e) {
            return resolveFormJson(formId, inlineFormJson);
        } catch (Exception e) {
            log.warn("根据 formId 获取模型动态表单失败：formId={}", formId, e);
            return inlineFormJson;
        }
    }

    /**
     * 获取流程审批时间轴
     */
    @Override
    public List<Map<String, Object>> getProcessHistory(String processInstanceId) {
        FlowHistoryPageVO page = getProcessHistoryPage(processInstanceId, 1, MAX_DETAIL_HISTORY_ITEMS);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FlowHistoryItemVO item : page.getRecords()) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("taskId", item.getTaskId());
            node.put("taskName", item.getTaskName());
            node.put("assigneeName", item.getAssigneeName());
            node.put("assigneeId", item.getAssigneeId());
            node.put("action", item.getAction());
            node.put("comment", item.getComment());
            node.put("signature", item.getSignature());
            node.put("approvalPointResults", item.getApprovalPointResults());
            node.put("createTime", item.getCreateTime());
            node.put("completeTime", item.getCompleteTime());
            result.add(node);
        }
        return result;
    }

    @Override
    public FlowHistoryPageVO getProcessHistoryPage(String processInstanceId, Integer pageNum, Integer pageSize) {
        FlowBusiness business = flowAccessGuard.requireProcessVisible(processInstanceId);
        Long tenantId = flowAccessGuard.requireTenant();
        long safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, MAX_DETAIL_HISTORY_ITEMS);
        IPage<FlowTask> taskPage = baseMapper.selectHistoryTasks(
                new Page<>(safePageNum, safePageSize), processInstanceId, tenantId);
        List<FlowTask> tasks = taskPage.getRecords();

        // 一次批量读取审批人，避免长流程历史逐任务回查组织服务。
        Map<String, Map<String, Object>> userInfoCache = new HashMap<>();
        Set<String> userIds = new LinkedHashSet<>();
        if (business != null && !isBlank(business.getApplyUserId())) {
            userIds.add(business.getApplyUserId());
        }
        tasks.stream()
                .map(FlowTask::getAssignee)
                .filter(id -> !isBlank(id))
                .forEach(userIds::add);
        if (flowOrgIntegrationService != null && !userIds.isEmpty()) {
            Map<String, Map<String, Object>> loaded =
                    flowOrgIntegrationService.getUserInfoBatch(new ArrayList<>(userIds));
            if (loaded != null) {
                userInfoCache.putAll(loaded);
            }
            userIds.forEach(id -> userInfoCache.putIfAbsent(id, Collections.emptyMap()));
        }

        List<FlowHistoryItemVO> records = new ArrayList<>();
        if (business != null && safePageNum == 1) {
            FlowHistoryItemVO startNode = new FlowHistoryItemVO();
            startNode.setTaskName("发起流程");
            startNode.setAssigneeName(resolveUserDisplayName(
                    business.getApplyUserId(), business.getApplyUserName(), userInfoCache));
            startNode.setAssigneeId(business.getApplyUserId());
            startNode.setAction("start");
            startNode.setComment("");
            String startTime = business.getApplyTime() != null
                    ? business.getApplyTime().toString() : business.getCreateTime() != null
                    ? business.getCreateTime().toString() : null;
            startNode.setCreateTime(startTime);
            startNode.setCompleteTime(startTime);
            records.add(startNode);
        }

        // 加入每个任务节点
        for (FlowTask task : tasks) {
            FlowHistoryItemVO node = new FlowHistoryItemVO();
            node.setTaskId(task.getTaskId());
            node.setTaskName(task.getTaskName());
            String assigneeName = resolveUserDisplayName(task.getAssignee(), task.getAssigneeName(), userInfoCache);
            node.setAssigneeName(assigneeName);
            node.setAssigneeId(task.getAssignee());
            node.setAction(FlowTaskStatus.historyActionOf(task.getStatus()));
            node.setComment(task.getComment() != null ? task.getComment() : "");
            node.setSignature(task.getSignature());
            node.setApprovalPointResults(readApprovalPointResults(task.getTaskId()));
            node.setCreateTime(task.getCreateTime() != null ? task.getCreateTime().toString() : null);
            node.setCompleteTime(task.getCompleteTime() != null ? task.getCompleteTime().toString() : null);
            records.add(node);
        }

        FlowHistoryPageVO result = new FlowHistoryPageVO();
        result.setPageNum(safePageNum);
        result.setPageSize(safePageSize);
        result.setTotal(taskPage.getTotal() + (business == null ? 0 : 1));
        // 发起节点只在第一页额外展示，不参与任务表分页游标，避免最后一页被错误标记为还有数据。
        result.setHasMore(taskPage.getCurrent() * taskPage.getSize() < taskPage.getTotal());
        result.setRecords(records);
        return result;
    }

    /**
     * 从流程定义ID提取流程Key
     */
    private String extractProcessKey(String processDefinitionId) {
        if (processDefinitionId == null) {
            return null;
        }
        // 格式：processKey:version:id
        String[] parts = processDefinitionId.split(":");
        return parts.length > 0 ? parts[0] : processDefinitionId;
    }

    private void recordTaskError(String processInstanceId, String taskId, String activityId,
                                  String activityName, String errorStage, Throwable e) {
        FlowErrorLog errorLog = new FlowErrorLog();
        errorLog.setProcessInstanceId(processInstanceId);
        errorLog.setTaskId(taskId);
        errorLog.setActivityId(activityId);
        errorLog.setActivityName(activityName);
        errorLog.setErrorStage(errorStage);
        flowErrorLogService.recordError(errorLog, e);
    }

    private static class NodeFormConfig {
        private String formKey;
        private String formJson;
        private String formUrl;
        private String formTarget;
        private String formMode;
        private String formName;
        private String providerKey;
        private String viewKey;
        private Map<String, Object> formRef = Map.of();
        private String formFieldPermissions;

        private boolean hasForm() {
            return (formKey != null && !formKey.isBlank())
                    || (formJson != null && !formJson.isBlank())
                    || (formUrl != null && !formUrl.isBlank());
        }
    }

    private static class TaskApprovalPolicy {
        private boolean allowApprove;
        private boolean allowReject;
        private boolean allowRejectToStart;
        private boolean allowDelegate;
        private boolean allowReturn;
        private boolean allowMultiReturn;
        private boolean allowTerminate;
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
            policy.requireSignature = false;
            policy.requireComment = true;
            return policy;
        }

        private boolean isAllowed(String action) {
            switch (action) {
                case ACTION_APPROVE:
                    return allowApprove;
                case ACTION_REJECT:
                    return allowReject;
                case ACTION_REJECT_TO_START:
                    return allowRejectToStart;
                case ACTION_DELEGATE:
                    return allowDelegate;
                case ACTION_RETURN:
                    return allowReturn || allowMultiReturn;
                case ACTION_TERMINATE:
                    return allowTerminate;
                default:
                    return false;
            }
        }
    }
}
