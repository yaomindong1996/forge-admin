package com.mdframe.forge.starter.message.sms;

/**
 * sms4j 的 Blend 表是进程级 Map，键为 configId。
 * 业务 configId 由用户填写，不同租户会重复（例如都叫 alibaba），必须加租户前缀。
 */
public final class SmsBlendIds {

    private SmsBlendIds() {
    }

    public static String runtimeId(Long tenantId, String configId) {
        long tenant = tenantId == null ? 0L : tenantId;
        String raw = configId == null ? "" : configId.trim();
        return tenant + ":" + raw;
    }
}
