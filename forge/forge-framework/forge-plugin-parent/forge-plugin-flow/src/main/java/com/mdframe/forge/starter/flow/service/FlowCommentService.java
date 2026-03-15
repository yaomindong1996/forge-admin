package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.starter.flow.entity.FlowComment;

import java.util.List;

/**
 * 流程审批意见服务接口
 */
public interface FlowCommentService extends IService<FlowComment> {

    /**
     * 添加审批意见
     *
     * @param processInstanceId 流程实例ID
     * @param processDefKey    流程定义KEY
     * @param taskId           任务ID
     * @param taskName         任务名称
     * @param type             类型
     * @param message          意见内容
     * @param userId           用户ID
     * @param userName         用户姓名
     * @return 审批意见
     */
    FlowComment addComment(String processInstanceId, String processDefKey,
                           String taskId, String taskName, String type,
                           String message, String userId, String userName);

    /**
     * 获取流程的所有审批意见
     *
     * @param processInstanceId 流程实例ID
     * @return 审批意见列表
     */
    List<FlowComment> getCommentsByProcessInstanceId(String processInstanceId);

    /**
     * 获取任务的审批意见
     *
     * @param taskId 任务ID
     * @return 审批意见列表
     */
    List<FlowComment> getCommentsByTaskId(String taskId);

    /**
     * 添加流程事件（如：流程开始、流程结束、任务转办等）
     *
     * @param processInstanceId 流程实例ID
     * @param processDefKey    流程定义KEY
     * @param message          事件消息
     * @param userId           用户ID
     * @param userName         用户姓名
     * @return 事件记录
     */
    FlowComment addEvent(String processInstanceId, String processDefKey,
                         String message, String userId, String userName);
}