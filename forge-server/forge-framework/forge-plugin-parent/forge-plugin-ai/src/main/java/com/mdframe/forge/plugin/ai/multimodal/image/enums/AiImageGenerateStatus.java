package com.mdframe.forge.plugin.ai.multimodal.image.enums;

import lombok.Getter;

/**
 * AI 图片生成状态。
 */
@Getter
public enum AiImageGenerateStatus {

    PENDING("pending", "待处理"),
    GENERATING("generating", "生成中"),
    SUCCESS("success", "成功"),
    FAILED("failed", "失败");

    private final String code;
    private final String label;

    AiImageGenerateStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public boolean matches(String value) {
        return value != null && this.code.equalsIgnoreCase(value.trim());
    }

    public boolean canTransitionTo(AiImageGenerateStatus next) {
        return switch (this) {
            case PENDING -> next == GENERATING || next == FAILED;
            case GENERATING -> next == SUCCESS || next == FAILED;
            case SUCCESS, FAILED -> false;
        };
    }
}
