package com.mdframe.forge.starter.flow.enums;

import lombok.Getter;

/** 流程抄送关系状态。 */
@Getter
public enum FlowCcStatus {
    ACTIVE(0, "有效"),
    REVOKED(1, "已撤回");

    private final int code;
    private final String label;

    FlowCcStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public boolean matches(Integer value) {
        return value != null && code == value;
    }
}
