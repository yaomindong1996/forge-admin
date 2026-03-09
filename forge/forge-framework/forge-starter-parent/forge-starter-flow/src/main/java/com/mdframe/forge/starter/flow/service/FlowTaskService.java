package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.entity.FlowTask;

import java.util.Map;

/**
 * 流程任务服务接口
 */
public interface FlowTaskService {

    /**
     * 我的待办任务
     */
    IPage<FlowTask> todoTasks(Page<FlowTask> page, String userId, String title, String category);

    /**
     * 我的已办任务
     */
    IPage<FlowTask> doneTasks(Page<FlowTask> page, String userId, String title, String category);

    /**
     * 我发起的流程
     */
    IPage<FlowTask> startedTasks(Page<FlowTask> page, String userId, String title, String category);

    /**
     * 签收任务
     */
    void claimTask(String taskId, String userId);

    /**
     * 审批通过
     */
    void approve(String taskId, String userId, String comment, Map<String, Object> variables);

    /**
     * 审批驳回
     */
    void reject(String taskId, String userId, String comment);

    /**
     * 转办
     */
    void delegate(String taskId, String userId, String targetUserId, String comment);

    /**
     * 委派
     */
    void delegateTask(String taskId, String userId, String delegateUserId, String comment);

    /**
     * 撤回流程
     */
    void withdraw(String processInstanceId, String userId);

    /**
     * 获取任务详情
     */
    FlowTask getTaskDetail(String taskId);

    /**
     * 获取流程图
     */
    byte[] getProcessDiagram(String processInstanceId);

    /**
     * 催办
     */
    void remind(String taskId);
}