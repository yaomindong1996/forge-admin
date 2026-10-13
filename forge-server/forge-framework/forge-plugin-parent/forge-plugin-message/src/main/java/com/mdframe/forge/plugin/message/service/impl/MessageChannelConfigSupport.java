package com.mdframe.forge.plugin.message.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.core.util.SensitiveDataUtil;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;

/**
 * 短信/邮件通道配置公共逻辑：租户解析、脱敏、最后启用保护。
 */
public final class MessageChannelConfigSupport {

    private static final Long DEFAULT_TENANT_ID = 1L;

    private MessageChannelConfigSupport() {
    }

    public static Long resolveTenantId(Long tenantId) {
        if (tenantId != null) {
            return tenantId;
        }
        Long fromContext = TenantContextHolder.getTenantId();
        if (fromContext != null) {
            return fromContext;
        }
        Long fromSession = SessionHelper.getTenantId();
        return fromSession != null ? fromSession : DEFAULT_TENANT_ID;
    }

    public static String maskSecret(String secret) {
        if (StrUtil.isBlank(secret)) {
            return secret;
        }
        // 与文件存储配置一致：整段替换为 MASK，便于保存时识别并保留原值
        return SensitiveDataUtil.maskSensitiveValue("secret", secret);
    }

    public static void assertCanDisable(boolean disabling, long otherEnabledCount, String channelLabel) {
        if (!disabling) {
            return;
        }
        if (otherEnabledCount <= 0) {
            throw new BusinessException("至少保留一条启用的" + channelLabel + "通道");
        }
    }

    public static boolean isDisabling(Integer currentStatus, Integer nextStatus) {
        return EnableStatus.ENABLED.matches(currentStatus) && EnableStatus.DISABLED.matches(nextStatus);
    }
}
