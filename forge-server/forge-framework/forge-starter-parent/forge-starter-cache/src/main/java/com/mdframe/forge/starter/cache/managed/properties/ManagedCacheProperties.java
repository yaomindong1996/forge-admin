package com.mdframe.forge.starter.cache.managed.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "forge.cache")
public class ManagedCacheProperties {

    /** 是否启用注解拦截。关闭后业务方法全部穿透。 */
    private boolean annotationEnabled = true;

    /** 应用编码，默认取 spring.application.name。 */
    private String applicationCode;

    /** Redis 控制面和数据对象前缀。 */
    private String namespace = "forge:managed-cache";

    /** Pub/Sub 丢失时重新校准策略快照的最小间隔。 */
    private long policyRefreshSeconds = 30;
}
