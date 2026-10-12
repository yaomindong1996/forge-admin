package com.mdframe.forge.starter.idempotent.service;

import cn.hutool.core.util.IdUtil;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.idempotent.exception.TokenInvalidException;
import com.mdframe.forge.starter.idempotent.constant.IdempotentConstant;
import com.mdframe.forge.starter.idempotent.properties.TokenProperties;
import com.mdframe.forge.starter.idempotent.enums.IdempotentTokenStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class RedisTokenService implements TokenService {

    private static final int CONSUMED_RETENTION_SECONDS = 60;
    private static final Pattern SAFE_TOKEN = Pattern.compile("[A-Za-z0-9_-]{1,128}");
    
    private final StringRedisTemplate redisTemplate;
    private final TokenProperties properties;
    
    
    @Override
    public String generateToken(String prefix) {
        if (properties.getExpire() <= 0) {
            throw new BusinessException("幂等 Token 有效期必须大于零");
        }
        String token = IdUtil.fastSimpleUUID();
        String key = buildTokenKey(prefix, token);
        Long generated = redisTemplate.execute(IdempotentTokenScripts.GENERATE, List.of(key),
                String.valueOf(System.currentTimeMillis()), IdempotentTokenStatus.UNUSED.getCode(),
                String.valueOf(properties.getExpire()));
        if (!Long.valueOf(1).equals(generated)) {
            throw new BusinessException("幂等 Token 生成失败，请重试");
        }
        log.debug("生成幂等 Token 成功");
        return token;
    }
    
    @Override
    public boolean validateToken(String token, String prefix) {
        if (!validToken(token)) {
            return false;
        }
        Long valid = redisTemplate.execute(IdempotentTokenScripts.VALIDATE,
                List.of(buildTokenKey(prefix, token)), IdempotentTokenStatus.UNUSED.getCode());
        return Long.valueOf(1).equals(valid);
    }
    
    @Override
    public void consumeToken(String token, String prefix) {
        // 保留旧 API，但失败必须显式拒绝，不能静默允许业务继续执行。
        if (!tryConsumeToken(token, prefix)) {
            throw new TokenInvalidException("Token 无效、已消费或已过期");
        }
    }

    @Override
    public boolean tryConsumeToken(String token, String prefix) {
        if (!validToken(token)) {
            return false;
        }
        Long consumed = redisTemplate.execute(IdempotentTokenScripts.CONSUME,
                List.of(buildTokenKey(prefix, token)), IdempotentTokenStatus.UNUSED.getCode(),
                IdempotentTokenStatus.CONSUMED.getCode(), String.valueOf(CONSUMED_RETENTION_SECONDS));
        return Long.valueOf(1).equals(consumed);
    }
    
    @Override
    public boolean isTokenConsumed(String token, String prefix) {
        if (!validToken(token)) {
            return false;
        }
        String key = buildTokenKey(prefix, token);
        String status = (String) redisTemplate.opsForHash().get(key, "status");
        return IdempotentTokenStatus.CONSUMED.matches(status);
    }

    private boolean validToken(String token) {
        return token != null && SAFE_TOKEN.matcher(token).matches();
    }
    
    private String buildTokenKey(String prefix, String token) {
        String actualPrefix = (prefix != null && !prefix.isEmpty()) ? prefix : "global";
        // 注解默认前缀与生成接口未传前缀必须落在同一命名空间，显式默认值也视为别名。
        if (IdempotentConstant.DEFAULT_PREFIX.equals(actualPrefix)) {
            actualPrefix = "global";
        }
        if (actualPrefix.length() > 128 || actualPrefix.indexOf('\r') >= 0 || actualPrefix.indexOf('\n') >= 0) {
            throw new BusinessException("幂等 Token 前缀无效");
        }
        return "idempotent:token:" + actualPrefix + ":" + token;
    }
}
