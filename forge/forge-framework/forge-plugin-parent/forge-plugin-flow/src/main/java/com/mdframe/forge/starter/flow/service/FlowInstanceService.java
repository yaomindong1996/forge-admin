package com.mdframe.forge.starter.flow.service;

import com.mdframe.forge.starter.flow.entity.FlowBusiness;

import java.util.Map;

/**
 * 流程实例服务接口
 */
public interface FlowInstanceService {

    /**
     * 发起流程
     *
     * @param modelKey    模型KEY
     * @param businessKey 业务KEY（唯一）
     * @param title       流程标题
     * @param variables   流程变量
     * @param userId      发起人ID
     * @param userName    发起人姓名
     * @param deptId      部门ID
     * @param deptName    部门名称
     * @return 流程实例ID
     */
    String startProcess(String modelKey, String businessKey, String title,
                        Map<String, Object> variables, String userId, String userName,
                        String deptId, String deptName);

    /**
     * 发起流程（带业务类型）
     */
    String startProcess(String modelKey, String businessKey, String businessType,
                        String title, Map<String, Object> variables, String userId,
                        String userName, String deptId, String deptName);

    /**
     * 获取流程实例状态
     */
    FlowBusiness getProcessStatus(String businessKey);

    /**
     * 终止流程
     */
    void terminateProcess(String businessKey, String userId, String reason);

    /**
     * 删除流程实例
     */
    void deleteProcess(String businessKey, String userId);

    /**
     * 获取流程变量
     */
    Map<String, Object> getProcessVariables(String businessKey);

    /**
     * 更新流程变量
     */
    void updateProcessVariables(String businessKey, Map<String, Object> variables);

    /**
     * 流程节点回退
     *
     * @param processInstanceId 流程实例ID
     * @param targetActivityId 目标节点ID
     * @param userId 操作人ID
     * @param reason 回退原因
     */
    void rollbackToActivity(String processInstanceId, String targetActivityId, String userId, String reason);

    /**
     * 任务转派（重新分配处理人）
     *
     * @param taskId 任务ID
     * @param newAssignee 新处理人ID
     * @param userId 操作人ID
     * @param reason 转派原因
     */
    void reassignTask(String taskId, String newAssignee, String userId, String reason);

    /**
     * 根据流程实例ID终止流程
     *
     * @param processInstanceId 流程实例ID
     * @param userId 操作人ID
     * @param reason 终止原因
     */
    void terminateProcessByInstanceId(String processInstanceId, String userId, String reason);
}