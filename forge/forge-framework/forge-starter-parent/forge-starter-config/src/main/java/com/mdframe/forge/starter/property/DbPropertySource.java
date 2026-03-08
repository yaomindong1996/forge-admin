package com.mdframe.forge.starter.property;

import org.springframework.core.env.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class DbPropertySource extends PropertySource<Map<String, String>> {
    private static final String NAME = "dbPropertySource";
    
    public DbPropertySource(Map<String, String> source) {
        super(NAME, source);
    }

    @Override
    public Object getProperty(String name) {
        return source.get(name);
    }
    
    @Override
    public boolean containsProperty(String name) {
        return this.source.containsKey(name);
    }
    
    /**
     * 获取可变的source，用于配置刷新
     */
    public Map<String, String> getSource() {
        return this.source;
    }
}
