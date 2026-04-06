package com.mdframe.forge.starter.idempotent.service;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.idempotent.dto.IdempotentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class RedisResultCacheService implements ResultCacheService {
    
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String CACHE_KEY_PREFIX = "idempotent:cache:";
    
    @Override
    public void cacheResult(String key, Object result, int expireSeconds) {
        String cacheKey = buildCacheKey(key);
        
        try {
            String resultJson = objectMapper.writeValueAsString(result);
            
            Map<String, String> cacheData = new HashMap<>();
            cacheData.put("requestId", IdUtil.fastSimpleUUID());
            cacheData.put("result", resultJson);
            cacheData.put("executeTime", String.valueOf(System.currentTimeMillis()));
            cacheData.put("status", IdempotentResult.STATUS_SUCCESS);
            
            redisTemplate.opsForHash().putAll(cacheKey, cacheData);
            redisTemplate.expire(cacheKey, expireSeconds, TimeUnit.SECONDS);
            
            log.debug("缓存执行结果成功: key={}, expire={}s", key, expireSeconds);
        } catch (Exception e) {
            log.error("缓存执行结果失败: key={}, error={}", key, e.getMessage(), e);
        }
    }
    
    @Override
    public void cacheException(String key, Throwable exception, int expireSeconds) {
        String cacheKey = buildCacheKey(key);
        
        try {
            Map<String, String> cacheData = new HashMap<>();
            cacheData.put("requestId", IdUtil.fastSimpleUUID());
            cacheData.put("exception", exception.getClass().getName() + ": " + exception.getMessage());
            cacheData.put("executeTime", String.valueOf(System.currentTimeMillis()));
            cacheData.put("status", IdempotentResult.STATUS_FAILED);
            
            redisTemplate.opsForHash().putAll(cacheKey, cacheData);
            redisTemplate.expire(cacheKey, expireSeconds, TimeUnit.SECONDS);
            
            log.debug("缓存异常结果成功: key={}, expire={}s", key, expireSeconds);
        } catch (Exception e) {
            log.error("缓存异常结果失败: key={}, error={}", key, e.getMessage(), e);
        }
    }
    
    @Override
    public IdempotentResult getCachedResult(String key) {
        String cacheKey = buildCacheKey(key);
        
        try {
            Boolean exists = redisTemplate.hasKey(cacheKey);
            if (!Boolean.TRUE.equals(exists)) {
                return null;
            }
            
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(cacheKey);
            if (entries == null || entries.isEmpty()) {
                return null;
            }
            
            IdempotentResult result = new IdempotentResult();
            result.setRequestId((String) entries.get("requestId"));
            result.setExecuteTime(Long.parseLong((String) entries.get("executeTime")));
            result.setStatus((String) entries.get("status"));
            
            String resultJson = (String) entries.get("result");
            if (resultJson != null && !resultJson.isEmpty()) {
                result.setResult(objectMapper.readValue(resultJson, Object.class));
            }
            
            String exceptionStr = (String) entries.get("exception");
            if (exceptionStr != null && !exceptionStr.isEmpty()) {
                result.setException(new RuntimeException(exceptionStr));
            }
            
            log.debug("获取缓存结果成功: key={}, status={}", key, result.getStatus());
            return result;
        } catch (Exception e) {
            log.error("获取缓存结果失败: key={}, error={}", key, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public void deleteCachedResult(String key) {
        String cacheKey = buildCacheKey(key);
        
        try {
            redisTemplate.delete(cacheKey);
            log.debug("删除缓存结果成功: key={}", key);
        } catch (Exception e) {
            log.error("删除缓存结果失败: key={}, error={}", key, e.getMessage(), e);
        }
    }
    
    @Override
    public boolean exists(String key) {
        String cacheKey = buildCacheKey(key);
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
        } catch (Exception e) {
            log.error("检查缓存存在失败: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public void markProcessing(String key, String requestId, int expireSeconds) {
        String cacheKey = buildCacheKey(key);
        
        try {
            Map<String, String> cacheData = new HashMap<>();
            cacheData.put("requestId", requestId);
            cacheData.put("executeTime", String.valueOf(System.currentTimeMillis()));
            cacheData.put("status", IdempotentResult.STATUS_PROCESSING);
            
            redisTemplate.opsForHash().putAll(cacheKey, cacheData);
            redisTemplate.expire(cacheKey, expireSeconds, TimeUnit.SECONDS);
            
            log.debug("标记处理中状态成功: key={}, requestId={}", key, requestId);
        } catch (Exception e) {
            log.error("标记处理中状态失败: key={}, error={}", key, e.getMessage(), e);
        }
    }
    
    private String buildCacheKey(String key) {
        return CACHE_KEY_PREFIX + key;
    }
}