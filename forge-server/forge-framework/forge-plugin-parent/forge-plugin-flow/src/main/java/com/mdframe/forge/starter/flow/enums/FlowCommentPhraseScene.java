package com.mdframe.forge.starter.flow.enums;

import lombok.Getter;

/**
 * 常用审批意见适用场景。
 */
@Getter
public enum FlowCommentPhraseScene {

    APPROVE("APPROVE", "同意"),
    REJECT("REJECT", "驳回"),
    ALL("ALL", "通用");

    private final String code;
    private final String label;

    FlowCommentPhraseScene(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public boolean matches(String value) {
        return value != null && this.code.equals(value.trim());
    }

    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (FlowCommentPhraseScene scene : values()) {
            if (scene.matches(value)) {
                return true;
            }
        }
        return false;
    }
}
