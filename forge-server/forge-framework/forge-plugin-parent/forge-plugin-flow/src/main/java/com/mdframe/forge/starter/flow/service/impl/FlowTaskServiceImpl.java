package com.mdframe.forge.starter.flow.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayAdapter;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayItem;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.flow.dto.ProcessDiagramInfo;
import com.mdframe.forge.starter.flow.dto.ProcessNodeInfo;
import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowErrorLog;
import com.mdframe.forge.starter.flow.entity.FlowForm;
import com.mdframe.forge.starter.flow.entity.FlowFormInstance;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowFormInstanceMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowErrorLogService;
import com.mdframe.forge.starter.flow.service.FlowFormService;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import com.mdframe.forge.starter.flow.service.FlowNodeConfigService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import com.mdframe.forge.starter.flow.service.FlowTaskService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
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

    private static final String FLOWABLE_NS = "http://flowable.org/bpmn";
    private static final String ACTION_APPROVE = "approve";
    private static final String ACTION_REJECT = "reject";
    private static final String ACTION_DELEGATE = "delegate";
    private static final String ACTION_RETURN = "return";
    private static final String ACTION_TERMINATE = "terminate";
    private static final String AUTO_APPROVAL_FIRST_ONLY = "firstOnly";
    private static final String AUTO_APPROVAL_CONSECUTIVE = "consecutive";
    private static final String AUTO_APPROVAL_NONE = "none";
    private static final String FORM_TYPE_BUSINESS = "business";
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
    private ISysUserService sysUserService;

    @Autowired
    private FlowErrorLogService flowErrorLogService;

    @Autowired(required = false)
    private FlowFormService flowFormService;

    @Autowired(required = false)
    private FlowFormInstanceMapper flowFormInstanceMapper;

    @Autowired(required = false)
    private FlowBusinessListDisplayAdapter flowBusinessListDisplayAdapter;

    @Override
    public IPage<FlowTask> todoTasks(Page<FlowTask> page, String userId, String title, String category, Integer status) {
        return enrichTaskPage(this.getBaseMapper().selectTodoTasks(page, userId, title, category, status));
    }

    @Override
    public IPage<FlowTask> doneTasks(Page<FlowTask> page, String userId, String title, String category, Integer status) {
        return enrichTaskPage(this.getBaseMapper().selectDoneTasks(page, userId, title, category, status));
    }

    @Override
    public IPage<FlowTask> startedTasks(Page<FlowTask> page, String userId, String title, String category, Integer status) {
        return enrichTaskPage(this.getBaseMapper().selectStartedTasks(page, userId, title, category, status));
    }

    @Override
    public IPage<FlowTask> candidateTasks(Page<FlowTask> page, String userId, String groupId, String title) {
        // 1. 查询 Flowable 候选任务
        List<Task> flowableTasks = new ArrayList<>();
        
        // 按候选人查询
        if (userId != null && !userId.isEmpty()) {
            List<Task> userCandidateTasks = taskService.createTaskQuery()
                    .taskCandidateUser(userId)
                    .taskUnassigned()
                    .list();
            flowableTasks.addAll(userCandidateTasks);
        }
        
        // 按候选组查询
        if (groupId != null && !groupId.isEmpty()) {
            List<Task> groupCandidateTasks = taskService.createTaskQuery()
                    .taskCandidateGroup(groupId)
                    .taskUnassigned()
                    .list();
            // 去重合并
            Set<String> existingTaskIds = flowableTasks.stream()
                    .map(Task::getId)
                    .collect(Collectors.toSet());
            for (Task task : groupCandidateTasks) {
                if (!existingTaskIds.contains(task.getId())) {
                    flowableTasks.add(task);
                }
            }
        }
        
        // 2. 获取任务ID列表
        if (flowableTasks.isEmpty()) {
            return page; // 返回空页
        }
        
        List<String> taskIds = flowableTasks.stream()
                .map(Task::getId)
                .collect(Collectors.toList());
        
        // 3. 从本地表查询任务详情（带分页）
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FlowTask::getTaskId, taskIds)
                .like(title != null, FlowTask::getTitle, title)
                .orderByDesc(FlowTask::getCreateTime);
        
        return enrichTaskPage(page(page, wrapper));
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
        taskService.claim(taskId, userId);
        
        FlowTask task = new FlowTask();
        task.setTaskId(taskId);
        task.setAssignee(userId);
        task.setStatus(1);
        task.setClaimTime(LocalDateTime.now());
        
        lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(task);
        log.info("签收任务：taskId={}, userId={}", taskId, userId);
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
                        String idempotencyKey, String requestDigest) {
        FlowTask storedTask = authorizeTaskAction(
                taskId, userId, tenantId, "APPROVE", idempotencyKey, requestDigest, 2);
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

        try {
            if (comment != null && !comment.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), comment);
            }

            Map<String, Object> completeVariables = mergeActionVariables(variables, true);
            completeTask(task, completeVariables);

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(2);
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
        FlowTask storedTask = authorizeTaskAction(
                taskId, userId, tenantId, "REJECT", idempotencyKey, requestDigest, 3);
        if (storedTask == null) {
            return;
        }
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateFlowableAssignee(task, userId);
        validateTaskAction(task, ACTION_REJECT, comment, signature);

        try {
            if (comment != null && !comment.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), comment);
            }

            completeTask(task, mergeActionVariables(null, false));

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(3);
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setCompleteTime(LocalDateTime.now());
            flowTask.setActionIdempotencyKey(idempotencyKey);
            flowTask.setActionRequestDigest(requestDigest);
            flowTask.setActionType(idempotencyKey == null ? null : "REJECT");
            updateTaskActionResultRequired(taskId, flowTask);

            log.info("审批驳回：taskId={}, userId={}", taskId, userId);
        } catch (Exception e) {
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), "TASK_REJECT", e);
            throw e;
        }
    }

    /**
     * 在 Flow 服务最终副作用边界重新校验租户、签收人与任务状态。
     * 返回 null 表示命中已成功的同请求幂等结果。
     */
    private FlowTask authorizeTaskAction(String taskId, String userId, Long tenantId,
                                         String actionType, String idempotencyKey,
                                         String requestDigest, int completedStatus) {
        FlowTask storedTask = baseMapper.selectByTaskIdForUpdate(taskId);
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
        if (!lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(flowTask)) {
            throw new IllegalStateException("FLOW_TASK_STATE_UPDATE_FAILED");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegate(String taskId, String userId, String targetUserId, String comment, String signature) {
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
            taskService.setAssignee(taskId, targetUserId);

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(0);
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setAssignee(targetUserId);
            flowTask.setOwner(owner);
            lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(flowTask);

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
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateTaskAction(task, ACTION_RETURN, comment, signature);

        try {
            String targetActivityId = findPreviousUserTaskActivityId(task);
            if (targetActivityId == null || targetActivityId.isEmpty()) {
                throw new RuntimeException("当前任务没有可退回的上一审批节点");
            }

            if (comment != null && !comment.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), "退回：" + comment);
            }

            List<String> currentActivityIds = runtimeService.getActiveActivityIds(task.getProcessInstanceId());
            if (currentActivityIds == null || currentActivityIds.isEmpty()) {
                currentActivityIds = Collections.singletonList(task.getTaskDefinitionKey());
            }
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(task.getProcessInstanceId())
                    .moveActivityIdsToSingleActivityId(currentActivityIds, targetActivityId)
                    .changeState();

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(7);
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setCompleteTime(LocalDateTime.now());
            lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(flowTask);

            log.info("退回任务：taskId={}, userId={}, targetActivityId={}", taskId, userId, targetActivityId);
        } catch (Exception e) {
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), "TASK_RETURN", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateTask(String taskId, String userId, String comment, String signature) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateTaskAction(task, ACTION_TERMINATE, comment, signature);

        try {
            String reason = comment != null && !comment.isBlank() ? comment : "审批人终结流程";
            taskService.addComment(taskId, task.getProcessInstanceId(), "终结流程：" + reason);
            runtimeService.deleteProcessInstance(task.getProcessInstanceId(), reason);

            FlowBusiness business = flowBusinessMapper.selectByProcessInstanceId(task.getProcessInstanceId());
            if (business != null) {
                business.setStatus("terminated");
                business.setEndTime(LocalDateTime.now());
                business.setUpdateTime(LocalDateTime.now());
                flowBusinessMapper.updateById(business);
            }

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(8);
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setCompleteTime(LocalDateTime.now());
            lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(flowTask);

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
     * Flowable 委派态任务不能直接 complete，需要先 resolve。
     */
    private void completeTask(Task task, Map<String, Object> variables) {
        String taskId = task.getId();
        if (DelegationState.PENDING.equals(task.getDelegationState())) {
            log.info("任务处于委派待解决状态，先 resolve 再 complete：taskId={}, assignee={}, owner={}",
                    taskId, task.getAssignee(), task.getOwner());
            taskService.resolveTask(taskId);
        }

        if (variables != null && !variables.isEmpty()) {
            runtimeService.setVariables(task.getProcessInstanceId(), variables);
            taskService.complete(taskId, variables);
        } else {
            taskService.complete(taskId);
        }
    }

    private void autoApproveRepeatedTasks(String processInstanceId) {
        if (isBlank(processInstanceId)) {
            return;
        }
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
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
        flowTask.setStatus(2);
        flowTask.setComment(comment);
        flowTask.setCompleteTime(LocalDateTime.now());
        lambdaUpdate().eq(FlowTask::getTaskId, task.getId()).update(flowTask);

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
    public void withdraw(String processInstanceId, String userId) {
        try {
            assertSubmitterWithdrawAllowed(processInstanceId, userId);
            runtimeService.deleteProcessInstance(processInstanceId, "用户撤回");

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(6);
            flowTask.setCompleteTime(LocalDateTime.now());
            lambdaUpdate().eq(FlowTask::getProcessInstanceId, processInstanceId).update(flowTask);

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
        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceId(processInstanceId);
        if (business != null && !isBlank(business.getApplyUserId())) {
            return Objects.equals(String.valueOf(business.getApplyUserId()), String.valueOf(userId));
        }
        Object initiator = runtimeService.getVariable(processInstanceId, "initiator");
        if (initiator != null && !isBlank(String.valueOf(initiator))) {
            return Objects.equals(String.valueOf(initiator), String.valueOf(userId));
        }
        log.warn("撤回申请未找到提交人信息，按历史兼容逻辑放行：processInstanceId={}, userId={}",
                processInstanceId, userId);
        return true;
    }

    @Override
    public FlowTask getTaskDetail(String taskId) {
        FlowTask task = getBaseMapper().selectByIdOrTaskId(taskId);
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
                    .list();
            
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
                diagramInfo.setStatus("running");
            } else if (historicProcessInstance.getDeleteReason() != null) {
                diagramInfo.setStatus("terminated");
            } else {
                diagramInfo.setStatus("completed");
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
                .list();
        
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
                .list();
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
                nodeInfo.setStatus("running");
            } else if (completedActivityMap.containsKey(flowNode.getId())) {
                nodeInfo.setStatus("completed");
                HistoricActivityInstance activity = completedActivityMap.get(flowNode.getId());
                nodeInfo.setStartTime(activity.getStartTime());
                nodeInfo.setEndTime(activity.getEndTime());
                if (activity.getDurationInMillis() != null) {
                    nodeInfo.setDuration(activity.getDurationInMillis());
                }
            } else {
                nodeInfo.setStatus("pending");
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
                        fillUserInfo(nodeInfo, assigneeIds);
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
                                fillUserInfo(nodeInfo, candidateUsers);
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
                            fillUserInfo(nodeInfo, assigneeIds);
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
    private void fillUserInfo(ProcessNodeInfo nodeInfo, List<String> userIds) {
        if (flowOrgIntegrationService == null || userIds == null || userIds.isEmpty()) {
            return;
        }
        
        List<String> names = new ArrayList<>();
        List<String> orgs = new ArrayList<>();
        List<Map<String, Object>> details = new ArrayList<>();
        
        for (String userId : userIds) {
            try {
                Map<String, Object> userInfo = flowOrgIntegrationService.getUserInfo(userId);
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public TaskFormInfo getTaskFormInfo(String taskId) {
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
        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceId(task.getProcessInstanceId());
        if (business != null) {
            formInfo.setBusinessKey(business.getBusinessKey());
            formInfo.setTitle(business.getTitle());
            formInfo.setStartUserId(business.getApplyUserId());
            formInfo.setStartUserName(business.getApplyUserName());
            formInfo.setStartDeptId(business.getApplyDeptId());
            formInfo.setStartDeptName(business.getApplyDeptName());
        }

        // 5. 读取流程模型和 BPMN 节点表单配置，同一次请求内复用给审批策略解析。
        FlowModel flowModel = !isBlank(processDefKey) ? flowModelService.getModelByKey(processDefKey) : null;
        FlowNode flowNode = resolveFormFlowNode(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
        applyFormConfiguration(formInfo, flowModel, flowNode);
        hydrateFormInstanceSnapshotIfNecessary(formInfo, task.getProcessInstanceId());

        // 6. 获取节点办理配置（BPMN扩展属性 + 节点配置表，配置表优先）
        TaskApprovalPolicy policy = getTaskApprovalPolicy(task, flowModel, flowNode);
        formInfo.setAllowApprove(policy.allowApprove);
        formInfo.setAllowReject(policy.allowReject);
        formInfo.setAllowDelegate(policy.allowDelegate);
        formInfo.setAllowReturn(policy.allowReturn);
        formInfo.setAllowTerminate(policy.allowTerminate);
        formInfo.setRequireSignature(policy.requireSignature);
        formInfo.setRequireComment(policy.requireComment);
        formInfo.setAllowRejectToStart(true);

        log.info("获取任务表单信息：taskId={}, formType={}, formKey={}",
                taskId, formInfo.getFormType(), formInfo.getFormKey());

        return formInfo;
    }

    @Override
    public TaskFormInfo getProcessFormInfo(String processInstanceId, String businessKey, String processDefKey,
                                           String taskId, String taskDefKey) {
        if (!isBlank(taskId)) {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task != null) {
                return getTaskFormInfo(taskId);
            }
        }

        FlowTask sourceTask = null;
        if (!isBlank(taskId)) {
            sourceTask = getBaseMapper().selectByIdOrTaskId(taskId);
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
            formInfo.setStartUserName(business.getApplyUserName());
            formInfo.setStartDeptId(business.getApplyDeptId());
            formInfo.setStartDeptName(business.getApplyDeptName());
        }

        Map<String, Object> variables = readProcessVariablesForForm(effectiveProcessInstanceId);
        if (!isBlank(effectiveBusinessKey)) {
            variables.putIfAbsent("businessKey", effectiveBusinessKey);
        }
        formInfo.setVariables(variables);
        applyFormConfiguration(formInfo, processDefinitionId, effectiveProcessDefKey, effectiveTaskDefKey);
        hydrateFormInstanceSnapshotIfNecessary(formInfo, effectiveProcessInstanceId);
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
        FlowBusiness business = null;
        if (!isBlank(processInstanceId)) {
            business = flowBusinessMapper.selectByProcessInstanceId(processInstanceId);
        }
        if (business == null && !isBlank(businessKey)) {
            business = flowBusinessMapper.selectByBusinessKey(businessKey);
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
        NodeFormConfig nodeForm = readNodeFormConfig(flowNode);
        formInfo.setFormFieldPermissions(nodeForm.formFieldPermissions);

        if (isBusinessNodeForm(flowModel, nodeForm.formKey, nodeForm.formMode)) {
            applyBusinessNodeFormConfiguration(formInfo, flowModel, nodeForm);
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
    }

    private boolean isBusinessNodeForm(FlowModel flowModel, String formKey, String formMode) {
        String normalizedMode = normalizeFormMode(formMode);
        if ("BUSINESS_CODE_FORM".equals(normalizedMode) || "BUSINESS_OBJECT_FORM".equals(normalizedMode)) {
            return true;
        }
        if (flowModel == null || !FORM_TYPE_BUSINESS.equalsIgnoreCase(flowModel.getFormType())) {
            return false;
        }
        return true;
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
        if (formInfo == null || FORM_TYPE_BUSINESS.equalsIgnoreCase(formInfo.getFormType())) {
            return;
        }
        if (!"dynamic".equalsIgnoreCase(formInfo.getFormType())
                && isBlank(formInfo.getFormKey())
                && isBlank(formInfo.getFormJson())) {
            return;
        }
        hydrateFormInstanceSnapshot(formInfo, processInstanceId);
    }

    private void hydrateFormInstanceSnapshot(TaskFormInfo formInfo, String processInstanceId) {
        if (flowFormInstanceMapper == null || processInstanceId == null || processInstanceId.isEmpty()) {
            return;
        }
        try {
            FlowFormInstance instance = flowFormInstanceMapper.selectByProcessInstanceId(processInstanceId);
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
        // 1. 查询该流程实例的所有任务（按创建时间排序）
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getProcessInstanceId, processInstanceId)
                .orderByAsc(FlowTask::getCreateTime);
        List<FlowTask> tasks = list(wrapper);

        // 2. 获取业务信息（用于展示发起节点）
        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceId(processInstanceId);

        List<Map<String, Object>> result = new ArrayList<>();

        // 3. 加入“发起”节点
        if (business != null) {
            Map<String, Object> startNode = new HashMap<>();
            startNode.put("taskName", "发起流程");
            startNode.put("assigneeName", business.getApplyUserName());
            startNode.put("assigneeId", business.getApplyUserId());
            startNode.put("action", "start");
            startNode.put("comment", "");
            startNode.put("createTime", business.getApplyTime() != null
                    ? business.getApplyTime().toString() : business.getCreateTime() != null
                    ? business.getCreateTime().toString() : null);
            startNode.put("completeTime", startNode.get("createTime"));
            result.add(startNode);
        }

        // 4. 加入每个任务节点
        // 状态到 action 的映射
        Map<Integer, String> statusActionMap = new HashMap<>();
        statusActionMap.put(0, "pending");
        statusActionMap.put(1, "claim");
        statusActionMap.put(2, "approve");
        statusActionMap.put(3, "reject");
        statusActionMap.put(4, "delegate");
        statusActionMap.put(5, "delegate");
        statusActionMap.put(6, "withdraw");
        statusActionMap.put(7, "return");
        statusActionMap.put(8, "terminate");

        for (FlowTask task : tasks) {
            // 待办且未完成的节点也要展示（表示当前处理中）
            Map<String, Object> node = new HashMap<>();
            node.put("taskId", task.getTaskId());
            node.put("taskName", task.getTaskName());
            // 安全处理：assignee 可能为空
            String assigneeName = "";
            if (task.getAssignee() != null && !task.getAssignee().trim().isEmpty()) {
                try {
                    SysUser sysUser = sysUserService.selectUserById(Long.parseLong(task.getAssignee()));
                    assigneeName = sysUser != null ? sysUser.getRealName() : task.getAssignee();
                } catch (NumberFormatException e) {
                    assigneeName = task.getAssignee();
                }
            }
            node.put("assigneeName", assigneeName);
            node.put("assigneeId", task.getAssignee());
            node.put("action", statusActionMap.getOrDefault(task.getStatus(), "pending"));
            node.put("comment", task.getComment() != null ? task.getComment() : "");
            node.put("signature", task.getSignature());
            node.put("createTime", task.getCreateTime() != null ? task.getCreateTime().toString() : null);
            node.put("completeTime", task.getCompleteTime() != null ? task.getCompleteTime().toString() : null);
            result.add(node);
        }

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
        private boolean allowDelegate;
        private boolean allowReturn;
        private boolean allowTerminate;
        private boolean requireSignature;
        private boolean requireComment;

        private static TaskApprovalPolicy defaultPolicy() {
            TaskApprovalPolicy policy = new TaskApprovalPolicy();
            policy.allowApprove = true;
            policy.allowReject = true;
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
                case ACTION_DELEGATE:
                    return allowDelegate;
                case ACTION_RETURN:
                    return allowReturn;
                case ACTION_TERMINATE:
                    return allowTerminate;
                default:
                    return false;
            }
        }
    }
}
