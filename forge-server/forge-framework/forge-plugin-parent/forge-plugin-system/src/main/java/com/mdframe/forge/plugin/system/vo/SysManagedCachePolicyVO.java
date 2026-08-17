package com.mdframe.forge.plugin.system.vo;

import com.mdframe.forge.starter.cache.managed.enums.CacheMode;
import com.mdframe.forge.starter.cache.managed.enums.CacheScope;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 受管缓存定义和当前有效策略。禁止增加 entry key 或 value 字段。
 */
@Data
public class SysManagedCachePolicyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String applicationCode;

    private String cacheName;

    private String description;

    private String source;

    private CacheScope scope;

    private List<CacheMode> allowedModes;

    private Boolean enabled;

    private CacheMode cacheMode;

    private Long localTtlSeconds;

    private Long redisTtlSeconds;

    private Integer localMaxSize;

    private Boolean cacheNull;

    private Long nullTtlSeconds;

    private Long policyVersion;

    private Boolean overridden;

    private Long hitCount;

    private Long missCount;

    private Long putCount;

    private Long evictionCount;

    private Long failureCount;
}
