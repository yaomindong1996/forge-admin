package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.dto.ProcessDiagramInfo;
import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
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
     * 候选任务（未签收的任务）
     * 支持按用户ID和/或组ID查询
     *
     * @param page    分页参数
     * @param userId  用户ID（可选）
     * @param groupId 组ID（可选）
     * @param title   标题过滤
     * @return 候选任务列表
     */
    IPage<FlowTask> candidateTasks(Page<FlowTask> page, String userId, String groupId, String title);

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
     * 获取流程图（高亮当前节点）
     *
     * @param processInstanceId 流程实例ID
     * @return PNG图片字节数组
     */
    byte[] getProcessDiagram(String processInstanceId);

    /**
     * 获取流程图详情（包含节点信息，用于交互式展示）
     *
     * @param processInstanceId 流程实例ID
     * @return 流程图详情
     */
    ProcessDiagramInfo getProcessDiagramInfo(String processInstanceId);

    /**
     * 催办
     */
    void remind(String taskId);

    /**
     * 获取任务表单信息
     * 包含表单类型、表单配置、流程变量等
     *
     * @param taskId 任务ID
     * @return 任务表单信息
     */
    TaskFormInfo getTaskFormInfo(String taskId);
}