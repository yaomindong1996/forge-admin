package com.mdframe.forge.starter.outbound.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "forge.outbound")
public class OutboundProperties {

    private Duration connectTimeout = Duration.ofSeconds(3);

    private Duration readTimeout = Duration.ofSeconds(5);

    private Duration writeTimeout = Duration.ofSeconds(5);

    private Duration callTimeout = Duration.ofSeconds(10);

    private long maxRequestBytes = 1024 * 1024;

    private long maxResponseBytes = 2 * 1024 * 1024;

    private boolean redirectsEnabled = false;

    private int maxRedirects = 3;

    /** 每个业务场景允许的最大并发出站请求数。 */
    private int bulkheadMaxConcurrent = 32;

    private boolean bulkheadEnabled = true;

    /** 连续失败达到阈值后暂时打开熔断。 */
    private boolean circuitBreakerEnabled = true;

    private int circuitBreakerFailureThreshold = 5;

    private Duration circuitBreakerOpenDuration = Duration.ofSeconds(30);
}
