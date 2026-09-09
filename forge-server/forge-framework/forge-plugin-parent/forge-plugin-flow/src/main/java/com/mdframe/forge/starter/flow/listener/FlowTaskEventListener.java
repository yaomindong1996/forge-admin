package com.mdframe.forge.starter.flow.listener;

import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowErrorLog;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.enums.FlowBusinessStatus;
import com.mdframe.forge.starter.flow.enums.FlowFormInstanceStatus;
import com.mdframe.forge.starter.flow.enums.FlowTaskStatus;
import com.mdframe.forge.starter.core.domain.FlowEventMessage;
import com.mdframe.forge.starter.flow.event.FlowTaskNotifyEvent;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowFormInstanceMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskCandidateMapper;
import com.mdframe.forge.starter.flow.entity.FlowTaskCandidate;
import com.mdframe.forge.starter.flow.enums.FlowTaskCandidateStatus;
import com.mdframe.forge.starter.flow.entity.FlowRecordParticipant;
import com.mdframe.forge.starter.flow.service.FlowErrorLogService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import com.mdframe.forge.starter.flow.service.FlowRecordParticipantService;
import com.mdframe.forge.starter.flow.service.FlowNodeConfigService;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.delegate.event.FlowableProcessEngineEvent;
import org.flowable.engine.HistoryService;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 流程任务事件监听器
 * 监听任务的创建、完成、删除等事件，同步数据到业务表；
 * 站内信/企微卡片/抄送/事件通知等副作用统一以 {@link FlowTaskNotifyEvent}
 * 发布，由 FlowTaskNotifyListener 在事务提交后异步处理
 */
@Slf4j
@Component
public class FlowTaskEventListener implements FlowableEventListener {

    private static final String PHYSICAL_CLEANUP_REASON_KEYWORD = "删除流程数据";

    @Value("${forge.flow.timeout.time-zone:Asia/Shanghai}")
    private String timeoutTimeZone;

    @Autowired
    @Lazy
    private FlowTaskMapper flowTaskMapper;

    @Autowired(required = false)
    @Lazy
    private FlowTaskCandidateMapper flowTaskCandidateMapper;

    @Autowired
    @Lazy
    private FlowBusinessMapper flowBusinessMapper;

    @Autowired
    @Lazy
    private TaskService taskService;
    
    @Autowired
    @Lazy
    private RuntimeService runtimeService;

    @Autowired
    @Lazy
    private HistoryService historyService;

    /** 通知事件发布器：通知类副作用在事务提交后由异步监听器执行 */
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    @Lazy
    private FlowErrorLogService flowErrorLogService;

    @Autowired(required = false)
    @Lazy
    private FlowOrgIntegrationService flowOrgIntegrationService;

    @Autowired(required = false)
    @Lazy
    private FlowFormInstanceMapper flowFormInstanceMapper;

    @Autowired(required = false)
    @Lazy
    private FlowRecordParticipantService flowRecordParticipantService;

    @Autowired(required = false)
    @Lazy
    private FlowNodeConfigService flowNodeConfigService;

    @Override
    public void onEvent(FlowableEvent event) {
        try {
            FlowableEventType eventType = (FlowableEventType) event.getType();

            switch (eventType.name()) {
                case "TASK_CREATED":
                    handleTaskCreated(event);
                    break;
                case "TASK_COMPLETED":
                    handleTaskCompleted(event);
                    break;
                case "TASK_ASSIGNED":
                    handleTaskAssigned(event);
                    break;
                case "ENTITY_DELETED":
                    handleTaskDeleted(event);
                    break;
                case "PROCESS_COMPLETED":
                    handleProcessCompleted(event);
                    break;
                case "PROCESS_CANCELLED":
                    handleProcessCancelled(event);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error("FlowTaskEventListener.onEvent 未捕获异常", e);
            recordEventListenerError(event, "EVENT_DISPATCH", e);
        }
    }

    /**
     * 处理任务创建事件
     */
    private void handleTaskCreated(FlowableEvent event) {
        try {
            TaskEntity task = (TaskEntity) ((FlowableEntityEvent) event).getEntity();
            log.debug("任务创建事件：taskId={}, name={}, assignee={}, processInstanceId={}",
                    task.getId(), task.getName(), task.getAssignee(), task.getProcessInstanceId());
            
            // 检查是否已存在
            FlowTask existingTask = flowTaskMapper.selectByTaskId(task.getId());
            if (existingTask != null) {
                log.debug("任务已存在，跳过创建：taskId={}", task.getId());
                return;
            }

            applyConfiguredDueDate(task);
            
            // 创建任务记录
            FlowTask flowTask = buildFlowTask(task);
            flowTask.setStatus(FlowTaskStatus.PENDING.getCode());
            
            log.debug("任务处理人: {}, 候选人: {}, 候选组: {}",
                    flowTask.getAssignee(), flowTask.getCandidateUsers(), flowTask.getCandidateGroups());

            // 审批人分配失败：无处理人且无候选人/候选组时，记录错误日志
            boolean noAssignee = flowTask.getAssignee() == null || flowTask.getAssignee().isEmpty();
            boolean noCandidateUsers = flowTask.getCandidateUsers() == null || flowTask.getCandidateUsers().isEmpty();
            boolean noCandidateGroups = flowTask.getCandidateGroups() == null || flowTask.getCandidateGroups().isEmpty();
            if (noAssignee && noCandidateUsers && noCandidateGroups) {
                log.warn("[审批人分配失败] 任务无处理人且无候选人: taskId={}, taskName={}, taskDefKey={}, processInstanceId={}",
                        task.getId(), task.getName(), task.getTaskDefinitionKey(), task.getProcessInstanceId());
                FlowErrorLog errorLog = new FlowErrorLog();
                errorLog.setProcessInstanceId(task.getProcessInstanceId());
                errorLog.setTaskId(task.getId());
                errorLog.setActivityId(task.getTaskDefinitionKey());
                errorLog.setActivityName(task.getName());
                errorLog.setErrorStage("TASK_ASSIGNEE_MISSING");
                errorLog.setErrorMessage(String.format(
                        "任务[%s]审批人分配失败：未找到处理人，请检查审批人配置或流程变量是否正确传入",
                        task.getName() != null ? task.getName() : task.getTaskDefinitionKey()));
                flowErrorLogService.recordError(errorLog, null);
            }
                
            // 获取业务信息（优先通过 processInstanceId 查询，查不到则通过 businessKey 查询）
            FlowBusiness business = getFlowBusiness(task.getProcessInstanceId());
            
            // 如果通过 processInstanceId 查不到，尝试通过 businessKey 查询
            if (business == null) {
                // 从 ProcessInstance 获取 businessKey
                try {
                    org.flowable.engine.runtime.ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                            .processInstanceId(task.getProcessInstanceId())
                            .singleResult();
                    if (processInstance != null) {
                        String businessKey = processInstance.getBusinessKey();
                        if (businessKey != null && !businessKey.isEmpty()) {
                            business = getFlowBusinessByBusinessKey(businessKey);
                            log.debug("通过 businessKey 查询业务信息: businessKey={}, business={}", businessKey, business);
                        }
                    }
                } catch (Exception e) {
                    log.warn("获取 ProcessInstance 失败: processInstanceId={}", task.getProcessInstanceId(), e);
                }
            } else {
                log.debug("通过 processInstanceId 查询业务信息: processInstanceId={}, business={}", task.getProcessInstanceId(), business);
            }
            
            if (business != null) {
                String normalizedApplyUserName = resolveUserDisplayName(
                        business.getApplyUserId(), business.getApplyUserName());
                if (!Objects.equals(normalizedApplyUserName, business.getApplyUserName())
                        && normalizedApplyUserName != null) {
                    business.setApplyUserName(normalizedApplyUserName);
                    flowBusinessMapper.updateById(business);
                }
                flowTask.setTenantId(business.getTenantId());
                flowTask.setTitle(business.getTitle());
                flowTask.setBusinessKey(business.getBusinessKey());
                flowTask.setBusinessType(business.getBusinessType());
                flowTask.setStartUserId(business.getApplyUserId());
                flowTask.setStartUserName(normalizedApplyUserName);
                flowTask.setStartDeptId(business.getApplyDeptId());
                flowTask.setStartDeptName(business.getApplyDeptName());
                log.debug("业务信息: title={}, businessKey={}, applyUserId={}, applyUserName={}",
                        business.getTitle(), business.getBusinessKey(), business.getApplyUserId(), business.getApplyUserName());
            } else {
                // 没有业务信息时设置默认値
                flowTask.setTitle(task.getName() + " - " + task.getProcessDefinitionId().split(":")[0]);
                log.warn("未找到业务信息，使用默认标题: processInstanceId={}, title={}",
                        task.getProcessInstanceId(), flowTask.getTitle());
            }
            
            flowTaskMapper.insert(flowTask);
            syncCandidateRelations(flowTask);
            log.info("创建待办任务成功：taskId={}, title={}, assignee={}, candidateUsers={}, candidateGroups={}",
                    task.getId(), flowTask.getTitle(), flowTask.getAssignee(), flowTask.getCandidateUsers(), flowTask.getCandidateGroups());
            // 事务提交后异步推送站内信/企微卡片等（按模型通知配置），不阻塞审批主链路
            eventPublisher.publishEvent(FlowTaskNotifyEvent.todo(flowTask, business, readTaskVariables(task)));
    
            // 发布 TASK_CREATED 事件，业务侧可监听并处理（如：发送待办通知、记录日志等）
            if (business != null) {
                FlowEventMessage msg = FlowEventMessage.ofTask(
                        FlowEventMessage.TASK_CREATED,
                        task.getProcessInstanceId(),
                        flowTask.getProcessDefKey(),
                        business.getBusinessKey(),
                        business.getTitle(),
                        business.getApplyUserId(),
                        business.getApplyUserName(),
                        task.getId(),
                        task.getTaskDefinitionKey(),
                        task.getName(),
                        flowTask.getAssignee(),
                        null,   // assigneeName 在创建时暂无姓名
                        null);
                fillTenantId(msg, business);
                publishEvent(msg, flowTask.getProcessDefKey());
            }
            
        } catch (Exception e) {
            log.error("处理任务创建事件失败", e);
            recordEventListenerError(event, "EVENT_TASK_CREATED", e);
        }
    }

    /**
     * 处理任务完成事件
     */
    private void handleTaskCompleted(FlowableEvent event) {
        try {
            TaskEntity task = (TaskEntity) ((FlowableEntityEvent) event).getEntity();
            log.info("任务完成事件：taskId={}, name={}", task.getId(), task.getName());
            
            // 业务信息只查一次，后续补全任务字段和发布事件复用
            FlowBusiness completedBusiness = getFlowBusiness(task.getProcessInstanceId());

            // 更新任务状态
            FlowTask flowTask = flowTaskMapper.selectByTaskId(task.getId());
            if (flowTask != null) {
                flowTask.setStatus(FlowTaskStatus.APPROVED.getCode());
                flowTask.setCompleteTime(LocalDateTime.now());
                flowTaskMapper.updateById(flowTask);
                log.info("更新任务状态为已完成：taskId={}", task.getId());
            } else {
                // 任务不存在，创建已完成的记录
                flowTask = buildFlowTask(task);
                flowTask.setStatus(FlowTaskStatus.APPROVED.getCode());
                flowTask.setCompleteTime(LocalDateTime.now());
                
                if (completedBusiness != null) {
                    flowTask.setTenantId(completedBusiness.getTenantId());
                    flowTask.setTitle(completedBusiness.getTitle());
                    flowTask.setBusinessKey(completedBusiness.getBusinessKey());
                    flowTask.setBusinessType(completedBusiness.getBusinessType());
                    flowTask.setStartUserId(completedBusiness.getApplyUserId());
                    flowTask.setStartUserName(resolveUserDisplayName(
                            completedBusiness.getApplyUserId(), completedBusiness.getApplyUserName()));
                }
                
                flowTaskMapper.insert(flowTask);
                log.info("创建已完成任务记录：taskId={}", task.getId());
            }
            // 发布 TASK_COMPLETED 事件，业务侧可监听具体节点完成情况（如：更新业务表审批节点状态等）
            eventPublisher.publishEvent(FlowTaskNotifyEvent.todoRead(task.getId(), completedBusiness));
            if (completedBusiness != null) {
                // 获取该任务的审批意见
                String comment = null;
                try {
                    List<org.flowable.engine.task.Comment> comments = taskService.getTaskComments(task.getId());
                    if (comments != null && !comments.isEmpty()) {
                        comment = comments.get(comments.size() - 1).getFullMessage();
                    }
                } catch (Exception ex) {
                    log.debug("获取任务意见失败: taskId={}", task.getId());
                }
                FlowEventMessage msg = FlowEventMessage.ofTask(
                        FlowEventMessage.TASK_COMPLETED,
                        task.getProcessInstanceId(),
                        flowTask.getProcessDefKey(),
                        completedBusiness.getBusinessKey(),
                        completedBusiness.getTitle(),
                        completedBusiness.getApplyUserId(),
                        completedBusiness.getApplyUserName(),
                        task.getId(),
                        task.getTaskDefinitionKey(),
                        task.getName(),
                        flowTask.getAssignee(),
                        null,   // assigneeName 暂无
                        comment);
                msg.setVariables(readTaskVariables(task));
                fillTenantId(msg, completedBusiness);
                publishEvent(msg, flowTask.getProcessDefKey());
                recordAssignee(completedBusiness, task, flowTask);
            }
            
        } catch (Exception e) {
            log.error("处理任务完成事件失败", e);
            recordEventListenerError(event, "EVENT_TASK_COMPLETED", e);
        }
    }

    /**
     * 处理任务分配事件
     */
    private void handleTaskAssigned(FlowableEvent event) {
        try {
            TaskEntity task = (TaskEntity) ((FlowableEntityEvent) event).getEntity();
            log.info("任务分配事件：taskId={}, assignee={}, owner={}", 
                    task.getId(), task.getAssignee(), task.getOwner());
            
            FlowTask flowTask = flowTaskMapper.selectByTaskId(task.getId());
            if (flowTask != null) {
                String assignee = normalizeTaskUserId(task.getAssignee(), task.getId(), "assignee");
                String owner = normalizeTaskUserId(task.getOwner(), task.getId(), "owner");
                flowTask.setAssignee(assignee);
                flowTask.setAssigneeName(resolveUserDisplayName(assignee, flowTask.getAssigneeName()));
                flowTask.setOwner(owner);
                
                if (assignee != null) {
                    if (owner != null && !owner.equals(assignee)) {
                        log.info("转派任务（owner存在且不同于assignee），保持待办状态：taskId={}, owner={}, assignee={}", 
                                task.getId(), owner, assignee);
                        flowTask.setStatus(FlowTaskStatus.PENDING.getCode());
                    } else {
                        log.info("任务签收（无owner或owner=assignee），设为已签收状态：taskId={}, assignee={}", 
                                task.getId(), assignee);
                        flowTask.setStatus(FlowTaskStatus.CLAIMED.getCode());
                        flowTask.setClaimTime(LocalDateTime.now());
                    }
                }
                flowTaskMapper.updateById(flowTask);
                log.info("更新任务处理人：taskId={}, assignee={}, status={}", 
                        task.getId(), assignee, flowTask.getStatus());
            }

            if (flowTask != null && flowTask.getAssignee() != null) {
                FlowBusiness assignedBusiness = getFlowBusiness(task.getProcessInstanceId());
                if (assignedBusiness != null) {
                    if (flowTask.getTenantId() == null && assignedBusiness.getTenantId() != null) {
                        flowTask.setTenantId(assignedBusiness.getTenantId());
                    }
                    eventPublisher.publishEvent(FlowTaskNotifyEvent.todo(flowTask, assignedBusiness, readTaskVariables(task)));
                    FlowEventMessage msg = FlowEventMessage.ofTask(
                            FlowEventMessage.TASK_ASSIGNED,
                            task.getProcessInstanceId(),
                            flowTask.getProcessDefKey(),
                            assignedBusiness.getBusinessKey(),
                            assignedBusiness.getTitle(),
                            assignedBusiness.getApplyUserId(),
                            assignedBusiness.getApplyUserName(),
                            task.getId(),
                            task.getTaskDefinitionKey(),
                            task.getName(),
                            flowTask.getAssignee(),
                            null,
                            null);
                    fillTenantId(msg, assignedBusiness);
                    publishEvent(msg, flowTask.getProcessDefKey());
                }
            }
            
        } catch (Exception e) {
            log.error("处理任务分配事件失败", e);
            recordEventListenerError(event, "EVENT_TASK_ASSIGNED", e);
        }
    }

    /**
     * 处理任务删除事件（流程终止等）
     */
    private void handleTaskDeleted(FlowableEvent event) {
        try {
            Object entity = ((FlowableEntityEvent) event).getEntity();
            
            // 只处理任务实体
            if (entity instanceof TaskEntity) {
                TaskEntity task = (TaskEntity) entity;
                log.info("任务删除事件：taskId={}, name={}", task.getId(), task.getName());
                
                // 更新任务状态为已取消
                FlowTask flowTask = flowTaskMapper.selectByTaskId(task.getId());
                if (flowTask != null) {
                    flowTask.setStatus(FlowTaskStatus.CANCELED.getCode());
                    flowTask.setCompleteTime(LocalDateTime.now());
                    flowTaskMapper.updateById(flowTask);
                    log.info("更新任务状态为已取消：taskId={}", task.getId());
                }
                eventPublisher.publishEvent(
                        FlowTaskNotifyEvent.todoRead(task.getId(), getFlowBusiness(task.getProcessInstanceId())));
            }
            
        } catch (Exception e) {
            log.error("处理任务删除事件失败", e);
            recordEventListenerError(event, "EVENT_TASK_DELETED", e);
        }
    }

    /**
     * 处理流程完成事件
     */
    private void handleProcessCompleted(FlowableEvent event) {
        try {
            Object entity = ((FlowableEntityEvent) event).getEntity();
            String processInstanceId = null;
            String approvalResult = null;
            Map<String, Object> processVariables = null;
            
            // PROCESS_COMPLETED 事件的 entity 是 ExecutionEntity
            if (entity instanceof org.flowable.engine.impl.persistence.entity.ExecutionEntity) {
                org.flowable.engine.impl.persistence.entity.ExecutionEntity execution =
                    (org.flowable.engine.impl.persistence.entity.ExecutionEntity) entity;
                processInstanceId = execution.getProcessInstanceId();
                processVariables = execution.getVariables();
                Object approvalResultVar = execution.getVariable("approvalResult");
                if (approvalResultVar != null) {
                    approvalResult = String.valueOf(approvalResultVar);
                }
                log.info("流程完成事件：processInstanceId={}, approvalResult={}", processInstanceId, approvalResult);
            }
            
            if (processInstanceId != null) {
                processVariables = readProcessVariables(processInstanceId, processVariables);
                if (approvalResult == null || approvalResult.isBlank()) {
                    Object approvalResultVar = processVariables.get("approvalResult");
                    if (approvalResultVar != null) {
                        approvalResult = String.valueOf(approvalResultVar);
                    }
                }
                boolean rejected = "reject".equalsIgnoreCase(approvalResult);

                // 更新 FlowBusiness 状态
                FlowBusiness business = getFlowBusiness(processInstanceId);
                if (business != null) {
                    business.setStatus(rejected
                            ? FlowBusinessStatus.REJECTED.getCode()
                            : FlowBusinessStatus.APPROVED.getCode());
                    business.setEndTime(LocalDateTime.now());
                    if (business.getApplyTime() != null) {
                        long duration = java.time.Duration.between(
                            business.getApplyTime(),
                            business.getEndTime()
                        ).toMillis();
                        business.setDuration(duration);
                    }
                    flowBusinessMapper.updateById(business);
                    updateFormInstanceStatus(
                            processInstanceId,
                            rejected
                                    ? FlowFormInstanceStatus.REJECTED.getCode()
                                    : FlowFormInstanceStatus.APPROVED.getCode(),
                            business.getTenantId());
                    log.info("更新流程业务状态为{}：processInstanceId={}",
                            rejected ? "已驳回" : "已通过", processInstanceId);

                    String eventType = rejected
                            ? FlowEventMessage.PROCESS_REJECTED
                            : FlowEventMessage.PROCESS_COMPLETED;
                    FlowEventMessage msg = FlowEventMessage.ofProcess(
                            eventType,
                            processInstanceId,
                            business.getProcessDefKey(),
                            business.getBusinessKey(),
                            business.getBusinessType(),
                            business.getTitle(),
                            business.getApplyUserId(),
                            business.getApplyUserName());
                    msg.setVariables(processVariables);
                    fillTenantId(msg, business);
                    publishEvent(msg, business.getProcessDefKey());
                    // 按模型通知配置向发起人推送审批结果（未配置 result 事件时无动作）
                    eventPublisher.publishEvent(
                            FlowTaskNotifyEvent.processResult(business, processVariables, rejected));
                    if (!rejected) {
                        eventPublisher.publishEvent(FlowTaskNotifyEvent.processCc(business, processVariables));
                    }
                } else {
                    log.warn("未找到流程业务记录：processInstanceId={}", processInstanceId);
                }
            }
            
        } catch (Exception e) {
            log.error("处理流程完成事件失败", e);
            recordEventListenerError(event, "EVENT_PROCESS_COMPLETED", e);
        }
    }

    private Map<String, Object> readTaskVariables(TaskEntity task) {
        Map<String, Object> taskVariables = null;
        try {
            taskVariables = taskService.getVariables(task.getId());
        } catch (Exception e) {
            log.debug("从任务读取流程变量失败，尝试从运行实例读取: taskId={}", task.getId());
        }
        return readProcessVariables(task.getProcessInstanceId(), taskVariables);
    }

    private Map<String, Object> readProcessVariables(String processInstanceId, Map<String, Object> currentVariables) {
        Map<String, Object> variables = new LinkedHashMap<>();
        if (currentVariables != null && !currentVariables.isEmpty()) {
            variables.putAll(currentVariables);
        }
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return variables;
        }

        try {
            Map<String, Object> runtimeVariables = runtimeService.getVariables(processInstanceId);
            if (runtimeVariables != null && !runtimeVariables.isEmpty()) {
                variables.putAll(runtimeVariables);
            }
        } catch (Exception e) {
            log.debug("从运行实例读取流程变量失败，尝试从历史变量兜底: processInstanceId={}", processInstanceId);
        }

        try {
            List<HistoricVariableInstance> historicVariables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .list();
            if (historicVariables != null) {
                for (HistoricVariableInstance variable : historicVariables) {
                    variables.put(variable.getVariableName(), variable.getValue());
                }
            }
        } catch (Exception e) {
            log.debug("从历史变量读取流程变量失败: processInstanceId={}", processInstanceId);
        }
        return variables;
    }

    /**
     * 处理流程取消事件
     */
    private void handleProcessCancelled(FlowableEvent event) {
        try {
            String processInstanceId = resolveProcessInstanceId(event);
            String cancelCause = resolveCancelCause(event);
            log.info("流程取消事件：eventClass={}, processInstanceId={}",
                    event.getClass().getName(), processInstanceId);
            
            if (processInstanceId != null) {
                if (isPhysicalCleanupCancel(cancelCause)) {
                    log.info("流程物理删除场景，跳过取消状态同步和事件通知：processInstanceId={}, reason={}",
                            processInstanceId, cancelCause);
                    return;
                }

                // 更新 FlowBusiness 状态为已取消
                FlowBusiness business = getFlowBusiness(processInstanceId);
                if (business != null) {
                    business.setStatus(FlowBusinessStatus.CANCELED.getCode());
                    business.setEndTime(LocalDateTime.now());
                    if (business.getApplyTime() != null) {
                        long duration = java.time.Duration.between(
                            business.getApplyTime(),
                            business.getEndTime()
                        ).toMillis();
                        business.setDuration(duration);
                    }
                    flowBusinessMapper.updateById(business);
                    updateFormInstanceStatus(
                            processInstanceId, FlowFormInstanceStatus.CANCELED.getCode(), business.getTenantId());
                    log.info("更新流程业务状态为已取消：processInstanceId={}", processInstanceId);

                    // 发布事件
                    FlowEventMessage msg = FlowEventMessage.ofProcess(
                            FlowEventMessage.PROCESS_CANCELED,
                            processInstanceId,
                            business.getProcessDefKey(),
                            business.getBusinessKey(),
                            business.getBusinessType(),
                            business.getTitle(),
                            business.getApplyUserId(),
                            business.getApplyUserName());
                    fillTenantId(msg, business);
                    publishEvent(msg, business.getProcessDefKey());
                } else {
                    log.warn("未找到流程业务记录：processInstanceId={}", processInstanceId);
                }
            } else {
                log.warn("流程取消事件未获取到流程实例ID：eventClass={}", event.getClass().getName());
            }
            
        } catch (Exception e) {
            log.error("处理流程取消事件失败", e);
            recordEventListenerError(event, "EVENT_PROCESS_CANCELLED", e);
        }
    }

    private boolean isPhysicalCleanupCancel(String cancelCause) {
        return cancelCause != null && cancelCause.contains(PHYSICAL_CLEANUP_REASON_KEYWORD);
    }

    private String resolveCancelCause(FlowableEvent event) {
        if (event instanceof FlowableCancelledEvent) {
            Object cause = ((FlowableCancelledEvent) event).getCause();
            return cause == null ? null : String.valueOf(cause);
        }
        return null;
    }

    String resolveProcessInstanceId(FlowableEvent event) {
        if (event == null) {
            return null;
        }
        if (event instanceof FlowableEngineEvent) {
            String processInstanceId = ((FlowableEngineEvent) event).getProcessInstanceId();
            if (processInstanceId != null && !processInstanceId.isBlank()) {
                return processInstanceId;
            }
        }
        if (event instanceof FlowableProcessEngineEvent) {
            try {
                DelegateExecution execution = ((FlowableProcessEngineEvent) event).getExecution();
                if (execution != null && execution.getProcessInstanceId() != null
                        && !execution.getProcessInstanceId().isBlank()) {
                    return execution.getProcessInstanceId();
                }
            } catch (Exception e) {
                log.debug("从流程事件执行实例获取流程实例ID失败: eventClass={}", event.getClass().getName(), e);
            }
        }
        if (event instanceof FlowableEntityEvent) {
            return resolveProcessInstanceIdFromEntity(((FlowableEntityEvent) event).getEntity());
        }
        return null;
    }

    private String resolveProcessInstanceIdFromEntity(Object entity) {
        if (entity instanceof TaskEntity) {
            return ((TaskEntity) entity).getProcessInstanceId();
        }
        if (entity instanceof ExecutionEntity) {
            return ((ExecutionEntity) entity).getProcessInstanceId();
        }
        return null;
    }

    /**
     * 构建 FlowTask 对象
     */
    private FlowTask buildFlowTask(TaskEntity task) {
        FlowTask flowTask = new FlowTask();
        flowTask.setTaskId(task.getId());
        flowTask.setTaskName(task.getName());
        flowTask.setTaskDefKey(task.getTaskDefinitionKey());
        flowTask.setTaskDefId(task.getTaskDefinitionId());
        flowTask.setProcessInstanceId(task.getProcessInstanceId());
        flowTask.setProcessDefId(task.getProcessDefinitionId());
        flowTask.setProcessDefKey(extractProcessKey(task.getProcessDefinitionId()));
        flowTask.setAssignee(normalizeTaskUserId(task.getAssignee(), task.getId(), "assignee"));
        flowTask.setAssigneeName(resolveUserDisplayName(flowTask.getAssignee(), null));
        flowTask.setOwner(normalizeTaskUserId(task.getOwner(), task.getId(), "owner"));
        flowTask.setCreateTime(LocalDateTime.now());
        
        if (task.getDueDate() != null) {
            flowTask.setDueDate(LocalDateTime.ofInstant(
                    task.getDueDate().toInstant(),
                    resolveTimeoutZone()));
        }
        
        flowTask.setPriority(task.getPriority());
        
        // 获取候选人/候选组信息
        try {
            List<org.flowable.identitylink.api.IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
            if (identityLinks != null && !identityLinks.isEmpty()) {
                List<String> candidateUsers = identityLinks.stream()
                        .filter(link -> "candidate".equals(link.getType()) && link.getUserId() != null)
                        .map(org.flowable.identitylink.api.IdentityLink::getUserId)
                        .collect(Collectors.toList());
                
                List<String> candidateGroups = identityLinks.stream()
                        .filter(link -> "candidate".equals(link.getType()) && link.getGroupId() != null)
                        .map(org.flowable.identitylink.api.IdentityLink::getGroupId)
                        .collect(Collectors.toList());
                
                if (!candidateUsers.isEmpty()) {
                    flowTask.setCandidateUsers(String.join(",", candidateUsers));
                    log.debug("任务候选人：taskId={}, candidateUsers={}", task.getId(), candidateUsers);
                }
                if (!candidateGroups.isEmpty()) {
                    flowTask.setCandidateGroups(String.join(",", candidateGroups));
                    log.debug("任务候选组：taskId={}, candidateGroups={}", task.getId(), candidateGroups);
                }
            }
        } catch (Exception e) {
            log.warn("获取任务候选人信息失败：taskId={}", task.getId(), e);
        }
        
        return flowTask;
    }

    /** 将 Flowable 候选身份同步到规范化关系表，逗号字段继续保留兼容读取。 */
    private void syncCandidateRelations(FlowTask flowTask) {
        if (flowTaskCandidateMapper == null || flowTask == null
                || flowTask.getTenantId() == null || flowTask.getTaskId() == null) {
            return;
        }
        syncCandidateValues(flowTask, flowTask.getCandidateUsers(), FlowTaskCandidate.TYPE_USER);
        syncCandidateValues(flowTask, flowTask.getCandidateGroups(), FlowTaskCandidate.TYPE_GROUP);
    }

    private void syncCandidateValues(FlowTask flowTask, String values, String candidateType) {
        if (values == null || values.isBlank()) {
            return;
        }
        for (String raw : values.split("[,;，；、]")) {
            String value = raw == null ? null : raw.trim();
            if (value == null || value.isEmpty()) {
                continue;
            }
            FlowTaskCandidate candidate = new FlowTaskCandidate();
            candidate.setTenantId(flowTask.getTenantId());
            candidate.setTaskId(flowTask.getTaskId());
            candidate.setProcessInstanceId(flowTask.getProcessInstanceId());
            candidate.setCandidateType(candidateType);
            candidate.setCandidateValue(value);
            candidate.setSource(FlowTaskCandidate.SOURCE_FLOWABLE);
            candidate.setStatus(FlowTaskCandidateStatus.ACTIVE.getCode());
            candidate.setCreateTime(flowTask.getCreateTime());
            candidate.setUpdateTime(LocalDateTime.now());
            try {
                flowTaskCandidateMapper.insertIgnore(candidate);
            } catch (Exception e) {
                log.warn("同步流程任务候选关系失败，不阻断任务创建：taskId={}, type={}",
                        flowTask.getTaskId(), candidateType, e);
            }
        }
    }

    private ZoneId resolveTimeoutZone() {
        try {
            return ZoneId.of(timeoutTimeZone);
        } catch (Exception e) {
            log.warn("流程超时配置时区非法，回退 Asia/Shanghai: {}", timeoutTimeZone);
            return ZoneId.of("Asia/Shanghai");
        }
    }

    /** 将节点超时配置落到 Flowable 原生 dueDate，供扫描器按索引筛选。 */
    private void applyConfiguredDueDate(TaskEntity task) {
        if (flowNodeConfigService == null || task.getProcessDefinitionId() == null
                || task.getTaskDefinitionKey() == null) {
            return;
        }
        try {
            String processDefinitionKey = task.getProcessDefinitionId().split(":")[0];
            FlowNodeConfig config = flowNodeConfigService.getByModelAndNode(
                    processDefinitionKey, task.getTaskDefinitionKey());
            if (config == null) {
                return;
            }
            Long timeoutMillis = flowNodeConfigService.getTimeoutMillis(config.getId());
            if (timeoutMillis == null || timeoutMillis <= 0) {
                return;
            }
            Date createTime = task.getCreateTime() == null ? new Date() : task.getCreateTime();
            Date dueDate = new Date(createTime.getTime() + timeoutMillis);
            task.setDueDate(dueDate);
            taskService.setDueDate(task.getId(), dueDate);
        } catch (Exception e) {
            log.warn("写入任务截止时间失败，保留超时扫描兜底: taskId={}", task.getId(), e);
        }
    }

    private void updateFormInstanceStatus(String processInstanceId, String status, Long tenantId) {
        if (flowFormInstanceMapper == null || processInstanceId == null || processInstanceId.isBlank()
                || tenantId == null || tenantId <= 0) {
            return;
        }
        try {
            flowFormInstanceMapper.updateStatusByProcessInstanceId(processInstanceId, status, tenantId);
        } catch (Exception e) {
            log.warn("更新流程表单实例状态失败: processInstanceId={}, status={}", processInstanceId, status, e);
        }
    }

    private String normalizeTaskUserId(String value, String taskId, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        String text = value.trim();
        if (isNumeric(text)) {
            return text;
        }
        if (flowOrgIntegrationService == null) {
            log.warn("任务{}不是用户ID且组织集成不可用：taskId={}, {}={}", fieldName, taskId, fieldName, text);
            return text;
        }
        try {
            List<Map<String, Object>> users = flowOrgIntegrationService.getUserList(text, null);
            List<String> exactUserIds = users.stream()
                    .filter(user -> matchesUser(text, user))
                    .map(user -> Objects.toString(user.get("id"), null))
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (exactUserIds.size() == 1) {
                String userId = exactUserIds.get(0);
                log.info("任务{}已从显示值归一为用户ID：taskId={}, raw={}, userId={}", fieldName, taskId, text, userId);
                return userId;
            }
            log.warn("任务{}无法唯一归一为用户ID：taskId={}, raw={}, matches={}", fieldName, taskId, text, exactUserIds.size());
        } catch (Exception e) {
            log.warn("任务{}归一用户ID失败：taskId={}, raw={}", fieldName, taskId, text, e);
        }
        return text;
    }

    private boolean matchesUser(String value, Map<String, Object> user) {
        if (user == null) {
            return false;
        }
        return value.equals(Objects.toString(user.get("id"), null))
                || value.equals(Objects.toString(user.get("username"), null))
                || value.equals(Objects.toString(user.get("name"), null))
                || value.equals(Objects.toString(user.get("realName"), null));
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String resolveUserDisplayName(String userId, String fallback) {
        if (userId != null && !userId.isBlank() && flowOrgIntegrationService != null) {
            try {
                Map<String, Object> userInfo = flowOrgIntegrationService.getUserInfo(userId.trim());
                if (userInfo != null) {
                    String name = firstNonBlank(
                            userInfo.get("realName"), userInfo.get("name"), userInfo.get("nickname"));
                    if (name != null && !name.isBlank()) {
                        return name;
                    }
                }
            } catch (Exception e) {
                log.debug("反查流程用户姓名失败: userId={}", userId, e);
            }
        }
        return fallback != null && !fallback.isBlank()
                ? fallback.trim()
                : userId;
    }

    private String firstNonBlank(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return String.valueOf(value).trim();
            }
        }
        return null;
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

    /**
     * 获取流程业务信息
     */
    private FlowBusiness getFlowBusiness(String processInstanceId) {
        try {
            return flowBusinessMapper.selectByProcessInstanceId(processInstanceId);
        } catch (Exception e) {
            log.warn("获取流程业务信息失败：processInstanceId={}", processInstanceId);
            return null;
        }
    }
    
    /**
     * 根据业务Key获取流程业务信息
     */
    private FlowBusiness getFlowBusinessByBusinessKey(String businessKey) {
        try {
            return flowBusinessMapper.selectByBusinessKey(businessKey);
        } catch (Exception e) {
            log.warn("获取流程业务信息失败：businessKey={}", businessKey);
            return null;
        }
    }

    /**
     * 发布 FlowModel 配置化事件通知（Redis Pub/Sub / HTTP Webhook）
     *
     * <p>实际的 FlowModel 查询与外部调用由 FlowTaskNotifyListener 在事务提交后异步执行</p>
     *
     * @param message       流程事件消息
     * @param processDefKey 流程定义 Key，用于查询 FlowModel 配置
     */
    private void publishEvent(FlowEventMessage message, String processDefKey) {
        if (processDefKey == null) {
            return;
        }
        eventPublisher.publishEvent(FlowTaskNotifyEvent.eventPublish(message, processDefKey));
    }

    private void fillTenantId(FlowEventMessage message, FlowBusiness business) {
        if (message != null && business != null && business.getTenantId() != null) {
            message.setTenantId(String.valueOf(business.getTenantId()));
        }
    }

    private void recordAssignee(FlowBusiness business, TaskEntity task, FlowTask flowTask) {
        if (flowRecordParticipantService == null || business == null) {
            return;
        }
        String userId = task != null && task.getAssignee() != null && !task.getAssignee().isBlank()
                ? task.getAssignee()
                : (flowTask == null ? null : flowTask.getAssignee());
        flowRecordParticipantService.record(business, userId, FlowRecordParticipant.ASSIGNEE);
    }

    /**
     * 安全记录事件监听器中的错误，避免记录错误本身再次抛出异常
     */
    private void recordEventListenerError(FlowableEvent event, String errorStage, Throwable e) {
        try {
            FlowErrorLog errorLog = new FlowErrorLog();
            errorLog.setErrorStage(errorStage);
            String processInstanceId = resolveProcessInstanceId(event);
            if (processInstanceId != null) {
                errorLog.setProcessInstanceId(processInstanceId);
            }
            if (event instanceof FlowableEntityEvent) {
                Object entity = ((FlowableEntityEvent) event).getEntity();
                if (entity instanceof TaskEntity) {
                    TaskEntity task = (TaskEntity) entity;
                    errorLog.setTaskId(task.getId());
                    errorLog.setActivityId(task.getTaskDefinitionKey());
                    errorLog.setActivityName(task.getName());
                }
            }
            flowErrorLogService.recordError(errorLog, e);
        } catch (Exception ex) {
            log.warn("记录事件错误日志失败", ex);
        }
    }

    @Override
    public boolean isFailOnException() {
        return false; // 不因监听器异常中断流程
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }
}
