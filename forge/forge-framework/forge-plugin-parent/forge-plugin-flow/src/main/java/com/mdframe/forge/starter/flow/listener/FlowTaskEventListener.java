package com.mdframe.forge.starter.flow.listener;

import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程任务事件监听器
 * 监听任务的创建、完成、删除等事件，同步数据到业务表
 */
@Slf4j
@Component
public class FlowTaskEventListener implements FlowableEventListener {

    @Autowired
    @Lazy
    private FlowTaskMapper flowTaskMapper;

    @Autowired
    @Lazy
    private FlowBusinessMapper flowBusinessMapper;
    
    @Autowired
    @Lazy
    private TaskService taskService;
    
    @Autowired
    @Lazy
    private RuntimeService runtimeService;

    @Override
    public void onEvent(FlowableEvent event) {
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
                // 没有业务信息时设置默认值
                flowTask.setTitle(task.getName() + " - " + task.getProcessDefinitionId().split(":")[0]);
                log.warn("未找到业务信息，使用默认标题: processInstanceId={}, title={}",
                        task.getProcessInstanceId(), flowTask.getTitle());
            }
            
            flowTaskMapper.insert(flowTask);
            log.info("创建待办任务成功：taskId={}, title={}, assignee={}, candidateUsers={}, candidateGroups={}",
                    task.getId(), flowTask.getTitle(), flowTask.getAssignee(), flowTask.getCandidateUsers(), flowTask.getCandidateGroups());
            log.info("==================================");
            
        } catch (Exception e) {
            log.error("处理任务创建事件失败", e);
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
            
        } catch (Exception e) {
            log.error("处理任务完成事件失败", e);
        }
    }

    /**
     * 处理任务分配事件
     */
    private void handleTaskAssigned(FlowableEvent event) {
        try {
            TaskEntity task = (TaskEntity) ((FlowableEntityEvent) event).getEntity();
            log.info("任务分配事件：taskId={}, assignee={}", task.getId(), task.getAssignee());
            
            // 更新任务处理人
            FlowTask flowTask = flowTaskMapper.selectByTaskId(task.getId());
            if (flowTask != null) {
                flowTask.setAssignee(task.getAssignee());
                if (task.getAssignee() != null) {
                    flowTask.setStatus(1); // 已签收
                    flowTask.setClaimTime(LocalDateTime.now());
                }
                flowTaskMapper.updateById(flowTask);
                log.info("更新任务处理人：taskId={}, assignee={}", task.getId(), task.getAssignee());
            }
            
        } catch (Exception e) {
            log.error("处理任务分配事件失败", e);
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
                    log.info("更新流程业务状态为已通过：processInstanceId={}", processInstanceId);
                } else {
                    log.warn("未找到流程业务记录：processInstanceId={}", processInstanceId);
                }
            }
            
        } catch (Exception e) {
            log.error("处理流程完成事件失败", e);
        }
    }

    /**
     * 处理流程取消事件
     */
    private void handleProcessCancelled(FlowableEvent event) {
        try {
            Object entity = ((FlowableEntityEvent) event).getEntity();
            String processInstanceId = null;
            
            // PROCESS_CANCELLED 事件的 entity 是 ExecutionEntity
            if (entity instanceof org.flowable.engine.impl.persistence.entity.ExecutionEntity) {
                org.flowable.engine.impl.persistence.entity.ExecutionEntity execution =
                    (org.flowable.engine.impl.persistence.entity.ExecutionEntity) entity;
                processInstanceId = execution.getProcessInstanceId();
                log.info("流程取消事件：processInstanceId={}", processInstanceId);
            }
            
            if (processInstanceId != null) {
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
                    log.info("更新流程业务状态为已取消：processInstanceId={}", processInstanceId);
                } else {
                    log.warn("未找到流程业务记录：processInstanceId={}", processInstanceId);
                }
            }
            
        } catch (Exception e) {
            log.error("处理流程取消事件失败", e);
        }
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
        flowTask.setAssignee(task.getAssignee());
        flowTask.setOwner(task.getOwner());
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