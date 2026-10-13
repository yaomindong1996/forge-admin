package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.plugin.system.entity.SysClient;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 内置客户端保护：禁止删除、改编码、禁用，避免 PC/APP/移动端/报表登录面失效。
 */
public final class SysClientAccessPolicy {

    /**
     * 模板内置客户端编码（与种子/迁移一致）。
     */
    static final Set<String> BUILT_IN_CLIENT_CODES = Set.of("pc", "app", "h5", "forge_report");

    private SysClientAccessPolicy() {
    }

    public static boolean isBuiltIn(String clientCode) {
        if (StrUtil.isBlank(clientCode)) {
            return false;
        }
        return BUILT_IN_CLIENT_CODES.contains(clientCode.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean isBuiltIn(SysClient client) {
        return client != null && isBuiltIn(client.getClientCode());
    }

    static void assertDeletable(SysClient existing) {
        if (isBuiltIn(existing)) {
            throw new BusinessException("系统内置客户端不能删除");
        }
    }

    /**
     * 内置客户端禁止改编码、禁用；名称/Token 等其它字段仍可维护。
     */
    static void assertMutable(SysClient existing, String nextClientCode, Integer nextStatus) {
        if (!isBuiltIn(existing)) {
            return;
        }
        if (StrUtil.isNotBlank(nextClientCode)
                && !Objects.equals(normalizeCode(existing.getClientCode()), normalizeCode(nextClientCode))) {
            throw new BusinessException("系统内置客户端编码不能修改");
        }
        if (nextStatus != null && !EnableStatus.ENABLED.matches(nextStatus)) {
            throw new BusinessException("系统内置客户端不能禁用");
        }
    }

    private static String normalizeCode(String clientCode) {
        return clientCode == null ? null : clientCode.trim().toLowerCase(Locale.ROOT);
    }
}
