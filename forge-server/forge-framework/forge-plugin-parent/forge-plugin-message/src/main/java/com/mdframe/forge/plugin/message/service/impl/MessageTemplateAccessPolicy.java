package com.mdframe.forge.plugin.message.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageTemplate;
import com.mdframe.forge.starter.core.exception.BusinessException;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 内置消息模板保护：禁止删除、改编码，避免流程/系统通知静默失效。
 */
final class MessageTemplateAccessPolicy {

    private static final Set<String> BUILT_IN_CODES = Set.of(
            "SYSTEM_NOTICE",
            "TASK_ASSIGN",
            "SMS_VERIFY_CODE",
            "APPROVAL_PASS",
            "FLOW_TODO_CARD",
            "FLOW_TODO_CARD_WECOM",
            "FLOW_TODO_CARD_DINGTALK",
            "FLOW_TODO_CARD_FEISHU",
            "FLOW_RESULT_CARD",
            "FLOW_RESULT_CARD_WECOM",
            "FLOW_CC_CARD",
            "FLOW_CC_CARD_WECOM"
    );

    private MessageTemplateAccessPolicy() {
    }

    static boolean isBuiltIn(String templateCode) {
        if (StrUtil.isBlank(templateCode)) {
            return false;
        }
        String normalized = templateCode.trim().toUpperCase(Locale.ROOT);
        if (BUILT_IN_CODES.contains(normalized)) {
            return true;
        }
        return normalized.startsWith("FLOW_");
    }

    static boolean isBuiltIn(SysMessageTemplate template) {
        return template != null && isBuiltIn(template.getTemplateCode());
    }

    static void assertDeletable(SysMessageTemplate existing) {
        if (isBuiltIn(existing)) {
            throw new BusinessException("系统内置消息模板不能删除");
        }
    }

    static void assertMutable(SysMessageTemplate existing, String nextTemplateCode) {
        if (!isBuiltIn(existing)) {
            return;
        }
        if (StrUtil.isNotBlank(nextTemplateCode)
                && !Objects.equals(normalize(existing.getTemplateCode()), normalize(nextTemplateCode))) {
            throw new BusinessException("系统内置消息模板编码不能修改");
        }
    }

    private static String normalize(String templateCode) {
        return templateCode == null ? null : templateCode.trim().toUpperCase(Locale.ROOT);
    }
}
