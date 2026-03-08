package com.mdframe.forge.starter.crypto.keyexchange;

import com.mdframe.forge.starter.cache.service.ICacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 会话密钥存储服务
 * 使用 Redis 缓存存储会话密钥
 */
@Slf4j
@RequiredArgsConstructor
public class SessionKeyStore {

    private static final String KEY_PREFIX = "crypto:session:";

    private final ICacheService cacheService;

    /**
     * 会话密钥过期时间（秒），默认2小时
     */
    private final long expireSeconds;

    /**
     * 存储会话密钥
     * @param sessionId 会话ID（可以是用户ID或token）
     * @param secretKey 对称加密密钥（Base64编码）
     */
    public void storeKey(String sessionId, String secretKey) {
        String cacheKey = KEY_PREFIX + sessionId;
        cacheService.set(cacheKey, secretKey, expireSeconds, TimeUnit.SECONDS);
        log.debug("存储会话密钥: {}", sessionId);
    }

    /**
     * 获取会话密钥
     * @param sessionId 会话ID
     * @return 对称加密密钥（Base64编码），不存在返回null
     */
    public String getKey(String sessionId) {
        String cacheKey = KEY_PREFIX + sessionId;
        Object value = cacheService.get(cacheKey);
        return value != null ? value.toString() : null;
    }

    /**
     * 删除会话密钥
     * @param sessionId 会话ID
     */
    public void removeKey(String sessionId) {
        String cacheKey = KEY_PREFIX + sessionId;
        cacheService.delete(cacheKey);
        log.debug("删除会话密钥: {}", sessionId);
    }

    /**
     * 刷新会话密钥过期时间
     * @param sessionId 会话ID
     */
    public void refreshKey(String sessionId) {
        String secretKey = getKey(sessionId);
        if (secretKey != null) {
            storeKey(sessionId, secretKey);
        }
    }

    /**
     * 检查会话密钥是否存在
     * @param sessionId 会话ID
     * @return true 存在，false 不存在
     */
    public boolean hasKey(String sessionId) {
        return getKey(sessionId) != null;
    }
}
