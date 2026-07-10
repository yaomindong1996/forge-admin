package com.mdframe.forge.plugin.ai.provider.support;

import org.springframework.util.StringUtils;

/**
 * AI 供应商密钥脱敏工具。
 */
public final class AiProviderSecretMasker {

    private static final int VISIBLE_EDGE_LENGTH = 4;
    private static final String MASK = "****";

    private AiProviderSecretMasker() {
    }

    public static String mask(String secret) {
        if (!StringUtils.hasText(secret)) {
            return secret;
        }
        if (secret.length() <= VISIBLE_EDGE_LENGTH * 2) {
            return MASK;
        }
        return secret.substring(0, VISIBLE_EDGE_LENGTH)
                + MASK
                + secret.substring(secret.length() - VISIBLE_EDGE_LENGTH);
    }

    public static boolean isUnchangedMask(String submitted, String persisted) {
        return StringUtils.hasText(persisted) && mask(persisted).equals(submitted);
    }
}
