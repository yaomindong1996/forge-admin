package com.mdframe.forge.starter.idempotent.service;

import com.mdframe.forge.starter.idempotent.properties.TokenProperties;
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
@DisplayName("Token服务单元测试")
class RedisTokenServiceTest {
    
    @Mock
    private StringRedisTemplate redisTemplate;
    
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    
    private TokenProperties tokenProperties;
    private RedisTokenService tokenService;
    
    @BeforeEach
    void setUp() {
        tokenProperties = new TokenProperties();
        tokenProperties.setExpire(300);
        tokenProperties.setHeader("X-Idempotent-Token");
        
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        
        tokenService = new RedisTokenService(redisTemplate, tokenProperties);
    }
    
    @Test
    @DisplayName("生成Token - 成功")
    void testGenerateToken_Success() {
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        
        String token = tokenService.generateToken("test");
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(32, token.length());
        
        verify(hashOperations).putAll(anyString(), any(Map.class));
        verify(redisTemplate).expire(anyString(), eq(300L), eq(TimeUnit.SECONDS));
    }
    
    @Test
    @DisplayName("验证Token - Token有效")
    void testValidateToken_Valid() {
        String token = "test-token";
        String prefix = "test";
        
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        
        Map<Object, Object> tokenData = new HashMap<>();
        tokenData.put("status", "UNUSED");
        when(hashOperations.entries(anyString())).thenReturn(tokenData);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        
        boolean isValid = tokenService.validateToken(token, prefix);
        
        assertTrue(isValid);
        verify(redisTemplate).hasKey(anyString());
    }
    
    @Test
    @DisplayName("验证Token - Token不存在")
    void testValidateToken_NotExists() {
        String token = "test-token";
        String prefix = "test";
        
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        
        boolean isValid = tokenService.validateToken(token, prefix);
        
        assertFalse(isValid);
    }
    
    @Test
    @DisplayName("验证Token - Token已消费")
    void testValidateToken_AlreadyConsumed() {
        String token = "test-token";
        String prefix = "test";
        
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        
        Map<Object, Object> tokenData = new HashMap<>();
        tokenData.put("status", "CONSUMED");
        when(hashOperations.entries(anyString())).thenReturn(tokenData);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        
        boolean isValid = tokenService.validateToken(token, prefix);
        
        assertFalse(isValid);
    }
    
    @Test
    @DisplayName("验证Token - Token为空")
    void testValidateToken_EmptyToken() {
        boolean isValid = tokenService.validateToken("", "test");
        assertFalse(isValid);
        
        isValid = tokenService.validateToken(null, "test");
        assertFalse(isValid);
    }
    
    @Test
    @DisplayName("消费Token - 成功")
    void testConsumeToken_Success() {
        String token = "test-token";
        String prefix = "test";
        
        tokenService.consumeToken(token, prefix);
        
        verify(hashOperations).put(anyString(), eq("status"), eq("CONSUMED"));
        verify(redisTemplate).expire(anyString(), eq(60L), eq(TimeUnit.SECONDS));
    }
    
    @Test
    @DisplayName("检查Token是否已消费 - 已消费")
    void testIsTokenConsumed_Consumed() {
        String token = "test-token";
        String prefix = "test";
        
        when(hashOperations.get(anyString(), eq("status"))).thenReturn("CONSUMED");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        
        boolean consumed = tokenService.isTokenConsumed(token, prefix);
        
        assertTrue(consumed);
    }
    
    @Test
    @DisplayName("检查Token是否已消费 - 未消费")
    void testIsTokenConsumed_NotConsumed() {
        String token = "test-token";
        String prefix = "test";
        
        when(hashOperations.get(anyString(), eq("status"))).thenReturn("UNUSED");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        
        boolean consumed = tokenService.isTokenConsumed(token, prefix);
        
        assertFalse(consumed);
    }
    
    @Test
    @DisplayName("生成Token - 默认前缀")
    void testGenerateToken_DefaultPrefix() {
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        
        String token = tokenService.generateToken(null);
        
        assertNotNull(token);
        verify(hashOperations).putAll(contains("global"), any(Map.class));
    }
}