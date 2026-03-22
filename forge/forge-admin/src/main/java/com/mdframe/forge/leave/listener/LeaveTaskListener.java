package com.mdframe.forge.leave.listener;

import com.mdframe.forge.leave.service.LeaveRequestService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 请假任务监听器
 * 用于在审批任务完成时更新业务数据
 *
 * @author forge
 */
@Slf4j
@Component
public class LeaveTaskListener implements TaskListener {

    @Autowired
    private LeaveRequestService leaveRequestService;

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        String taskId = delegateTask.getId();
        String processInstanceId = delegateTask.getProcessInstanceId();
        
        log.info("请假任务监听器触发，taskId: {}, eventName: {}", taskId, eventName);
        
        if ("complete".equals(eventName)) {
            // 任务完成时，保存审批信息
            handleTaskComplete(delegateTask);
        }
    }

    /**
     * 处理任务完成事件
     */
    private void handleTaskComplete(DelegateTask delegateTask) {
        // 获取流程变量
        String businessKey = (String) delegateTask.getVariable("businessKey");
        String approveResult = (String) delegateTask.getVariable("approveResult");
        String approveComment = (String) delegateTask.getVariable("approveComment");
        String approveAttachments = (String) delegateTask.getVariable("approveAttachments");
        
        // 获取审批人信息
        String approveUserId = delegateTask.getAssignee();
        
        if (businessKey == null) {
            log.warn("无法获取businessKey，跳过审批信息更新");
            return;
        }
        
        // 判断审批结果
        boolean approved = "approve".equals(approveResult);
        
        // 更新业务数据
        leaveRequestService.updateApproveInfo(
            businessKey,
            approveUserId,
            null, // 审批人姓名可以从用户服务获取
            approveComment,
            approveAttachments,
            approved
        );
        
        log.info("请假审批信息已更新，businessKey: {}, approved: {}", businessKey, approved);
    }
}
