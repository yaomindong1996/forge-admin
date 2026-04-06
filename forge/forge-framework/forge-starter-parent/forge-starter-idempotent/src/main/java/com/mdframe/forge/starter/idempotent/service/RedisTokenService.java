package com.mdframe.forge.starter.idempotent.service;

import cn.hutool.core.util.IdUtil;
import com.mdframe.forge.starter.idempotent.properties.TokenProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class RedisTokenService implements TokenService {
    
    private final StringRedisTemplate redisTemplate;
    private final TokenProperties properties;
    
    private static final String TOKEN_STATUS_UNUSED = "UNUSED";
    private static final String TOKEN_STATUS_CONSUMED = "CONSUMED";
    
    @Override
    public String generateToken(String prefix) {
        String token = IdUtil.fastSimpleUUID();
        String key = buildTokenKey(prefix, token);
        
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("createTime", String.valueOf(System.currentTimeMillis()));
        tokenData.put("status", TOKEN_STATUS_UNUSED);
        
        redisTemplate.opsForHash().putAll(key, tokenData);
        redisTemplate.expire(key, properties.getExpire(), TimeUnit.SECONDS);
        
        log.debug("生成Token成功: {}, prefix: {}, expire: {}s", token, prefix, properties.getExpire());
        return token;
    }
    
    @Override
    public boolean validateToken(String token, String prefix) {
        if (token == null || token.isEmpty()) {
            log.warn("Token为空");
            return false;
        }
        
        String key = buildTokenKey(prefix, token);
        
        Boolean exists = redisTemplate.hasKey(key);
        if (!Boolean.TRUE.equals(exists)) {
            log.warn("Token不存在: {}", token);
            return false;
        }
        
        String status = (String) redisTemplate.opsForHash().get(key, "status");
        if (TOKEN_STATUS_CONSUMED.equals(status)) {
            log.warn("Token已被消费: {}", token);
            return false;
        }
        
        log.debug("Token验证成功: {}", token);
        return true;
    }
    
    @Override
    public void consumeToken(String token, String prefix) {
        String key = buildTokenKey(prefix, token);
        
        redisTemplate.opsForHash().put(key, "status", TOKEN_STATUS_CONSUMED);
        redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        
        log.debug("Token已消费: {}", token);
    }
    
    @Override
    public boolean isTokenConsumed(String token, String prefix) {
        String key = buildTokenKey(prefix, token);
        String status = (String) redisTemplate.opsForHash().get(key, "status");
        return TOKEN_STATUS_CONSUMED.equals(status);
    }
    
    private String buildTokenKey(String prefix, String token) {
        String actualPrefix = (prefix != null && !prefix.isEmpty()) ? prefix : "global";
        return "idempotent:token:" + actualPrefix + ":" + token;
    }
}