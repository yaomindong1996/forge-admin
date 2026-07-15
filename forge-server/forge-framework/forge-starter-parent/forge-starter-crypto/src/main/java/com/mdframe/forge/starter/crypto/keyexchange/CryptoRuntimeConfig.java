package com.mdframe.forge.starter.crypto.keyexchange;

import com.mdframe.forge.starter.core.context.CryptoProperties;

import java.util.List;

/**
 * 提供给浏览器的安全加解密运行配置，不包含对称密钥及 RSA 密钥材料。
 */
public record CryptoRuntimeConfig(
        boolean enabled,
        String algorithm,
        boolean enableDynamicKey,
        boolean enableApiCrypto,
        boolean enableFieldCrypto,
        boolean enableReplayProtection,
        long replayTimeWindow,
        List<String> replayIncludePaths,
        List<String> replayExcludePaths,
        List<String> excludePaths
) {

    public static CryptoRuntimeConfig from(CryptoProperties properties) {
        return new CryptoRuntimeConfig(
                Boolean.TRUE.equals(properties.getEnabled()),
                properties.getAlgorithm(),
                Boolean.TRUE.equals(properties.getEnableDynamicKey()),
                Boolean.TRUE.equals(properties.getEnableApiCrypto()),
                Boolean.TRUE.equals(properties.getEnableFieldCrypto()),
                Boolean.TRUE.equals(properties.getEnableReplayProtection()),
                properties.getReplayTimeWindow() == null ? 300L : properties.getReplayTimeWindow(),
                safeList(properties.getReplayIncludePaths()),
                safeList(properties.getReplayExcludePaths()),
                safeList(properties.getExcludePaths())
        );
    }

    private static List<String> safeList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
