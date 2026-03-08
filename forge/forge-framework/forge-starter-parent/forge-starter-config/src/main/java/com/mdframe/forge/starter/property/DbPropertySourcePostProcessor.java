package com.mdframe.forge.starter.property;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbPropertySourcePostProcessor implements EnvironmentPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(DbPropertySourcePostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 1. 先加载数据库配置（从默认配置源，如application.properties）
        ConfigDbProperties configDbProperties = new ConfigDbProperties();
        Binder.get(environment).bind("config.datasource", Bindable.ofInstance(configDbProperties));
        if (configDbProperties.getUrl() == null || configDbProperties.getUsername() == null
                || configDbProperties.getPassword() == null) {
            return;
        }
        // 2. 创建数据源
        DataSource dataSource = DataSourceBuilder.create()
                .url(configDbProperties.getUrl())
                .username(configDbProperties.getUsername())
                .password(configDbProperties.getPassword())
                .driverClassName(configDbProperties.getDriverClassName())
                .build();

        // 3. 创建JdbcTemplate并加载属性
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            DbPropertySource dbPropertySource = new DbPropertySource(loadProperties(jdbcTemplate));
            // 4. 将自定义PropertySource添加到环境中（优先级高于默认配置）
            environment.getPropertySources().addFirst(dbPropertySource);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info("数据库配置源已注册");
    }
    
    
    private Map<String, String> loadProperties(JdbcTemplate jdbcTemplate) {
        // 优先尝试从 sys_config 表加载（企业级配置表）
        Map<String, String> result = new HashMap<>();
        try {
            String sql = "SELECT config_key, config_value FROM sys_config WHERE config_type = 'Y'";
            List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
            if (!CollectionUtils.isEmpty(data)) {
                data.forEach(row -> {
                    Object keyObj = row.get("config_key");
                    Object valueObj = row.get("config_value");
                    if (keyObj != null && valueObj != null) {
                        String key = keyObj.toString();
                        String value = valueObj.toString();
                        result.put(key, value);
                        // 同时添加驼峰格式的key，支持双向兼容
                        String camelCaseKey = convertToCamelCase(key);
                        if (!camelCaseKey.equals(key)) {
                            result.put(camelCaseKey, value);
                        }
                    }
                });
                //result.put("forge.log.print-operation-log","true");
            }
            log.info("从 sys_config 表加载了 {} 个配置项（含驼峰格式）", result.size());
        } catch (Exception e) {
            log.warn("从 sys_config 表加载配置失败，尝试从 config_properties 表加载: {}", e.getMessage());
            // 降级方案：尝试从 config_properties 表加载
            try {
                String sql = "SELECT `key`, `value` FROM config_properties WHERE enabled = 1";
                List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
                if (!CollectionUtils.isEmpty(data)) {
                    data.forEach(row -> {
                        Object keyObj = row.get("key");
                        Object valueObj = row.get("value");
                        if (keyObj != null && valueObj != null) {
                            String key = keyObj.toString();
                            String value = valueObj.toString();
                            result.put(key, value);
                            // 同时添加驼峰格式的key
                            String camelCaseKey = convertToCamelCase(key);
                            if (!camelCaseKey.equals(key)) {
                                result.put(camelCaseKey, value);
                            }
                        }
                    });
                }
                log.info("从 config_properties 表加载了 {} 个配置项（含驼峰格式）", result.size());
            } catch (Exception ex) {
                log.error("从数据库加载配置失败", ex);
            }
        }
        return result;
    }
    
    /**
     * 将中划线格式转换为驼峰格式
     * 例如：max-login-attempts -> maxLoginAttempts
     */
    private String convertToCamelCase(String key) {
        if (key == null || !key.contains("-")) {
            return key;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : key.toCharArray()) {
            if (c == '-') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }
}
