package com.mdframe.forge.starter.core.domain;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 流程事件类型枚举
 * <p>
 * 统一管理所有流程事件的类型标识，代替各模块中散落的字符串常量。
 * {@link FlowEventMessage} 和 {@code @FlowCallback} 中的事件类型字符串均对应此枚举。
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 发布事件时
 * FlowEventMessage.ofProcess(FlowEventType.PROCESS_COMPLETED.code(), ...);
 *
 * // 接收事件时（类型安全比较）
 * if (ctx.getEventTypeEnum() == FlowEventType.PROCESS_COMPLETED) { ... }
 *
 * // 判断是否为流程级事件
 * if (FlowEventType.of(ctx.getEvent()).isProcessLevel()) { ... }
 * </pre>
 *
 * @author forge
 */
public enum FlowEventType {

    // ==================== 流程级事件 ====================

    /** 流程已发起 */
    PROCESS_STARTED("PROCESS_STARTED", "流程已发起", true),

    /** 流程已通过（全部审批节点完成） */
    PROCESS_COMPLETED("PROCESS_COMPLETED", "流程已通过", true),

    /** 流程已驳回 */
    PROCESS_REJECTED("PROCESS_REJECTED", "流程已驳回", true),

    /** 流程已取消/撤回 */
    PROCESS_CANCELED("PROCESS_CANCELED", "流程已取消", true),

    // ==================== 任务级事件 ====================

    /** 审批任务已创建（待办产生） */
    TASK_CREATED("TASK_CREATED", "待办已创建", false),

    /** 审批任务已完成（某节点审批完成） */
    TASK_COMPLETED("TASK_COMPLETED", "任务已完成", false),

    /** 任务被分配/签收 */
    TASK_ASSIGNED("TASK_ASSIGNED", "任务已分配", false);

    // ==================== 快速查找表 ====================

    private static final Map<String, FlowEventType> CODE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(FlowEventType::code, Function.identity()));

    // ==================== 实例字段 ====================

    private final String code;
    private final String label;
    /** true = 流程级事件；false = 任务级事件 */
    private final boolean processLevel;

    FlowEventType(String code, String label, boolean processLevel) {
        this.code = code;
        this.label = label;
        this.processLevel = processLevel;
    }

    /** 返回与 {@link FlowEventMessage#eventType} 字段对应的字符串值 */
    public String code() {
        return code;
    }

    /** 返回可读名称（用于日志、UI 展示） */
    public String label() {
        return label;
    }

    /** 是否为流程级事件（PROCESS_*），否则为任务级事件（TASK_*） */
    public boolean isProcessLevel() {
        return processLevel;
    }

    /**
     * 通过 code 字符串获取枚举，未知类型返回 null
     *
     * @param code 事件类型字符串，如 "PROCESS_COMPLETED"
     * @return 对应枚举，找不到时返回 null
     */
    public static FlowEventType of(String code) {
        if (code == null) return null;
        return CODE_MAP.get(code);
    }

    @Override
    public String toString() {
        return code;
    }
}
