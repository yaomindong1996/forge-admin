package com.mdframe.forge.starter.cache.service.impl;

import com.mdframe.forge.starter.cache.service.ICacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RList;
import org.redisson.api.RType;
import org.redisson.api.RedissonClient;
import org.redisson.api.NodeType;
import org.redisson.api.NodesGroup;
import org.redisson.api.Node;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基于Redisson的缓存服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedissonCacheServiceImpl implements ICacheService {

    private final RedissonClient redissonClient;

    @Override
    public void set(String key, Object value) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        bucket.set(value);
    }

    @Override
    public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        bucket.set(value, timeout, timeUnit);
    }

    @Override
    public void set(String key, Object value, Duration duration) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        bucket.set(value, duration);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    @Override
    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    @Override
    public long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        return redissonClient.getKeys().delete(keys.toArray(new String[0]));
    }

    @Override
    public boolean hasKey(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    @Override
    public boolean expire(String key, long timeout, TimeUnit timeUnit) {
        return redissonClient.getBucket(key).expire(Duration.ofMillis(timeUnit.toMillis(timeout)));
    }

    @Override
    public long getExpire(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        long remainTimeToLive = bucket.remainTimeToLive();
        return remainTimeToLive == -1 ? -1 : remainTimeToLive / 1000;
    }

    @Override
    public long deletePattern(String pattern) {
        RKeys keys = redissonClient.getKeys();
        Iterable<String> keysByPattern = keys.getKeysByPattern(pattern);
        long count = 0;
        for (String key : keysByPattern) {
            keys.delete(key);
            count++;
        }
        return count;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String hashKey) {
        RMap<String, T> map = redissonClient.getMap(key);
        return map.get(hashKey);
    }

    @Override
    public void hSet(String key, String hashKey, Object value) {
        RMap<String, Object> map = redissonClient.getMap(key);
        map.put(hashKey, value);
    }

    @Override
    public long hDelete(String key, Object... hashKeys) {
        RMap<Object, Object> map = redissonClient.getMap(key);
        int removed = 0;
        for (Object hashKey : hashKeys) {
            if (map.remove(hashKey) != null) {
                removed++;
            }
        }
        return removed;
    }

    @Override
    public boolean hHasKey(String key, String hashKey) {
        RMap<String, Object> map = redissonClient.getMap(key);
        return map.containsKey(hashKey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> hGetAll(String key) {
        RMap<String, T> map = redissonClient.getMap(key);
        return map.readAllMap();
    }

    @Override
    public long sAdd(String key, Object... values) {
        RSet<Object> set = redissonClient.getSet(key);
        return set.addAll(Set.of(values)) ? values.length : 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Set<T> sMembers(String key) {
        RSet<T> set = redissonClient.getSet(key);
        return set.readAll();
    }

    @Override
    public boolean sIsMember(String key, Object value) {
        RSet<Object> set = redissonClient.getSet(key);
        return set.contains(value);
    }

    @Override
    public long sRemove(String key, Object... values) {
        RSet<Object> set = redissonClient.getSet(key);
        return set.removeAll(Set.of(values)) ? values.length : 0;
    }

    @Override
    public long increment(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(delta);
    }

    @Override
    public long decrement(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(-delta);
    }

    @Override
    public List<String> getKeysByPattern(String pattern, int page, int pageSize) {
        RKeys keys = redissonClient.getKeys();
        Iterable<String> keysByPattern = keys.getKeysByPattern(pattern);
        
        // 转换为列表并分页
        List<String> allKeys = new ArrayList<>();
        for (String key : keysByPattern) {
            allKeys.add(key);
        }
        
        // 计算分页
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, allKeys.size());
        
        if (start >= allKeys.size()) {
            return new ArrayList<>();
        }
        
        return allKeys.subList(start, end);
    }

    @Override
    public long countKeysByPattern(String pattern) {
        RKeys keys = redissonClient.getKeys();
        Iterable<String> keysByPattern = keys.getKeysByPattern(pattern);
        long count = 0;
        for (String ignored : keysByPattern) {
            count++;
        }
        return count;
    }

    @Override
    public String getType(String key) {
        RKeys keys = redissonClient.getKeys();
        RType type = keys.getType(key);
        return type != null ? type.name() : "NONE";
    }

    @Override
    public Map<String, Object> getCacheInfo(String key) {
        Map<String, Object> info = new HashMap<>();
        info.put("key", key);
        
        // 获取类型
        String type = getType(key);
        info.put("type", type);
        
        // 获取过期时间
        long ttl = getExpire(key);
        info.put("ttl", ttl);
        
        // 根据类型获取值
        Object value = null;
        try {
            switch (type) {
                case "STRING":
                    // 使用RBucket获取原始字节数据,避免反序列化错误
                    RBucket<Object> bucket = redissonClient.getBucket(key);
                    try {
                        value = bucket.get();
                    } catch (Exception e) {
                        // 如果反序列化失败,尝试以字符串方式读取
                        log.warn("反序列化失败,尝试以字符串方式读取: key={}", key);
                        org.redisson.client.codec.StringCodec stringCodec = new org.redisson.client.codec.StringCodec();
                        RBucket<String> stringBucket = redissonClient.getBucket(key, stringCodec);
                        value = stringBucket.get();
                    }
                    break;
                case "HASH":
                    value = hGetAll(key);
                    break;
                case "SET":
                    value = sMembers(key);
                    break;
                case "LIST":
                    RList<Object> list = redissonClient.getList(key);
                    value = list.readAll();
                    break;
                default:
                    // 默认尝试获取
                    try {
                        value = get(key);
                    } catch (Exception e) {
                        log.warn("获取值失败,尝试以字符串方式读取: key={}", key);
                        org.redisson.client.codec.StringCodec stringCodec = new org.redisson.client.codec.StringCodec();
                        RBucket<String> stringBucket = redissonClient.getBucket(key, stringCodec);
                        value = stringBucket.get();
                    }
            }
        } catch (Exception e) {
            log.error("获取缓存值失败: key={}, type={}", key, type, e);
            value = "Error: " + e.getMessage();
        }
        
        info.put("value", value);
        
        return info;
    }

    @Override
    public List<String> keys(String pattern) {
        RKeys rKeys = redissonClient.getKeys();
        Iterable<String> keysByPattern = rKeys.getKeysByPattern(pattern);
        
        List<String> result = new ArrayList<>();
        for (String key : keysByPattern) {
            result.add(key);
        }
        return result;
    }

    @Override
    public Map<String, Object> getServerInfo() {
        Map<String, Object> info = new HashMap<>();
        try {
            // 使用Redis INFO命令获取服务器信息
            String serverInfo = executeInfoCommand("server");
            Map<String, String> parsedInfo = parseInfo(serverInfo);
            
            info.put("redisVersion", parsedInfo.get("redis_version"));
            info.put("os", parsedInfo.get("os"));
            info.put("archBits", parsedInfo.get("arch_bits"));
            info.put("processId", parsedInfo.get("process_id"));
            info.put("uptimeInSeconds", parsedInfo.get("uptime_in_seconds"));
        } catch (Exception e) {
            log.warn("获取Redis服务器信息失败", e);
            info.put("error", "获取服务器信息失败: " + e.getMessage());
        }
        return info;
    }

    @Override
    public Map<String, Object> getMemoryInfo() {
        Map<String, Object> info = new HashMap<>();
        try {
            // 使用Redis INFO命令获取内存信息
            String memoryInfo = executeInfoCommand("memory");
            Map<String, String> parsedInfo = parseInfo(memoryInfo);
            
            info.put("usedMemory", parsedInfo.get("used_memory"));
            info.put("usedMemoryHuman", parsedInfo.get("used_memory_human"));
            info.put("usedMemoryRss", parsedInfo.get("used_memory_rss"));
            info.put("usedMemoryRssHuman", parsedInfo.get("used_memory_rss_human"));
            info.put("totalSystemMemory", parsedInfo.get("total_system_memory"));
            info.put("totalSystemMemoryHuman", parsedInfo.get("total_system_memory_human"));
            info.put("maxMemory", parsedInfo.get("maxmemory"));
            info.put("maxMemoryHuman", parsedInfo.get("maxmemory_human"));
            info.put("memFragmentationRatio", parsedInfo.get("mem_fragmentation_ratio"));
        } catch (Exception e) {
            log.warn("获取Redis内存信息失败", e);
            info.put("error", "获取内存信息失败: " + e.getMessage());
        }
        return info;
    }

    @Override
    public Map<String, Object> getStatsInfo() {
        Map<String, Object> info = new HashMap<>();
        try {
            // 使用Redis INFO命令获取统计信息
            String statsInfo = executeInfoCommand("stats");
            Map<String, String> parsedInfo = parseInfo(statsInfo);
            
            info.put("totalConnectionsReceived", parsedInfo.get("total_connections_received"));
            info.put("totalCommandsProcessed", parsedInfo.get("total_commands_processed"));
            info.put("instantaneousOpsPerSec", parsedInfo.get("instantaneous_ops_per_sec"));
            info.put("instantaneousInputKbps", parsedInfo.get("instantaneous_input_kbps"));
            info.put("instantaneousOutputKbps", parsedInfo.get("instantaneous_output_kbps"));
            info.put("rejectedConnections", parsedInfo.get("rejected_connections"));
            info.put("syncFull", parsedInfo.get("sync_full"));
            info.put("syncPartialOk", parsedInfo.get("sync_partial_ok"));
            info.put("syncPartialErr", parsedInfo.get("sync_partial_err"));
            info.put("expiredKeys", parsedInfo.get("expired_keys"));
            info.put("evictedKeys", parsedInfo.get("evicted_keys"));
            info.put("keyspaceHits", parsedInfo.get("keyspace_hits"));
            info.put("keyspaceMisses", parsedInfo.get("keyspace_misses"));
            info.put("pubsubChannels", parsedInfo.get("pubsub_channels"));
            info.put("pubsubPatterns", parsedInfo.get("pubsub_patterns"));
            
            // 计算命中率
            String hits = parsedInfo.get("keyspace_hits");
            String misses = parsedInfo.get("keyspace_misses");
            if (hits != null && misses != null) {
                try {
                    long h = Long.parseLong(hits);
                    long m = Long.parseLong(misses);
                    long total = h + m;
                    if (total > 0) {
                        double hitRate = (double) h / total * 100;
                        info.put("hitRate", String.format("%.2f%%", hitRate));
                    } else {
                        info.put("hitRate", "0%");
                    }
                } catch (NumberFormatException e) {
                    info.put("hitRate", "N/A");
                }
            }
        } catch (Exception e) {
            log.warn("获取Redis统计信息失败", e);
            info.put("error", "获取统计信息失败: " + e.getMessage());
        }
        return info;
    }
    
    /**
     * 执行Redis INFO命令
     */
    private String executeInfoCommand(String section) {
        // 使用 Redisson 的 NodesGroup API 执行 INFO 命令
        NodesGroup<?> nodesGroup = redissonClient.getNodesGroup();
        Node.InfoSection infoSection;
        switch (section.toLowerCase()) {
            case "server":
                infoSection = Node.InfoSection.SERVER;
                break;
            case "memory":
                infoSection = Node.InfoSection.MEMORY;
                break;
            case "stats":
                infoSection = Node.InfoSection.STATS;
                break;
            default:
                infoSection = Node.InfoSection.ALL;
        }
        Map<String, String> infoMap = nodesGroup.getNodes(NodeType.MASTER).iterator().next().info(infoSection);
        // 将 Map 转换为字符串格式
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : infoMap.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * 解析Redis INFO命令返回的字符串
     */
    private Map<String, String> parseInfo(String info) {
        Map<String, String> result = new HashMap<>();
        if (info == null || info.isEmpty()) {
            return result;
        }
        
        String[] lines = info.split("\\r?\\n");
        for (String line : lines) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex);
                String value = line.substring(colonIndex + 1);
                result.put(key, value);
            }
        }
        return result;
    }
}
