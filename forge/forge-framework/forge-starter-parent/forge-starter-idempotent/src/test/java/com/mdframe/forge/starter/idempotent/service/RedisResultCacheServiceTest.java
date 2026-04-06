package com.mdframe.forge.starter.idempotent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.idempotent.dto.IdempotentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("结果缓存服务单元测试")
class RedisResultCacheServiceTest {
    
    @Mock
    private StringRedisTemplate redisTemplate;
    
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    
    private ObjectMapper objectMapper;
    private RedisResultCacheService resultCacheService;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        resultCacheService = new RedisResultCacheService(redisTemplate, objectMapper);
    }
    
    @Test
    @DisplayName("缓存执行结果 - 成功")
    void testCacheResult_Success() throws Exception {
        String key = "test-key";
        Object result = "test-result";
        int expireSeconds = 3600;
        
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        
        resultCacheService.cacheResult(key, result, expireSeconds);
        
        verify(hashOperations).putAll(anyString(), any(Map.class));
        verify(redisTemplate).expire(anyString(), eq((long) expireSeconds), eq(TimeUnit.SECONDS));
    }
    
    @Test
    @DisplayName("缓存异常结果 - 成功")
    void testCacheException_Success() {
        String key = "test-key";
        Exception exception = new RuntimeException("test-exception");
        int expireSeconds = 3600;
        
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        
        resultCacheService.cacheException(key, exception, expireSeconds);
        
        verify(hashOperations).putAll(anyString(), any(Map.class));
        verify(redisTemplate).expire(anyString(), eq((long) expireSeconds), eq(TimeUnit.SECONDS));
    }
    
    @Test
    @DisplayName("获取缓存结果 - 存在")
    void testGetCachedResult_Exists() throws Exception {
        String key = "test-key";
        String resultJson = "\"test-result\"";
        
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        
        Map<Object, Object> cacheData = new HashMap<>();
        cacheData.put("requestId", "test-request-id");
        cacheData.put("result", resultJson);
        cacheData.put("executeTime", String.valueOf(System.currentTimeMillis()));
        cacheData.put("status", "SUCCESS");
        
        when(hashOperations.entries(anyString())).thenReturn(cacheData);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        
        IdempotentResult result = resultCacheService.getCachedResult(key);
        
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("test-request-id", result.getRequestId());
    }
    
    @Test
    @DisplayName("获取缓存结果 - 不存在")
    void testGetCachedResult_NotExists() {
        String key = "test-key";
        
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        
        IdempotentResult result = resultCacheService.getCachedResult(key);
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("删除缓存结果 - 成功")
    void testDeleteCachedResult_Success() {
        String key = "test-key";
        
        resultCacheService.deleteCachedResult(key);
        
        verify(redisTemplate).delete(contains(key));
    }
    
    @Test
    @DisplayName("检查缓存存在 - 存在")
    void testExists_True() {
        String key = "test-key";
        
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        
        boolean exists = resultCacheService.exists(key);
        
        assertTrue(exists);
    }
    
    @Test
    @DisplayName("检查缓存存在 - 不存在")
    void testExists_False() {
        String key = "test-key";
        
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        
        boolean exists = resultCacheService.exists(key);
        
        assertFalse(exists);
    }
    
    @Test
    @DisplayName("标记处理中状态 - 成功")
    void testMarkProcessing_Success() {
        String key = "test-key";
        String requestId = "test-request-id";
        int expireSeconds = 600;
        
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        
        resultCacheService.markProcessing(key, requestId, expireSeconds);
        
        verify(hashOperations).putAll(anyString(), any(Map.class));
        verify(redisTemplate).expire(anyString(), eq((long) expireSeconds), eq(TimeUnit.SECONDS));
    }
}