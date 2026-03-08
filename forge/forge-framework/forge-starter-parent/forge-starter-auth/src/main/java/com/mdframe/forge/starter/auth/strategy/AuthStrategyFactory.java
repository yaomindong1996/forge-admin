package com.mdframe.forge.starter.auth.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证策略工厂
 * 负责管理和选择合适的认证策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthStrategyFactory {

    private final List<IAuthStrategy> authStrategies;
    
    /**
     * 策略缓存：key = authType_client, value = strategy
     */
    private final Map<String, IAuthStrategy> strategyCache = new ConcurrentHashMap<>();

    /**
     * 根据认证类型和客户端类型获取认证策略
     *
     * @param authType   认证类型
     * @param userClient 用户客户端类型
     * @return 认证策略
     */
    public IAuthStrategy getStrategy(String authType, String userClient) {
        // 构建缓存key
        String cacheKey = buildCacheKey(authType, userClient);
        
        // 先从缓存获取
        IAuthStrategy cachedStrategy = strategyCache.get(cacheKey);
        if (cachedStrategy != null) {
            return cachedStrategy;
        }

        // 查找匹配的策略
        IAuthStrategy matchedStrategy = findMatchingStrategy(authType, userClient);
        
        if (matchedStrategy == null) {
            log.error("未找到匹配的认证策略: authType={}, userClient={}", authType, userClient);
            throw new RuntimeException("不支持的认证方式: " + authType + 
                    (userClient != null ? " (客户端: " + userClient + ")" : ""));
        }

        // 缓存策略
        strategyCache.put(cacheKey, matchedStrategy);
        
        log.debug("选择认证策略: authType={}, userClient={}, strategy={}", 
                authType, userClient, matchedStrategy.getClass().getSimpleName());
        
        return matchedStrategy;
    }

    /**
     * 查找匹配的策略
     * 优先级：精确匹配（authType + client） > 类型匹配（authType）
     */
    private IAuthStrategy findMatchingStrategy(String authType, String userClient) {
        IAuthStrategy typeOnlyMatch = null;
        
        for (IAuthStrategy strategy : authStrategies) {
            // 认证类型不匹配，跳过
            if (!authType.equals(strategy.getAuthType())) {
                continue;
            }
            
            String supportedClient = strategy.getSupportedClient();
            
            // 策略支持所有客户端（supportedClient为null）
            if (supportedClient == null) {
                typeOnlyMatch = strategy;
                continue;
            }
            
            // 精确匹配：authType和client都匹配
            if (userClient != null && supportedClient.equals(userClient)) {
                return strategy;
            }
        }
        
        // 返回类型匹配的策略（支持所有客户端的策略）
        return typeOnlyMatch;
    }

    /**
     * 构建缓存key
     */
    private String buildCacheKey(String authType, String userClient) {
        return authType + "_" + (userClient != null ? userClient : "default");
    }

    /**
     * 清空策略缓存（用于动态添加策略后刷新）
     */
    public void clearCache() {
        strategyCache.clear();
        log.info("认证策略缓存已清空");
    }

    /**
     * 获取所有已注册的策略
     */
    public List<IAuthStrategy> getAllStrategies() {
        return authStrategies;
    }
}
