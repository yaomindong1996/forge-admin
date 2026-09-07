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

    /**
     * 数据权限控制面元数据所在数据源。
     */
    private String metadataDatasource = "master";

    /**
     * 默认数据权限配置租户。租户未配置时回退到该租户的通用配置。
     */
    private Long defaultConfigTenantId = 1L;

    /**
     * 未配置 mapperMethod 时的处理策略。WARN 用于兼容迁移，DENY 用于严格生产门禁。
     */
    private UnconfiguredPolicy unconfiguredPolicy = UnconfiguredPolicy.WARN;

    /**
     * 已配置数据权限的查询发生上下文或 SQL 改写异常时是否失败关闭。
     */
    private Boolean failClosedOnError = true;

    /** 同一平台的 Admin/Flow/多副本使用相同主题；共用 Redis 的独立环境应配置不同主题。 */
    private String cacheRefreshTopic = "forge:datascope:metadata:refresh";

    public enum UnconfiguredPolicy {
        WARN,
        DENY
    }
}
