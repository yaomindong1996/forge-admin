package com.mdframe.forge.starter.idempotent.service;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

/** 原子语义内存夹具，不执行 Lua，不代表外部 Redis。 */
public final class TokenRedisFixture {
    public final StringRedisTemplate redis = mock(StringRedisTemplate.class, RETURNS_DEEP_STUBS);
    private final Map<String, Entry> entries = new HashMap<>();
    private long clock;
    public volatile boolean available = true;

    public TokenRedisFixture() {
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any(Object[].class)))
                .thenAnswer(call -> eval(call.getArgument(0), call.getArgument(1),
                        (Object[]) call.getRawArguments()[2]));
        when(redis.opsForHash().get(anyString(), eq("status"))).thenAnswer(call -> status(call.getArgument(0)));
    }

    public synchronized void put(String token, String prefix, String status, long ttlMillis) {
        entries.put(key(token, prefix), new Entry(status, clock + ttlMillis));
    }

    public synchronized long ttl(String token, String prefix) {
        Entry entry = current(key(token, prefix));
        return entry == null ? -2 : entry.expires - clock;
    }

    public synchronized void advance(long millis) {
        clock += millis;
    }

    public static String key(String token, String prefix) {
        return "idempotent:token:" + (prefix == null || prefix.isEmpty() ? "global" : prefix) + ":" + token;
    }

    private synchronized Long eval(RedisScript<Long> script, List<String> keys, Object[] args) {
        if (!available) {
            throw new DataAccessResourceFailureException("fixture Redis unavailable");
        }
        String key = keys.get(0);
        Entry entry = current(key);
        if (script == IdempotentTokenScripts.GENERATE) {
            if (entry != null) {
                return 0L;
            }
            entries.put(key, new Entry(args[1].toString(), clock + Long.parseLong(args[2].toString()) * 1000));
            return 1L;
        }
        if (entry == null || !args[0].equals(entry.status)) {
            return 0L;
        }
        if (script == IdempotentTokenScripts.VALIDATE) {
            return 1L;
        }
        if (script != IdempotentTokenScripts.CONSUME) {
            throw new AssertionError("unexpected token script");
        }
        entry.status = args[1].toString();
        entry.expires = clock + Long.parseLong(args[2].toString()) * 1000;
        return 1L;
    }

    private synchronized String status(String key) {
        Entry entry = current(key);
        return entry == null ? null : entry.status;
    }

    private Entry current(String key) {
        Entry entry = entries.get(key);
        if (entry != null && entry.expires <= clock) {
            entries.remove(key);
            return null;
        }
        return entry;
    }

    private static final class Entry {
        private String status;
        private long expires;
        private Entry(String status, long expires) {
            this.status = status;
            this.expires = expires;
        }
    }
}
