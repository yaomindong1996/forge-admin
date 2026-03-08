package com.mdframe.forge.starter.datascope.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数据权限配置属性
 */
@Data
@ConfigurationProperties(prefix = "forge.datascope")
public class DataScopeProperties {
    
    /**
     * 是否启用数据权限控制
     */
    private Boolean enabled = true;
    
    /**
     * 是否打印SQL改写日志
     */
    private Boolean printSql = false;
}
