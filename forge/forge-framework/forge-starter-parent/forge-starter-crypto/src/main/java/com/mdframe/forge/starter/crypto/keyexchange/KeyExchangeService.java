package com.mdframe.forge.starter.crypto.keyexchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 密钥交换服务
 * 处理前端密钥协商请求
 */
@Slf4j
@RequiredArgsConstructor
public class KeyExchangeService {

    private final RsaKeyPairHolder rsaKeyPairHolder;
    private final SessionKeyStore sessionKeyStore;

    /**
     * 获取 RSA 公钥
     * @return Base64编码的RSA公钥
     */
    public String getPublicKey() {
        return rsaKeyPairHolder.getPublicKeyBase64();
    }

    /**
     * 密钥交换 - 接收前端加密的会话密钥
     * @param sessionId 会话ID
     * @param encryptedKey RSA公钥加密后的会话密钥（Base64编码）
     * @return true 成功，false 失败
     */
    public boolean exchangeKey(String sessionId, String encryptedKey) {
        try {
            // 使用 RSA 私钥解密获取会话密钥
            String sessionKey = rsaKeyPairHolder.decryptByPrivateKey(encryptedKey);
            
            // 存储会话密钥
            sessionKeyStore.storeKey(sessionId, sessionKey);
            
            log.info("密钥交换成功: sessionId={}", sessionId);
            return true;
        } catch (Exception e) {
            log.error("密钥交换失败: sessionId={}, error={}", sessionId, e.getMessage());
            return false;
        }
    }

    /**
     * 获取会话密钥
     * @param sessionId 会话ID
     * @return 会话密钥（Base64编码），不存在返回null
     */
    public String getSessionKey(String sessionId) {
        return sessionKeyStore.getKey(sessionId);
    }

    /**
     * 清除会话密钥（用于登出时）
     * @param sessionId 会话ID
     */
    public void clearSessionKey(String sessionId) {
        sessionKeyStore.removeKey(sessionId);
    }
}
