package com.mdframe.forge.starter.flow.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程任务状态，对应 {@code sys_flow_task.status}。
 */
@Getter
public enum FlowTaskStatus {

    PENDING(0, "待办", "pending"),
    CLAIMED(1, "已签收", "claim"),
    APPROVED(2, "已通过", "approve"),
    REJECTED(3, "已驳回", "reject"),
    DELEGATED(4, "已转办", "delegate"),
    CANCELED(5, "已取消", "cancel"),
    WITHDRAWN(6, "已撤回", "withdraw"),
    RETURNED(7, "已退回", "return"),
    TERMINATED(8, "已终结", "terminate");

    private final int code;
    private final String label;
    private final String historyAction;

    public static final List<Integer> TODO_CODES = List.of(PENDING.code, CLAIMED.code);

    public static final List<Integer> DONE_CODES = Arrays.stream(values())
            .filter(status -> status != PENDING && status != CLAIMED && status != WITHDRAWN)
            .map(FlowTaskStatus::getCode)
            .collect(Collectors.toList());

    FlowTaskStatus(int code, String label, String historyAction) {
        this.code = code;
        this.label = label;
        this.historyAction = historyAction;
    }

    public boolean matches(Integer value) {
        return value != null && this.code == value;
    }

    public static FlowTaskStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (FlowTaskStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    public static String historyActionOf(Integer code) {
        FlowTaskStatus status = of(code);
        return status == null ? PENDING.getHistoryAction() : status.getHistoryAction();
    }

    public static boolean isActionable(Integer code) {
        return PENDING.matches(code) || CLAIMED.matches(code);
    }

    public static List<Integer> todoCodes() {
        return TODO_CODES;
    }

    public static List<Integer> doneCodes() {
        return DONE_CODES;
    }
}
