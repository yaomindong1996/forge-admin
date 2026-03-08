package com.mdframe.forge.starter.core.context;

import com.mdframe.forge.starter.core.annotation.config.RefreshScope;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 日志配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "forge.log")
@RefreshScope
public class LogProperties {

    /**
     * 是否启用操作日志
     */
    private Boolean enableOperationLog = true;

    /**
     * 是否启用登录日志
     */
    private Boolean enableLoginLog = true;

    /**
     * 请求参数最大长度（超过则截断）
     */
    private Integer requestParamsMaxLength = 2000;

    /**
     * 响应结果最大长度（超过则截断）
     */
    private Integer responseResultMaxLength = 2000;

    /**
     * 排除的URL路径（不记录操作日志）
     */
    private String[] excludePaths = new String[]{
            "/auth/captcha",
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    /**
     * 是否在控制台打印操作日志
     */
    private Boolean printOperationLog = false;

    /**
     * 是否在控制台打印登录日志
     */
    private Boolean printLoginLog = true;

    /**
     * 线程池核心线程数
     */
    private Integer threadPoolCoreSize = 2;

    /**
     * 线程池最大线程数
     */
    private Integer threadPoolMaxSize = 5;

    /**
     * 线程池队列容量
     */
    private Integer threadPoolQueueCapacity = 500;
}
