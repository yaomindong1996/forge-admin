package com.mdframe.forge.starter.flow.service;

import java.util.List;

/**
 * 流程超时处理服务接口
 *
 * @author forge
 */
public interface FlowTimeoutService {

    /**
     * 检查并处理超时任务
     * 定时任务调用此方法
     */
    void checkAndHandleTimeoutTasks();

    /**
     * 处理单个超时任务
     * @param taskId 任务ID
     * @param timeoutAction 超时动作
     * @return 是否处理成功
     */
    boolean handleTimeoutTask(String taskId, String timeoutAction);

    /**
     * 发送超时通知
     * @param taskId 任务ID
     * @param notifyType 通知类型：email/sms/system
     * @return 是否发送成功
     */
    boolean sendTimeoutNotification(String taskId, String notifyType);

    /**
     * 获取即将超时的任务列表
     * @param advanceMinutes 提前多少分钟预警
     * @return 任务ID列表
     */
    List<String> getUpcomingTimeoutTasks(int advanceMinutes);

    /**
     * 计算任务剩余时间（毫秒）
     * @param taskId 任务ID
     * @return 剩余时间（毫秒），负数表示已超时
     */
    Long getRemainingTime(String taskId);
}