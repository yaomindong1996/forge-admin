package com.mdframe.forge.starter.property.scope;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义Refresh作用域
 * 支持Bean的动态刷新
 */
public class RefreshScopeImpl implements Scope {
    
    private final Map<String, Object> scopedObjects = new ConcurrentHashMap<>();
    private final Map<String, Runnable> destructionCallbacks = new ConcurrentHashMap<>();
    
    @Override
    @NonNull
    public Object get(@NonNull String name, @NonNull ObjectFactory<?> objectFactory) {
        return scopedObjects.computeIfAbsent(name, k -> objectFactory.getObject());
    }
    
    @Override
    public Object remove(@NonNull String name) {
        destructionCallbacks.remove(name);
        return scopedObjects.remove(name);
    }
    
    @Override
    public void registerDestructionCallback(@NonNull String name, @NonNull Runnable callback) {
        destructionCallbacks.put(name, callback);
    }
    
    @Override
    public Object resolveContextualObject(@NonNull String key) {
        return null;
    }
    
    @Override
    public String getConversationId() {
        return "refresh";
    }
    
    /**
     * 刷新所有Bean（清除缓存，下次获取时重新创建）
     */
    public void refreshAll() {
        destructionCallbacks.values().forEach(Runnable::run);
        destructionCallbacks.clear();
        scopedObjects.clear();
    }
    
    /**
     * 刷新指定Bean
     */
    public void refresh(String name) {
        Runnable callback = destructionCallbacks.remove(name);
        if (callback != null) {
            callback.run();
        }
        scopedObjects.remove(name);
    }
}
