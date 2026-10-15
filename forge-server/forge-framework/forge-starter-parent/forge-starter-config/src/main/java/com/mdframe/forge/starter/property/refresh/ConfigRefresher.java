package com.mdframe.forge.starter.property.refresh;

import com.mdframe.forge.starter.property.DbConfigLoader;
import com.mdframe.forge.starter.property.DbPropertySource;
import com.mdframe.forge.starter.property.event.ConfigRefreshEvent;
import com.mdframe.forge.starter.property.scope.RefreshScopeImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 配置刷新器
 * 负责刷新PropertySource和RefreshScope Bean
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigRefresher {
    
    private final ApplicationContext applicationContext;
    private final ConfigurableEnvironment environment;
    private final RefreshScopeImpl refreshScope;
    private final JdbcTemplate jdbcTemplate;
    
    public synchronized boolean refresh(Map<String, String> newProperties) {
        try {
            Map<String, String> propertiesFromDb = loadPropertiesFromDb();
            newProperties.putAll(propertiesFromDb);
            // 2. 获取旧配置
            DbPropertySource dbPropertySource = getDbPropertySource();
            if (dbPropertySource == null) {
                log.warn("未找到数据库配置源");
                return false;
            }
            // 4. 更新PropertySource
            dbPropertySource.getSource().clear();
            dbPropertySource.getSource().putAll(newProperties);
            // 5. 刷新RefreshScope Bean
            refreshScope.refreshAll();
            return true;
        } catch (Exception e) {
            log.error("配置刷新失败", e);
            return false;
        }
    }
    
    /**
     * 刷新配置
     */
    public synchronized boolean refresh() {
        try {
            // 1. 从数据库重新加载配置
            Map<String, String> newProperties = loadPropertiesFromDb();
            
            // 2. 获取旧配置
            DbPropertySource dbPropertySource = getDbPropertySource();
            if (dbPropertySource == null) {
                log.warn("未找到数据库配置源");
                return false;
            }
            
            Map<String, String> oldProperties = new HashMap<>(dbPropertySource.getSource());
            
            // 3. 检查是否有变化：无变化视为成功的空操作。
            // 不可返回 false，否则 saveCryptoConfig 等调用方会把「已保存但无需刷新」误判为失败。
            if (oldProperties.equals(newProperties)) {
                log.debug("配置无变化，跳过刷新");
                return true;
            }
            
            // 4. 更新PropertySource
            dbPropertySource.getSource().clear();
            dbPropertySource.getSource().putAll(newProperties);
            
            // 5. 刷新RefreshScope Bean
            refreshScope.refreshAll();
            
            // 6. 发布配置变更事件
            //publishRefreshEvent(oldProperties, newProperties);
            
            // 7. 发布Environment变更事件（兼容Spring Cloud）
            publishEnvironmentChangeEvent(oldProperties, newProperties);
            
            log.info("配置刷新成功，变更项数: {}", getChangedKeys(oldProperties, newProperties).size());
            return true;
        } catch (Exception e) {
            log.error("配置刷新失败", e);
            return false;
        }
    }
    
    /**
     * 从数据库加载配置（sys_config 与 sys_config_group 内存合并，分组优先）
     */
    private Map<String, String> loadPropertiesFromDb() {
        return DbConfigLoader.load(jdbcTemplate);
    }
    
    /**
     * 获取数据库配置源
     */
    private DbPropertySource getDbPropertySource() {
        return environment.getPropertySources().stream()
                .filter(ps -> ps instanceof DbPropertySource)
                .map(ps -> (DbPropertySource) ps)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 发布配置刷新事件
     */
    private void publishRefreshEvent(Map<String, String> oldProperties, Map<String, String> newProperties) {
        ConfigRefreshEvent event = new ConfigRefreshEvent(this, oldProperties, newProperties);
        applicationContext.publishEvent(event);
    }
    
    /**
     * 发布Environment变更事件（兼容Spring Cloud）
     */
    private void publishEnvironmentChangeEvent(Map<String, String> oldProperties, Map<String, String> newProperties) {
//        Set<String> changedKeys = getChangedKeys(oldProperties, newProperties);
//        if (!changedKeys.isEmpty()) {
//            EnvironmentChangeEvent event = new EnvironmentChangeEvent(changedKeys);
//            applicationContext.publishEvent(event);
//        }
    }
    
    /**
     * 获取变更的配置键
     */
    private Set<String> getChangedKeys(Map<String, String> oldProperties, Map<String, String> newProperties) {
        Set<String> changed = new HashSet<>();
        
        // 检查新增和修改的配置
        newProperties.forEach((key, newValue) -> {
            String oldValue = oldProperties.get(key);
            if (!newValue.equals(oldValue)) {
                changed.add(key);
            }
        });
        
        // 检查删除的配置
        oldProperties.keySet().forEach(key -> {
            if (!newProperties.containsKey(key)) {
                changed.add(key);
            }
        });
        
        return changed;
    }
}
