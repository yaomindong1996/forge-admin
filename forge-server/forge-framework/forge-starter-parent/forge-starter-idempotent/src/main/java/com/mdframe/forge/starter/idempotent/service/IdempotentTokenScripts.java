package com.mdframe.forge.starter.idempotent.service;

import org.springframework.data.redis.core.script.DefaultRedisScript;

/** 单 key 脚本兼容 Redis 6 / Cluster；不在 Java 端拆分校验、状态写入和 TTL。 */
final class IdempotentTokenScripts {

    static final DefaultRedisScript<Long> GENERATE = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) ~= 0 then return 0 end
            redis.call('HSET', KEYS[1], 'createTime', ARGV[1], 'status', ARGV[2])
            redis.call('EXPIRE', KEYS[1], ARGV[3])
            return 1
            """, Long.class);

    static final DefaultRedisScript<Long> VALIDATE = new DefaultRedisScript<>("""
            if redis.call('PTTL', KEYS[1]) <= 0 then return 0 end
            if redis.call('HGET', KEYS[1], 'status') == ARGV[1] then return 1 end
            return 0
            """, Long.class);

    static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>("""
            if redis.call('PTTL', KEYS[1]) <= 0 then return 0 end
            if redis.call('HGET', KEYS[1], 'status') ~= ARGV[1] then return 0 end
            redis.call('HSET', KEYS[1], 'status', ARGV[2])
            redis.call('EXPIRE', KEYS[1], ARGV[3])
            return 1
            """, Long.class);

    private IdempotentTokenScripts() { }
}
