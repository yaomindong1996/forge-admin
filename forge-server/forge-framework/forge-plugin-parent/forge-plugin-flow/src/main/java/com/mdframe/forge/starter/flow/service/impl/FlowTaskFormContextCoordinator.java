package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.security.FlowAccessGuard;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * 任务与流程表单上下文协调器。
 *
 * <p>使用 Coordinator 编排访问守卫、业务关联、流程定义定位、运行/历史变量、
 * 表单配置解析和任务动作视图，不参与表单保存或任务状态变更。</p>
 */
@Slf4j
final class FlowTaskFormContextCoordinator {

    private static final String RETURN_SOURCE_ACTIVITY_ID = "FLOW_RETURN_SOURCE_ACTIVITY_ID";
    private static final String RETURN_TARGET_ACTIVITY_ID = "FLOW_RETURN_TARGET_ACTIVITY_ID";
    private static final String RETURN_TO_START_PENDING = "FLOW_RETURN_TO_START_PENDING";

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final FlowTaskMapper flowTaskMapper;
    private final FlowBusinessMapper flowBusinessMapper;
    private final FlowModelService flowModelService;
    private final FlowAccessGuard flowAccessGuard;
    private final FlowTaskFormConfigurationResolver formResolver;
    private final FlowTaskNodePolicy nodePolicy;
    private final BiFunction<String, String, String> processDefinitionKeyResolver;
    private final BiFunction<String, String, String> userDisplayNameResolver;
    private final BiPredicate<Task, String> processStarterPredicate;

    FlowTaskFormContextCoordinator(TaskService taskService,
                                   RuntimeService runtimeService,
                                   RepositoryService repositoryService,
                                   HistoryService historyService,
                                   FlowTaskMapper flowTaskMapper,
                                   FlowBusinessMapper flowBusinessMapper,
                                   FlowModelService flowModelService,
                                   FlowAccessGuard flowAccessGuard,
                                   FlowTaskFormConfigurationResolver formResolver,
                                   FlowTaskNodePolicy nodePolicy,
                                   BiFunction<String, String, String> processDefinitionKeyResolver,
                                   BiFunction<String, String, String> userDisplayNameResolver,
                                   BiPredicate<Task, String> processStarterPredicate) {
        this.taskService = taskService;
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
        this.historyService = historyService;
        this.flowTaskMapper = flowTaskMapper;
        this.flowBusinessMapper = flowBusinessMapper;
        this.flowModelService = flowModelService;
        this.flowAccessGuard = flowAccessGuard;
        this.formResolver = formResolver;
        this.nodePolicy = nodePolicy;
        this.processDefinitionKeyResolver = processDefinitionKeyResolver;
        this.userDisplayNameResolver = userDisplayNameResolver;
        this.processStarterPredicate = processStarterPredicate;
    }

    TaskFormInfo getTaskFormInfo(String taskId) {
        FlowTask visibleTask = flowAccessGuard.requireTaskVisible(taskId);
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            // forge_flow_task 可见但引擎运行时任务已不存在（脏数据/已办结）时按业务 404，避免冒泡成 500
            throw new BusinessException(404, "任务不存在：" + taskId);
        }

        TaskFormInfo formInfo = new TaskFormInfo();
        formInfo.setTaskId(taskId);
        formInfo.setTaskName(task.getName());
        formInfo.setTaskDefKey(task.getTaskDefinitionKey());
        formInfo.setProcessInstanceId(task.getProcessInstanceId());
        formInfo.setStatus(visibleTask.getStatus());
        formInfo.setAssignee(visibleTask.getAssignee());
        formInfo.setCandidateUsers(visibleTask.getCandidateUsers());
        formInfo.setCandidateGroups(visibleTask.getCandidateGroups());

        String processDefKey = processDefinitionKeyResolver.apply(task.getProcessDefinitionId(), null);
        formInfo.setProcessDefKey(processDefKey);
        Map<String, Object> variables = taskService.getVariables(taskId);
        formInfo.setVariables(variables);

        Long taskTenantId = visibleTask.getTenantId() != null
                ? visibleTask.getTenantId() : SessionHelper.getTenantId();
        FlowBusiness business = taskTenantId == null
                ? null
                : flowBusinessMapper.selectByProcessInstanceIdAndTenantId(
                        task.getProcessInstanceId(), taskTenantId);
        applyBusinessContext(formInfo, business);

        FlowModel flowModel = !isBlank(processDefKey) ? flowModelService.getModelByKey(processDefKey) : null;
        BpmnModel bpmnModel = isBlank(task.getProcessDefinitionId()) ? null
                : repositoryService.getBpmnModel(task.getProcessDefinitionId());
        FlowNode flowNode = formResolver.resolveFormFlowNode(bpmnModel, task.getTaskDefinitionKey());
        formResolver.applyFormConfiguration(formInfo, flowModel, flowNode);
        formResolver.hydrateFormInstanceSnapshotIfNecessary(
                formInfo, task.getProcessInstanceId(), taskTenantId);

        nodePolicy.applyApprovalPolicy(formInfo, task, flowModel, flowNode);
        formInfo.setReturnTargets(buildReturnTargets(task, formInfo.getAllowMultiReturn()));
        populateDirectSendInfo(formInfo, task, bpmnModel, variables);
        nodePolicy.applyNodePolicy(formInfo, flowNode);

        log.info("获取任务表单信息：taskId={}, formType={}, formKey={}",
                taskId, formInfo.getFormType(), formInfo.getFormKey());
        return formInfo;
    }

    TaskFormInfo getProcessFormInfo(String processInstanceId, String businessKey, String processDefKey,
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
        FlowTask sourceTask = isBlank(taskId)
                ? null : flowTaskMapper.selectByIdOrTaskIdAndTenant(taskId, tenantId);
        FlowBusiness business = resolveFlowBusiness(processInstanceId, businessKey, tenantId);
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
        String effectiveProcessDefKey = processDefinitionKeyResolver.apply(processDefinitionId, rawProcessDefKey);
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
        applyBusinessContext(formInfo, business);

        Map<String, Object> variables = readProcessVariablesForForm(effectiveProcessInstanceId);
        if (!isBlank(effectiveBusinessKey)) {
            variables.putIfAbsent("businessKey", effectiveBusinessKey);
        }
        formInfo.setVariables(variables);
        formResolver.applyFormConfiguration(
                formInfo, processDefinitionId, effectiveProcessDefKey, effectiveTaskDefKey);
        formResolver.hydrateFormInstanceSnapshotIfNecessary(
                formInfo, effectiveProcessInstanceId, tenantId);
        disableTaskActions(formInfo);
        return formInfo;
    }

    private void applyBusinessContext(TaskFormInfo formInfo, FlowBusiness business) {
        if (business == null) {
            return;
        }
        formInfo.setBusinessKey(business.getBusinessKey());
        formInfo.setTitle(business.getTitle());
        formInfo.setStartUserId(business.getApplyUserId());
        formInfo.setStartUserName(userDisplayNameResolver.apply(
                business.getApplyUserId(), business.getApplyUserName()));
        formInfo.setStartDeptId(business.getApplyDeptId());
        formInfo.setStartDeptName(business.getApplyDeptName());
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

    private void populateDirectSendInfo(TaskFormInfo formInfo, Task task, BpmnModel bpmnModel,
                                        Map<String, Object> variables) {
        Object source = variables == null ? null : variables.get(RETURN_SOURCE_ACTIVITY_ID);
        Object target = variables == null ? null : variables.get(RETURN_TARGET_ACTIVITY_ID);
        Object returnToStartPending = variables == null ? null : variables.get(RETURN_TO_START_PENDING);
        boolean returnedToHistoricalNode = target != null
                && Objects.equals(String.valueOf(target), task.getTaskDefinitionKey());
        boolean returnedToStart = Boolean.TRUE.equals(readBoolean(returnToStartPending));
        if (source == null || (!returnedToHistoricalNode && !returnedToStart)) {
            formInfo.setAllowDirectSend(false);
            return;
        }
        String sourceId = String.valueOf(source);
        FlowElement element = formResolver.resolveFormFlowNode(bpmnModel, sourceId);
        if (!(element instanceof UserTask)) {
            formInfo.setAllowDirectSend(false);
            return;
        }
        if (returnedToStart && !processStarterPredicate.test(task, task.getAssignee())) {
            formInfo.setAllowDirectSend(false);
            return;
        }
        formInfo.setAllowDirectSend(true);
        formInfo.setReturnSourceActivityId(sourceId);
        formInfo.setReturnSourceActivityName(element.getName());
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

    private FlowBusiness resolveFlowBusiness(String processInstanceId, String businessKey, Long tenantId) {
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
            return historicTasks == null || historicTasks.isEmpty()
                    ? null : historicTasks.get(0).getTaskDefinitionKey();
        } catch (Exception e) {
            log.debug("读取历史任务定义Key失败: processInstanceId={}", processInstanceId);
            return null;
        }
    }

    private void disableTaskActions(TaskFormInfo formInfo) {
        formInfo.setAllowApprove(false);
        formInfo.setAllowReject(false);
        formInfo.setAllowDelegate(false);
        formInfo.setAllowReturn(false);
        formInfo.setAllowTerminate(false);
        formInfo.setRequireComment(false);
        formInfo.setRequireSignature(false);
        formInfo.setAllowRejectToStart(false);
    }

    private Boolean readBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
                return false;
            }
        }
        return null;
    }

    private Long requireTenantId() {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_TASK_TENANT_REQUIRED");
        }
        return tenantId;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
