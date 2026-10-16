package com.mdframe.forge.plugin.ai.agent.engine.lease;

import org.springframework.data.redis.core.script.DefaultRedisScript;

/** 一个会话只访问一个 key，兼容 Redis 6 与 Cluster；续租/释放都比较不可复用的 owner nonce。 */
final class AgentSessionLeaseScripts {
    static final DefaultRedisScript<Long> ACQUIRE = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) ~= 0 then return 0 end
            redis.call('HSET', KEYS[1], 'owner', ARGV[1], 'user', ARGV[2], 'tenant', ARGV[3], 'cancel', '0')
            redis.call('PEXPIRE', KEYS[1], ARGV[4])
            return 1
            """, Long.class);

    static final DefaultRedisScript<Long> RENEW = new DefaultRedisScript<>("""
            if redis.call('PTTL', KEYS[1]) <= 0 then return 0 end
            if redis.call('HGET', KEYS[1], 'owner') ~= ARGV[1] then return 0 end
            redis.call('PEXPIRE', KEYS[1], ARGV[2])
            if redis.call('HGET', KEYS[1], 'cancel') == '1' then return 2 end
            return 1
            """, Long.class);

    static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>("""
            if redis.call('HGET', KEYS[1], 'owner') == ARGV[1] then return redis.call('DEL', KEYS[1]) end
            return 0
            """, Long.class);

    static final DefaultRedisScript<Long> CANCEL = new DefaultRedisScript<>("""
            if redis.call('PTTL', KEYS[1]) <= 0 then return 0 end
            if redis.call('HGET', KEYS[1], 'user') ~= ARGV[1]
                or redis.call('HGET', KEYS[1], 'tenant') ~= ARGV[2] then return -1 end
            redis.call('HSET', KEYS[1], 'cancel', '1')
            return 1
            """, Long.class);

    private AgentSessionLeaseScripts() { }
}
