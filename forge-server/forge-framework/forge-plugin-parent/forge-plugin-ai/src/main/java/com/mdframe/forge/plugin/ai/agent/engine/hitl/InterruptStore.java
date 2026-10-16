package com.mdframe.forge.plugin.ai.agent.engine.hitl;

import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** v2 中断快照，10 分钟 TTL；先验身份后 CAS 消费，兼容 Redis 6.0，不使用 GETDEL。 */
@Component
@RequiredArgsConstructor
public class InterruptStore {

    static final String KEY_PREFIX = "agent:interrupt:v2:";
    private static final long TTL_MINUTES = 10;
    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public String save(ReactContext context) {
        String interruptId = UUID.randomUUID().toString();
        String value = JSON.toJSONString(AgentInterruptSnapshot.from(context, interruptId));
        redisTemplate.opsForValue().set(KEY_PREFIX + interruptId, value, TTL_MINUTES, TimeUnit.MINUTES);
        return interruptId;
    }

    public InterruptState get(String interruptId) {
        if (!validId(interruptId)) {
            return null;
        }
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + interruptId);
        if (value == null) {
            return null;
        }
        AgentInterruptSnapshot snapshot;
        try {
            snapshot = JSON.parseObject(value, AgentInterruptSnapshot.class);
        } catch (RuntimeException ex) {
            throw new BusinessException("中断状态无效，请重新发起对话");
        }
        if (snapshot == null || !interruptId.equals(snapshot.getInterruptId())) {
            throw new BusinessException("中断状态无效，请重新发起对话");
        }
        snapshot.validate();
        return new InterruptState(snapshot, value);
    }

    public boolean consume(String interruptId, InterruptState state) {
        if (!validId(interruptId) || !interruptId.equals(state.snapshot().getInterruptId())) {
            return false;
        }
        Long deleted = redisTemplate.execute(CONSUME, List.of(KEY_PREFIX + interruptId), state.rawValue());
        return Long.valueOf(1).equals(deleted);
    }

    private boolean validId(String value) {
        try {
            return value != null && UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public record InterruptState(AgentInterruptSnapshot snapshot, String rawValue) {
        public ReactContext getContext() {
            return snapshot.restore();
        }
    }
}
