package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowTaskService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
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
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
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

    @Override
    public IPage<FlowTask> todoTasks(Page<FlowTask> page, String userId, String title, String category) {
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowTask::getAssignee, userId)
                .eq(FlowTask::getStatus, 0)
                .like(title != null, FlowTask::getTitle, title)
                .orderByDesc(FlowTask::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public IPage<FlowTask> doneTasks(Page<FlowTask> page, String userId, String title, String category) {
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowTask::getAssignee, userId)
                .in(FlowTask::getStatus, Arrays.asList(2, 3, 4, 5))
                .like(title != null, FlowTask::getTitle, title)
                .orderByDesc(FlowTask::getCompleteTime);
        return page(page, wrapper);
    }

    @Override
    public IPage<FlowTask> startedTasks(Page<FlowTask> page, String userId, String title, String category) {
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowTask::getStartUserId, userId)
                .like(title != null, FlowTask::getTitle, title)
                .orderByDesc(FlowTask::getCreateTime);
        return page(page, wrapper);
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
        
        return page(page, wrapper);
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
    public void approve(String taskId, String userId, String comment, Map<String, Object> variables) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }

        if (comment != null && !comment.isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        }

        if (variables != null && !variables.isEmpty()) {
            taskService.complete(taskId, variables);
        } else {
            taskService.complete(taskId);
        }

        FlowTask flowTask = new FlowTask();
        flowTask.setStatus(2);
        flowTask.setComment(comment);
        flowTask.setCompleteTime(LocalDateTime.now());
        lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(flowTask);
        
        log.info("审批通过：taskId={}, userId={}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(String taskId, String userId, String comment) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }

        if (comment != null && !comment.isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        }

        taskService.complete(taskId);

        FlowTask flowTask = new FlowTask();
        flowTask.setStatus(3);
        flowTask.setComment(comment);
        flowTask.setCompleteTime(LocalDateTime.now());
        lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(flowTask);
        
        log.info("审批驳回：taskId={}, userId={}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegate(String taskId, String userId, String targetUserId, String comment) {
        taskService.delegateTask(taskId, targetUserId);
        
        FlowTask flowTask = new FlowTask();
        flowTask.setStatus(4);
        flowTask.setComment(comment);
        flowTask.setAssignee(targetUserId);
        flowTask.setCompleteTime(LocalDateTime.now());
        lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(flowTask);
        
        log.info("转办任务：taskId={}, from={}, to={}", taskId, userId, targetUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegateTask(String taskId, String userId, String delegateUserId, String comment) {
        delegate(taskId, userId, delegateUserId, comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String processInstanceId, String userId) {
        runtimeService.deleteProcessInstance(processInstanceId, "用户撤回");
        
        FlowTask flowTask = new FlowTask();
        flowTask.setStatus(6);
        flowTask.setCompleteTime(LocalDateTime.now());
        lambdaUpdate().eq(FlowTask::getProcessInstanceId, processInstanceId).update(flowTask);
        
        log.info("撤回流程：processInstanceId={}, userId={}", processInstanceId, userId);
    }

    @Override
    public FlowTask getTaskDetail(String taskId) {
        return lambdaQuery().eq(FlowTask::getTaskId, taskId).one();
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
            
            // 2. 获取BPMN模型
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel == null) {
                log.warn("BPMN模型不存在：{}", processDefinitionId);
                return null;
            }
            
            // 3. 获取流程定义信息
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            
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
            
            // 5. 获取当前活动节点（运行中）
            List<String> currentActivityIds = new ArrayList<>();
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            
            if (processInstance != null) {
                // 流程还在运行中，获取当前活动节点
                currentActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
            }
            
            // 6. 使用流程图生成器生成图片
            ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
            
            // 设置中文字体（解决中文乱码）
            String activityFontName = "宋体";
            String labelFontName = "宋体";
            String annotationFontName = "宋体";
            
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
                return output.toByteArray();
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("生成流程图失败：processInstanceId={}", processInstanceId, e);
            return null;
        }
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
}
