package com.mdframe.forge.plugin.capability.secureaction.source;

import java.util.List;

/**
 * 业务动作能力注册候选项及其可发布诊断。
 */
public record BusinessActionRegistrationAction(
        String actionCode,
        String actionName,
        String actionType,
        Integer status,
        boolean publishable,
        String unavailableReason,
        List<String> stepTypes) {
}
