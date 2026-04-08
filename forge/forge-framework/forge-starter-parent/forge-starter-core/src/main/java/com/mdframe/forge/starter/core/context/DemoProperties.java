package com.mdframe.forge.starter.core.context;

import com.mdframe.forge.starter.core.annotation.config.RefreshScope;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 演示环境配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "forge.demo")
@RefreshScope
public class DemoProperties {
    
    /**
     * 是否启用演示模式
     */
    private Boolean enabled = false;
    
    /**
     * 演示环境提示消息
     */
    private String message = "演示环境禁止修改操作";
    
    /**
     * 白名单URL列表（支持通配符）
     */
    private List<String> whitelistUrls = new ArrayList<>();
    
    /**
     * 拦截的HTTP方法
     */
    private List<String> blockedMethods = Arrays.asList(
        "POST", "PUT", "DELETE", "PATCH"
    );
}