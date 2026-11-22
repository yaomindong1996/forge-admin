package com.mdframe.forge.plugin.ai.provider.support;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.persistence.PersistentCiphertext;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI 密钥加密服务，委托 PersistentCryptoService。
 * 写入侧统一加密；读取侧兼容 FPC1、旧无前缀密文，以及演示/手工残留的明文 Key。
 */
@Component
@RequiredArgsConstructor
public class AiSecretCrypto {

    private final PersistentCryptoService cryptoService;

    /**
     * 加密明文密钥。algorithm 传 null 使用默认算法。
     */
    public String encrypt(String plain) {
        if (!StringUtils.hasText(plain)) {
            return plain;
        }
        return cryptoService.encrypt(plain, null);
    }

    /**
     * 解密密文。algorithm 传 null 使用默认算法。
     * 兼容 legacy 无前缀密文与 versioned FPC1: 密文。
     */
    public String decrypt(String cipher) {
        if (!StringUtils.hasText(cipher)) {
            return cipher;
        }
        return cryptoService.decrypt(cipher, null);
    }

    /**
     * 将库内 api_key 还原为调用上游所需的明文。
     * <ul>
     *   <li>FPC1 活动/历史密文：正常解密</li>
     *   <li>未知 keyId：提示与当前环境密钥不匹配，需重新保存</li>
     *   <li>无前缀旧密文：尝试解密；若旧密文读取已关闭且值像厂商明文 Key，则原样返回</li>
     * </ul>
     */
    public String resolvePlainSecret(String stored) {
        if (!StringUtils.hasText(stored)) {
            return stored;
        }
        PersistentCiphertext parsed = cryptoService.inspect(stored, null);
        return switch (parsed.format()) {
            case ACTIVE, HISTORICAL -> decrypt(stored);
            case UNKNOWN_KEY -> throw new BusinessException(
                    "API Key 密文与当前环境密钥不匹配，请重新填写并保存 API Key");
            case LEGACY -> resolveLegacyOrPlain(stored);
            case EMPTY -> stored;
            case UNKNOWN -> {
                if (looksLikeVendorApiKey(stored)) {
                    yield stored;
                }
                throw new BusinessException("API Key 格式无法识别，请重新填写并保存 API Key");
            }
        };
    }

    private String resolveLegacyOrPlain(String stored) {
        try {
            return decrypt(stored);
        }
        catch (BusinessException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            // 社区默认关闭 legacy-read：演示库/手工改库留下的明文 Key 会被误判为旧密文。
            if (message.contains("旧无版本密文读取已关闭") && looksLikeVendorApiKey(stored)) {
                return stored;
            }
            if (message.contains("旧无版本密文读取已关闭")) {
                throw new BusinessException("API Key 无法解密（旧密文读取已关闭），请重新填写并保存 API Key");
            }
            throw ex;
        }
    }

    /**
     * 粗判是否为厂商明文 API Key（OpenAI/DeepSeek sk-、通义 ak- 等），避免把明文当旧密文解。
     */
    static boolean looksLikeVendorApiKey(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith(PersistentCiphertext.VERSION + ":")) {
            return false;
        }
        if (trimmed.startsWith("sk-") || trimmed.startsWith("sk_")
                || trimmed.startsWith("ak-") || trimmed.startsWith("AK")
                || trimmed.startsWith("Bearer ")) {
            return true;
        }
        // 常见厂商 Key：可打印、无冒号分段、长度足够；旧 SM4 密文多为更长的 Base64。
        if (trimmed.length() < 16 || trimmed.length() > 256 || trimmed.indexOf(':') >= 0) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < 33 || c > 126) {
                return false;
            }
        }
        return true;
    }

    /**
     * 是否需要进行解密还原。优先使用 {@link #resolvePlainSecret(String)}。
     * 非空不一定是密文：演示库可能残留明文 Key。
     */
    public static boolean isEncrypted(String value) {
        return value != null && !value.isEmpty();
    }
}
