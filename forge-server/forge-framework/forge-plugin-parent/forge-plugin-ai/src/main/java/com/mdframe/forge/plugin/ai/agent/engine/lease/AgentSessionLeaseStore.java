package com.mdframe.forge.plugin.ai.agent.engine.lease;

import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AgentSessionLeaseStore {
    private final StringRedisTemplate redis;

    public boolean acquire(Owner owner, long ttlMillis) {
        Long result = redis.execute(AgentSessionLeaseScripts.ACQUIRE, List.of(owner.key()), owner.nonce(),
                String.valueOf(owner.userId()), String.valueOf(owner.tenantId()), String.valueOf(ttlMillis));
        return Long.valueOf(1).equals(result);
    }

    public long renew(Owner owner, long ttlMillis) {
        Long result = redis.execute(AgentSessionLeaseScripts.RENEW, List.of(owner.key()),
                owner.nonce(), String.valueOf(ttlMillis));
        return result == null ? 0 : result;
    }

    public void release(Owner owner) {
        redis.execute(AgentSessionLeaseScripts.RELEASE, List.of(owner.key()), owner.nonce());
    }

    public void cancel(String sessionId, Long userId, Long tenantId) {
        Long result = redis.execute(AgentSessionLeaseScripts.CANCEL, List.of(key(sessionId)),
                String.valueOf(userId), String.valueOf(tenantId));
        if (result == null) {
            throw new BusinessException("Agent 取消请求未完成，请重试");
        }
        if (result == -1L) {
            throw new BusinessException(403, "无权停止该 Agent 会话");
        }
    }

    static String key(String sessionId) {
        if (sessionId == null || sessionId.isBlank() || sessionId.length() > 64) {
            throw new BusinessException(400, "会话标识不能为空且不能超过 64 字符");
        }
        try {
            // 会话主键全局唯一，不能按请求租户拆 key，否则跨租户请求会形成两份占用。
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(sessionId.getBytes(StandardCharsets.UTF_8));
            return "agent:session:lease:v1:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    public record Owner(String key, String nonce, Long userId, Long tenantId) { }
}
