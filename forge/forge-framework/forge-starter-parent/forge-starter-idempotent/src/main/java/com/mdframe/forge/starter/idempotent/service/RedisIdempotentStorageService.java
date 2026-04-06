package com.mdframe.forge.starter.idempotent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisIdempotentStorageService implements IdempotentStorageService {
    private final StringRedisTemplate stringRedisTemplate;

    public RedisIdempotentStorageService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryAcquire(String key, int expireSeconds) {
        try {
            Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", expireSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.warn("Redis异常，自动降级，跳过幂等校验", e);
            return true;
        }
    }

    @Override
    public void release(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("释放幂等键失败: {}", key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("检查幂等键存在失败: {}", key, e);
            return false;
        }
    }
}
