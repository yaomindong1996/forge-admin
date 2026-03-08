package com.mdframe.forge.starter.crypto.cache;

import com.mdframe.forge.starter.cache.service.ICacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 防重放令牌缓存
 */
@Slf4j
@RequiredArgsConstructor
public class ReplayTokenCache {

    private static final String CACHE_PREFIX = "crypto:replay:";

    private final ICacheService cacheService;

    /**
     * 缓存nonce
     *
     * @param nonce     随机字符串
     * @param expireSeconds 过期时间（秒）
     */
    public void cache(String nonce, long expireSeconds) {
        String key = CACHE_PREFIX + nonce;
        cacheService.set(key, "1", expireSeconds, TimeUnit.SECONDS);
        log.debug("缓存防重放nonce: {}, 过期时间: {}秒", nonce, expireSeconds);
    }

    /**
     * 检查nonce是否已存在
     *
     * @param nonce 随机字符串
     * @return 是否存在
     */
    public boolean exists(String nonce) {
        String key = CACHE_PREFIX + nonce;
        return cacheService.hasKey(key);
    }
}
