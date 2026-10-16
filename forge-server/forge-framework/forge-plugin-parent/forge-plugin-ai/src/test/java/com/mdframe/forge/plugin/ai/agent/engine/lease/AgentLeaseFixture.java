package com.mdframe.forge.plugin.ai.agent.engine.lease;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 共享内存的单 key 原子语义 fixture，不解释 Lua，也不是外部 Redis 验收。 */
public final class AgentLeaseFixture {
    private final Map<String, Entry> data = new HashMap<>();
    private final List<Runnable> heartbeats = new CopyOnWriteArrayList<>();
    private long clock;
    public volatile boolean available = true;
    public final StringRedisTemplate redis = new StringRedisTemplate() {
        @Override
        public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
            return script.getResultType().cast(eval(script.getScriptAsString(), keys.get(0), args));
        }
    };

    public AgentSessionLeaseRegistry registry() {
        var scheduler = mock(AgentLeaseHeartbeatScheduler.class);
        when(scheduler.schedule(any(Runnable.class), any(Duration.class)))
                .thenAnswer(call -> {
                    heartbeats.add(call.getArgument(0));
                    return mock(ScheduledFuture.class);
                });
        return new AgentSessionLeaseRegistry(new AgentSessionLeaseStore(redis), scheduler,
                new AgentSessionLeaseProperties());
    }

    public void heartbeat() {
        heartbeats.forEach(Runnable::run);
    }

    public synchronized void advance(long millis) {
        clock += millis;
    }

    public synchronized boolean occupied(String id) {
        return current(AgentSessionLeaseStore.key(id)) != null;
    }

    private synchronized Long eval(String script, String key, Object[] args) {
        if (!available) {
            throw new DataAccessResourceFailureException("fixture Redis unavailable");
        }
        Entry entry = current(key);
        if (script.equals(AgentSessionLeaseScripts.ACQUIRE.getScriptAsString())) {
            if (entry != null) {
                return 0L;
            }
            var fields = new HashMap<String, String>();
            fields.put("owner", args[0].toString());
            fields.put("user", args[1].toString());
            fields.put("tenant", args[2].toString());
            fields.put("cancel", "0");
            data.put(key, new Entry(fields, clock + Long.parseLong(args[3].toString())));
            return 1L;
        }
        if (script.equals(AgentSessionLeaseScripts.CANCEL.getScriptAsString())) {
            if (entry == null) {
                return 0L;
            }
            if (!entry.fields.get("user").equals(args[0]) || !entry.fields.get("tenant").equals(args[1])) {
                return -1L;
            }
            entry.fields.put("cancel", "1");
            return 1L;
        }
        if (entry == null || !entry.fields.get("owner").equals(args[0])) {
            return 0L;
        }
        if (script.equals(AgentSessionLeaseScripts.RELEASE.getScriptAsString())) {
            data.remove(key);
            return 1L;
        }
        if (!script.equals(AgentSessionLeaseScripts.RENEW.getScriptAsString())) {
            throw new AssertionError("unexpected lease script");
        }
        entry.expires = clock + Long.parseLong(args[1].toString());
        return "1".equals(entry.fields.get("cancel")) ? 2L : 1L;
    }

    private Entry current(String key) {
        Entry entry = data.get(key);
        if (entry != null && entry.expires <= clock) {
            data.remove(key);
            return null;
        }
        return entry;
    }

    private static final class Entry {
        private final Map<String, String> fields;
        private long expires;
        private Entry(Map<String, String> fields, long expires) {
            this.fields = fields;
            this.expires = expires;
        }
    }
}
