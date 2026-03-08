package com.mdframe.forge.starter.property.event;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 配置刷新事件
 */
public class ConfigRefreshEvent extends ApplicationEvent {
    
    private final Map<String, String> oldProperties;
    private final Map<String, String> newProperties;
    
    public ConfigRefreshEvent(Object source, Map<String, String> oldProperties, Map<String, String> newProperties) {
        super(source);
        this.oldProperties = oldProperties;
        this.newProperties = newProperties;
    }
    
    public Map<String, String> getOldProperties() {
        return oldProperties;
    }
    
    public Map<String, String> getNewProperties() {
        return newProperties;
    }
    
    /**
     * 获取变更的配置项
     */
    public Map<String, String> getChangedProperties() {
        Map<String, String> changed = new java.util.HashMap<>();
        newProperties.forEach((key, newValue) -> {
            String oldValue = oldProperties.get(key);
            if (!newValue.equals(oldValue)) {
                changed.put(key, newValue);
            }
        });
        return changed;
    }
}
