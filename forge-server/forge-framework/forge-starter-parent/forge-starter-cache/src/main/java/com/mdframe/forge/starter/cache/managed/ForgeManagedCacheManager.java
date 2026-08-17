package com.mdframe.forge.starter.cache.managed;

import com.mdframe.forge.starter.cache.managed.enums.CacheMode;
import com.mdframe.forge.starter.cache.managed.model.CacheControlAction;
import com.mdframe.forge.starter.cache.managed.model.CacheControlMessage;
import com.mdframe.forge.starter.cache.managed.model.CacheDefinition;
import com.mdframe.forge.starter.cache.managed.model.CacheLookup;
import com.mdframe.forge.starter.cache.managed.model.CachePolicyOverride;
import com.mdframe.forge.starter.cache.managed.model.EffectiveCachePolicy;
import com.mdframe.forge.starter.cache.managed.model.ManagedCacheValue;
import com.mdframe.forge.starter.cache.managed.model.ManagedCacheView;
import com.mdframe.forge.starter.cache.managed.properties.ManagedCacheProperties;
import com.mdframe.forge.starter.cache.managed.store.LocalCacheHandle;
import com.mdframe.forge.starter.cache.managed.store.ManagedCacheHandle;
import com.mdframe.forge.starter.cache.managed.store.MultiLevelCacheHandle;
import com.mdframe.forge.starter.cache.managed.store.RedisCacheHandle;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.LocalCachedMapCacheOptions;
import org.redisson.api.RLocalCachedMapCache;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Forge 受管缓存统一运行时。Redis 控制面失败不影响本地定义和业务调用。
 */
@Slf4j
public class ForgeManagedCacheManager {

    private static final String DEFINITION_SUFFIX = ":control:definitions";
    private static final String POLICY_SUFFIX = ":control:policies";
    private static final String TOPIC_SUFFIX = ":control:events";

    private final RedissonClient redissonClient;
    private final ManagedCacheProperties properties;
    private final ConcurrentMap<String, CacheDefinition> definitions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CachePolicyOverride> overrides = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ManagedCacheHandle> handles = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CacheCounters> counters = new ConcurrentHashMap<>();
    private final AtomicLong nextPolicyRefreshNanos = new AtomicLong();

    public ForgeManagedCacheManager(@Nullable RedissonClient redissonClient, ManagedCacheProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
        initializeControlPlane();
    }

    public void register(CacheDefinition definition) {
        validateDefinition(definition);
        definitions.compute(definition.identity(), (identity, existing) -> {
            if (existing != null && !existing.equals(definition)) {
                throw new IllegalStateException("缓存定义冲突: " + identity);
            }
            return definition;
        });
        if (redissonClient != null) {
            try {
                definitionMap().fastPut(definition.identity(), definition);
            } catch (RuntimeException exception) {
                recordFailure(definition.identity(), "注册缓存定义", exception);
            }
        }
    }

    public CacheLookup get(CacheDefinition definition, String key) {
        register(definition);
        EffectiveCachePolicy policy = effectivePolicy(definition);
        if (!policy.enabled()) {
            return CacheLookup.miss();
        }
        CacheCounters cacheCounters = counters(definition.identity());
        try {
            CacheLookup lookup = handle(definition, policy).get(key);
            if (lookup.hit()) {
                cacheCounters.hits.increment();
            } else {
                cacheCounters.misses.increment();
            }
            return lookup;
        } catch (RuntimeException exception) {
            cacheCounters.failures.increment();
            log.warn("读取受管缓存失败，已穿透: cache={}", definition.identity(), exception);
            return CacheLookup.miss();
        }
    }

    public void put(CacheDefinition definition, String key, Object value) {
        register(definition);
        EffectiveCachePolicy policy = effectivePolicy(definition);
        if (!policy.enabled() || value == null && !policy.cacheNull()) {
            return;
        }
        long localTtl = value == null ? policy.nullTtlSeconds() : policy.localTtlSeconds();
        long storageTtl = policy.cacheMode() == CacheMode.LOCAL
                ? localTtl
                : value == null ? policy.nullTtlSeconds() : policy.redisTtlSeconds();
        ManagedCacheValue cacheValue = new ManagedCacheValue(
                value,
                value == null,
                TimeUnit.SECONDS.toNanos(localTtl));
        try {
            handle(definition, policy).put(key, cacheValue, storageTtl);
            counters(definition.identity()).puts.increment();
        } catch (RuntimeException exception) {
            recordFailure(definition.identity(), "写入受管缓存", exception);
        }
    }

    public void evict(CacheDefinition definition, String key) {
        register(definition);
        try {
            handle(definition, effectivePolicy(definition)).evict(key);
            counters(definition.identity()).evictions.increment();
        } catch (RuntimeException exception) {
            recordFailure(definition.identity(), "删除受管缓存", exception);
        }
    }

    public void clear(CacheDefinition definition) {
        register(definition);
        clearWithoutPublish(definition.applicationCode(), definition.cacheName());
        publish(new CacheControlMessage(
                CacheControlAction.CLEAR,
                definition.applicationCode(),
                definition.cacheName(),
                null));
    }

    public void clear(String applicationCode, String cacheName) {
        CacheDefinition definition = findDefinition(applicationCode, cacheName);
        if (definition == null) {
            throw new IllegalArgumentException("缓存定义不存在: " + applicationCode + "::" + cacheName);
        }
        clear(definition);
    }

    public void applyOverride(CachePolicyOverride override) {
        CacheDefinition definition = findDefinition(override.applicationCode(), override.cacheName());
        if (definition == null) {
            throw new IllegalArgumentException("缓存定义不存在: " + override.identity());
        }
        validatePolicy(definition, EffectiveCachePolicy.from(definition, override));
        clearWithoutPublish(override.applicationCode(), override.cacheName());
        overrides.put(override.identity(), override);
        if (redissonClient != null) {
            try {
                policyMap().fastPut(override.identity(), override);
            } catch (RuntimeException exception) {
                recordFailure(override.identity(), "保存缓存策略快照", exception);
            }
        }
        publish(new CacheControlMessage(
                CacheControlAction.APPLY,
                override.applicationCode(),
                override.cacheName(),
                override));
    }

    public void removeOverride(String applicationCode, String cacheName) {
        String identity = identity(applicationCode, cacheName);
        clearWithoutPublish(applicationCode, cacheName);
        overrides.remove(identity);
        if (redissonClient != null) {
            try {
                policyMap().fastRemove(identity);
            } catch (RuntimeException exception) {
                recordFailure(identity, "删除缓存策略快照", exception);
            }
        }
        publish(new CacheControlMessage(CacheControlAction.RESET, applicationCode, cacheName, null));
    }

    public CacheDefinition findDefinition(String applicationCode, String cacheName) {
        String identity = identity(applicationCode, cacheName);
        CacheDefinition local = definitions.get(identity);
        if (local != null || redissonClient == null) {
            return local;
        }
        try {
            CacheDefinition remote = definitionMap().get(identity);
            if (remote != null) {
                definitions.putIfAbsent(identity, remote);
            }
            return remote;
        } catch (RuntimeException exception) {
            recordFailure(identity, "读取缓存定义", exception);
            return null;
        }
    }

    public List<ManagedCacheView> listCaches() {
        Map<String, CacheDefinition> merged = new HashMap<>();
        if (redissonClient != null) {
            try {
                merged.putAll(definitionMap().readAllMap());
                overrides.putAll(policyMap().readAllMap());
            } catch (RuntimeException exception) {
                log.warn("读取受管缓存控制面失败，返回当前实例快照", exception);
            }
        }
        merged.putAll(definitions);
        List<ManagedCacheView> result = new ArrayList<>();
        for (CacheDefinition definition : merged.values()) {
            CacheCounters snapshot = counters(definition.identity());
            result.add(new ManagedCacheView(
                    definition,
                    effectivePolicy(definition),
                    overrides.containsKey(definition.identity()),
                    snapshot.hits.sum(),
                    snapshot.misses.sum(),
                    snapshot.puts.sum(),
                    snapshot.evictions.sum(),
                    snapshot.failures.sum()));
        }
        result.sort(Comparator.comparing(view -> view.definition().identity()));
        return result;
    }

    private ManagedCacheHandle handle(CacheDefinition definition, EffectiveCachePolicy policy) {
        return handles.computeIfAbsent(definition.identity(), ignored -> createHandle(definition, policy));
    }

    private ManagedCacheHandle createHandle(CacheDefinition definition, EffectiveCachePolicy policy) {
        if (policy.cacheMode() == CacheMode.LOCAL) {
            return new LocalCacheHandle(policy.localMaxSize());
        }
        if (redissonClient == null) {
            throw new IllegalStateException("Redis客户端不可用");
        }
        String cacheName = properties.getNamespace() + ":data:"
                + definition.applicationCode() + ":" + definition.cacheName();
        if (policy.cacheMode() == CacheMode.REDIS) {
            RMapCache<String, ManagedCacheValue> cache = redissonClient.getMapCache(cacheName);
            return new RedisCacheHandle(cache);
        }
        long localTtl = policy.cacheNull()
                ? Math.min(policy.localTtlSeconds(), policy.nullTtlSeconds())
                : policy.localTtlSeconds();
        LocalCachedMapCacheOptions<String, ManagedCacheValue> options =
                LocalCachedMapCacheOptions.<String, ManagedCacheValue>defaults()
                        .cacheProvider(LocalCachedMapCacheOptions.CacheProvider.CAFFEINE)
                        .cacheSize(policy.localMaxSize())
                        .timeToLive(localTtl, TimeUnit.SECONDS)
                        .syncStrategy(LocalCachedMapCacheOptions.SyncStrategy.INVALIDATE)
                        .reconnectionStrategy(LocalCachedMapCacheOptions.ReconnectionStrategy.CLEAR)
                        .storeMode(LocalCachedMapCacheOptions.StoreMode.LOCALCACHE_REDIS);
        RLocalCachedMapCache<String, ManagedCacheValue> cache =
                redissonClient.getLocalCachedMapCache(cacheName, options);
        return new MultiLevelCacheHandle(cache);
    }

    private EffectiveCachePolicy effectivePolicy(CacheDefinition definition) {
        refreshPolicySnapshotIfDue();
        EffectiveCachePolicy policy = EffectiveCachePolicy.from(definition, overrides.get(definition.identity()));
        validatePolicy(definition, policy);
        return policy;
    }

    private void refreshPolicySnapshotIfDue() {
        if (redissonClient == null) {
            return;
        }
        long now = System.nanoTime();
        long next = nextPolicyRefreshNanos.get();
        if (now < next || !nextPolicyRefreshNanos.compareAndSet(
                next, now + TimeUnit.SECONDS.toNanos(properties.getPolicyRefreshSeconds()))) {
            return;
        }
        try {
            Map<String, CachePolicyOverride> remote = policyMap().readAllMap();
            Map<String, CachePolicyOverride> previous = new HashMap<>(overrides);
            overrides.clear();
            overrides.putAll(remote);
            previous.forEach((identity, policy) -> {
                if (!policy.equals(remote.get(identity))) {
                    closeHandle(identity);
                }
            });
            remote.forEach((identity, policy) -> {
                if (!policy.equals(previous.get(identity))) {
                    closeHandle(identity);
                }
            });
        } catch (RuntimeException exception) {
            log.warn("校准受管缓存策略快照失败，继续使用当前本地快照", exception);
        }
    }

    private void validateDefinition(CacheDefinition definition) {
        if (definition.applicationCode() == null || definition.applicationCode().isBlank()
                || definition.cacheName() == null
                || !definition.cacheName().matches("[a-zA-Z0-9][a-zA-Z0-9:._-]{1,127}")) {
            throw new IllegalArgumentException("缓存应用编码或名称不合法");
        }
        validatePolicy(definition, EffectiveCachePolicy.from(definition, null));
    }

    private void validatePolicy(CacheDefinition definition, EffectiveCachePolicy policy) {
        if (policy.cacheMode() == null || !definition.allowedModes().contains(policy.cacheMode())) {
            throw new IllegalArgumentException("缓存模式超出代码允许范围: " + definition.identity());
        }
        if (policy.localTtlSeconds() <= 0 || policy.redisTtlSeconds() <= 0
                || policy.localMaxSize() <= 0 || policy.nullTtlSeconds() <= 0) {
            throw new IllegalArgumentException("缓存TTL和本地容量必须大于0: " + definition.identity());
        }
        if (policy.cacheMode() == CacheMode.MULTI
                && policy.localTtlSeconds() > policy.redisTtlSeconds()) {
            throw new IllegalArgumentException("多级缓存本地TTL不能大于Redis TTL: " + definition.identity());
        }
    }

    private void clearWithoutPublish(String applicationCode, String cacheName) {
        String identity = identity(applicationCode, cacheName);
        ManagedCacheHandle handle = handles.remove(identity);
        if (handle == null) {
            CacheDefinition definition = findDefinition(applicationCode, cacheName);
            if (definition == null) {
                return;
            }
            try {
                handle = createHandle(definition, effectivePolicy(definition));
            } catch (RuntimeException exception) {
                recordFailure(identity, "创建待清理缓存句柄", exception);
                return;
            }
        }
        try {
            handle.clear();
            handle.close();
            counters(identity).evictions.increment();
        } catch (RuntimeException exception) {
            recordFailure(identity, "清空受管缓存", exception);
        }
    }

    private void closeHandle(String identity) {
        ManagedCacheHandle handle = handles.remove(identity);
        if (handle == null) {
            return;
        }
        try {
            handle.close();
        } catch (RuntimeException exception) {
            recordFailure(identity, "关闭旧缓存句柄", exception);
        }
    }

    private void initializeControlPlane() {
        if (redissonClient == null) {
            return;
        }
        try {
            overrides.putAll(policyMap().readAllMap());
            controlTopic().addListener(CacheControlMessage.class, (channel, message) -> onControlMessage(message));
        } catch (RuntimeException exception) {
            log.warn("初始化受管缓存控制面失败，运行时将使用代码默认策略", exception);
        }
    }

    private void onControlMessage(CacheControlMessage message) {
        if (message == null) {
            return;
        }
        clearWithoutPublish(message.applicationCode(), message.cacheName());
        String identity = identity(message.applicationCode(), message.cacheName());
        if (message.action() == CacheControlAction.APPLY && message.policy() != null) {
            overrides.put(identity, message.policy());
        } else if (message.action() == CacheControlAction.RESET) {
            overrides.remove(identity);
        }
    }

    private void publish(CacheControlMessage message) {
        if (redissonClient == null) {
            return;
        }
        try {
            controlTopic().publish(message);
        } catch (RuntimeException exception) {
            recordFailure(identity(message.applicationCode(), message.cacheName()), "发布缓存控制事件", exception);
        }
    }

    private RMap<String, CacheDefinition> definitionMap() {
        return redissonClient.getMap(properties.getNamespace() + DEFINITION_SUFFIX);
    }

    private RMap<String, CachePolicyOverride> policyMap() {
        return redissonClient.getMap(properties.getNamespace() + POLICY_SUFFIX);
    }

    private RTopic controlTopic() {
        return redissonClient.getTopic(properties.getNamespace() + TOPIC_SUFFIX);
    }

    private String identity(String applicationCode, String cacheName) {
        return applicationCode + "::" + cacheName;
    }

    private CacheCounters counters(String identity) {
        return counters.computeIfAbsent(identity, ignored -> new CacheCounters());
    }

    private void recordFailure(String identity, String operation, RuntimeException exception) {
        counters(identity).failures.increment();
        log.warn("{}失败: cache={}", operation, identity, exception);
    }

    private static final class CacheCounters {
        private final LongAdder hits = new LongAdder();
        private final LongAdder misses = new LongAdder();
        private final LongAdder puts = new LongAdder();
        private final LongAdder evictions = new LongAdder();
        private final LongAdder failures = new LongAdder();
    }
}
