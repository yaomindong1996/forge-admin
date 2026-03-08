package com.mdframe.forge.starter.apiconfig.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.mdframe.forge.starter.apiconfig.config.ApiConfigProperties;
import com.mdframe.forge.starter.apiconfig.domain.dto.ApiConfigInfo;
import com.mdframe.forge.starter.apiconfig.domain.entity.SysApiConfig;
import com.mdframe.forge.starter.apiconfig.mapper.SysApiConfigMapper;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.cache.service.ICacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * API配置管理器实现（核心决策引擎 + 缓存管理）
 * 使用两级缓存架构：L1(Caffeine) + L2(Redis)
 */
@Slf4j
@Service
public class ApiConfigManagerImpl implements IApiConfigManager {

    private final SysApiConfigMapper apiConfigMapper;
    private final ApiConfigProperties configProperties;
    private final ICacheService cacheService;
    private final ObjectMapper objectMapper;

    /**
     * Redis Key前缀
     */
    private static final String REDIS_KEY_PREFIX = "api:config:";

    /**
     * L1缓存（Caffeine本地缓存）
     */
    private final Cache<String, ApiConfigInfo> localCache;

    /**
     * 所有启用的配置列表缓存（用于Ant路径匹配）
     */
    private List<ApiConfigInfo> allEnabledConfigsCache;
    
    private static final AntPathMatcher matcher = new AntPathMatcher();
    
    @Autowired
    public ApiConfigManagerImpl(SysApiConfigMapper apiConfigMapper,
                            ApiConfigProperties configProperties,
                            ICacheService cacheService,
                            ObjectMapper objectMapper) {
        this.apiConfigMapper = apiConfigMapper;
        this.configProperties = configProperties;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;

        // 初始化L1缓存
        this.localCache = Caffeine.newBuilder()
                .maximumSize(configProperties.getCache().getLocal().getMaxSize())
                .expireAfterWrite(configProperties.getCache().getLocal().getExpireMinutes(), TimeUnit.MINUTES)
                .recordStats()
                .build();
        List<SysApiConfig> configs = apiConfigMapper.selectAllEnabled();
        if (CollUtil.isNotEmpty(configs) && CollUtil.isEmpty(allEnabledConfigsCache)) {
            allEnabledConfigsCache = configs.stream()
                    .map(ApiConfigInfo::fromEntity)
                    .collect(Collectors.toList());
        }
        log.info("API配置管理器初始化完成，L1缓存最大容量={}, 过期时间={}分钟",
                configProperties.getCache().getLocal().getMaxSize(),
                configProperties.getCache().getLocal().getExpireMinutes());
    }

    @Override
    public ApiConfigInfo getApiConfig(String urlPath, String method) {
        if (urlPath == null || method == null) {
            return null;
        }

        // 使用Ant路径匹配查找最匹配的配置
        ApiConfigInfo config = findBestMatchConfig(urlPath, method);
        if (config != null) {
            log.debug("API配置匹配成功: {} {} -> {}", method, urlPath, config.getUrlPath());
            return config;
        }

        log.debug("API配置未找到: {} {}", urlPath);
        return null;
    }

    /**
     * 使用Ant路径匹配查找最匹配的配置
     *
     * @param urlPath 请求路径（可能包含路径参数，如 /generator/{tableId}）
     * @param method 请求方法
     * @return 最匹配的配置
     */
    private ApiConfigInfo findBestMatchConfig(String urlPath, String method) {
        // 遍历所有启用的配置，使用Ant路径匹配
        ApiConfigInfo bestMatch = null;
        int bestScore = -1;

        for (ApiConfigInfo config : allEnabledConfigsCache) {
            // 检查请求方法是否匹配
            if (!method.equalsIgnoreCase(config.getReqMethod())) {
                continue;
            }
            // 使用Ant路径匹配
            if (matcher.match(config.getUrlPath(), urlPath)) {
                // 计算匹配分数：路径越精确，分数越高
                int score = calculateMatchScore(urlPath, config.getUrlPath());
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = config;
                }
            }
        }

        return bestMatch;
    }

    /**
     * 计算路径匹配分数
     *
     * @param requestPath 请求路径
     * @param configPath 配置中的路径（可能包含通配符）
     * @return 匹配分数（0-100，越高越精确）
     */
    private int calculateMatchScore(String requestPath, String configPath) {
        // 精确匹配：完全相等
        if (requestPath.equals(configPath)) {
            return 100;
        }

        // 通配符匹配：配置路径包含通配符
        if (configPath.contains("*") || configPath.contains("?") || configPath.contains("**")) {
            // 计算路径段数
            String[] requestParts = requestPath.split("/");
            String[] configParts = configPath.split("/");
            int matchCount = 0;
            for (String part : requestParts) {
                for (String configPart : configParts) {
                    if (configPart.equals(part) || configPart.equals("*")) {
                        matchCount++;
                        break;
                    }
                }
            }
            // 根据匹配段数计算分数
            return matchCount * 20;
        }

        // 前缀匹配：请求路径以配置路径开头
        if (requestPath.startsWith(configPath)) {
            return 50;
        }

        return 0;
    }

    @Override
    public ApiConfigInfo getApiConfigByCode(String apiCode) {
        if (apiCode == null) {
            return null;
        }

        // 从数据库查询
        SysApiConfig entity = apiConfigMapper.selectByApiCode(apiCode);
        if (entity != null) {
            return ApiConfigInfo.fromEntity(entity);
        }
        return null;
    }

    @Override
    public void refreshApiConfig(String urlPath, String method) {
        String cacheKey = buildCacheKey(urlPath, method);
        // 清除L1缓存
        localCache.invalidate(cacheKey);
        // 清除L2缓存
        deleteFromRedis(cacheKey);
        log.info("刷新API配置缓存: {}", cacheKey);
    }

    @Override
    public void refreshApiConfigById(Long configId) {
        SysApiConfig config = apiConfigMapper.selectById(configId);
        if (config != null) {
            // 根据配置ID刷新缓存
            String cacheKey = buildCacheKey(config.getUrlPath(), config.getReqMethod());
            // 清除L1缓存
            localCache.invalidate(cacheKey);
            // 清除L2缓存
            deleteFromRedis(cacheKey);
            log.info("刷新API配置缓存: id={}, {} {}", configId, cacheKey);
        }
    }

    @Override
    public void refreshAllApiConfig() {
        // 清除L1缓存
        localCache.invalidateAll();
        // 清除L2缓存
        clearAllFromRedis();
        log.info("刷新所有API配置缓存");
    }

    @Override
    public void refreshApiConfigByModule(String moduleCode) {
        List<SysApiConfig> configs = apiConfigMapper.selectByModuleCode(moduleCode);
        for (SysApiConfig config : configs) {
            refreshApiConfig(config.getUrlPath(), config.getReqMethod());
        }
        log.info("刷新模块[{}]的API配置缓存，共{}条", moduleCode, configs.size());
    }

    @Override
    public boolean needAuth(String urlPath, String method) {
        ApiConfigInfo config = getApiConfig(urlPath, method);
        return config != null && config.getNeedAuth();
    }

    @Override
    public boolean needEncrypt(String urlPath, String method) {
        ApiConfigInfo config = getApiConfig(urlPath, method);
        return config != null && config.getNeedEncrypt();
    }

    @Override
    public boolean needTenant(String urlPath, String method) {
        ApiConfigInfo config = getApiConfig(urlPath, method);
        return config != null && config.getNeedTenant();
    }

    @Override
    public boolean needLimit(String urlPath, String method) {
        ApiConfigInfo config = getApiConfig(urlPath, method);
        return config != null && config.getNeedLimit();
    }

    @Override
    public List<ApiConfigInfo> getAllEnabledConfigs() {
        List<SysApiConfig> entities = apiConfigMapper.selectAllEnabled();
        return entities.stream()
                .map(ApiConfigInfo::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void warmUpCache() {
        log.info("开始预热API配置缓存...");
        long startTime = System.currentTimeMillis();

        List<SysApiConfig> configs = apiConfigMapper.selectAllEnabled();
        for (SysApiConfig entity : configs) {
            ApiConfigInfo config = ApiConfigInfo.fromEntity(entity);
            String cacheKey = buildCacheKey(entity.getUrlPath(), entity.getReqMethod());
            // 写入L2缓存
            putToRedis(cacheKey, config);
            // 写入L1缓存
            localCache.put(cacheKey, config);
        }
        if (CollUtil.isEmpty(allEnabledConfigsCache)) {
            allEnabledConfigsCache = configs.stream()
                    .map(ApiConfigInfo::fromEntity)
                    .collect(Collectors.toList());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("API配置缓存预热完成，共{}条配置，耗时{}ms", configs.size(), elapsed);
    }

    @Override
    public void clearAllCache() {
        localCache.invalidateAll();
        clearAllFromRedis();
        log.info("清空所有API配置缓存");
    }

    @Override
    public String getCacheStats() {
        CacheStats stats = localCache.stats();
        return String.format(
                "L1缓存统计 - 命中率: %.2f%%, 命中次数: %d, 未命中次数: %d, 加载次数: %d, 驱逐次数: %d",
                stats.hitRate() * 100,
                stats.hitCount(),
                stats.missCount(),
                stats.loadCount(),
                stats.evictionCount()
        );
    }

    /**
     * 构建缓存Key
     */
    private String buildCacheKey(String urlPath, String method) {
        return urlPath + ":" + method;
    }

    /**
     * 从Redis获取配置
     */
    private ApiConfigInfo getFromRedis(String key) {
        try {
            String redisKey = REDIS_KEY_PREFIX + key;
            Object value = cacheService.get(redisKey);
            if (value instanceof ApiConfigInfo) {
                return (ApiConfigInfo) value;
            } else if (value instanceof String) {
                return objectMapper.readValue((String) value, ApiConfigInfo.class);
            }
            return null;
        } catch (Exception e) {
            log.warn("从Redis获取API配置失败: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 写入Redis缓存
     */
    private void putToRedis(String key, ApiConfigInfo config) {
        try {
            String redisKey = REDIS_KEY_PREFIX + key;
            long expireSeconds = configProperties.getCache().getRedis().getExpireSeconds();
            cacheService.set(redisKey, config, expireSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入Redis缓存失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 从Redis删除配置
     */
    private void deleteFromRedis(String key) {
        try {
            String redisKey = REDIS_KEY_PREFIX + key;
            cacheService.delete(redisKey);
        } catch (Exception e) {
            log.warn("删除Redis缓存失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 清空Redis所有API配置缓存
     */
    private void clearAllFromRedis() {
        try {
            cacheService.delete(cacheService.keys(REDIS_KEY_PREFIX + "*"));
        } catch (Exception e) {
            log.warn("清空Redis缓存失败: error={}", e.getMessage());
        }
    }
}
