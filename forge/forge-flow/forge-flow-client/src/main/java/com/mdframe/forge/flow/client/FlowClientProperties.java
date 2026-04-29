package com.mdframe.forge.flow.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 流程客户端配置属性
 * <p>
 * 在 application.yml 中配置：
 * <pre>
 * forge:
 *   flow:
 *     client:
 *       url: http://flow-service:8080   # 流程服务地址
 *       token: your-service-token       # 服务间鉴权 Token（可选）
 *       connect-timeout: 3000           # 连接超时(ms)，默认 3000
 *       read-timeout: 10000             # 读取超时(ms)，默认 10000
 * </pre>
 *
 * @author forge
 */
@Data
@ConfigurationProperties(prefix = "forge.flow.client")
public class FlowClientProperties {

    /**
     * 流程服务地址
     */
    private String url = "http://localhost:8080";

    /**
     * 服务间鉴权 Token（可选，透传到 Authorization: Bearer <token>）
     */
    private String token;

    /**
     * 连接超时（毫秒）
     */
    private int connectTimeout = 3000;

    /**
     * 读取超时（毫秒）
     */
    private int readTimeout = 10000;

    /**
     * 是否自动订阅 Redis 流程事件频道（需引入 spring-data-redis 且配置了 Redis）
     * 默认 true，设为 false 可关闭自动订阅
     */
    private boolean redisSubscribe = true;
}
