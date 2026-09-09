package com.mdframe.forge.starter.flow.enums;

/** 候选关系状态。 */
public enum FlowTaskCandidateStatus {
    INACTIVE(0),
    ACTIVE(1);

    private final int code;

    FlowTaskCandidateStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public boolean matches(Integer value) {
        return value != null && value == code;
    }
}
