package com.mdframe.forge.starter.websocket.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * WebSocket消息类型枚举
 */
@Getter
@AllArgsConstructor
public enum MessageType {

    // ==================== 认证相关 ====================
    /**
     * 强制下线
     */
    AUTH_KICKOUT("auth.kickout", "强制下线"),

    /**
     * 账号被顶
     */
    AUTH_REPLACED("auth.replaced", "账号被顶"),

    /**
     * 账号封禁
     */
    AUTH_BANNED("auth.banned", "账号封禁"),

    /**
     * 密码过期提醒
     */
    AUTH_PASSWORD_EXPIRE("auth.password_expire", "密码过期提醒"),

    // ==================== 系统通知 ====================
    /**
     * 系统通知
     */
    SYSTEM_NOTICE("system.notice", "系统通知"),

    /**
     * 系统警告
     */
    SYSTEM_ALERT("system.alert", "系统警告"),

    /**
     * 系统维护通知
     */
    SYSTEM_MAINTENANCE("system.maintenance", "系统维护通知"),

    // ==================== 任务相关 ====================
    /**
     * 任务进度
     */
    TASK_PROGRESS("task.progress", "任务进度"),

    /**
     * 任务完成
     */
    TASK_COMPLETE("task.complete", "任务完成"),

    /**
     * 任务失败
     */
    TASK_FAILED("task.failed", "任务失败"),

    // ==================== 业务通知 ====================
    /**
     * 订单状态变更
     */
    ORDER_STATUS("order.status", "订单状态"),

    /**
     * 新消息
     */
    MESSAGE_NEW("message.new", "新消息"),

    /**
     * 审批通知
     */
    APPROVAL_NOTICE("approval.notice", "审批通知"),

    // ==================== 自定义 ====================
    /**
     * 自定义消息
     */
    CUSTOM("custom", "自定义消息");

    /**
     * 消息类型代码
     */
    private final String code;

    /**
     * 消息类型描述
     */
    private final String description;

    /**
     * 根据代码获取枚举
     */
    public static MessageType fromCode(String code) {
        for (MessageType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return CUSTOM;
    }
}
