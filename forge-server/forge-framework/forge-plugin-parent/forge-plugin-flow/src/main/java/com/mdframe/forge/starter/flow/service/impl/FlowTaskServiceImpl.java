package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayAdapter;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayItem;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.starter.flow.dto.FlowApprovalPointResultDTO;
import com.mdframe.forge.starter.flow.dto.ProcessDiagramInfo;
import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowErrorLog;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.enums.FlowTaskStatus;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowFormInstanceMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskCandidateMapper;
import com.mdframe.forge.starter.flow.service.FlowErrorLogService;
import com.mdframe.forge.starter.flow.service.FlowFormService;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import com.mdframe.forge.starter.flow.service.FlowNodeConfigService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import com.mdframe.forge.starter.flow.service.FlowTaskService;
import com.mdframe.forge.starter.flow.security.FlowAccessGuard;
import com.mdframe.forge.starter.flow.security.FlowCandidateMembershipResolver;
import com.mdframe.forge.starter.flow.vo.FlowHistoryItemVO;
import com.mdframe.forge.starter.flow.vo.FlowHistoryPageVO;
import com.mdframe.forge.starter.flow.vo.FlowTaskSignRelationVO;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 流程任务服务实现
 */
@Slf4j
@Service
public class FlowTaskServiceImpl extends ServiceImpl<FlowTaskMapper, FlowTask> implements FlowTaskService {

    private static final int MAX_DETAIL_HISTORY_ITEMS = 1000;

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

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;
    
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

    @Autowired
    private FlowCandidateMembershipResolver candidateMembershipResolver;

    @Override
    public IPage<FlowTask> todoTasks(Page<FlowTask> page, String userId, String title, String category, Integer status) {
        return enrichTaskPage(this.getBaseMapper().selectTodoTasks(page, userId, title, category, status,
                SessionHelper.getTenantId(), candidateMembershipResolver.resolveCurrentSessionGroups()));
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

    @Override
    public List<FlowTask> activeTasksByProcessInstances(Collection<String> processInstanceIds, String userId) {
        if (processInstanceIds == null || processInstanceIds.isEmpty() || isBlank(userId)) {
            return List.of();
        }
        List<String> ids = processInstanceIds.stream()
                .filter(id -> !isBlank(id))
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return List.of();
        }
        return this.getBaseMapper().selectActiveTasksByProcessInstances(
                ids, userId.trim(), requireTenantId());
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
        Set<String> groups = candidateMembershipResolver.resolveCurrentSessionGroups();
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
    public void approve(String taskId, String userId, String comment, String signature,
                        Map<String, Object> variables) {
        approve(taskId, userId, comment, signature, variables, SessionHelper.getTenantId(), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(String taskId, String userId, String comment, String signature,
                        Map<String, Object> variables, Long tenantId,
                        String idempotencyKey, String requestDigest,
                        List<FlowApprovalPointResultDTO> approvalPointResults) {
        taskActionCoordinator().approve(
                taskId, userId, comment, signature, variables, tenantId,
                idempotencyKey, requestDigest, approvalPointResults);
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
        taskActionCoordinator().reject(
                taskId, userId, comment, signature, tenantId, idempotencyKey, requestDigest, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectToStart(String taskId, String userId, String comment, String signature,
                              Long tenantId, String idempotencyKey, String requestDigest) {
        taskActionCoordinator().reject(
                taskId, userId, comment, signature, tenantId, idempotencyKey, requestDigest, true);
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
        taskActionCoordinator().delegate(
                taskId, userId, targetUserId, comment, signature, tenantId, idempotencyKey, requestDigest);
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
        taskActionCoordinator().returnTask(
                taskId, userId, comment, signature, requestedTargetActivityId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reassignByInitiator(String taskId, String userId, String targetUserId, String reason) {
        taskActionCoordinator().reassignByInitiator(taskId, userId, targetUserId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateTask(String taskId, String userId, String comment, String signature) {
        taskActionCoordinator().terminateTask(taskId, userId, comment, signature);
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
        addSignCoordinator().mutate(
                taskId, userId, targetUserId, reason, signMode,
                SessionHelper.getTenantId(), null, null, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSign(String taskId, String userId, String targetUserId, String reason, String signMode,
                        Long tenantId, String idempotencyKey, String requestDigest) {
        addSignCoordinator().mutate(
                taskId, userId, targetUserId, reason, signMode,
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
        dynamicSignCoordinator().mutate(
                taskId, userId, targetUserId, reason, signMode,
                SessionHelper.getTenantId(), null, null, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reduceSign(String taskId, String userId, String targetUserId, String reason, String signMode,
                           Long tenantId, String idempotencyKey, String requestDigest) {
        dynamicSignCoordinator().mutate(
                taskId, userId, targetUserId, reason, signMode,
                tenantId, idempotencyKey, requestDigest, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlowTaskSignRelationVO> getSignRelations(String taskId, String userId) {
        return dynamicSignCoordinator().getSignRelations(taskId, userId);
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

        Boolean allowed = taskNodePolicy().readBooleanProcessAttribute(
                instance.getProcessDefinitionId(), "allowSubmitterWithdraw");
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
        return processDiagramService().getProcessDiagram(processInstanceId);
    }

    @Override
    public ProcessDiagramInfo getProcessDiagramInfo(String processInstanceId) {
        return processDiagramService().getProcessDiagramInfo(processInstanceId);
    }

    @Override
    public ProcessDiagramInfo getProcessDiagramInfo(String processInstanceId, boolean includeImage) {
        return processDiagramService().getProcessDiagramInfo(processInstanceId, includeImage);
    }

    private FlowProcessDiagramService processDiagramService() {
        return new FlowProcessDiagramService(
                runtimeService,
                taskService,
                repositoryService,
                historyService,
                processEngineConfiguration,
                flowOrgIntegrationService,
                flowAccessGuard
        );
    }
    @Override
    public void remind(String taskId) {
        new FlowTaskRemindCoordinator(
                taskService,
                flowAccessGuard,
                messageService,
                stringRedisTemplate,
                SessionHelper::getTenantId,
                SessionHelper::getUserId
        ).remind(taskId);
    }

    private FlowTaskNodePolicy taskNodePolicy() {
        return new FlowTaskNodePolicy(
                repositoryService,
                historyService,
                taskService,
                flowModelService,
                flowNodeConfigService,
                processDefinitionId -> resolveProcessDefinitionKey(processDefinitionId, null)
        );
    }

    private FlowTaskFormConfigurationResolver formConfigurationResolver() {
        return new FlowTaskFormConfigurationResolver(
                repositoryService,
                taskService,
                flowModelService,
                flowFormService,
                flowFormInstanceMapper,
                taskNodePolicy(),
                this::resolveProcessDefinitionKey
        );
    }

    private FlowTaskFormContextCoordinator formContextCoordinator() {
        return new FlowTaskFormContextCoordinator(
                taskService,
                runtimeService,
                repositoryService,
                historyService,
                getBaseMapper(),
                flowBusinessMapper,
                flowModelService,
                flowAccessGuard,
                formConfigurationResolver(),
                taskNodePolicy(),
                this::resolveProcessDefinitionKey,
                this::resolveUserDisplayName,
                this::isProcessStarterTask
        );
    }

    private FlowTaskDynamicSignCoordinator dynamicSignCoordinator() {
        return dynamicSignCoordinator((taskId, userId) -> assertTaskMutationActor(taskId, userId, false));
    }

    /** 加签在操作人校验之后再校验节点策略；减签不受节点策略限制。 */
    private FlowTaskDynamicSignCoordinator addSignCoordinator() {
        return dynamicSignCoordinator((taskId, userId) -> {
            assertTaskMutationActor(taskId, userId, false);
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task != null) {
                taskNodePolicy().validateAddSign(task);
            }
        });
    }

    private FlowTaskDynamicSignCoordinator dynamicSignCoordinator(BiConsumer<String, String> mutationActorGuard) {
        return new FlowTaskDynamicSignCoordinator(
                runtimeService,
                taskService,
                repositoryService,
                getBaseMapper(),
                flowTaskCandidateMapper,
                flowAccessGuard,
                mutationActorGuard,
                this::validateReassignTarget
        );
    }

    private FlowTaskActionCoordinator taskActionCoordinator() {
        return new FlowTaskActionCoordinator(
                runtimeService,
                taskService,
                repositoryService,
                historyService,
                getBaseMapper(),
                flowBusinessMapper,
                flowErrorLogService,
                taskNodePolicy(),
                formConfigurationResolver(),
                this::assertTaskMutationActor,
                this::assertTaskTenantForAction,
                this::validateReassignTarget,
                this::isProcessStarterTask
        );
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

    private String textValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private Long requireTenantId() {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_TASK_TENANT_REQUIRED");
        }
        return tenantId;
    }

    private boolean updateTaskByTenant(String taskId, FlowTask task) {
        return getBaseMapper().updateByTaskIdAndTenant(taskId, requireTenantId(), task) > 0;
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
        return formContextCoordinator().getTaskFormInfo(taskId);
    }

    @Override
    public TaskFormInfo getProcessFormInfo(String processInstanceId, String businessKey, String processDefKey,
                                           String taskId, String taskDefKey) {
        return formContextCoordinator().getProcessFormInfo(
                processInstanceId, businessKey, processDefKey, taskId, taskDefKey);
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
            node.setApprovalPointResults(taskNodePolicy().readApprovalPointResults(task.getTaskId()));
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

}
