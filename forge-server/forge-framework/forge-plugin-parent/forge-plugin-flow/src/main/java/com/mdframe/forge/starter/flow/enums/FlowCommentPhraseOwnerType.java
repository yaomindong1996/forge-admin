package com.mdframe.forge.starter.flow.enums;

import lombok.Getter;

/**
 * 常用审批意见归属。
 */
@Getter
public enum FlowCommentPhraseOwnerType {

    TENANT(0, "企业常用"),
    USER(1, "我的常用");

    private final int code;
    private final String label;

    FlowCommentPhraseOwnerType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public boolean matches(Integer value) {
        return value != null && this.code == value;
    }

    public static boolean isValid(Integer value) {
        if (value == null) {
            return false;
        }
        for (FlowCommentPhraseOwnerType type : values()) {
            if (type.matches(value)) {
                return true;
            }
        }
        return false;
    }
}
