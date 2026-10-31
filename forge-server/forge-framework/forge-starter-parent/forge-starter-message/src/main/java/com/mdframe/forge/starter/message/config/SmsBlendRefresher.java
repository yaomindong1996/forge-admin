package com.mdframe.forge.starter.message.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.core.factory.SmsFactory;

/**
 * 短信配置变更后重建 sms4j Blend。
 */
@Slf4j
@RequiredArgsConstructor
public class SmsBlendRefresher {

    private final ReadConfig readConfig;

    public void refresh() {
        try {
            SmsFactory.createSmsBlend(readConfig);
            log.info("短信通道 Blend 已刷新");
        } catch (Exception ex) {
            log.warn("短信通道 Blend 刷新失败: {}", ex.getMessage());
        }
    }

    /**
     * 只注册指定运行时 configId。找不到配置时 sms4j 会抛异常，调用方应视为该通道不可用并 failover。
     */
    public void ensure(String runtimeId) {
        if (runtimeId == null || runtimeId.isBlank()) {
            return;
        }
        try {
            SmsFactory.createSmsBlend(readConfig, runtimeId);
        } catch (Exception ex) {
            log.warn("按通道注册短信 Blend 失败: runtimeId={}, err={}", runtimeId, ex.getMessage());
        }
    }
}
