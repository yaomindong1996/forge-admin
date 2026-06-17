package com.mdframe.forge.starter.flow.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowErrorLog;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.core.domain.FlowEventMessage;
import com.mdframe.forge.starter.flow.event.FlowEventPublisher;
import com.mdframe.forge.starter.flow.event.FlowWebhookNotifier;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowFormInstanceMapper;
import com.mdframe.forge.starter.flow.mapper.FlowModelMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowErrorLogService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
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
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程任务事件监听器
 * 监听任务的创建、完成、删除等事件，同步数据到业务表
 */
@Slf4j
@Component
public class FlowTaskEventListener implements FlowableEventListener {

    private static final String PHYSICAL_CLEANUP_REASON_KEYWORD = "删除流程数据";

    @Autowired
    @Lazy
    private FlowTaskMapper flowTaskMapper;

    @Autowired
    @Lazy
    private FlowBusinessMapper flowBusinessMapper;

    @Autowired
    @Lazy
    private FlowModelMapper flowModelMapper;
    
    @Autowired
    @Lazy
    private TaskService taskService;
    
    @Autowired
    @Lazy
    private RuntimeService runtimeService;

    /** Redis Pub/Sub 发布器（可选，未引入 Redis 依赖时为 null）*/
    @Autowired(required = false)
    @Lazy
    private FlowEventPublisher flowEventPublisher;

    /** HTTP Webhook 回调器 */
    @Autowired
    @Lazy
    private FlowWebhookNotifier flowWebhookNotifier;

    @Autowired
    @Lazy
    private FlowErrorLogService flowErrorLogService;

    @Autowired(required = false)
    @Lazy
    private FlowOrgIntegrationService flowOrgIntegrationService;

    @Autowired(required = false)
    @Lazy
    private MessageService messageService;

    @Autowired(required = false)
    @Lazy
    private FlowFormInstanceMapper flowFormInstanceMapper;

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
            log.info("========== 任务创建事件 ==========");
            log.info("taskId={}, name={}, assignee={}",
                    task.getId(), task.getName(), task.getAssignee());
            log.info("processInstanceId={}, processDefinitionId={}",
                    task.getProcessInstanceId(), task.getProcessDefinitionId());
            
            // 检查是否已存在
            FlowTask existingTask = flowTaskMapper.selectByTaskId(task.getId());
            if (existingTask != null) {
                log.debug("任务已存在，跳过创建：taskId={}", task.getId());
                return;
            }
            
            // 创建任务记录
            FlowTask flowTask = buildFlowTask(task);
            flowTask.setStatus(0); // 待办状态
            
            log.info("任务处理人: {}, 候选人: {}, 候选组: {}",
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
                            log.info("通过 businessKey 查询业务信息: businessKey={}, business={}", businessKey, business);
                        }
                    }
                } catch (Exception e) {
                    log.warn("获取 ProcessInstance 失败: processInstanceId={}", task.getProcessInstanceId(), e);
                }
            } else {
                log.info("通过 processInstanceId 查询业务信息: processInstanceId={}, business={}", task.getProcessInstanceId(), business);
            }
            
            if (business != null) {
                flowTask.setTitle(business.getTitle());
                flowTask.setBusinessKey(business.getBusinessKey());
                flowTask.setBusinessType(business.getBusinessType());
                flowTask.setStartUserId(business.getApplyUserId());
                flowTask.setStartUserName(business.getApplyUserName());
                flowTask.setStartDeptId(business.getApplyDeptId());
                flowTask.setStartDeptName(business.getApplyDeptName());
                log.info("业务信息: title={}, businessKey={}, applyUserId={}, applyUserName={}",
                        business.getTitle(), business.getBusinessKey(), business.getApplyUserId(), business.getApplyUserName());
            } else {
                // 没有业务信息时设置默认値
                flowTask.setTitle(task.getName() + " - " + task.getProcessDefinitionId().split(":")[0]);
                log.warn("未找到业务信息，使用默认标题: processInstanceId={}, title={}",
                        task.getProcessInstanceId(), flowTask.getTitle());
            }
            
            flowTaskMapper.insert(flowTask);
            log.info("创建待办任务成功：taskId={}, title={}, assignee={}, candidateUsers={}, candidateGroups={}",
                    task.getId(), flowTask.getTitle(), flowTask.getAssignee(), flowTask.getCandidateUsers(), flowTask.getCandidateGroups());
            sendTaskCreatedMessage(flowTask);
            log.info("==================================");
    
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
            
            // 更新任务状态
            FlowTask flowTask = flowTaskMapper.selectByTaskId(task.getId());
            if (flowTask != null) {
                flowTask.setStatus(2); // 已完成
                flowTask.setCompleteTime(LocalDateTime.now());
                flowTaskMapper.updateById(flowTask);
                log.info("更新任务状态为已完成：taskId={}", task.getId());
            } else {
                // 任务不存在，创建已完成的记录
                flowTask = buildFlowTask(task);
                flowTask.setStatus(2); // 已完成
                flowTask.setCompleteTime(LocalDateTime.now());
                
                FlowBusiness business = getFlowBusiness(task.getProcessInstanceId());
                if (business != null) {
                    flowTask.setTitle(business.getTitle());
                    flowTask.setBusinessKey(business.getBusinessKey());
                    flowTask.setBusinessType(business.getBusinessType());
                    flowTask.setStartUserId(business.getApplyUserId());
                    flowTask.setStartUserName(business.getApplyUserName());
                }
                
                flowTaskMapper.insert(flowTask);
                log.info("创建已完成任务记录：taskId={}", task.getId());
            }

            // 发布 TASK_COMPLETED 事件，业务侧可监听具体节点完成情况（如：更新业务表审批节点状态等）
            FlowBusiness completedBusiness = getFlowBusiness(task.getProcessInstanceId());
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
                publishEvent(msg, flowTask.getProcessDefKey());
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
                flowTask.setOwner(owner);
                
                if (assignee != null) {
                    if (owner != null && !owner.equals(assignee)) {
                        log.info("转派任务（owner存在且不同于assignee），保持待办状态：taskId={}, owner={}, assignee={}", 
                                task.getId(), owner, assignee);
                        flowTask.setStatus(0);
                    } else {
                        log.info("任务签收（无owner或owner=assignee），设为已签收状态：taskId={}, assignee={}", 
                                task.getId(), assignee);
                        flowTask.setStatus(1);
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
                    flowTask.setStatus(5); // 已取消
                    flowTask.setCompleteTime(LocalDateTime.now());
                    flowTaskMapper.updateById(flowTask);
                    log.info("更新任务状态为已取消：taskId={}", task.getId());
                }
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
            
            // PROCESS_COMPLETED 事件的 entity 是 ExecutionEntity
            if (entity instanceof org.flowable.engine.impl.persistence.entity.ExecutionEntity) {
                org.flowable.engine.impl.persistence.entity.ExecutionEntity execution =
                    (org.flowable.engine.impl.persistence.entity.ExecutionEntity) entity;
                processInstanceId = execution.getProcessInstanceId();
                log.info("流程完成事件：processInstanceId={}", processInstanceId);
            }
            
            if (processInstanceId != null) {
                // 更新 FlowBusiness 状态为已通过
                FlowBusiness business = getFlowBusiness(processInstanceId);
                if (business != null) {
                    business.setStatus("approved"); // 已通过
                    business.setEndTime(LocalDateTime.now());
                    if (business.getApplyTime() != null) {
                        long duration = java.time.Duration.between(
                            business.getApplyTime(),
                            business.getEndTime()
                        ).toMillis();
                        business.setDuration(duration);
                    }
                    flowBusinessMapper.updateById(business);
                    updateFormInstanceStatus(processInstanceId, "APPROVED");
                    log.info("更新流程业务状态为已通过：processInstanceId={}", processInstanceId);

                    // 发布事件（方案B: Redis Pub/Sub ＋ 方案C: HTTP Webhook）
                    FlowEventMessage msg = FlowEventMessage.ofProcess(
                            FlowEventMessage.PROCESS_COMPLETED,
                            processInstanceId,
                            business.getProcessDefKey(),
                            business.getBusinessKey(),
                            business.getBusinessType(),
                            business.getTitle(),
                            business.getApplyUserId(),
                            business.getApplyUserName());
                    publishEvent(msg, business.getProcessDefKey());
                } else {
                    log.warn("未找到流程业务记录：processInstanceId={}", processInstanceId);
                }
            }
            
        } catch (Exception e) {
            log.error("处理流程完成事件失败", e);
            recordEventListenerError(event, "EVENT_PROCESS_COMPLETED", e);
        }
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
                    business.setStatus("canceled"); // 已取消
                    business.setEndTime(LocalDateTime.now());
                    if (business.getApplyTime() != null) {
                        long duration = java.time.Duration.between(
                            business.getApplyTime(),
                            business.getEndTime()
                        ).toMillis();
                        business.setDuration(duration);
                    }
                    flowBusinessMapper.updateById(business);
                    updateFormInstanceStatus(processInstanceId, "CANCELED");
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
        flowTask.setOwner(normalizeTaskUserId(task.getOwner(), task.getId(), "owner"));
        flowTask.setCreateTime(LocalDateTime.now());
        
        if (task.getDueDate() != null) {
            flowTask.setDueDate(LocalDateTime.ofInstant(
                    task.getDueDate().toInstant(),
                    java.time.ZoneId.systemDefault()));
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

    private void sendTaskCreatedMessage(FlowTask flowTask) {
        if (messageService == null || flowTask == null || flowTask.getTaskId() == null) {
            return;
        }
        Set<Long> receiverIds = resolveTaskMessageReceivers(flowTask);
        if (receiverIds.isEmpty()) {
            log.warn("待办任务没有可推送的站内信接收人: taskId={}, assignee={}, candidateUsers={}, candidateGroups={}",
                    flowTask.getTaskId(), flowTask.getAssignee(), flowTask.getCandidateUsers(), flowTask.getCandidateGroups());
            return;
        }

        MessageSendRequestDTO request = new MessageSendRequestDTO();
        request.setTitle("您有新的流程待办");
        request.setContent("您有一个待办任务需要处理：" + safeText(flowTask.getTitle(), flowTask.getTaskName()));
        request.setType("SYSTEM");
        request.setChannel("WEB");
        request.setSendScope("USERS");
        request.setUserIds(receiverIds);
        request.setParams(Map.of(
                "taskId", flowTask.getTaskId(),
                "processInstanceId", safeText(flowTask.getProcessInstanceId(), ""),
                "jumpUrl", "/flow/todo?taskId=" + flowTask.getTaskId()
        ));
        try {
            messageService.sendIfAbsent(request, "FLOW_TODO", flowTask.getTaskId());
            log.info("待办站内信已推送: taskId={}, receivers={}", flowTask.getTaskId(), receiverIds);
        } catch (Exception e) {
            log.warn("待办站内信推送失败，不阻断流程: taskId={}", flowTask.getTaskId(), e);
        }
    }

    private Set<Long> resolveTaskMessageReceivers(FlowTask flowTask) {
        Set<Long> receiverIds = new HashSet<>();
        addUserId(receiverIds, flowTask.getAssignee());
        addUserIds(receiverIds, flowTask.getCandidateUsers());
        if (receiverIds.isEmpty()) {
            addCandidateGroupUsers(receiverIds, flowTask.getCandidateGroups());
        }
        return receiverIds;
    }

    private void addUserIds(Set<Long> receiverIds, String csv) {
        if (csv == null || csv.isBlank()) {
            return;
        }
        for (String item : csv.split(",")) {
            addUserId(receiverIds, item);
        }
    }

    private void addUserId(Set<Long> receiverIds, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            receiverIds.add(Long.parseLong(raw.trim()));
        } catch (NumberFormatException e) {
            log.warn("待办站内信接收人不是数值用户ID，已跳过: raw={}", raw);
        }
    }

    private void addCandidateGroupUsers(Set<Long> receiverIds, String candidateGroups) {
        if (flowOrgIntegrationService == null || candidateGroups == null || candidateGroups.isBlank()) {
            return;
        }
        for (String rawGroup : candidateGroups.split(",")) {
            String group = rawGroup == null ? null : rawGroup.trim();
            if (group == null || group.isEmpty()) {
                continue;
            }
            List<String> userIds = resolveGroupUsers(group);
            for (String userId : userIds) {
                addUserId(receiverIds, userId);
            }
        }
    }

    private List<String> resolveGroupUsers(String group) {
        try {
            List<String> roleUsers = isNumeric(group)
                    ? flowOrgIntegrationService.getUserIdsByRoleId(group)
                    : flowOrgIntegrationService.getUserIdsByRoleCode(group);
            if (roleUsers != null && !roleUsers.isEmpty()) {
                return roleUsers;
            }
            if (isNumeric(group)) {
                List<String> deptUsers = flowOrgIntegrationService.getUserIdsByDeptId(group);
                if (deptUsers != null) {
                    return deptUsers;
                }
            }
        } catch (Exception e) {
            log.warn("解析候选组用户失败: group={}", group, e);
        }
        return List.of();
    }

    private void updateFormInstanceStatus(String processInstanceId, String status) {
        if (flowFormInstanceMapper == null || processInstanceId == null || processInstanceId.isBlank()) {
            return;
        }
        try {
            flowFormInstanceMapper.updateStatusByProcessInstanceId(processInstanceId, status);
        } catch (Exception e) {
            log.warn("更新流程表单实例状态失败: processInstanceId={}, status={}", processInstanceId, status, e);
        }
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
     * 统一发布流程事件：根据 FlowModel.notifyType 互斥选择通知方式
     *
     * <ul>
     *   <li>{@code redis}   → 方案B: Redis Pub/Sub</li>
     *   <li>{@code webhook} → 方案C: HTTP Webhook（读取 FlowModel.webhookUrl）</li>
     *   <li>{@code none} 或未配置 → 不发送任何通知</li>
     * </ul>
     *
     * @param message       流程事件消息
     * @param processDefKey 流程定义 Key，用于查询 FlowModel 配置
     */
    private void publishEvent(FlowEventMessage message, String processDefKey) {
        if (processDefKey == null) {
            return;
        }
        try {
            FlowModel model = flowModelMapper.selectOne(
                    new LambdaQueryWrapper<FlowModel>()
                            .eq(FlowModel::getModelKey, processDefKey)
                            .last("LIMIT 1"));
            if (model == null) {
                log.debug("[FlowEvent] 未找到 FlowModel 配置，跳过通知: processDefKey={}", processDefKey);
                return;
            }

            String notifyType = model.getNotifyType();
            if (notifyType == null || "none".equalsIgnoreCase(notifyType)) {
                log.debug("[FlowEvent] notifyType=none，跳过通知: processDefKey={}", processDefKey);
                return;
            }

            // 方案B: Redis Pub/Sub
            if ("redis".equalsIgnoreCase(notifyType)) {
                if (flowEventPublisher != null) {
                    flowEventPublisher.publish(message);
                } else {
                    log.warn("[FlowEvent] notifyType=redis 但 FlowEventPublisher 未初始化（请确认已引入 spring-boot-starter-data-redis）");
                }
                return;
            }

            // 方案C: HTTP Webhook
            if ("webhook".equalsIgnoreCase(notifyType)) {
                if (model.getWebhookUrl() != null && !model.getWebhookUrl().isBlank()) {
                    flowWebhookNotifier.notify(model.getWebhookUrl(), message);
                } else {
                    log.warn("[FlowEvent] notifyType=webhook 但 webhookUrl 未配置: processDefKey={}", processDefKey);
                }
                return;
            }

            log.warn("[FlowEvent] 未知的 notifyType={}，跳过通知", notifyType);

        } catch (Exception e) {
            log.warn("[FlowEvent] 发布事件失败，不影响主流程: processDefKey={}, error={}", processDefKey, e.getMessage(), e);
        }
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
