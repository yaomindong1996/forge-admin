package com.mdframe.forge.plugin.system.dto;

import com.mdframe.forge.starter.cache.managed.enums.CacheMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 受管缓存策略编辑参数。缓存名和允许模式仍由代码定义约束。
 */
@Data
public class SysCachePolicyEditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "应用编码不能为空")
    @Size(max = 64, message = "应用编码不能超过64个字符")
    private String applicationCode;

    @NotBlank(message = "缓存名不能为空")
    @Size(max = 128, message = "缓存名不能超过128个字符")
    private String cacheName;

    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    @NotNull(message = "缓存模式不能为空")
    private CacheMode cacheMode;

    @NotNull(message = "本地TTL不能为空")
    @Positive(message = "本地TTL必须大于0")
    private Long localTtlSeconds;

    @NotNull(message = "Redis TTL不能为空")
    @Positive(message = "Redis TTL必须大于0")
    private Long redisTtlSeconds;

    @NotNull(message = "本地容量不能为空")
    @Positive(message = "本地容量必须大于0")
    private Integer localMaxSize;

    @NotNull(message = "空值缓存配置不能为空")
    private Boolean cacheNull;

    @NotNull(message = "空值TTL不能为空")
    @Positive(message = "空值TTL必须大于0")
    private Long nullTtlSeconds;

    private Long policyVersion;
}
