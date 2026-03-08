package com.mdframe.forge.starter.property.example;

import com.mdframe.forge.starter.core.annotation.config.RefreshScope;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 配置示例
 *
 * 使用说明：
 * 1. 添加@RefreshScope注解，使配置支持动态刷新
 * 2. 使用@ConfigurationProperties绑定配置前缀
 * 3. 数据库中修改配置后，30秒内自动刷新（或调用刷新接口立即生效）
 */
@Component
@RefreshScope  // 关键：支持配置动态刷新
@ConfigurationProperties(prefix = "app")
@Data
public class AppConfigExample {
    
    /**
     * 应用名称
     * 对应数据库配置键: app.name
     */
    private String name;
    
    /**
     * 应用版本
     * 对应数据库配置键: app.version
     */
    private String version;
    
    /**
     * 功能开关示例
     * 对应数据库配置键: app.featureEnabled
     */
    private Boolean featureEnabled;
}
