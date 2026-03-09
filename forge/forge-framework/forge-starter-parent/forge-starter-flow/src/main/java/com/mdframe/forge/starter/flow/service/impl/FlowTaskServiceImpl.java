package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowTaskService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 流程任务服务实现（简化版）
 */
@Slf4j
@Service
public class FlowTaskServiceImpl extends ServiceImpl<FlowTaskMapper, FlowTask> implements FlowTaskService {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private org.flowable.engine.RepositoryService repositoryService;

    @Override
    public IPage<FlowTask> todoTasks(Page<FlowTask> page, String userId, String title, String category) {
        // 简化：从本地表查询
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
    @Transactional(rollbackFor = Exception.class)
    public void claimTask(String taskId, String userId) {
        taskService.claim(taskId, userId);
        
        FlowTask task = new FlowTask();
        task.setTaskId(taskId);
        task.setAssignee(userId);
        task.setStatus(1);
        task.setClaimTime(java.time.LocalDateTime.now());
        
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
        flowTask.setCompleteTime(java.time.LocalDateTime.now());
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
        flowTask.setCompleteTime(java.time.LocalDateTime.now());
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
        flowTask.setCompleteTime(java.time.LocalDateTime.now());
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
        flowTask.setCompleteTime(java.time.LocalDateTime.now());
        lambdaUpdate().eq(FlowTask::getProcessInstanceId, processInstanceId).update(flowTask);
        
        log.info("撤回流程：processInstanceId={}, userId={}", processInstanceId, userId);
    }

    @Override
    public FlowTask getTaskDetail(String taskId) {
        return lambdaQuery().eq(FlowTask::getTaskId, taskId).one();
    }

    @Override
    public byte[] getProcessDiagram(String processInstanceId) {
        // TODO: 实现流程图生成
        // 需要注入 ProcessEngine 获取 ProcessDiagramGenerator
        log.warn("流程图生成功能待实现：processInstanceId={}", processInstanceId);
        return null;
    }

    @Override
    public void remind(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task != null) {
            log.info("催办任务：taskId={}, taskName={}", taskId, task.getName());
        }
    }
}