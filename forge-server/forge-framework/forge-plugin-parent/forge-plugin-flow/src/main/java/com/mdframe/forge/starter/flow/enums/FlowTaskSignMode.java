package com.mdframe.forge.starter.flow.enums;

/** 动态加签关系模式。当前运行时只执行 PARALLEL，其他模式保留协议供后续编排。 */
public enum FlowTaskSignMode {
    BEFORE("BEFORE"),
    AFTER("AFTER"),
    PARALLEL("PARALLEL");

    private final String code;

    FlowTaskSignMode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static FlowTaskSignMode fromCode(String value) {
        if (value == null || value.isBlank()) {
            return PARALLEL;
        }
        for (FlowTaskSignMode mode : values()) {
            if (mode.code.equalsIgnoreCase(value.trim())) {
                return mode;
            }
        }
        throw new IllegalArgumentException("FLOW_TASK_SIGN_MODE_INVALID");
    }
}
