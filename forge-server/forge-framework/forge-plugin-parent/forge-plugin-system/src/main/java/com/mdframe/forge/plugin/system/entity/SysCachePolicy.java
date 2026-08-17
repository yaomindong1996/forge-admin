package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.cache.managed.enums.CacheMode;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 受管缓存运行策略覆盖。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_cache_policy")
public class SysCachePolicy extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String applicationCode;

    private String cacheName;

    private Boolean enabled;

    private CacheMode cacheMode;

    private Long localTtlSeconds;

    private Long redisTtlSeconds;

    private Integer localMaxSize;

    private Boolean cacheNull;

    private Long nullTtlSeconds;

    private Long policyVersion;

    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}
