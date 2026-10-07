package com.mdframe.forge.plugin.system.enums;

import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PluginReviewDecision {
    APPROVE_BUILD("approve_build"), CLOSE_TASK("close_task");
    private final String code;

    public static PluginReviewDecision from(String value) {
        for (var decision : values()) {
            if (decision.code.equals(value)) {
                return decision;
            }
        }
        throw new BusinessException(400, "不支持的插件核查操作");
    }
}
