package com.mdframe.forge.starter.message.channel;

import org.dromara.sms4j.api.SmsBlend;

/**
 * 只按运行时 configId 取 Blend。找不到就补注册再取一次。
 * 禁止落到 {@code SmsFactory.getSmsBlend()}：无参重载是进程级负载均衡，会改用其它通道甚至其它租户的密钥。
 */
final class SmsBlendResolver {

    private SmsBlendResolver() {
    }

    static SmsBlend resolve(String runtimeId, SmsBlendLookup lookup, Runnable ensure) {
        if (runtimeId == null || runtimeId.isBlank()) {
            return null;
        }
        SmsBlend blend = lookup.find(runtimeId);
        if (blend != null) {
            return blend;
        }
        if (ensure != null) {
            ensure.run();
        }
        return lookup.find(runtimeId);
    }

    @FunctionalInterface
    interface SmsBlendLookup {
        SmsBlend find(String runtimeId);
    }
}
