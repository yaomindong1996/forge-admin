package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import com.mdframe.forge.starter.flow.service.FlowNodeConfigService;
import com.mdframe.forge.starter.flow.service.FlowTimeoutService;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程超时处理服务实现
 *
 * @author forge
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowTimeoutServiceImpl implements FlowTimeoutService {

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final FlowNodeConfigService flowNodeConfigService;

    /**
     * 定时检查超时任务（每5分钟执行一次）
     */
    @Override
    //@Scheduled(fixedRate = 300000)
    @IgnoreTenant
    public void checkAndHandleTimeoutTasks() {
        log.debug("开始检查超时任务...");
        
        TenantContextHolder.executeIgnore(() -> {
            try {
                // 查询所有活动任务
                List<Task> activeTasks = taskService.createTaskQuery()
                        .active()
                        .list();
                
                for (Task task : activeTasks) {
                    try {
                        checkTaskTimeout(task);
                    } catch (Exception e) {
                        log.error("检查任务超时失败: taskId={}, error={}", task.getId(), e.getMessage());
                    }
                }
                
                log.debug("超时任务检查完成，共检查 {} 个任务", activeTasks.size());
            } catch (Exception e) {
                log.error("检查超时任务异常", e);
            }
        });
        
    }

    /**
     * 检查单个任务是否超时
     */
    private void checkTaskTimeout(Task task) {
        // 获取流程定义ID和节点ID
        String processDefinitionId = task.getProcessDefinitionId();
        String taskDefinitionKey = task.getTaskDefinitionKey();
        
        // 获取流程实例以获取模型ID
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();
        
        if (processInstance == null) {
            return;
        }
        
        // 从流程定义ID中提取模型Key
        String processKey = processDefinitionId.split(":")[0];
        
        // 获取节点配置
        FlowNodeConfig nodeConfig = flowNodeConfigService.getByModelAndNode(
                processInstance.getProcessDefinitionKey(), taskDefinitionKey);
        
        if (nodeConfig == null || nodeConfig.getTimeoutAction() == null
                || "none".equals(nodeConfig.getTimeoutAction())) {
            return;
        }
        
        // 计算超时时间
        Long timeoutMillis = flowNodeConfigService.getTimeoutMillis(nodeConfig.getId());
        if (timeoutMillis == null || timeoutMillis <= 0) {
            return;
        }
        
        // 获取任务创建时间
        Date createTime = task.getCreateTime();
        if (createTime == null) {
            return;
        }
        
        // 计算是否超时
        long elapsed = System.currentTimeMillis() - createTime.getTime();
        if (elapsed >= timeoutMillis) {
            log.info("任务已超时: taskId={}, taskName={}, elapsed={}ms, timeout={}ms",
                    task.getId(), task.getName(), elapsed, timeoutMillis);
            handleTimeoutTask(task.getId(), nodeConfig.getTimeoutAction());
        }
    }

    @Override
    public boolean handleTimeoutTask(String taskId, String timeoutAction) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            log.warn("任务不存在: {}", taskId);
            return false;
        }
        
        try {
            switch (timeoutAction) {
                case "auto_pass":
                    // 自动通过
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("timeout_auto_pass", true);
                    variables.put("timeout_action", "auto_pass");
                    taskService.complete(taskId, variables);
                    log.info("超时任务自动通过: taskId={}", taskId);
                    break;
                    
                case "auto_reject":
                    // 自动拒绝
                    Map<String, Object> rejectVariables = new HashMap<>();
                    rejectVariables.put("timeout_auto_reject", true);
                    rejectVariables.put("timeout_action", "auto_reject");
                    taskService.complete(taskId, rejectVariables);
                    log.info("超时任务自动拒绝: taskId={}", taskId);
                    break;
                    
                case "notify":
                    // 发送通知
                    sendTimeoutNotification(taskId, "system");
                    log.info("超时任务已发送通知: taskId={}", taskId);
                    break;
                    
                default:
                    log.warn("未知的超时动作: {}", timeoutAction);
                    return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("处理超时任务失败: taskId={}, action={}", taskId, timeoutAction, e);
            return false;
        }
    }

    @Override
    public boolean sendTimeoutNotification(String taskId, String notifyType) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            log.warn("任务不存在: {}", taskId);
            return false;
        }
        
        String assignee = task.getAssignee();
        if (assignee == null || assignee.isEmpty()) {
            // 如果没有指定办理人，获取候选人
            List<String> candidates = getCandidateUsers(taskId);
            if (candidates.isEmpty()) {
                log.warn("任务没有办理人或候选人: {}", taskId);
                return false;
            }
            assignee = String.join(",", candidates);
        }
        
        String message = String.format("您的审批任务【%s】即将超时，请及时处理！", task.getName());
        
        try {
            switch (notifyType) {
                case "email":
                    // TODO: 发送邮件通知
                    log.info("发送邮件超时通知: to={}, message={}", assignee, message);
                    break;
                    
                case "sms":
                    // TODO: 发送短信通知
                    log.info("发送短信超时通知: to={}, message={}", assignee, message);
                    break;
                    
                case "system":
                    // TODO: 发送系统消息
                    log.info("发送系统超时通知: to={}, message={}", assignee, message);
                    break;
                    
                default:
                    log.warn("未知的通知类型: {}", notifyType);
                    return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("发送超时通知失败: taskId={}, notifyType={}", taskId, notifyType, e);
            return false;
        }
    }

    @Override
    public List<String> getUpcomingTimeoutTasks(int advanceMinutes) {
        List<Task> activeTasks = taskService.createTaskQuery()
                .active()
                .list();
        
        List<String> upcomingTaskIds = new ArrayList<>();
        long advanceMillis = advanceMinutes * 60L * 1000;
        
        for (Task task : activeTasks) {
            Long remainingTime = getRemainingTime(task.getId());
            if (remainingTime != null && remainingTime > 0 && remainingTime <= advanceMillis) {
                upcomingTaskIds.add(task.getId());
            }
        }
        
        return upcomingTaskIds;
    }

    @Override
    public Long getRemainingTime(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return null;
        }
        
        // 获取流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();
        
        if (processInstance == null) {
            return null;
        }
        
        // 获取节点配置
        FlowNodeConfig nodeConfig = flowNodeConfigService.getByModelAndNode(
                processInstance.getProcessDefinitionKey(), task.getTaskDefinitionKey());
        
        if (nodeConfig == null) {
            return null;
        }
        
        Long timeoutMillis = flowNodeConfigService.getTimeoutMillis(nodeConfig.getId());
        if (timeoutMillis == null || timeoutMillis <= 0) {
            return null;
        }
        
        Date createTime = task.getCreateTime();
        if (createTime == null) {
            return null;
        }
        
        long elapsed = System.currentTimeMillis() - createTime.getTime();
        return timeoutMillis - elapsed;
    }

    /**
     * 获取任务的候选人列表
     */
    private List<String> getCandidateUsers(String taskId) {
        // 获取候选用户
        List<String> candidateUsers = taskService.getIdentityLinksForTask(taskId).stream()
                .filter(link -> "candidate".equals(link.getType()) && link.getUserId() != null)
                .map(link -> link.getUserId())
                .collect(Collectors.toList());
        
        return candidateUsers;
    }
}
