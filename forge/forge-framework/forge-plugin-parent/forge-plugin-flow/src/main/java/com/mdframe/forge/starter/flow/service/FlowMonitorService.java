package com.mdframe.forge.starter.flow.service;

import java.util.List;
import java.util.Map;

/**
 * 流程监控服务接口
 * 提供流程运行时的监控和统计功能
 *
 * @author forge
 */
public interface FlowMonitorService {

    /**
     * 获取流程实例统计概览
     * @return 统计数据Map
     */
    Map<String, Object> getProcessInstanceOverview();

    /**
     * 获取任务统计概览
     * @return 统计数据Map
     */
    Map<String, Object> getTaskOverview();

    /**
     * 获取指定流程定义的实例统计
     * @param processDefinitionKey 流程定义Key
     * @return 统计数据Map
     */
    Map<String, Object> getProcessInstanceStats(String processDefinitionKey);

    /**
     * 获取流程实例列表
     * @param processDefinitionKey 流程定义Key（可选）
     * @param status 状态：running/completed/suspended
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 流程实例列表
     */
    Map<String, Object> getProcessInstanceList(String processDefinitionKey, String status, int pageNum, int pageSize);

    /**
     * 获取流程实例详情
     * @param processInstanceId 流程实例ID
     * @return 流程实例详情
     */
    Map<String, Object> getProcessInstanceDetail(String processInstanceId);

    /**
     * 获取流程实例的执行历史
     * @param processInstanceId 流程实例ID
     * @return 执行历史列表
     */
    List<Map<String, Object>> getExecutionHistory(String processInstanceId);

    /**
     * 获取任务执行统计
     * @param processDefinitionKey 流程定义Key（可选）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 任务执行统计
     */
    Map<String, Object> getTaskExecutionStats(String processDefinitionKey, String startTime, String endTime);

    /**
     * 获取超时任务列表
     * @param processDefinitionKey 流程定义Key（可选）
     * @return 超时任务列表
     */
    List<Map<String, Object>> getTimeoutTasks(String processDefinitionKey);

    /**
     * 获取即将超时任务列表
     * @param processDefinitionKey 流程定义Key（可选）
     * @param advanceMinutes 提前预警分钟数
     * @return 即将超时任务列表
     */
    List<Map<String, Object>> getUpcomingTimeoutTasks(String processDefinitionKey, int advanceMinutes);

    /**
     * 获取流程效率分析
     * @param processDefinitionKey 流程定义Key
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 效率分析数据
     */
    Map<String, Object> getProcessEfficiencyAnalysis(String processDefinitionKey, String startTime, String endTime);

    /**
     * 获取节点耗时统计
     * @param processDefinitionKey 流程定义Key
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 节点耗时统计
     */
    List<Map<String, Object>> getNodeDurationStats(String processDefinitionKey, String startTime, String endTime);

    /**
     * 获取审批人效率统计
     * @param userId 用户ID（可选）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审批人效率统计
     */
    List<Map<String, Object>> getApproverEfficiencyStats(String userId, String startTime, String endTime);

    /**
     * 获取流程瓶颈分析
     * @param processDefinitionKey 流程定义Key
     * @return 瓶颈分析数据
     */
    Map<String, Object> getProcessBottleneckAnalysis(String processDefinitionKey);

    /**
     * 获取流程实例的活动节点
     * @param processInstanceId 流程实例ID
     * @return 活动节点列表
     */
    List<Map<String, Object>> getActiveNodes(String processInstanceId);

    /**
     * 获取流程变量
     * @param processInstanceId 流程实例ID
     * @return 流程变量Map
     */
    Map<String, Object> getProcessVariables(String processInstanceId);

    /**
     * 获取流程图高亮数据
     * @param processInstanceId 流程实例ID
     * @return 高亮数据（包含已完成节点、当前节点、已执行连线）
     */
    Map<String, Object> getProcessDiagramHighlight(String processInstanceId);

    /**
     * 获取流程部署统计
     * @return 部署统计数据
     */
    List<Map<String, Object>> getDeploymentStats();

    /**
     * 获取流程定义版本列表
     * @param processDefinitionKey 流程定义Key
     * @return 版本列表
     */
    List<Map<String, Object>> getProcessDefinitionVersions(String processDefinitionKey);
}