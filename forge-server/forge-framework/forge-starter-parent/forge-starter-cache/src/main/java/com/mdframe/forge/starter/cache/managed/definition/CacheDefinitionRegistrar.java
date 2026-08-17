package com.mdframe.forge.starter.cache.managed.definition;

import com.mdframe.forge.starter.cache.managed.ForgeManagedCacheManager;
import com.mdframe.forge.starter.cache.managed.annotation.ForgeCacheConfig;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * 在单例初始化完成后注册代码声明，使管理端无需等待首次业务调用。
 */
public class CacheDefinitionRegistrar implements SmartInitializingSingleton {

    private final ConfigurableListableBeanFactory beanFactory;
    private final CacheDefinitionResolver definitionResolver;
    private final ForgeManagedCacheManager cacheManager;

    public CacheDefinitionRegistrar(ConfigurableListableBeanFactory beanFactory,
                                    CacheDefinitionResolver definitionResolver,
                                    ForgeManagedCacheManager cacheManager) {
        this.beanFactory = beanFactory;
        this.definitionResolver = definitionResolver;
        this.cacheManager = cacheManager;
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            Class<?> beanType = beanFactory.getType(beanName, false);
            if (beanType == null) {
                continue;
            }
            for (ForgeCacheConfig config : beanType.getAnnotationsByType(ForgeCacheConfig.class)) {
                cacheManager.register(definitionResolver.resolve(beanType, config.name()));
            }
        }
    }
}
