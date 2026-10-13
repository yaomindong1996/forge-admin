package com.mdframe.forge.plugin.message.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageBizType;
import com.mdframe.forge.starter.core.exception.BusinessException;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 内置消息业务类型保护：禁止删除、改编码，避免流程待办跳转配置失效。
 */
final class MessageBizTypeAccessPolicy {

    static final Set<String> BUILT_IN_CODES = Set.of(
            "FLOW_TODO",
            "FLOW_TASK_OVERDUE",
            "SYSTEM"
    );

    private MessageBizTypeAccessPolicy() {
    }

    static boolean isBuiltIn(String bizType) {
        if (StrUtil.isBlank(bizType)) {
            return false;
        }
        return BUILT_IN_CODES.contains(bizType.trim().toUpperCase(Locale.ROOT));
    }

    static boolean isBuiltIn(SysMessageBizType entity) {
        return entity != null && isBuiltIn(entity.getBizType());
    }

    static void assertDeletable(SysMessageBizType existing) {
        if (isBuiltIn(existing)) {
            throw new BusinessException("系统内置业务类型不能删除");
        }
    }

    static void assertMutable(SysMessageBizType existing, String nextBizType) {
        if (!isBuiltIn(existing)) {
            return;
        }
        if (StrUtil.isNotBlank(nextBizType)
                && !Objects.equals(normalize(existing.getBizType()), normalize(nextBizType))) {
            throw new BusinessException("系统内置业务类型编码不能修改");
        }
    }

    private static String normalize(String bizType) {
        return bizType == null ? null : bizType.trim().toUpperCase(Locale.ROOT);
    }
}
